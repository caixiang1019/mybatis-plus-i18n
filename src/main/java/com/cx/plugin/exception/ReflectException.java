package com.cx.plugin.exception;

/**
 * UncheckedException
 * 严重错误,不需要catch,需要解决
 * Created by caixiang on 2017/8/16.
 */
public class ReflectException extends RuntimeException {

    public ReflectException(String message) {
        super(message);
    }

    public ReflectException(Throwable throwable) {
        super(throwable);
    }

    public ReflectException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
