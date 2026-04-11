package com.navercorp.pinpoint.plugin.graalvm;

import com.navercorp.pinpoint.bootstrap.interceptor.ReturnThrowAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.ReturnThrowBeforeInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.annotation.IgnoreMethod;
import com.navercorp.pinpoint.bootstrap.plugin.graalvm.DynamicClassLoader;

public class GraalVmInterceptor2 implements ReturnThrowBeforeInterceptor<Class<?>> {

    private ClassLoader dynamicClassLoader;

    public GraalVmInterceptor2(ClassLoader dynamicClassLoader) {
        this.dynamicClassLoader = dynamicClassLoader;
    }

    @Override
    public Class<?> before(Object target, Object[] args) throws Throwable {
        if(target instanceof DynamicClassLoader) {
            return dynamicClassLoader.loadClass((String)args[0]);
        }
        return null;
    }

    @Override
    @IgnoreMethod
    public Class<?> after(Object target, Object[] args, Object result, Throwable throwable) throws Throwable {
        return null;
    }
}
