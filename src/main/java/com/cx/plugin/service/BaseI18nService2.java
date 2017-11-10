package com.cx.plugin.service;

import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.entity.TableFieldInfo;
import com.baomidou.mybatisplus.entity.TableInfo;
import com.baomidou.mybatisplus.toolkit.TableInfoHelper;
import com.cx.plugin.annotations.I18nField;
import com.cx.plugin.domain.BaseI18nDomain;
import com.cx.plugin.domain.BaseI18nMetaData;
import com.cx.plugin.exception.SqlProcessInterceptorException;
import com.cx.plugin.plugins.I18nSqlProcessInterceptor;
import com.cx.plugin.util.ReflectionUtil;
import com.cx.plugin.util.SqlExecuteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.springframework.beans.BeanUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by caixiang on 2017/8/31.
 */
@Slf4j
@Service

public class BaseI18nService2 {

    private static final String ID_CONSTANT = "id";

    private DataSource dataSource;

    public static ConcurrentMap<Class<?>, Reflector> i18nDomainMethodCache = new ConcurrentHashMap<>();

    public BaseI18nService2(DataSource dataSource) {
        this.dataSource = dataSource;
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

        if (entity == null) {
            return entity;
        }
        Locale locale = LocaleContextHolder.getLocale();
        try (Connection connection = dataSource.getConnection()) {
            Class clazz = entity.getClass();
            TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
            if (tableInfo == null) {
                throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
            }
            List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
            Long baseTableId = (Long) ReflectionUtil.getMethodValue(entity, ID_CONSTANT);
            List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
            StringBuilder sb = new StringBuilder("SELECT");
            if (baseTableId != null) {
                tableFieldInfoList.forEach(f -> {
                    if (i18nFieldNameList.contains(f.getProperty())) {
                        sb.append(" base." + f.getColumn() + " AS base_" + f.getProperty() + ",i18n." + f.getColumn() + " AS " + f.getProperty() + ",");
                    } else {
                        if (f.getColumn().equals(f.getProperty())) {
                            sb.append(" base." + f.getColumn() + ",");
                        } else {
                            sb.append(" base." + f.getColumn() + " AS " + f.getProperty() + ",");
                        }
                    }

                });
                sb.append("base.id FROM ").append(tableInfo.getTableName() + " base ").append("LEFT JOIN ")
                        .append(tableInfo.getTableName() + "_i18n i18n ON base.id = i18n.id WHERE base.id = ? ")
                        .append("AND i18n.language = ?;");
                List<Object> parameterList = new ArrayList<>();
                parameterList.add(baseTableId);
                parameterList.add(locale.toString());
                List<Object> resultList = SqlExecuteUtil.executeForListWithManyParameters(connection, sb.toString(), parameterList, clazz, tableFieldInfoList, entity, i18nFieldNameList);
                if (resultList.size() > 1) {
                    throw new SqlProcessInterceptorException("数据有问题,一个id和language至多匹配一条记录!");
                } else if (resultList.size() == 1) {
                    //保留那些不与表的column对应的属性值,suggested by 淡定
                    BeanUtils.copyProperties(entity, resultList.get(0), i18nFieldNameList.toArray(new String[]{}));
                    return (T) resultList.get(0);
                } else {
                    log.info("未找到匹配的记录,将返回原entity");
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
        if (CollectionUtils.isEmpty(entityList)) {
            return entityList;
        }
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
        Map<String, T> resultMap = new HashMap<String, T>();
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
                    Invoker setMethodInvoker = i18nDomainMethodCache.get(clazz).getSetInvoker(t);
                    try {
                        if (setMethodInvoker instanceof MethodInvoker) {
                            ReflectionUtil.specificProcessInvoker((MethodInvoker) setMethodInvoker, resultSet, t, result, null, i18nFieldNameList);
                        } else {
                            Object[] paramField = {resultSet.getObject(t)};
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

    /**
     * 查询原表数据&i18n表数据list
     *
     * @param idList 主键id集合
     * @param clazz  继承I18nDomain的类信息
     * @param <T>
     * @return
     */
    public <T extends BaseI18nDomain> List<T> selectListBaseTableInfoWithI18n(List<Long> idList, Class<T> clazz) {
        return idList.stream().map(id -> selectOneBaseTableInfoWithI18n(id, clazz)).collect(Collectors.toList());
    }

    /**
     * 查询原表数据&i18n表数据
     *
     * @param id    主键id
     * @param clazz 继承I18nDomain的类信息
     * @param <T>
     * @return
     */
    public <T extends BaseI18nDomain> T selectOneBaseTableInfoWithI18n(Long id, Class<T> clazz) {
        try (Connection connection = dataSource.getConnection()) {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
            if (tableInfo == null) {
                throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
            }
            T instance = clazz.newInstance();
            List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
            List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
            StringBuilder sbBase = new StringBuilder("SELECT ");
            tableFieldInfoList.forEach(t -> {
                sbBase.append(t.getColumn() + " AS " + t.getProperty() + ",");
                if (i18nFieldNameList.contains(t.getProperty())) {
                    sbBase.append(t.getColumn() + " AS base_" + t.getProperty() + ",");
                }
            });
            sbBase.append("id FROM ").append(tableInfo.getTableName()).append(" WHERE id =?;");
            List<Object> parameterList = new ArrayList<>();
            parameterList.add(id);
            //拿到Base表信息
            List<Object> resultList = SqlExecuteUtil.executeForListWithManyParameters(connection, sbBase.toString(), parameterList, clazz, tableFieldInfoList, null, i18nFieldNameList);
            if (resultList.size() > 1) {
                throw new RuntimeException("数据有问题,一个id至多匹配一条记录!");
            } else if (resultList.size() == 1) {
                instance = (T) resultList.get(0);
            }
            Map i18n = getI18nMap(clazz, id);
            Invoker setMethodInvoker = BaseI18nService2.i18nDomainMethodCache.get(clazz).getSetInvoker("i18n");
            Object[] param = {i18n};
            setMethodInvoker.invoke(instance, param);
            return instance;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        } catch (InstantiationException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    /**
     * 查询主表翻译后数据&i18n表数据list
     *
     * @param idList 主键id集合
     * @param clazz  继承I18nDomain的类信息
     * @param <T>
     * @return
     */
    public <T extends BaseI18nDomain> List<T> selectListTranslatedTableInfoWithI18n(List<Long> idList, Class<T> clazz) {
        return idList.stream().map(id -> selectOneTranslatedTableInfoWithI18n(id, clazz)).collect(Collectors.toList());
    }

    /**
     * 查询主表翻译后数据&i18n表数据
     *
     * @param id    主键id
     * @param clazz 继承I18nDomain的类信息
     * @param <T>
     * @return
     */
    public <T extends BaseI18nDomain> T selectOneTranslatedTableInfoWithI18n(Long id, Class<T> clazz) {
        try (Connection connection = dataSource.getConnection()) {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
            if (tableInfo == null) {
                throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
            }
            T instance = clazz.newInstance();
            List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
            List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
            StringBuilder sbBase = new StringBuilder("SELECT ");
            tableFieldInfoList.forEach(t -> {
                if (i18nFieldNameList.contains(t.getProperty())) {
                    sbBase.append("base." + t.getColumn() + " AS base_" + t.getProperty() + ",i18n." + t.getColumn() + " AS " + t.getProperty() + ",");
                } else {
                    sbBase.append("base." + t.getColumn() + " AS " + t.getProperty() + ",");

                }
            });
            sbBase.append("base.id,i18n.language FROM ").append(tableInfo.getTableName() + " base INNER JOIN ").append(tableInfo.getTableName() + "_i18n i18n ON base.id = i18n.id").append(" WHERE base.id =?;");
            List<Object> parameterList = new ArrayList<>();
            parameterList.add(id);
            //拿到翻译后数据信息
            Map<Long, Map<String, Object>> map = SqlExecuteUtil.executeForMapWithManyParameters(connection, sbBase.toString(), parameterList, tableFieldInfoList, i18nFieldNameList);
            I18nSqlProcessInterceptor i18nSqlProcessInterceptor = new I18nSqlProcessInterceptor();
            List<Object> resultList = i18nSqlProcessInterceptor.convertBaseMap2List(clazz, map, null, i18nFieldNameList);
            if (resultList.size() > 1) {
                throw new RuntimeException("数据有问题,一个id至多匹配一条记录!");
            } else if (resultList.size() == 1) {
                instance = (T) resultList.get(0);
            }
            Map i18n = getI18nMap(clazz, id);
            Invoker setMethodInvoker = BaseI18nService2.i18nDomainMethodCache.get(clazz).getSetInvoker("i18n");
            Object[] param = {i18n};
            setMethodInvoker.invoke(instance, param);
            return instance;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 根据class和id查询i18n Map信息
     *
     * @param clazz 继承I18nDomain的类class
     * @param id    表id值
     * @return
     */
    public Map<String, List<Map<String, String>>> getI18nMap(Class<? extends BaseI18nDomain> clazz, Long id) {

        TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
        if (tableInfo == null) {
            throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
        }
        List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
        List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);
        List<Object> parameterList = new ArrayList<>();
        parameterList.add(id);
        //组装I18n信息为Map<String, List<HashMap<String, String>>>
        try (Connection connection = dataSource.getConnection()) {
            StringBuilder sbI18n = new StringBuilder("SELECT ");
            tableFieldInfoList.forEach(t -> {
                if (i18nFieldNameList.contains(t.getProperty())) {
                    sbI18n.append(t.getColumn() + " AS " + t.getProperty() + ",");
                }
            });
            sbI18n.append("language FROM ").append(tableInfo.getTableName() + "_i18n").append(" WHERE id =?;");
            try (PreparedStatement psm = connection.prepareStatement(sbI18n.toString())) {
                for (int i = 0; i < parameterList.size(); i++) {
                    psm.setObject(i + 1, parameterList.get(i));
                }
                ResultSet resultSet = psm.executeQuery();
                List<BaseI18nMetaData> baseI18nMetaDataList = new ArrayList<>();
                while (resultSet.next()) {
                    String language = resultSet.getString("language");
                    i18nFieldNameList.forEach(t -> {
                        try {
                            baseI18nMetaDataList.add(BaseI18nMetaData.builder().language(language).field(t).value(resultSet.getObject(t) == null ? null : String.valueOf(resultSet.getObject(t))).build());
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
                Map<String, List<Map<String, String>>> map = convertList2Map(baseI18nMetaDataList);
                return map;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 新增或更新I18n信息
     *
     * @param originDataMap i18n
     * @param clazz         实体类class
     * @param id            主键id
     */
    public void insertOrUpdateI18n(Map<String, List<Map<String, String>>> originDataMap, Class<?> clazz, Long id) {
        List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(clazz, I18nField.class);

        TableName tableName = clazz.getAnnotation(TableName.class);
        if (tableName == null || StringUtils.isEmpty(tableName.value())) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            List<BaseI18nMetaData> baseI18nMetaDataList = new ArrayList<>();
            Map<String, Map<String, String>> metaDataMap = new HashMap<>();
            if (originDataMap != null) {
                originDataMap.forEach((k, v) -> v.forEach(p -> {
                    //暂不抛出异常,只处理被@I18nField注解的field的metaData
                    if (i18nFieldNameList.contains(k))
                        baseI18nMetaDataList.add(BaseI18nMetaData.builder().field(k).language(p.get("language")).value(p.get("value")).build());
                }));
                //k:language, v: k1:filed,v1:value
                baseI18nMetaDataList.forEach(b -> {
                    String language = b.getLanguage();
                    Map map = metaDataMap.keySet().contains(language) ? metaDataMap.get(language) : new HashMap<String, String>();
                    map.put(b.getField(), b.getValue());
                    metaDataMap.put(language, map);
                });
            }

            I18nSqlProcessInterceptor i18nSqlProcessInterceptor = new I18nSqlProcessInterceptor();
            i18nSqlProcessInterceptor.execInsertOrUpdateList(metaDataMap, tableName.value(), connection, id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Map convertList2Map(List<BaseI18nMetaData> baseI18nMetaDataList) {
        Map<String, List<HashMap<String, String>>> map = new HashMap<>();
        for (BaseI18nMetaData baseI18nMetaData : baseI18nMetaDataList) {
            String field = baseI18nMetaData.getField();
            List subList;
            if (map.containsKey(field)) {
                subList = map.get(field);

            } else {
                subList = new ArrayList<HashMap<String, String>>();
                map.put(field, subList);

            }
            //若value 为null,则不构造map
            if (baseI18nMetaData.getValue() != null) {
                Map elementMap = new HashMap<String, String>();
                elementMap.put("language", baseI18nMetaData.getLanguage());
                elementMap.put("value", baseI18nMetaData.getValue());
                subList.add(elementMap);
            }
        }
        return map;
    }
}