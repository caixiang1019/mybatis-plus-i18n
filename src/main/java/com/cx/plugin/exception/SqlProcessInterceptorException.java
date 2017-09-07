package com.cx.plugin.exception;

/**
 * SqlProcessInterceptor异常处理
 * Created by caixiang on 2017/9/6.
 */
public class SqlProcessInterceptorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SqlProcessInterceptorException(String message) {
        super(message);
    }

    public SqlProcessInterceptorException(Throwable throwable) {
        super(throwable);
    }

    public SqlProcessInterceptorException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
