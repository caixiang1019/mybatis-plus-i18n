package com.cx.plugin.util;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.toolkit.MapUtils;
import com.baomidou.mybatisplus.toolkit.StringUtils;
import com.cx.plugin.enums.MethodPrefixEnum;
import com.cx.plugin.exception.ReflectException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by caixiang on 2017/8/15.
 */
@Slf4j
public class ReflectionUtil {

    private static final String TYPE_NAME_PREFIX = "class ";

    /**
     * 反射 method 方法名，例如 getId
     *
     * @param field
     * @param str   属性字符串内容
     * @return
     */
    public static String getMethodCapitalize(Field field, final String str) {
        Class<?> fieldType = field.getType();
        return StringUtils.concatCapitalize(boolean.class.equals(fieldType) ? "is" : "get", str);
    }

    /**
     * 反射get/set 方法名, 不需要对boolean的Field特殊处理
     *
     * @param methodPrefixEnum get/set enum
     * @param str              属性字符串内容
     * @return
     */
    public static String methodNameCaptalize(MethodPrefixEnum methodPrefixEnum, final String str) {
        return StringUtils.concatCapitalize(methodPrefixEnum.getPrefix(), str);
    }

    /**
     * 获取 public get方法的值(cls 与 entity 并不一定完全关联)
     *
     * @param cls
     * @param entity 实体
     * @param str    属性字符串内容
     * @return Object
     */
    public static Object getMethodValue(Class<?> cls, Object entity, String str) {
        Map<String, Field> fieldMaps = getFieldMap(cls);
        try {
            if (MapUtils.isEmpty(fieldMaps)) {
                throw new ReflectException(
                        String.format("Error: NoSuchField in %s for %s.  Cause:", cls.getSimpleName(), str));
            }
            Method method = cls.getMethod(getMethodCapitalize(fieldMaps.get(str), str));
            return method.invoke(entity);
        } catch (NoSuchMethodException e) {
            throw new ReflectException(String.format("Error: NoSuchMethod in %s.  Cause:", cls.getSimpleName()) + e);
        } catch (IllegalAccessException e) {
            throw new ReflectException(String.format("Error: Cannot execute a private method. in %s.  Cause:",
                    cls.getSimpleName())
                    + e);
        } catch (InvocationTargetException e) {
            throw new ReflectException("Error: InvocationTargetException on getMethodValue.  Cause:" + e);
        }
    }

    /**
     * 获取 public get方法的值
     *
     * @param entity 实体
     * @param str    属性字符串内容
     * @return Object
     */
    public static Object getMethodValue(Object entity, String str) {
        if (null == entity) {
            return null;
        }
        return getMethodValue(entity.getClass(), entity, str);
    }

