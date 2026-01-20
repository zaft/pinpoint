package com.navercorp.pinpoint.profiler.context.provider.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PluginClassLoader extends URLClassLoader {

    private final Set<ClassLoader> dynamicClassLoaders = new HashSet<>();

    private final Set<String> loop = new HashSet<>();


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
            if(loop.contains(name)) {
                throw new ClassNotFoundException(name);
            }
            loop.add(name);
            try {
                Class<?> cls = super.loadClass(name);
                if (cls != null) {
                    return cls;
                }
                throw new ClassNotFoundException(name);
            } catch (ClassNotFoundException e) {
                if (!dynamicClassLoaders.isEmpty()) {
                    for (ClassLoader dynamicClassLoader : dynamicClassLoaders) {
                        try {
                           return dynamicClassLoader.loadClass(name);
                        } catch (ClassNotFoundException ignored) {

                        }
                    }
                }
                throw e;
            } finally {
                loop.remove(name);
            }
        }
    }

//    public ClassLoader getDynamicClassLoader() {
//        return dynamicClassLoader;
//    }

    public void setDynamicClassLoader(ClassLoader dynamicClassLoader) {
        if(dynamicClassLoader == null) {
            return;
        }
        this.dynamicClassLoaders.add(dynamicClassLoader);
    }
}
