package com.cx.plugin.plugins;

import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.toolkit.StringUtils;
import com.cx.plugin.domain.BaseI18nDomain;
import com.cx.plugin.domain.BaseI18nMetaData;
import com.cx.plugin.domain.SqlSegment;
import com.cx.plugin.util.ReflectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

/**
 * 拼接sql方式i18n处理拦截器
 * Created by caixiang on 2017/8/23.
 */

@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class I18nSqlSegmentProcessInterceptor implements Interceptor {

    private static final String ID_CONSTANT = "id";
    private static final String LANGUAGE_CONSTANT = "language";
    private static final String VALUE_CONSTANT = "value";
    //i18n的标记,暂时给出,尚未用到
    private boolean i18nFlag = false;

    private HashMap<String, List<HashMap<String, String>>> metaData;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object baseResult = invocation.proceed();

        //考虑是否可以拿到baseTableName,暂未用到
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        Class clazz = parameter.getClass();
        //有i18n需求的处理,强制要求继承BaseI18nDomain 并且在TYPE加@I18nTable
        if (clazz.getSuperclass().isAssignableFrom(BaseI18nDomain.class) && clazz.isAnnotationPresent(TableName.class)) {
            TableName tableName = (TableName) clazz.getAnnotation(TableName.class);
            String i18nTableName = tableName.value() + "_i18n";
            BaseI18nDomain baseI18nDomain = (BaseI18nDomain) parameter;
            metaData = (HashMap) baseI18nDomain.getI18n();

            List<BaseI18nMetaData> baseI18nMetaDataList = new ArrayList<>();
            for (String str : metaData.keySet()) {

                List<Map<String, String>> l = (ArrayList) metaData.get(str);
                for (Map m : l) {
                    String field = str;
                    String str2 = (String) m.get(LANGUAGE_CONSTANT);
                    String str3 = (String) m.get(VALUE_CONSTANT);
                    baseI18nMetaDataList.add(BaseI18nMetaData.builder().field(field).language(str2).value(str3).build());
                }
            }

            /**
             * K:language
             * V:Sql to execute
             */
            Map<String, SqlSegment> sqlMap = new HashMap<>();
            for (BaseI18nMetaData baseI18nMetaData : baseI18nMetaDataList) {
                SqlSegment sqlSegment = SqlSegment.builder().sqlHeader("INSERT INTO " + i18nTableName + " ").middleStr(" VALUES ").build();
                String language = baseI18nMetaData.getLanguage();
                if (sqlMap.keySet().contains(language)) {
                    sqlSegment = sqlMap.get(language);
                }

                //处理field&value
                constructI18nSqlSegment(sqlSegment, baseI18nMetaData.getField(), baseI18nMetaData.getValue());
                String[] sqlArray = sqlSegment.getFieldsStr().substring(1, sqlSegment.getFieldsStr().length() - 1).split(",");
                //id不存在,增加id
                if (!Arrays.asList(sqlArray).contains(ID_CONSTANT)) {
                    constructI18nSqlSegment(sqlSegment, ID_CONSTANT, ReflectionUtil.getMethodValue(parameter, ID_CONSTANT));
                }
                //language不存在,增加language
                if (!Arrays.asList(sqlArray).contains(LANGUAGE_CONSTANT)) {
                    constructI18nSqlSegment(sqlSegment, LANGUAGE_CONSTANT, language);
                }
                sqlMap.put(language, sqlSegment);
            }

            Executor executor = (Executor) invocation.getTarget();
            Connection connection = executor.getTransaction().getConnection();
            //执行效率高于map.keySet,循环遍历key再getValue方式
            Iterator iterator = sqlMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                SqlSegment sqlSegment = (SqlSegment) entry.getValue();
                StringBuilder sb = new StringBuilder();
                String sql = sb.append(sqlSegment.getSqlHeader())
                        .append(sqlSegment.getFieldsStr())
                        .append(sqlSegment.getMiddleStr())
                        .append(sqlSegment.getValuesStr())
                        .toString();
                Statement sm = connection.createStatement();
                sm.execute(sql);
            }

            //boolean s = sm2.execute("INSERT INTO art_dep (id,name,code,phone,address,age) VALUES (34,'caixiang','dasdfCode','1232131231','大大',35);");

        }

        return baseResult;
    }


    /**
     * TODO crud考虑的时候要分层
     * 处理sql之insert片段方法
     *
     * @param sqlSegment
     * @param fieldName
     * @param fieldValue
     */
    private void constructI18nSqlSegment(SqlSegment sqlSegment, String fieldName, Object fieldValue) {
        String fieldStr = sqlSegment.getFieldsStr() == null ? "" : sqlSegment.getFieldsStr();
        if (fieldStr.startsWith("(")) {
            int endIndex = fieldStr.indexOf(")");
            sqlSegment.setFieldsStr(fieldStr.substring(0, endIndex) + "," + fieldName + fieldStr.substring(endIndex));
        } else {
            sqlSegment.setFieldsStr(fieldStr + "(" + fieldName + ")");
        }
        String valueStr = sqlSegment.getValuesStr() == null ? "" : sqlSegment.getValuesStr();
        if (valueStr.startsWith("(")) {
            int endIndex = valueStr.indexOf(")");
            sqlSegment.setValuesStr(valueStr.substring(0, endIndex) + ",'" + fieldValue + "'" + valueStr.substring(endIndex));
        } else {
            sqlSegment.setValuesStr(valueStr + "('" + fieldValue + "')");
        }
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
        String stopProceed = properties.getProperty("i18nFlag");
        if (StringUtils.isNotEmpty(stopProceed)) {
            this.i18nFlag = Boolean.valueOf(stopProceed);
        }
    }

    public boolean isI18nFlag() {
        return i18nFlag;
    }

    public void setI18nFlag(boolean i18nFlag) {
        this.i18nFlag = i18nFlag;
    }

}
//            if(s){
//                ResultSet rs = sm2.getResultSet();
//                ResultSetMetaData rsmd = rs.getMetaData();
//                int columnCount = rsmd.getColumnCount();
//                while(rs.next())
//                {
//                    for(int i =0;i<columnCount;i++){
//                        System.out.print(rs.getString(i + 1) + "\t");
//                    }
//                    System.out.print("\n");
//                }
//            }