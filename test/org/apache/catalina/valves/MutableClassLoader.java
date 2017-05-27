package org.apache.catalina.valves;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

public class MutableClassLoader extends ClassLoader {

	private URLClassLoader delegate;
	private List<URL> urls = new ArrayList<URL>();

	public MutableClassLoader(URLClassLoader original) {
		this.urls.addAll(Arrays.asList(original.getURLs()));
		createDelegate();
	}

	private void createDelegate() {
		delegate = new URLClassLoader(urls.toArray(new URL[urls.size()]));
	}

	public void add(URL url) {
		urls.add(url);
		createDelegate();
	}

	public void remove(URL url) {
		urls.remove(url);
		createDelegate();
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public InputStream getResourceAsStream(String name) {
		return delegate.getResourceAsStream(name);
	}

	public String toString() {
		return delegate.toString();
	}

	public void close() throws IOException {
		delegate.close();
	}

	public URL[] getURLs() {
		return delegate.getURLs();
	}

	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return delegate.loadClass(name);
	}

	public URL findResource(String name) {
		return delegate.findResource(name);
	}

	public Enumeration<URL> findResources(String name) throws IOException {
		return delegate.findResources(name);
	}

	public URL getResource(String name) {
		return delegate.getResource(name);
	}

	public Enumeration<URL> getResources(String name) throws IOException {
		return delegate.getResources(name);
	}

	public void setDefaultAssertionStatus(boolean enabled) {
		delegate.setDefaultAssertionStatus(enabled);
	}

	public void setPackageAssertionStatus(String packageName, boolean enabled) {
		delegate.setPackageAssertionStatus(packageName, enabled);
	}

	public void setClassAssertionStatus(String className, boolean enabled) {
		delegate.setClassAssertionStatus(className, enabled);
	}

	public void clearAssertionStatus() {
		delegate.clearAssertionStatus();
	}

}
