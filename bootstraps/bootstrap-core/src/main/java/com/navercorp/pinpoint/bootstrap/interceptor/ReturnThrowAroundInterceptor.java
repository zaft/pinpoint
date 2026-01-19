package com.navercorp.pinpoint.bootstrap.interceptor;

public interface ReturnThrowAroundInterceptor<T> extends ReturnAroundInterceptor<T> {

    T before(Object target, Object[] args) throws Throwable;

    T after(Object target, Object[] args, Object result, Throwable throwable) throws Throwable;
}
