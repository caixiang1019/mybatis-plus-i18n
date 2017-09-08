package com.cx.plugin.util;

import com.baomidou.mybatisplus.entity.TableFieldInfo;
import com.cx.plugin.enums.MethodPrefixEnum;
import com.cx.plugin.service.BaseI18nService2;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            return excuteForIdsWithoutParameters(connection, sql);
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
    public static List<Long> excuteForIdsWithoutParameters(Connection connection, String sql) {
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
     * @return
     */
    public static List executeForListWithManyParameters(Connection connection, String sql, List<Object> parameterList, Class domainClass, List<TableFieldInfo> tableFieldInfoList) {
        List<Object> objectList = new ArrayList();
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameterList.size(); i++) {
                psm.setObject(i + 1, parameterList.get(i));
            }
            ResultSet resultSet = psm.executeQuery();
            while (resultSet.next()) {
                Object result = domainClass.newInstance();
                tableFieldInfoList.forEach(t -> {
                    Method setMethod = BaseI18nService2.i18nDomainSetMethodCache.get(domainClass).get(ReflectionUtil.methodNameCaptalize(MethodPrefixEnum.SET,t.getProperty()));
                    try {
                        setMethod.invoke(result, resultSet.getObject(t.getProperty()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                });
                Method idSetMethod = BaseI18nService2.i18nDomainSetMethodCache.get(domainClass).get(MethodPrefixEnum.SET + "Id");
                idSetMethod.invoke(result, resultSet.getLong("id"));
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
     * 针对selectOne/selectList操作
     * 对拿到的结果集特殊处理:使得如果匹配i18n的language就返回翻译field的value,不匹配的记录返回base记录
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
            while (resultSet.next()) {
                Map<String, Object> subMap = new HashMap<>();
                Long id = resultSet.getLong("id");
                if (LocaleContextHolder.getLocale().toString().equalsIgnoreCase(resultSet.getString("language"))) {
                    for (TableFieldInfo tableFieldInfo : tableFieldInfoList) {
                        subMap.put(tableFieldInfo.getProperty(), resultSet.getObject(tableFieldInfo.getProperty()));
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
        return map;
    }

}
