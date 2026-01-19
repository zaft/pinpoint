package com.navercorp.pinpoint.bootstrap.interceptor;

public interface ReturnAroundInterceptor<T> extends Interceptor {

    T before(Object target, Object[] args);

    T after(Object target, Object[] args, Object result, Throwable throwable) throws Throwable;
}
