package com.cx.plugin.service;

import com.baomidou.mybatisplus.entity.TableFieldInfo;
import com.baomidou.mybatisplus.entity.TableInfo;
import com.baomidou.mybatisplus.toolkit.TableInfoHelper;
import com.cx.plugin.annotations.I18nField;
import com.cx.plugin.domain.BaseI18nDomain;
import com.cx.plugin.domain.BaseI18nMetaData;
import com.cx.plugin.exception.SqlProcessInterceptorException;
import com.cx.plugin.util.ReflectionUtil;
import com.cx.plugin.util.SqlExecuteUtil;
import lombok.extern.slf4j.Slf4j;
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
        log.info("Get locale from locale resolver: " + locale.toString());
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
                    //保留那些不与表的column对应的属性值,suggested by 淡然
                    BeanUtils.copyProperties(entity, resultList.get(0), i18nFieldNameList.toArray(new String[]{}));
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
        Map<String, T> resultMap = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
            if (tableInfo == null) {
                throw new SqlProcessInterceptorException("未找到clazz对应tableInfo实例,只支持被mybatis-plus扫描到的domain类,请检查!");
            }
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
                if (null == i18nDomainMethodCache.get(clazz)) {
                    throw new SqlProcessInterceptorException(clazz.getName() + "尚未初始化,请检查!");
                }
                i18nFieldNameList.forEach(t -> {
                    Invoker setMethodInvoker = i18nDomainMethodCache.get(clazz).getSetInvoker(t);
                    try {
                        if (setMethodInvoker instanceof MethodInvoker) {
                            ReflectionUtil.specificProcessInvoker((MethodInvoker) setMethodInvoker, resultSet, t, result);
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
     * @param idList 主键id集合
     * @param clazz  继承I18nDomain的类信息
     * @param <T>
     * @return
     */
    public <T extends BaseI18nDomain> List<T> selectListBaseTableInfoWithI18n(List<Long> idList, Class<T> clazz) {
        return idList.stream().map(id -> selectOneBaseTableInfoWithI18n(id, clazz)).collect(Collectors.toList());
    }

    /**
     * @param id    主键id集合
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
            tableFieldInfoList.forEach(t -> sbBase.append(t.getColumn() + " AS " + t.getProperty() + ","));
            sbBase.append("id FROM ").append(tableInfo.getTableName()).append(" WHERE id =?;");
            List<Object> parameterList = new ArrayList<>();
            parameterList.add(id);
            //拿到Base表信息
            List<Object> resultList = SqlExecuteUtil.executeForListWithManyParameters(connection, sbBase.toString(), parameterList, clazz, tableFieldInfoList);
            if (resultList.size() > 1) {
                throw new RuntimeException("数据有问题,一个id至多匹配一条记录!");
            } else if (resultList.size() == 1) {
                instance = (T) resultList.get(0);
            }

            //组装I18n信息为Map<String, List<HashMap<String, String>>>
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
                Map<String, List<HashMap<String, String>>> map = convertList2Map(baseI18nMetaDataList);
                Invoker setMethodInvoker = BaseI18nService2.i18nDomainMethodCache.get(clazz).getSetInvoker("i18n");
                Object[] param = {map};
                setMethodInvoker.invoke(instance, param);
                return instance;
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return null;
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
