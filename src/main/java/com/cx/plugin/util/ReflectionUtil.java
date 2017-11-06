package com.cx.plugin.util;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.toolkit.MapUtils;
import com.baomidou.mybatisplus.toolkit.StringUtils;
import com.cx.plugin.enums.MethodPrefixEnum;
import com.cx.plugin.exception.ReflectException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.joda.time.DateTime;

import java.lang.reflect.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    public static String getMethodCapitalize(Field field, MethodPrefixEnum methodPrefixEnum, String str) {
        Class<?> fieldType = field.getType();
        //处理boolean.class||Boolean.class
        str = StringUtils.removeIsPrefixIfBoolean(str, fieldType);
        switch (methodPrefixEnum) {
            case GET:
                return StringUtils.concatCapitalize(boolean.class.equals(fieldType) ? "is" : "get", str);
            case SET:
                return StringUtils.concatCapitalize("set", str);
            default:
                throw new ReflectException("Only support reflect get/set method!");
        }
    }

    /**
     * 反射get/set 方法名, 不需要对boolean的Field特殊处理
     *
     * @param methodPrefixEnum get/set enum
     * @param str              属性字符串内容
     * @return
     */
    public static String methodNameCapitalize(MethodPrefixEnum methodPrefixEnum, final String str) {
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
            Method method = cls.getMethod(getMethodCapitalize(fieldMaps.get(str), MethodPrefixEnum.GET, str));
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
     * 获取指定包下父类为supClazz的所有类的属性的get&set方法,组装成方便使用的Reflector对象
     *
     * @param packagePath 不能为null
     * @param supClazz    父类Class
     * @return
     */
    public static ConcurrentMap<Class<?>, Reflector> getReflectorsFromPackage(List<String> packagePath, Class supClazz) {
        if (CollectionUtils.isEmpty(packagePath)) {
            throw new ReflectException("VFS scan have not initialized yet!");
        }
        ConcurrentHashMap<Class<?>, Reflector> map = new ConcurrentHashMap<>();
        packagePath.stream().forEach(p -> {
            List<Class<?>> classList = PackageScannerUtil.getClassFromSuperClass(p, supClazz);
            if (CollectionUtils.isEmpty(classList)) {
                log.warn("packagePath: " + p + ",Can not find specific class which needs to be initialized!");
            }
            classList.forEach(clz -> map.put(clz, new Reflector(clz)));
        });

        return map;
    }

    /**
     * 针对Invoker缓存方式的特殊处理对象设值
     *
     * @param setMethodInvoker MethodInvoker实例
     * @param data             数据集
     * @param property         field.getName
     * @param result           要设值得对象
     * @param i18nFieldList    多语言字段list
     */
    public static void specificProcessInvoker(MethodInvoker setMethodInvoker, Object data, String property, Object result, List<String> i18nFieldList) {
        Field methodField = null;

        try {
            methodField = setMethodInvoker.getClass().getDeclaredField("method");
            methodField.setAccessible(true);
            Method method = (Method) methodField.get(setMethodInvoker);
            //确定是set方法且只有一个参数
            Class parameterClazz = method.getParameterTypes()[0];
            Object[] paramField;
            if (data instanceof ResultSet) {
                ResultSet resultSet = (ResultSet) data;
                //针对UUID、DateTime特殊处理,如有其它特殊类型,需要放在这里处理
                if (parameterClazz == UUID.class) {
                    paramField = new Object[]{UUID.fromString((String) resultSet.getObject(property))};
                    setMethodInvoker.invoke(result, paramField);
                } else if (parameterClazz == DateTime.class) {
                    paramField = new Object[]{new DateTime(resultSet.getObject(property))};
                } else {
                    if (i18nFieldList.contains(property)) {
                        paramField = new Object[]{ReflectionUtil.isObjectNullOrStringBlank(resultSet.getObject(property)) ? resultSet.getObject("base_" + property) : resultSet.getObject(property)};
                    } else {
                        paramField = new Object[]{resultSet.getObject(property)};
                    }
                }
            } else {
                if (parameterClazz == UUID.class) {
                    paramField = new Object[]{UUID.fromString(String.valueOf(data))};
                } else if (parameterClazz == DateTime.class) {
                    paramField = new Object[]{new DateTime(data)};
                } else {
                    paramField = new Object[]{data};
                }
            }
            setMethodInvoker.invoke(result, paramField);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    /**
     * @param o
     * @return
     */
    public static boolean isObjectNullOrStringBlank(Object o) {

        if (o == null) {
            return true;
        }
        if (o instanceof String) {
            return org.apache.commons.lang.StringUtils.isBlank((String) o);
        } else {
            return false;
        }
    }

}
