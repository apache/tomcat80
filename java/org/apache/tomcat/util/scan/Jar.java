/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.util.scan;

/**
 * Provides an abstraction for use by the various classes that need to scan
 * JARs. The classes provided by the JRE for accessing JARs ({@link java.util.jar.JarFile} and
 * {@link java.util.jar.JarInputStream}) have significantly different performance
 * characteristics depending on the form of the URL used to access the JAR.
 * For file based JAR {@link java.net.URL}s, {@link java.util.jar.JarFile} is faster but for non-file
 * based {@link java.net.URL}s, {@link java.util.jar.JarFile} creates a copy of the JAR in the
 * temporary directory so {@link java.util.jar.JarInputStream} is faster.
 *
 * @deprecated Use {@link org.apache.tomcat.Jar} instead.
 *             This class will be removed from Tomcat 8.5.x onwards.
 */
@Deprecated
public interface Jar extends org.apache.tomcat.Jar {
}
