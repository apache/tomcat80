/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.scan;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.buf.UriUtil;
import org.apache.tomcat.util.res.StringManager;

/**
 * The default {@link JarScanner} implementation scans the WEB-INF/lib directory
 * followed by the provided classloader and then works up the classloader
 * hierarchy. This implementation is sufficient to meet the requirements of the
 * Servlet 3.0 specification as well as to provide a number of Tomcat specific
 * extensions. The extensions are:
 * <ul>
 *   <li>Scanning the classloader hierarchy (enabled by default)</li>
 *   <li>Testing all files to see if they are JARs (disabled by default)</li>
 *   <li>Testing all directories to see if they are exploded JARs
 *       (disabled by default)</li>
 * </ul>
 * All of the extensions may be controlled via configuration.
 */
public class StandardJarScanner implements JarScanner {

    private static final Log log = LogFactory.getLog(StandardJarScanner.class);

    /**
     * The string resources for this package.
     */
    private static final StringManager sm =
        StringManager.getManager(Constants.Package);

    /**
     * Controls the classpath scanning extension.
     */
    private boolean scanClassPath = true;
    public boolean isScanClassPath() {
        return scanClassPath;
    }
    public void setScanClassPath(boolean scanClassPath) {
        this.scanClassPath = scanClassPath;
    }

    /**
     * Controls the testing all files to see of they are JAR files extension.
     */
    private boolean scanAllFiles = false;
    public boolean isScanAllFiles() {
        return scanAllFiles;
    }
    public void setScanAllFiles(boolean scanAllFiles) {
        this.scanAllFiles = scanAllFiles;
    }

    /**
     * Controls the testing all directories to see of they are exploded JAR
     * files extension.
     */
    private boolean scanAllDirectories = false;
    public boolean isScanAllDirectories() {
        return scanAllDirectories;
    }
    public void setScanAllDirectories(boolean scanAllDirectories) {
        this.scanAllDirectories = scanAllDirectories;
    }

    /**
     * Controls the testing of the bootstrap classpath which consists of the
     * runtime classes provided by the JVM and any installed system extensions.
     */
    private boolean scanBootstrapClassPath = false;
    public boolean isScanBootstrapClassPath() {
        return scanBootstrapClassPath;
    }
    public void setScanBootstrapClassPath(boolean scanBootstrapClassPath) {
        this.scanBootstrapClassPath = scanBootstrapClassPath;
    }

    /**
     * Controls the filtering of the results from the scan for JARs
     */
    private JarScanFilter jarScanFilter = new StandardJarScanFilter();
    @Override
    public JarScanFilter getJarScanFilter() {
        return jarScanFilter;
    }
    @Override
    public void setJarScanFilter(JarScanFilter jarScanFilter) {
        this.jarScanFilter = jarScanFilter;
    }

    /**
     * Tests is the file is a zip file
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static boolean isZipFile(File file) throws IOException {
        if (file.isDirectory()) {
            return false;
        }
        if (!file.canRead()) {
            throw new IOException("Cannot read file " + file.getAbsolutePath());
        }
        if (file.length() < 4) {
            return false;
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            return in.readInt() == 0x504b0304;
        }
    }

    /**
     * Returns the URLS of all jars managed directly or indirectly (given
     * "Class-Path" manifest relationships) by the class loader
     *
     * @param cl
     * @return
     */
    public static URL[] getURLs(URLClassLoader cl) {
        HashSet<URL> visited = new HashSet<>();
        URL[] urLs = cl.getURLs();
        if (urLs != null) {
            for (URL urL : urLs) {
                try {
                    visitJar(visited, urL);
                } catch (IOException iOException) {
                    throw new RuntimeException(iOException);
                }
            }
        }
        URL[] ret = new URL[visited.size()];
        int i = 0;
        for (URL url : visited) {
            ret[i++] = url;
        }
        return ret;
    }

