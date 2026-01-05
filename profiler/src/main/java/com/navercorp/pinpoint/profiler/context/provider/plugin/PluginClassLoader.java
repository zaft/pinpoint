package com.navercorp.pinpoint.profiler.context.provider.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

public class PluginClassLoader extends URLClassLoader {

    private ClassLoader dynamicParent;


    public PluginClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public PluginClassLoader(URL[] urls) {
        super(urls);
    }

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        synchronized (getClassLoadingLock(name)) {
            try {
                Class<?> cls = super.loadClass(name);
                if (cls != null) {
                    return cls;
                }
            } catch (ClassNotFoundException e) {
                if (dynamicParent != null) {
                    return dynamicParent.loadClass(name);
                }
                throw e;
            }
            throw new ClassNotFoundException(name);
        }
    }

    public ClassLoader getDynamicParent() {
        return dynamicParent;
    }

    public void setDynamicParent(ClassLoader dynamicParent) {
        this.dynamicParent = dynamicParent;
    }
}
