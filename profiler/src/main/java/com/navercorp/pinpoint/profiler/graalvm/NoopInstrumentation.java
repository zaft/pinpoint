package com.navercorp.pinpoint.profiler.graalvm;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;

/**
 * Build-time placeholder. Never invoked at runtime.
 * Module-related methods (redefineModule / isModifiableModule, JDK 9+) are
 * intentionally not implemented — bootstrap-core targets JDK 8.
 */
public class NoopInstrumentation implements Instrumentation {

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        return false;
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return false;
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
    }

    @Override
    public boolean isRedefineClassesSupported() {
        return false;
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions)
            throws ClassNotFoundException, UnmodifiableClassException {
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        return false;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getAllLoadedClasses() {
        return new Class<?>[0];
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getInitiatedClasses(ClassLoader loader) {
        return new Class<?>[0];
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        return 0L;
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        return false;
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
    }
}
