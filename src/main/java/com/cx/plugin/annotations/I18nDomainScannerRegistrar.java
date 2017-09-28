package com.cx.plugin.annotations;

import com.baomidou.mybatisplus.toolkit.CollectionUtils;
import com.cx.plugin.domain.BaseI18nDomain;
import com.cx.plugin.service.BaseI18nService2;
import com.cx.plugin.util.ReflectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.VFS;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caixiang on 2017/9/26.
 */
@Slf4j
public class I18nDomainScannerRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(I18nDomainScan.class.getName()));
        List<String> basePackages = new ArrayList<String>();
        for (String pkg : annoAttrs.getStringArray("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : annoAttrs.getStringArray("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (Class<?> clazz : annoAttrs.getClassArray("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (CollectionUtils.isNotEmpty(basePackages)) {
            //方法缓存
            VFS.addImplClass(SpringBootVFS.class);
            BaseI18nService2.i18nDomainMethodCache = ReflectionUtil.getReflectorsFromPackage(basePackages, BaseI18nDomain.class);
        } else {
            log.info("I18nDomainScan is not configured,if not use i18n interceptor,that's ok!");
        }
    }
}