    /**
     * Performs a DFS traversal over the graph of jars given the "Class-Path"
     * reference
     *
     * @param visited
     * @param jar
     * @throws IOException
     */
    public static void visitJar(Set<URL> visited, URL jar) throws IOException {
        File file = new File(jar.getFile());
        if (!file.exists()) {
            return;
        }
        visited.add(jar);

        if (!isZipFile(file)) {
            return;
        }

        try (ZipFile zipFile = new ZipFile(file)) {
            ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
            if (manifestEntry == null) {
                return;
            }
            Manifest manifest = new Manifest(zipFile.getInputStream(manifestEntry));
            String classPath = manifest.getMainAttributes().getValue("Class-Path");
            if (classPath == null) {
                return;
            }
            String[] entries = classPath.split(" ");
            for (String entry : entries) {
                File f = new File(entry);
                if (!f.isAbsolute()) {
                    f = new File(file.getParentFile(), entry);
                }
                URL u = f.toURI().toURL();
                if (!visited.contains(u)) {
                    visitJar(visited, u);
                }
            }
        }
    }

    /**
     * Scan the provided ServletContext and class loader for JAR files. Each JAR
     * file found will be passed to the callback handler to be processed.
     *
     * @param scanType      The type of JAR scan to perform. This is passed to
     *                          the filter which uses it to determine how to
     *                          filter the results
     * @param context       The ServletContext - used to locate and access
     *                      WEB-INF/lib
     * @param callback      The handler to process any JARs found
     */
    @Override
    public void scan(JarScanType scanType, ServletContext context,
            JarScannerCallback callback) {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("jarScan.webinflibStart"));
        }

        Set<URL> processedURLs = new HashSet<>();

        // Scan WEB-INF/lib
        Set<String> dirList = context.getResourcePaths(Constants.WEB_INF_LIB);
        if (dirList != null) {
            Iterator<String> it = dirList.iterator();
            while (it.hasNext()) {
                String path = it.next();
                if (path.endsWith(Constants.JAR_EXT) &&
                        getJarScanFilter().check(scanType,
                                path.substring(path.lastIndexOf('/')+1))) {
                    // Need to scan this JAR
                    if (log.isDebugEnabled()) {
                        log.debug(sm.getString("jarScan.webinflibJarScan", path));
                    }
                    URL url = null;
                    try {
                        url = context.getResource(path);
                        processedURLs.add(url);
                        process(scanType, callback, url, path, true);
                    } catch (IOException e) {
                        log.warn(sm.getString("jarScan.webinflibFail", url), e);
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace(sm.getString("jarScan.webinflibJarNoScan", path));
                    }
                }
            }
        }

        // Scan WEB-INF/classes
        if (isScanAllDirectories()) {
            try {
                URL url = context.getResource("/WEB-INF/classes/META-INF");
                if (url != null) {
                    // Class path scanning will look at WEB-INF/classes since
                    // that is the URL that Tomcat's web application class
                    // loader returns. Therefore, it is this URL that needs to
                    // be added to the set of processed URLs.
                    URL webInfURL = context.getResource("/WEB-INF/classes");
                    if (webInfURL != null) {
                        processedURLs.add(webInfURL);
                    }
                    try {
                        callback.scanWebInfClasses();
                    } catch (IOException e) {
                        log.warn(sm.getString("jarScan.webinfclassesFail"), e);
                    }
                }
            } catch (MalformedURLException e) {
                // Ignore
            }
        }

        // Scan the classpath
        if (isScanClassPath()) {
            if (log.isTraceEnabled()) {
                log.trace(sm.getString("jarScan.classloaderStart"));
            }

            ClassLoader stopLoader = null;
            if (!isScanBootstrapClassPath()) {
                // Stop when we reach the bootstrap class loader
                stopLoader = ClassLoader.getSystemClassLoader().getParent();
            }

            ClassLoader classLoader = context.getClassLoader();

            // JARs are treated as application provided until the common class
            // loader is reached.
            boolean isWebapp = true;

            while (classLoader != null && classLoader != stopLoader) {
                if (classLoader instanceof URLClassLoader) {
                    if (isWebapp) {
                        isWebapp = isWebappClassLoader(classLoader);
                    }
                    URL[] urls = getURLs((URLClassLoader) classLoader);
                    for (int i=0; i<urls.length; i++) {
                        if (processedURLs.contains(urls[i])) {
                            // Skip this URL it has already been processed
                            continue;
                        }

                        ClassPathEntry cpe = new ClassPathEntry(urls[i]);

                        // JARs are scanned unless the filter says not to.
                        // Directories are scanned for pluggability scans or
                        // if scanAllDirectories is enabled unless the
                        // filter says not to.
                        if ((cpe.isJar() ||
                                scanType == JarScanType.PLUGGABILITY ||
                                isScanAllDirectories()) &&
                                        getJarScanFilter().check(scanType,
                                        cpe.getName())) {
                            if (log.isDebugEnabled()) {
                                log.debug(sm.getString(
                                        "jarScan.classloaderJarScan", urls[i]));
                            }
                            try {
                                process(scanType, callback, urls[i], null, isWebapp);
                            } catch (IOException ioe) {
                                log.warn(sm.getString(
                                        "jarScan.classloaderFail", urls[i]),
                                        ioe);
                            }
                        } else {
                            // JAR / directory has been skipped
                            if (log.isTraceEnabled()) {
                                log.trace(sm.getString(
                                        "jarScan.classloaderJarNoScan",
                                        urls[i]));
                            }
                        }
                    }
                }
                classLoader = classLoader.getParent();
            }
        }
    }


    /*
     * Since class loader hierarchies can get complicated, this method attempts
     * to apply the following rule: A class loader is a web application class
     * loader unless it loaded this class (StandardJarScanner) or is a parent
     * of the class loader that loaded this class.
     *
     * This should mean:
     *   the webapp class loader is an application class loader
     *   the shared class loader is an application class loader
     *   the server class loader is not an application class loader
     *   the common class loader is not an application class loader
     *   the system class loader is not an application class loader
     *   the bootstrap class loader is not an application class loader
     */
    private boolean isWebappClassLoader(ClassLoader classLoader) {
        ClassLoader nonWebappLoader = StandardJarScanner.class.getClassLoader();

        while (nonWebappLoader != null) {
            if (nonWebappLoader == classLoader) {
                return false;
            }
            nonWebappLoader = nonWebappLoader.getParent();
        }
        return true;
    }


    /*
     * Scan a URL for JARs with the optional extensions to look at all files
     * and all directories.
     */
    private void process(JarScanType scanType, JarScannerCallback callback,
            URL url, String webappPath, boolean isWebapp) throws IOException {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("jarScan.jarUrlStart", url));
        }

        URLConnection conn = url.openConnection();
        if (conn instanceof JarURLConnection) {
            callback.scan((JarURLConnection) conn, webappPath, isWebapp);
        } else {
            String urlStr = url.toString();
            if (urlStr.startsWith("file:") || urlStr.startsWith("http:") || urlStr.startsWith("https:")) {
                if (urlStr.endsWith(Constants.JAR_EXT)) {
                    URL jarURL = UriUtil.buildJarUrl(urlStr);
                    callback.scan((JarURLConnection) jarURL.openConnection(),
                            webappPath, isWebapp);
                } else {
                    File f;
                    try {
                        f = new File(url.toURI());
                        if (f.isFile() && isScanAllFiles()) {
                            // Treat this file as a JAR
                            URL jarURL = UriUtil.buildJarUrl(f);
                            callback.scan(
                                    (JarURLConnection) jarURL.openConnection(),
                                    webappPath, isWebapp);
                        } else if (f.isDirectory()) {
                            if (scanType == JarScanType.PLUGGABILITY) {
                                callback.scan(f, webappPath, isWebapp);
                            } else {
                                File metainf = new File(f.getAbsoluteFile() +
                                        File.separator + "META-INF");
                                if (metainf.isDirectory()) {
                                    callback.scan(f, webappPath, isWebapp);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        ExceptionUtils.handleThrowable(t);
                        // Wrap the exception and re-throw
                        IOException ioe = new IOException();
                        ioe.initCause(t);
                        throw ioe;
                    }
                }
            }
        }

    }


    private static class ClassPathEntry {

        private final boolean jar;
        private final String name;

        public ClassPathEntry(URL url) {
            String path = url.getPath();
            int end = path.indexOf(Constants.JAR_EXT);
            if (end != -1) {
                jar = true;
                int start = path.lastIndexOf('/', end);
                name = path.substring(start + 1, end + 4);
            } else {
                jar = false;
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                int start = path.lastIndexOf('/');
                name = path.substring(start + 1);
            }

        }

        public boolean isJar() {
            return jar;
        }

        public String getName() {
            return name;
        }
    }
}
