package com.navercorp.pinpoint.profiler.context.provider.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PluginClassLoader extends URLClassLoader {

    private Set<ClassLoader> dynamicParents = new HashSet<>();

    private Set<String> current = Collections.synchronizedSet(new HashSet<>());

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public PluginClassLoader(URL[] urls) {
        super(urls);
    }

    public PluginClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        synchronized (getClassLoadingLock(name)) {
            if (current.contains(name)) {
                throw new ClassNotFoundException(name);
            }
            if(name.equals("org.springframework.web.context.request.WebRequest")) {
                System.out.println();
            }
            try {
                current.add(name);
                Class<?> cls = super.loadClass(name);
                if (cls != null) {
                    return cls;
                }
            } catch (ClassNotFoundException e) {
                if (!dynamicParents.isEmpty()) {
                    for (ClassLoader dynamicParent : dynamicParents) {
                        try {
                            Class<?> cls = dynamicParent.loadClass(name);
                            if (cls != null) {
                                return cls;
                            }
                        } catch (ClassNotFoundException cnf) {
                        }
                    }
                }
                throw e;
            } finally {
                current.remove(name);
            }
            throw new ClassNotFoundException(name);
        }

    }

    @Override
    public URL getResource(String name) {
        Objects.requireNonNull(name);
        URL url = super.getResource(name);
        if (url != null) {
            return url;
        }
        if (!dynamicParents.isEmpty()) {
            for (ClassLoader dynamicParent : dynamicParents) {
                url = dynamicParent.getResource(name);
                if (url != null) {
                    return url;
                }
            }
        }
        return url;
    }

    public void setDynamicClassLoader(ClassLoader classLoader) {
        if (classLoader != null)
            dynamicParents.add(classLoader);
    }

}