    /**
     * <p>
     * 反射对象获取泛型
     * </p>
     *
     * @param clazz 对象
     * @param index 泛型所在位置
     * @return Class
     */
    @SuppressWarnings("rawtypes")
    public static Class getSuperClassGenricType(final Class clazz, final int index) {
        Type genType = clazz.getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)) {
            log.warn(String.format("Warn: %s's superclass not ParameterizedType", clazz.getSimpleName()));
            return Object.class;
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (index >= params.length || index < 0) {
            log.warn(String.format("Warn: Index: %s, Size of %s's Parameterized Type: %s .", index, clazz.getSimpleName(),
                    params.length));
            return Object.class;
        }
        if (!(params[index] instanceof Class)) {
            log.warn(String.format("Warn: %s not set the actual class on superclass generic parameter", clazz.getSimpleName()));
            return Object.class;
        }
        return (Class) params[index];
    }

    /**
     * 获取该类的所有属性列表
     *
     * @param clazz 反射类
     * @return
     */
    public static Map<String, Field> getFieldMap(Class<?> clazz) {
        List<Field> fieldList = getFieldList(clazz);
        Map<String, Field> fieldMap = Collections.emptyMap();
        if (CollectionUtils.isNotEmpty(fieldList)) {
            fieldMap = new LinkedHashMap<>();
            for (Field field : fieldList) {
                fieldMap.put(field.getName(), field);
            }
        }
        return fieldMap;
    }

    /**
     * 获取该类的所有属性列表
     *
     * @param clazz 反射类
     * @return
     */
    public static List<Field> getFieldList(Class<?> clazz) {
        if (null == clazz) {
            return null;
        }
        List<Field> fieldList = new LinkedList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            /* 过滤静态属性 */
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            /* 过滤 transient关键字修饰的属性 */
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            fieldList.add(field);
        }
        /* 处理父类字段 */
        Class<?> superClass = clazz.getSuperclass();
        if (superClass.equals(Object.class)) {
            return fieldList;
        }
        /* 排除重载属性 */
        return excludeOverrideSuperField(fieldList, getFieldList(superClass));
    }

    /**
     * 排序重置父类属性
     *
     * @param fieldList      子类属性
     * @param superFieldList 父类属性
     */
    public static List<Field> excludeOverrideSuperField(List<Field> fieldList, List<Field> superFieldList) {
        // 子类属性
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fieldList) {
            fieldMap.put(field.getName(), field);
        }
        for (Field superField : superFieldList) {
            if (null == fieldMap.get(superField.getName())) {
                // 加入重置父类属性
                fieldList.add(superField);
            }
        }
        return fieldList;
    }

    public static Type[] getParameterizedTypes(Object object) {
        Type superclassType = object.getClass().getGenericSuperclass();
        if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
            return null;
        }
        return ((ParameterizedType) superclassType).getActualTypeArguments();
    }

    public static String getClassName(Type type) {
        if (type == null) {
            return "";
        }
        String className = type.toString();
        if (className.startsWith(TYPE_NAME_PREFIX)) {
            className = className.substring(TYPE_NAME_PREFIX.length());
        }
        return className;
    }

    public static Class<?> getClass(Type type)
            throws ClassNotFoundException {
        String className = getClassName(type);
        if (className == null || className.isEmpty()) {
            return null;
        }
        return Class.forName(className);
    }

    public static Class<?> extractModelClass(Class<?> mapperClass) {
        if (mapperClass == BaseMapper.class) {
            log.warn(" Current Class is BaseMapper ");
            return null;
        } else {
            Type[] types = mapperClass.getGenericInterfaces();
            ParameterizedType target = null;
            for (Type type : types) {
                if (type instanceof ParameterizedType && BaseMapper.class.isAssignableFrom(mapperClass)) {
                    target = (ParameterizedType) type;
                    break;
                }
            }
            return target == null ? null : (Class<?>) target.getActualTypeArguments()[0];
        }
    }

    /**
     * 拿到clazz中所有带注解specificAnnotationClass的field的name集合
     *
     * @param clazz                   主类Class
     * @param specificAnnotationClass 注解类Class
     * @return
     */
    public static List<String> getSpecificAnnotationFieldNameList(Class clazz, Class specificAnnotationClass) {
        List list = new ArrayList<String>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(specificAnnotationClass)) {
                list.add(field.getName());
            }
        }
        return list;
    }

    /**
     * 获取指定包下的所有类的属性的get/set方法,支持
     *
     * @param packagePath
     * @param methodPrefixEnum
     * @param clazz
     * @return
     */
    public static Map<String, Map<String, Method>> getMethodsFromClass(String packagePath, MethodPrefixEnum methodPrefixEnum, Class clazz) {
        List<Class<?>> classList = PackageScannerUtil.getClassFromSuperClass(packagePath, clazz);
        Map<String, Map<String, Method>> map = new HashMap<>();
        classList.forEach(clz -> {
            List<Method> setMethodList = Arrays.stream(clz.getDeclaredFields()).map(f -> {
                Method method = null;
                try {
                    switch (methodPrefixEnum) {
                        case SET:
                            method = clz.getMethod(ReflectionUtil.methodNameCaptalize(methodPrefixEnum, f.getName()), f.getType());
                            break;
                        case GET:
                            method = clz.getMethod(ReflectionUtil.methodNameCaptalize(methodPrefixEnum, f.getName()));
                            break;
                        default:
                            throw new ReflectException("Only support get or set method!");
                    }

                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return method;
            }).filter(m -> m != null).collect(Collectors.toList());
            Map<String, Method> methodMap = setMethodList.stream().collect(Collectors.toMap(Method::getName, Function.identity(), (k1, k2) -> k2));
            map.put(clz.getName(), methodMap);
        });
        return map;
    }

}
