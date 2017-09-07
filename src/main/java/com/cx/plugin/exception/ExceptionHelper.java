package com.cx.plugin.exception;

import com.cx.plugin.annotations.Desc;

import java.lang.reflect.Field;

/**
 * Created by caixiang on 2017/8/25.
 */
public class ExceptionHelper {

    public static String getMessage(Enum e) {
        if (e == null) {
            e = BaseException.UNKNOWN_EXCEPTION;
        }
        try {
            Field field = e.getClass().getField(e.name());
            Desc desc = field.getAnnotation(Desc.class);
            if (desc == null || desc.value() == null || "".equals(desc.value().trim())) {
                throw new RuntimeException("BaseException缺省实例上的注解缺少value或者value为空,请联系系统管理员!");
            } else {
                return desc.value();
            }
        } catch (NoSuchFieldException e1) {
            e1.printStackTrace();
            throw new RuntimeException("未知系统异常,请联系系统管理员!");
        }
    }

    public static String getCode(Enum e) {
        if (e == null) {
            return BaseException.UNKNOWN_EXCEPTION.name();
        } else {
            return e.name();
        }

    }

//    public static void main(String[] args) {
//        getMessage(ParameterException.PARAMETER_NOT_SATISFIED);
//    }
}
