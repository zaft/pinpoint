/*
 * Copyright 2016 NAVER Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.bootstrap.interceptor;

import com.navercorp.pinpoint.bootstrap.interceptor.annotation.IgnoreMethod;

import java.util.Objects;

/**
 * @author jaehong.kim
 */
public class ExceptionThrowHandleReturnBeforeInterceptor<T> implements ReturnThrowAroundInterceptor<T>, DelegateInterceptor {

    private final ReturnAroundInterceptor<T> delegate;
    private final ExceptionHandler exceptionHandler;

    public ExceptionThrowHandleReturnBeforeInterceptor(ReturnAroundInterceptor<T> delegate, ExceptionHandler exceptionHandler) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
    }

    @Override
    public T before(Object target, Object[] args) throws Throwable {
        try {
            return this.delegate.before(target, args);
        } catch (Throwable t) {
            throw t;
        }
    }

    @Override
    @IgnoreMethod
    public T after(Object target, Object[] args, Object result, Throwable throwable) throws Throwable {
        return null;
    }

    @Override
    public ReturnAroundInterceptor<T> getDelegate() {
        return delegate;
    }
}