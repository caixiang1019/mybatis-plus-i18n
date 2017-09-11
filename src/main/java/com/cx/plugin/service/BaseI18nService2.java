package com.cx.plugin.service;

import com.baomidou.mybatisplus.entity.TableFieldInfo;
import com.baomidou.mybatisplus.entity.TableInfo;
import com.baomidou.mybatisplus.toolkit.TableInfoHelper;
import com.cx.plugin.annotations.I18nField;
import com.cx.plugin.domain.BaseI18nDomain;
import com.cx.plugin.enums.MethodPrefixEnum;
import com.cx.plugin.util.ReflectionUtil;
import com.cx.plugin.util.SqlExecuteUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by caixiang on 2017/8/31.
 */
@Slf4j
@Service
public class BaseI18nService2 {

    private static final String ID_CONSTANT = "id";

    private Environment env;
    private DataSource dataSource;

    public static Map<String, Map<String, Method>> i18nDomainSetMethodCache = new HashMap<>();

    public BaseI18nService2(Environment env, DataSource dataSource) {
        this.env = env;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initI18nDomainMethod() {
        if (i18nDomainSetMethodCache.size() == 0) {
            //方法缓存
            i18nDomainSetMethodCache = ReflectionUtil.getMethodsFromClass(env.getProperty("i18n.domain.package"), MethodPrefixEnum.SET, BaseI18nDomain.class);
        }
    }

    /**
     * 若根据id和language匹配不到记录,返回传入的entity,不再多处理了,减少一次查询
     * 如果想根据id直接拿到翻译好的记录,直接调用mp的selectById等方法
     *
     * @param entity declaration : subClass of BaseI8nDomain
     * @param <T>
     * @return
     */
    public <T extends BaseI18nDomain> T convertOneByLocale(T entity) {

        Locale locale = LocaleContextHolder.getLocale();
        try (Connection connection = dataSource.getConnection()) {
            Class clazz = entity.getClass();
            TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
            List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
            Long baseTableId = (Long) ReflectionUtil.getMethodValue(entity, ID_CONSTANT);
            List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
            StringBuilder sb = new StringBuilder("SELECT");
            if (baseTableId != null) {
                tableFieldInfoList.forEach(f -> {
                    if (i18nFieldNameList.contains(f.getProperty())) {
                        sb.append(" i18n." + f.getColumn() + " AS " + f.getProperty() + ",");
                    } else {
                        if (f.getColumn().equals(f.getProperty())) {
                            sb.append(" base." + f.getColumn() + ",");
                        } else {
                            sb.append(" base." + f.getColumn() + " AS " + f.getProperty() + ",");
                        }
                    }

                });
                sb.append("base.id FROM ").append(tableInfo.getTableName() + " base ").append("INNER JOIN ")
                        .append(tableInfo.getTableName() + "_i18n i18n ON base.id = i18n.id WHERE base.id = ? ")
                        .append("AND i18n.language = ?;");
                List<Object> parameterList = new ArrayList<>();
                parameterList.add(baseTableId);
                parameterList.add(locale.toString());
                List<Object> resultList = SqlExecuteUtil.executeForListWithManyParameters(connection, sb.toString(), parameterList, clazz, tableFieldInfoList);
                if (resultList.size() > 1) {
                    throw new RuntimeException("数据有问题,一个id和language至多匹配一条记录!");
                } else if (resultList.size() == 1) {
                    return (T) resultList.get(0);
                }
            } else {
                log.info("id必传!根据id和language匹配做翻译!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entity;
    }

    /**
     * 返回List<T>
     *
     * @param entityList
     * @param <T>
     * @return
     */
    public <T extends BaseI18nDomain> List<T> convertListByLocale(List<T> entityList) {
        return entityList.stream().map(e -> convertOneByLocale(e)).collect(Collectors.toList());
    }

    /**
     * 根据传入的id以及Class 返回Domain上被@I18nField标识的field值(借用T的实例存这些field)
     *
     * @param id    base表主键id
     * @param clazz 继承BaseI18nDomain类的Class
     * @param <T>
     * @return
     */
    public <T extends BaseI18nDomain> Map<String, T> getI18nInfo(Long id, Class<T> clazz) {
        Map<String, T> resultMap = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
            List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
            List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
            StringBuilder sb = new StringBuilder("SELECT ");
            tableFieldInfoList.forEach(t -> {
                if (i18nFieldNameList.contains(t.getProperty())) {
                    sb.append(t.getColumn() + " AS " + t.getProperty() + ",");
                }
            });
            sb.append("language FROM ").append(tableInfo.getTableName() + "_i18n").append(" WHERE id =?;");
            resultMap = getI18nInfoMap(connection, sb.toString(), id, clazz, i18nFieldNameList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    private <T extends BaseI18nDomain> Map<String, T> getI18nInfoMap(Connection connection, String sql, Long id, Class clazz, List<String> i18nFieldNameList) {
        Map<String, T> resultMap = new HashMap();
        try (PreparedStatement psm = connection.prepareStatement(sql)) {
            psm.setLong(1, id);
            ResultSet resultSet = psm.executeQuery();
            while (resultSet.next()) {
                Object result = clazz.newInstance();
                i18nFieldNameList.forEach(t -> {
                    Method setMethod = i18nDomainSetMethodCache.get(clazz.getName()).get(ReflectionUtil.methodNameCapitalize(MethodPrefixEnum.SET, t));
                    try {
                        setMethod.invoke(result, resultSet.getObject(t));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
                resultMap.put(resultSet.getString("language"), (T) result);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultMap;
    }


}
