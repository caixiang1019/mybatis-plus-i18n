package com.cx.plugin.service;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.IService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.cx.plugin.annotations.I18nField;
import com.cx.plugin.domain.BaseI18nDomain;
import com.cx.plugin.enums.MethodPrefixEnum;
import com.cx.plugin.util.ReflectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

/**
 * i18nService
 * M: BaseMapper原始Mapper
 * N: IService扩展i18nService
 * S: 扩展i18nEntity
 * T: ServiceImpl原始ServiceImpl
 * <p>
 * Created by caixiang on 2017/8/15.
 */
@Slf4j
public abstract class BaseI18nService<M extends BaseMapper<T>, N extends IService<S>, S, T extends BaseI18nDomain> extends ServiceImpl<M, T> {

    private static final String ID_CONSTANT = "id";
    private static final String LANGUAGE_CONSTANT = "language";

    @Autowired
    private N extI18nService;

    @Transactional
    @Override
    public boolean insert(T entity) {
        //baseMapper的返回结果
        boolean flag = retBool(baseMapper.insert(entity));

        Class<?> superClass = entity.getClass().getSuperclass();
        //可以考虑把BaseI18nDomain的metaData改为public
        Field[] fields = superClass.getDeclaredFields();
        Field field = fields[0];
        field.setAccessible(true);
        HashMap metaDataMap = null;

        try {
            metaDataMap = (HashMap) field.get(entity);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        S sInstance = null;
        Type[] parameterizedTypes = ReflectionUtil.getParameterizedTypes(this);
        try {
            //2为泛型参数的索引下标
            Class<S> clazz = (Class<S>) ReflectionUtil.getClass(parameterizedTypes[2]);
            sInstance = clazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        Field[] baseEntityFields = entity.getClass().getDeclaredFields();

        for (Field baseEntityField : baseEntityFields) {
            if (baseEntityField.isAnnotationPresent(I18nField.class)) {
                String baseEntityFieldName = baseEntityField.getName();
                processI18nEntityField(sInstance, baseEntityFieldName, ReflectionUtil.getMethodValue(entity, baseEntityFieldName));

            } else if (ID_CONSTANT.equals(baseEntityField.getName())) {
                processI18nEntityField(sInstance, ID_CONSTANT, ReflectionUtil.getMethodValue(baseMapper.selectOne(entity), ID_CONSTANT));
            }
        }

        String languageStr = (String) metaDataMap.get(LANGUAGE_CONSTANT);
        if (StringUtils.isNotEmpty(languageStr)) {
            processI18nEntityField(sInstance, LANGUAGE_CONSTANT, languageStr);
        }

        return flag && extI18nService.insert(sInstance);
    }

    @Override
    public List<T> selectList(Wrapper<T> wrapper) {
        log.info("mySelectList");
        //List<S> list = extI18nService.selectList(null);
        //log.info(list.toString());
        return baseMapper.selectList(wrapper);
    }

    @Transactional
    @Override
    public boolean update(T entity, Wrapper<T> wrapper) {
        return retBool(baseMapper.update(entity, wrapper));
    }

    @Transactional
    @Override
    public boolean delete(Wrapper<T> wrapper) {
        return retBool(baseMapper.delete(wrapper));
    }


    /**
     * @param sInstance  扩展i18nEntity
     * @param fieldStr   FieldName
     * @param fieldValue FieldValue
     */
    private void processI18nEntityField(S sInstance, String fieldStr, Object fieldValue) {
        try {
            Method[] methods = sInstance.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().contains(ReflectionUtil.methodNameCapitalize(MethodPrefixEnum.SET, fieldStr))) {
                    method.invoke(sInstance, fieldValue);
                }
            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}