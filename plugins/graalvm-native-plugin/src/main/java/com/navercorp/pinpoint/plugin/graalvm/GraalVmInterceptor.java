package com.navercorp.pinpoint.plugin.graalvm;

import com.navercorp.pinpoint.bootstrap.interceptor.ReturnAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.plugin.graalvm.DynamicClassLoader;

public class GraalVmInterceptor implements ReturnAroundInterceptor<Class<?>> {

    private ClassLoader dynamicClassLoader;

    public GraalVmInterceptor(ClassLoader dynamicClassLoader) {
        this.dynamicClassLoader = dynamicClassLoader;
    }

    @Override
    public Class<?> before(Object target, Object[] args) {
        if(target instanceof DynamicClassLoader) {
            DynamicClassLoader dcl = (DynamicClassLoader) target;
            if(dcl.getDynamicClassLoader() == null) {
                dcl.setDynamicClassLoader(dynamicClassLoader);
            }
            try {
               return dcl.getDynamicClassLoader().loadClass((String)args[0]);
            } catch (ClassNotFoundException e) {
            }
        }

        return null;
//        throw new RuntimeException(target.getClass() +" not cast DynamicClassLoader");
    }

    @Override
    public Class<?> after(Object target, Object[] args, Object result, Throwable throwable) {
        return (Class<?>) result;
    }
}
