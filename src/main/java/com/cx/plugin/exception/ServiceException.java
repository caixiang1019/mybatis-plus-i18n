package com.cx.plugin.exception;

import lombok.Data;

/**
 * Created by caixiang on 2017/8/24.
 */
@Data
public class ServiceException extends Exception {

    protected String errorCode;

    protected String errorMessage;


    public ServiceException() {
    }

    public ServiceException(Enum e) {
        super(ExceptionHelper.getMessage(e));
        errorCode = ExceptionHelper.getCode(e);
        errorMessage = ExceptionHelper.getMessage(e);

    }

    public ServiceException(Enum e, Throwable cause) {
        super(ExceptionHelper.getMessage(e), cause);
        errorCode = ExceptionHelper.getCode(e);
        errorMessage = ExceptionHelper.getMessage(e);
    }

    public ServiceException(Throwable cause) {
        super(cause);

    }

    public ServiceException(Enum e, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(ExceptionHelper.getMessage(e), cause, enableSuppression, writableStackTrace);
        errorCode = ExceptionHelper.getCode(e);
        errorMessage = ExceptionHelper.getMessage(e);
    }
}
