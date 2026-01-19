package com.navercorp.pinpoint.bootstrap.interceptor;

public interface DelegateInterceptor extends Interceptor {


    Interceptor getDelegate();

}
