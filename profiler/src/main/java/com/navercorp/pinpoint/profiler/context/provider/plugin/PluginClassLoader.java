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
        if(dynamicParent != null) {
            Class<?> cls =  dynamicParent.loadClass(name);
            if(cls != null) {
                return cls;
            }
        }
        return super.loadClass(name);
    }

    public ClassLoader getDynamicParent() {
        return dynamicParent;
    }

    public void setDynamicParent(ClassLoader dynamicParent) {
        this.dynamicParent = dynamicParent;
    }
}
