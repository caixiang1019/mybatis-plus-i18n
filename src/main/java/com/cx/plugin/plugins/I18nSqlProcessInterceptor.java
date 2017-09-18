package com.cx.plugin.plugins;

import com.baomidou.mybatisplus.entity.TableFieldInfo;
import com.baomidou.mybatisplus.entity.TableInfo;
import com.baomidou.mybatisplus.enums.SqlMethod;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.toolkit.StringUtils;
import com.baomidou.mybatisplus.toolkit.TableInfoHelper;
import com.cx.plugin.annotations.I18nField;
import com.cx.plugin.domain.BaseI18nDomain;
import com.cx.plugin.domain.BaseI18nMetaData;
import com.cx.plugin.exception.SqlProcessInterceptorException;
import com.cx.plugin.service.BaseI18nService2;
import com.cx.plugin.util.ReflectionUtil;
import com.cx.plugin.util.SqlExecuteUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.context.i18n.LocaleContextHolder;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 拼接sql方式i18n处理拦截器
 * Created by caixiang on 2017/8/23.
 */
@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class I18nSqlProcessInterceptor implements Interceptor {

    private static final String ID_CONSTANT = "id";
    private static final String LANGUAGE_CONSTANT = "language";
    private static final String VALUE_CONSTANT = "value";
    private static final String DELIMITER_DOT = ".";

    private TableInfo tableInfo;
    private String baseMethodStr;
    private MappedStatement ms;
    //参数对象
    private Object parameter;
    //参数类Class
    private Class parameterClass;
    //这个mappedStatement关联的Domain类Class
    private Class domainClass;
    private Connection connection;
    private final static EnumMap<SqlCommandType, String[]> supportedOperationMap = new EnumMap<>(SqlCommandType.class);

    static {
        //支持的操作缓存
        supportedOperationMap.put(SqlCommandType.INSERT, new String[]{SqlMethod.INSERT_ONE.getMethod(), SqlMethod.INSERT_ONE_ALL_COLUMN.getMethod()});
        supportedOperationMap.put(SqlCommandType.UPDATE, new String[]{SqlMethod.UPDATE.getMethod(), SqlMethod.UPDATE_ALL_COLUMN_BY_ID.getMethod(), SqlMethod.UPDATE_BY_ID.getMethod()});
        supportedOperationMap.put(SqlCommandType.DELETE, new String[]{SqlMethod.DELETE.getMethod(), SqlMethod.DELETE_BY_ID.getMethod(), SqlMethod.DELETE_BY_MAP.getMethod()});
        supportedOperationMap.put(SqlCommandType.SELECT, new String[]{SqlMethod.SELECT_BY_ID.getMethod(), SqlMethod.SELECT_LIST.getMethod(), SqlMethod.SELECT_ONE.getMethod(), SqlMethod.SELECT_MAPS.getMethod()});
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object baseResult;

        //初始化
        init(invocation);

        switch (ms.getSqlCommandType()) {
            case INSERT: {
                //先执行,否则拿不到id
                baseResult = invocation.proceed();
                List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(parameterClass, I18nField.class);

                if (BaseI18nDomain.class.isAssignableFrom(parameterClass) && methodSupported(SqlCommandType.INSERT, baseMethodStr) && CollectionUtils.isNotEmpty(i18nFieldNameList)) {
                    String baseTableName = tableInfo.getTableName();
                    Map<String, Map<String, String>> metaDataMap = constructMetaDataMap(parameter, i18nFieldNameList);
                    execInsertSql(metaDataMap, baseTableName, connection, (Long) ReflectionUtil.getMethodValue(parameter, ID_CONSTANT));
                }
                break;
            }
            case UPDATE: {
                BoundSql boundSql = ms.getSqlSource().getBoundSql(parameter);
                if (Map.class.isAssignableFrom(boundSql.getParameterObject().getClass()) && Map.class.isAssignableFrom(parameterClass) && methodSupported(SqlCommandType.UPDATE, baseMethodStr)) {
                    MapperMethod.ParamMap parameterMap = (MapperMethod.ParamMap) boundSql.getParameterObject();
                    if (parameterMap.containsKey("ew")) {
                        //update method
                        Object entityWrapper = parameterMap.get("ew");
                        //update 不带where 直接返回原逻辑
                        if (entityWrapper == null) {
                            return invocation.proceed();
                        }
                        Object entity = ((EntityWrapper) entityWrapper).getEntity();
                        List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
                        List<String> parametersStrList = parameterMappingList.stream().map(s -> s.getProperty()).collect(Collectors.toList());
                        //只取ew.entity.XXX
                        List<Object> valueList = parametersStrList.stream().filter(s -> s.contains("ew.entity"))
                                .map(s -> s.substring(s.lastIndexOf(DELIMITER_DOT) + 1)).collect(Collectors.toList()).stream()
                                .map(p -> ReflectionUtil.getMethodValue(entity, p)).collect(Collectors.toList());
                        // 取 where(包含) 之后的str
                        String conditionStr = getSqlFromBaseSql(boundSql.getSql(), SqlCommandType.UPDATE, null, null, null);
                        //select 先查出来符合检索条件的id
                        StringBuilder selectIdSb = new StringBuilder("SELECT id FROM ").append(tableInfo.getTableName()).append(" ").append(conditionStr).append(";");
                        List<Long> idList = SqlExecuteUtil.executeForIdsWithParameters(connection, valueList, selectIdSb.toString());
                        if (CollectionUtils.isNotEmpty(idList)) {
                            String idStr = idList.stream().map(id -> String.valueOf(id)).collect(Collectors.toList()).stream().collect(Collectors.joining(","));
                            //delete 删除i18n表匹配的id记录
                            StringBuilder deleteSb = new StringBuilder("DELETE FROM ").append(tableInfo.getTableName()).append("_i18n WHERE id IN (").append(idStr).append(");");
                            SqlExecuteUtil.executeForNoResultWithoutParameters(connection, deleteSb.toString());

                            //insert 根据传入的i18n元数据插入i18n表
                            Object baseEntity = parameterMap.get("et");
                            Class baseEntityClass = baseEntity.getClass();
                            idList.forEach(id -> {
                                List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(baseEntityClass, I18nField.class);
                                Map<String, Map<String, String>> metaDataMap = constructMetaDataMap(baseEntity, i18nFieldNameList);
                                execInsertSql(metaDataMap, tableInfo.getTableName(), connection, id);
                            });
                        }
                    } else if (parameterMap.containsKey("et") && BaseI18nDomain.class.isAssignableFrom(parameterMap.get("et").getClass())) {
                        // updateById,updateAllColumnsById method
                        String baseTableName = tableInfo.getTableName();
                        Long baseTableId = (Long) ReflectionUtil.getMethodValue(parameterMap.get("et"), ID_CONSTANT);
                        //updateById,updateAllColumnsById传入的entity必须有id,不然无法确定改哪个
                        if (baseTableId != null) {
                            String sqlOrigin = "DELETE FROM ${tableName}_i18n WHERE id = ?;";
                            Map<String, String> sqlParamMap = new HashMap<>();
                            sqlParamMap.put("tableName", baseTableName);
                            String deleteSql = StrSubstitutor.replace(sqlOrigin, sqlParamMap);
                            //1.delete
                            SqlExecuteUtil.executeForNoResultWithParameterId(connection, deleteSql, baseTableId);
                            //2.insert
                            List<String> i18nFieldNameList = ReflectionUtil.getSpecificAnnotationFieldNameList(parameterMap.get("et").getClass(), I18nField.class);
                            Map<String, Map<String, String>> metaDataMap = constructMetaDataMap(parameterMap.get("et"), i18nFieldNameList);
                            execInsertSql(metaDataMap, baseTableName, connection, baseTableId);
                        } else{
                            log.info("Can not find the value of id of parameter 'entity'!");
                        }
                    }
                } else {
                    log.info("Parameter'class: " + parameterClass.getName() + ",i18n interceptor is not supported for this api now!");
                }
                baseResult = invocation.proceed();
                break;
            }
            //这里的delete原本就是物理删除,逻辑先留着
            case DELETE: {
                if (Map.class.isAssignableFrom(parameterClass) && methodSupported(SqlCommandType.DELETE, baseMethodStr)) {
                    BoundSql boundSql = ms.getSqlSource().getBoundSql(parameter);
                    MapperMethod.ParamMap parameterMap = (MapperMethod.ParamMap) boundSql.getParameterObject();
                    List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
                    List<Object> valueList = new ArrayList<>();
                    if (parameterMap.containsKey("ew")) {
                        //delete 不带where 直接返回原逻辑
                        Object entityWrapper = parameterMap.get("ew");
                        if (entityWrapper == null) {
                            return invocation.proceed();
                        }
                        Object entity = ((EntityWrapper) entityWrapper).getEntity();
                        //禁止全表delete! 等价于entity!=null
                        List<String> parametersStrList = parameterMappingList.stream().map(s -> s.getProperty().substring(s.getProperty().lastIndexOf(DELIMITER_DOT) + 1)).collect(Collectors.toList());
                        valueList = parametersStrList.stream().map(p -> ReflectionUtil.getMethodValue(entity, p)).collect(Collectors.toList());
                    } else if (parameterMap.containsKey("cm")) {
                        HashMap<String, Object> entity = (HashMap) parameterMap.get("cm");
                        List<String> parametersStrList = parameterMappingList.stream().map(s -> s.getProperty().substring(s.getProperty().indexOf("[") + 1, s.getProperty().indexOf("]"))).collect(Collectors.toList());
                        valueList = parametersStrList.stream().map(p -> entity.get(p)).collect(Collectors.toList());

                    }
                    String deleteSql = getSqlFromBaseSql(boundSql.getSql(), SqlCommandType.DELETE, null, null, null);
                    SqlExecuteUtil.executeForNoResultWithManyParameters(connection, deleteSql, valueList);

                } else if (Long.class.isAssignableFrom(parameterClass) && methodSupported(SqlCommandType.DELETE, baseMethodStr)) {
                    BoundSql boundSql = ms.getSqlSource().getBoundSql(parameter);
                    Long id = (Long) boundSql.getParameterObject();
                    String deleteSql = getSqlFromBaseSql(boundSql.getSql(), SqlCommandType.DELETE, null, null, null);
                    SqlExecuteUtil.executeForNoResultWithParameterId(connection, deleteSql, id);
                } else {
                    log.info("Parameter'class: " + parameterClass.getName() + ",i18n interceptor is not supported for this api now!");
                }
                //后执行,否则delete关联时候有问题
                baseResult = invocation.proceed();
                break;
            }
            case SELECT: {
                //selectById
                BoundSql boundSql = ms.getSqlSource().getBoundSql(parameter);
                String baseSql = boundSql.getSql();
                List<String> i18nFieldList = ReflectionUtil.getSpecificAnnotationFieldNameList(domainClass, I18nField.class);
                List<TableFieldInfo> tableFieldInfoList = tableInfo.getFieldList();
                //根据id和Locale处理多语言
                if (Long.class.isAssignableFrom(parameterClass) && methodSupported(SqlCommandType.SELECT, baseMethodStr)) {
                    //selectById
                    String selectSql = getSqlFromBaseSql(baseSql, SqlCommandType.SELECT, tableFieldInfoList, i18nFieldList, null);
                    //顺序很重要,根据where里面的?对应的column顺序
                    List<Object> parameterList = new ArrayList<>();
                    parameterList.add(parameter);
                    parameterList.add(LocaleContextHolder.getLocale().toString());
                    List<Object> objectList = SqlExecuteUtil.executeForListWithManyParameters(connection, selectSql, parameterList, domainClass, tableFieldInfoList);

                    //返回值
                    if (CollectionUtils.isNotEmpty(objectList)) {
                        return objectList;
                    }
                } else if (Map.class.isAssignableFrom(parameterClass) && methodSupported(SqlCommandType.SELECT, baseMethodStr)) {
                    //selectOne,selectList
                    EntityWrapper entityWrapper = (EntityWrapper) ((Map) boundSql.getParameterObject()).get("ew");
                    String sqlWhere = entityWrapper.getSqlSegment();
                    HashMap<String, String> paramValuePairs = (HashMap) entityWrapper.getParamNameValuePairs();
                    for (Map.Entry e : paramValuePairs.entrySet()) {
                        sqlWhere = sqlWhere.replace("#{ew.paramNameValuePairs." + e.getKey() + "}", "'" + e.getValue() + "'").replace(tableInfo.getTableName(), tableInfo.getTableName() + " base");
                    }
                    String selectSql = getSqlFromBaseSql(baseSql, SqlCommandType.SELECT, tableFieldInfoList, i18nFieldList, sqlWhere);

                    Map<Long, Map<String, Object>> map = SqlExecuteUtil.executeForMapWithoutParameters(connection, selectSql, tableFieldInfoList, i18nFieldList);
                    List<Object> resultList = convertBaseMap2List(map);
                    if (CollectionUtils.isNotEmpty(resultList)) {
                        return resultList;
                    }
                } else {
                    log.info("Parameter'class: " + parameterClass.getName() + ",i18n interceptor is not supported for this api now!");
                }
                //后执行
                baseResult = invocation.proceed();
                break;
            }
            default:
                throw new SqlProcessInterceptorException("非CRUD操作,请检查!");
        }

        return baseResult;
    }


    /**
     * 根据sqlCommondType分类
     * 依赖tableFieldInfoList、i18nFieldList加工baseSql
     *
     * @param baseSql            原始Sql
     * @param sqlCommandType     原始CommandType
     * @param tableFieldInfoList 原始tableFieldInfo
     * @param i18nFieldList      i18nField注解标识的属性集合
     * @param sqlWhere           针对select的一种特殊sql处理
     * @return
     */
    private String getSqlFromBaseSql(String baseSql, SqlCommandType sqlCommandType, List<TableFieldInfo> tableFieldInfoList, List<String> i18nFieldList, String sqlWhere) {
        StringBuilder sb = new StringBuilder();
        switch (sqlCommandType) {
            case SELECT: {
                if (StringUtils.isEmpty(sqlWhere)) {
                    for (TableFieldInfo tableFieldInfo : tableFieldInfoList) {
                        if (i18nFieldList.contains(tableFieldInfo.getProperty())) {
                            baseSql = baseSql.replaceAll(tableFieldInfo.getColumn(), "i18n." + tableFieldInfo.getColumn());
                        } else {
                            baseSql = baseSql.replaceAll(tableFieldInfo.getColumn(), "base." + tableFieldInfo.getColumn());
                        }
                    }
                    //id特殊处理
                    baseSql = baseSql.replaceFirst("id", "base.id");
                    sb.append(baseSql.substring(0, baseSql.indexOf("WHERE"))).append("base INNER JOIN ").append(tableInfo.getTableName())
                            .append("_i18n i18n ON base.id = i18n.id ").append(baseSql.substring(baseSql.indexOf("WHERE")).replaceFirst("id", "base.id")).append(" AND i18n.language = ?;");
                } else {
                    String sqlHeader = baseSql.substring(0, baseSql.indexOf("FROM"));

                    for (TableFieldInfo tableFieldInfo : tableFieldInfoList) {
                        if (i18nFieldList.contains(tableFieldInfo.getProperty())) {
                            //i18n的多语言field和base表的多语言field都拿出来
                            sqlHeader = sqlHeader.replaceAll(tableFieldInfo.getColumn(), "i18n." + tableFieldInfo.getColumn()) + ",base." + tableFieldInfo.getColumn() + " AS base_" + tableFieldInfo.getProperty();
                        } else {
                            sqlHeader = sqlHeader.replaceAll(tableFieldInfo.getColumn(), "base." + tableFieldInfo.getColumn());
                        }
                        sqlWhere = sqlWhere.replaceAll(tableFieldInfo.getColumn().replaceAll("`", ""), "base." + tableFieldInfo.getColumn().replaceAll("`", ""));
                    }
                    //id特殊处理
                    sqlWhere = sqlWhere.replaceFirst("id", "base.id");
                    sqlHeader = sqlHeader.replaceFirst("id", "base.id");
                    sb.append(sqlHeader).append(",i18n.language FROM ").append(tableInfo.getTableName()).append(" base INNER JOIN ").append(tableInfo.getTableName())
                            .append("_i18n i18n  ON base.id = i18n.id ").append(sqlWhere).append(";");
                }
                break;
            }
            case UPDATE: {
                sb.append(baseSql.substring(baseSql.indexOf("WHERE")));
                break;
            }
            case DELETE: {
                int offset = baseSql.indexOf("FROM", 0);
                int firstBlankIndex = baseSql.indexOf(" ", offset);
                int secondBlankIndex = baseSql.indexOf(" ", firstBlankIndex + 1);
                String tableName = baseSql.substring(firstBlankIndex, secondBlankIndex).trim();
                sb.append(baseSql.substring(0, firstBlankIndex)).append(" ").append(tableName + "_i18n").append(" ")
                        .append("WHERE id IN (SELECT id FROM ").append(tableName).append(baseSql.substring(secondBlankIndex))
                        .append(");");
                break;
            }
            default:
                throw new SqlProcessInterceptorException("目前只支持DELETE!");
        }

        return sb.toString();
    }

    /**
     * 执行insertSql
     *
     * @param metaDataMap   整理过的metaData
     * @param baseTableName 基本表名称
     * @param connection    数据库连接
     * @param baseTableId   基本表id值
     */
    private void execInsertSql(Map<String, Map<String, String>> metaDataMap, String baseTableName, Connection connection, Long baseTableId) {
        metaDataMap.forEach((k, v) -> {
            String sqlOrigin = "INSERT INTO ${tableName}_i18n(id,language${fieldList}) VALUES(?,?${valueList});";
            StringBuilder fieldListSb = new StringBuilder();
            StringBuilder valueListSb = new StringBuilder();
            Map<String, String> sqlParamMap = new HashMap<>();
            sqlParamMap.put("tableName", baseTableName);
            sqlParamMap.put("fieldList", "");
            sqlParamMap.put("valueList", "");
            //field 校验
            v.forEach((k1, v1) -> {
                //驼峰字段处理depName->dep_name
                if (StringUtils.containsUpperCase(k1)) {
                    k1 = StringUtils.camelToUnderline(k1);
                }
                fieldListSb.append("," + k1);
                valueListSb.append(",'" + v1 + "'");
                sqlParamMap.put("fieldList", fieldListSb.toString());
                sqlParamMap.put("valueList", valueListSb.toString());
            });
            String sql = StrSubstitutor.replace(sqlOrigin, sqlParamMap);
            try (PreparedStatement psm = connection.prepareStatement(sql)) {
                psm.setLong(1, baseTableId);
                psm.setString(2, k);
                psm.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 将原始metaData(Map<String,List<Map<String,String>>>)转换成Map<String, Map<String, String>>
     *
     * @param parameter         原始参数,包含metaData
     * @param i18nFieldNameList class含@I18nField注解的fieldNameList
     * @return
     */
    private Map<String, Map<String, String>> constructMetaDataMap(Object parameter, List<String> i18nFieldNameList) {
        HashMap<String, List<HashMap<String, String>>> metaData = (HashMap) ((BaseI18nDomain) parameter).getI18n();
        List<BaseI18nMetaData> baseI18nMetaDataList = new ArrayList<>();
        metaData.forEach((k, v) -> v.forEach(p -> {
            //暂不抛出异常,只处理被@I18nField注解的field的metaData
            if (i18nFieldNameList.contains(k))
                baseI18nMetaDataList.add(BaseI18nMetaData.builder().field(k).language(p.get(LANGUAGE_CONSTANT)).value(p.get(VALUE_CONSTANT)).build());
        }));
        //k:language, v: k1:filed,v1:value
        Map<String, Map<String, String>> metaDataMap = new HashMap<>();
        for (BaseI18nMetaData baseI18nMetaData : baseI18nMetaDataList) {
            String language = baseI18nMetaData.getLanguage();
            Map subMap = new HashMap<String, String>();
            if (metaDataMap.keySet().contains(language)) {
                subMap = metaDataMap.get(language);
            }
            subMap.put(baseI18nMetaData.getField(), baseI18nMetaData.getValue());
            metaDataMap.put(language, subMap);
        }
        return metaDataMap;
    }

    /**
     * @param map
     * @return
     */
    private List convertBaseMap2List(Map<Long, Map<String, Object>> map) {
        List<Object> resultList = new ArrayList<>();
        map.forEach((id, subMap) -> {
            try {
                Object result = domainClass.newInstance();
                Invoker idSetMethodInvoker = BaseI18nService2.i18nDomainMethodCache.get(domainClass).getSetInvoker(ID_CONSTANT);
                Object[] param = {id};
                idSetMethodInvoker.invoke(result, param);

                subMap.forEach((fieldName, filedValue) -> {
                    Invoker setMethodInvoker = BaseI18nService2.i18nDomainMethodCache.get(domainClass).getSetInvoker(fieldName);
                    try {
                        if (setMethodInvoker instanceof MethodInvoker) {
                            ReflectionUtil.specificProcessInvoker((MethodInvoker) setMethodInvoker, filedValue, fieldName, result);
                        } else {
                            Object[] paramField = {filedValue};
                            setMethodInvoker.invoke(result, paramField);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });
                resultList.add(result);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        });
        return resultList;
    }

    /**
     * 方法是否支持
     *
     * @param sqlCommandType CRUD
     * @param methodStr      methodNameStr
     * @return
     */
    private Boolean methodSupported(SqlCommandType sqlCommandType, String methodStr) {
        String[] methodArray = supportedOperationMap.get(sqlCommandType);
        if (methodArray == null) {
            return false;
        }
        return Arrays.asList(methodArray).contains(methodStr);
    }

    private void init(Invocation invocation) {
        parameter = invocation.getArgs()[1];
        ms = (MappedStatement) invocation.getArgs()[0];

        //基本方法名
        baseMethodStr = ms.getId().substring(ms.getId().lastIndexOf(DELIMITER_DOT) + 1);
        parameterClass = parameter.getClass();
        Executor executor = (Executor) invocation.getTarget();
        try {
            connection = executor.getTransaction().getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            domainClass = ReflectionUtil.extractModelClass(Class.forName(ms.getId().substring(0, ms.getId().lastIndexOf(DELIMITER_DOT))));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        tableInfo = TableInfoHelper.getTableInfo(domainClass);

    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        //do not need properties temporarily
    }

}