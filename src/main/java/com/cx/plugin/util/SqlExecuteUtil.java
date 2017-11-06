package com.cx.plugin.util;

import com.baomidou.mybatisplus.entity.TableFieldInfo;
import com.cx.plugin.exception.SqlProcessInterceptorException;
import com.cx.plugin.service.BaseI18nService2;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caixiang on 2017/9/6.
 */
public class SqlExecuteUtil {

    /**
     * 有参数的预处理执行,返回Id的集合
     *
     * @param connection
     * @param valueList
     * @param sql
     * @return
     */
    public static List<Long> executeForIdsWithParameters(Connection connection, List<Object> valueList, String sql) {
        List<Long> idList = new ArrayList<>();
        if (CollectionUtils.isEmpty(valueList)) {
            return executeForIdsWithoutParameters(connection, sql);
        }
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            for (int i = 0; i < valueList.size(); i++) {
                psm.setObject(i + 1, valueList.get(i));
            }
            ResultSet resultSet = psm.executeQuery();
            while (resultSet.next()) {
                idList.add(resultSet.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idList;
    }

    /**
     * 返回Id的集合
     * 无参数
     * 预处理执行
     *
     * @param connection
     * @param sql
     * @return
     */
    public static List<Long> executeForIdsWithoutParameters(Connection connection, String sql) {
        List<Long> idList = new ArrayList<>();
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            ResultSet resultSet = psm.executeQuery();
            while (resultSet.next()) {
                idList.add(resultSet.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idList;
    }

    /**
     * 不需要返回结果
     * 无参数
     * 预处理执行
     *
     * @param connection
     * @param sql
     */
    public static void executeForNoResultWithoutParameters(Connection connection, String sql) {
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            psm.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 不需要返回结果
     * 只有一个参数:参数是Id
     * 预处理执行
     *
     * @param connection
     * @param sql
     * @param id
     */
    public static void executeForNoResultWithParameterId(Connection connection, String sql, Long id) {
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            psm.setLong(1, id);
            psm.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 不需要返回结果
     * 有多个参数
     * 预处理执行
     *
     * @param connection
     * @param sql
     * @param parameterList
     */
    public static void executeForNoResultWithManyParameters(Connection connection, String sql, List<Object> parameterList) {
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            if (CollectionUtils.isEmpty(parameterList)) {
                executeForNoResultWithoutParameters(connection, sql);
            } else {
                for (int i = 0; i < parameterList.size(); i++) {
                    psm.setObject(i + 1, parameterList.get(i));
                }
                psm.execute();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 多参数并且带返回结果的预处理执行
     * 完全匹配tableFieldInfo返回反射设值之后的对象list
     *
     * @param connection
     * @param sql
     * @param parameterList
     * @param domainClass
     * @param tableFieldInfoList
     * @param i18nFieldList
     * @return
     */
    public static List executeForListWithManyParameters(Connection connection, String sql, List<Object> parameterList, Class domainClass, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList) {
        List<Object> objectList = new ArrayList();
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameterList.size(); i++) {
                psm.setObject(i + 1, parameterList.get(i));
            }
            ResultSet resultSet = psm.executeQuery();
            while (resultSet.next()) {
                Object result = domainClass.newInstance();
                if (null == BaseI18nService2.i18nDomainMethodCache.get(domainClass)) {
                    throw new SqlProcessInterceptorException(domainClass.getName() + "尚未初始化,请检查!");
                }
                tableFieldInfoList.forEach(t -> {
                    Invoker setMethodInvoker = BaseI18nService2.i18nDomainMethodCache.get(domainClass).getSetInvoker(t.getProperty());
                    try {
                        if (setMethodInvoker instanceof MethodInvoker) {
                            ReflectionUtil.specificProcessInvoker((MethodInvoker) setMethodInvoker, resultSet, t.getProperty(), result, i18nFieldList);
                        } else {
                            Object[] paramField = {ReflectionUtil.isObjectNullOrStringBlank(resultSet.getObject(t.getProperty())) ? resultSet.getObject("base_" + t.getProperty()) : resultSet.getObject(t.getProperty())};
                            setMethodInvoker.invoke(result, paramField);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                });
                Invoker idSetMethodInvoker = BaseI18nService2.i18nDomainMethodCache.get(domainClass).getSetInvoker("id");
                Object[] params = {resultSet.getLong("id")};

                idSetMethodInvoker.invoke(result, params);
                objectList.add(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return objectList;
    }

    /**
     * 针对selectOne/selectList操作,不带参数
     *
     * @param connection
     * @param sql
     * @param tableFieldInfoList
     * @param i18nFieldList
     * @return
     */
    public static Map executeForMapWithoutParameters(Connection connection, String sql, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList) {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            ResultSet resultSet = psm.executeQuery();
            processResult(map, resultSet, tableFieldInfoList, i18nFieldList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 针对selectOne/selectList操作,带参数
     *
     * @param connection
     * @param sql
     * @param parameterList      参数list
     * @param tableFieldInfoList
     * @param i18nFieldList
     * @return
     */
    public static Map executeForMapWithManyParameters(Connection connection, String sql, List<Object> parameterList, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList) {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameterList.size(); i++) {
                psm.setObject(i + 1, parameterList.get(i));
            }
            ResultSet resultSet = psm.executeQuery();
            processResult(map, resultSet, tableFieldInfoList, i18nFieldList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 对拿到的结果集特殊处理:使得如果匹配i18n的language就返回翻译field的value,不匹配的记录返回base记录
     *
     * @param map
     * @param resultSet
     * @param tableFieldInfoList
     * @param i18nFieldList
     */
    private static void processResult(Map<Long, Map<String, Object>> map, ResultSet resultSet, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList) {
        try {
            while (resultSet.next()) {
                Map<String, Object> subMap = new HashMap<>();
                Long id = resultSet.getLong("id");
                if (LocaleContextHolder.getLocale().toString().equalsIgnoreCase(resultSet.getString("language"))) {
                    for (TableFieldInfo tableFieldInfo : tableFieldInfoList) {
                        subMap.put(tableFieldInfo.getProperty(), resultSet.getObject(tableFieldInfo.getProperty()));
                        if (i18nFieldList.contains(tableFieldInfo.getProperty())) {
                            subMap.put(tableFieldInfo.getProperty(), ReflectionUtil.isObjectNullOrStringBlank(resultSet.getObject(tableFieldInfo.getProperty())) ? resultSet.getObject("base_" + tableFieldInfo.getProperty()) : resultSet.getObject(tableFieldInfo.getProperty()));
                        }
                    }
                    map.put(id, subMap);
                } else {
                    if (!map.containsKey(id)) {
                        for (TableFieldInfo tableFieldInfo : tableFieldInfoList) {
                            subMap.put(tableFieldInfo.getProperty(), resultSet.getObject(tableFieldInfo.getProperty()));
                            if (i18nFieldList.contains(tableFieldInfo.getProperty())) {
                                subMap.put(tableFieldInfo.getProperty(), resultSet.getObject("base_" + tableFieldInfo.getProperty()));
                            }
                        }
                        map.put(id, subMap);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
