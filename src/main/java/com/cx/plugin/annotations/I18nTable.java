package com.cx.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by caixiang on 2017/8/22.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface I18nTable {
    //关联的i18n的TableName
    String refTableName() default "";
//    //关联的i18n的Class
//    Class refClass();
}
