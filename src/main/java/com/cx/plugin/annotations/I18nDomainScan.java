package com.cx.plugin.annotations;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by caixiang on 2017/9/26.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(I18nDomainScannerRegistrar.class)
public @interface I18nDomainScan {


    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};
}
