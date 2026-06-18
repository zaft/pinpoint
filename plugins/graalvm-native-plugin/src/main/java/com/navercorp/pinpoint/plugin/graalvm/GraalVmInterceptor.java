package com.navercorp.pinpoint.plugin.graalvm;

import com.navercorp.pinpoint.bootstrap.interceptor.ReturnThrowAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.plugin.graalvm.DynamicClassLoader;

public class GraalVmInterceptor implements ReturnThrowAroundInterceptor<Class<?>> {

    private ClassLoader dynamicClassLoader;

    public GraalVmInterceptor(ClassLoader dynamicClassLoader) {
        this.dynamicClassLoader = dynamicClassLoader;
    }

    @Override
    public Class<?> before(Object target, Object[] args) {
        return null;
    }

    @Override
    public Class<?> after(Object target, Object[] args, Object result, Throwable throwable) throws Throwable {
        if (throwable == null) {
            return (Class<?>) result;
        } else {
            if (throwable instanceof ClassNotFoundException) {
                return dynamicClassLoader.loadClass((String) args[0]);
            }
        }
        throw throwable;
    }
}
