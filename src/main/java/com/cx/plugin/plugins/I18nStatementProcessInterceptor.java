package com.cx.plugin.plugins;

import com.baomidou.mybatisplus.toolkit.StringUtils;
import com.cx.plugin.annotations.I18nTable;
import com.cx.plugin.domain.BaseI18nDomain;
import com.cx.plugin.domain.BaseI18nMetaData;
import com.cx.plugin.enums.MethodPrefixEnum;
import com.cx.plugin.util.ReflectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 新建domain反射设置方式i18n处理拦截器
 * Created by caixiang on 2017/8/21.
 */

@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class I18nStatementProcessInterceptor implements Interceptor {

    private static final String ID_CONSTANT = "id";
    private static final String METADATA_CONSTANT = "metaData";
    private static final String LANGUAGE_CONSTANT = "language";
    //i18n的标记,暂时给出,尚未用到
    private boolean i18nFlag = false;

    private HashMap<String, List<HashMap<String, String>>> metaData;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Object baseResult = invocation.proceed();

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        String baseMsId = ms.getId();
        String i18nMsId = new StringBuilder().append(baseMsId.substring(0, baseMsId.indexOf("Mapper")))
                .append("I18n")
                .append(baseMsId.substring(baseMsId.indexOf("Mapper")))
                .toString();
        MappedStatement i18nMs = ms.getConfiguration().getMappedStatement(i18nMsId);
        Class clazz = parameter.getClass();
        //有i18n需求的处理,强制要求继承BaseI18nDomain 并且在TYPE加@I18nTable
        if (clazz.getSuperclass().isAssignableFrom(BaseI18nDomain.class) && clazz.isAnnotationPresent(I18nTable.class)) {

            I18nTable i18nTable = (I18nTable) clazz.getAnnotation(I18nTable.class);
            Class refI18nClazz = null;
            //empty i18nParameter obj
            BaseI18nDomain baseI18nDomain = (BaseI18nDomain) parameter;
            metaData = (HashMap) baseI18nDomain.getI18n();

            List<BaseI18nMetaData> baseI18nMetaDataList = new ArrayList<>();
            for (String str : metaData.keySet()) {

                List<Map<String, String>> l = (ArrayList) metaData.get(str);
                for (Map m : l) {
                    String field = str;
                    String str2 = (String) m.get("language");
                    String str3 = (String) m.get("value");
                    baseI18nMetaDataList.add(BaseI18nMetaData.builder().field(field).language(str2).value(str3).build());
                }
            }

            Map<String, Object> m = new HashMap<>();
            for (BaseI18nMetaData baseI18nMetaData : baseI18nMetaDataList) {
                String language = baseI18nMetaData.getLanguage();
                Object i18nParameter = refI18nClazz.newInstance();
                if (m.keySet().contains(language)) {
                    i18nParameter = m.get(language);
                }
                //处理field&value
                processI18nParameterField(i18nParameter, baseI18nMetaData.getField(), baseI18nMetaData.getValue());
                //增加Id
                if (ReflectionUtil.getMethodValue(i18nParameter, ID_CONSTANT) == null) {
                    processI18nParameterField(i18nParameter, ID_CONSTANT, ReflectionUtil.getMethodValue(parameter, ID_CONSTANT));
                }
                //增加Language
                if (ReflectionUtil.getMethodValue(i18nParameter, LANGUAGE_CONSTANT) == null) {
                    processI18nParameterField(i18nParameter, LANGUAGE_CONSTANT, language);
                }
                m.put(language, i18nParameter);
            }
            Executor executor = (Executor) invocation.getTarget();
            //与60行的Map遍历方式不同,此种效率更高
            Iterator iterator = m.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                executor.update(i18nMs, entry.getValue());
            }
        }
        return baseResult;
    }

    private void processI18nParameterField(Object instance, String fieldStr, Object fieldValue) throws Throwable {
        Method[] methods = instance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().contains(ReflectionUtil.methodNameCaptalize(MethodPrefixEnum.SET, fieldStr))) {
                method.invoke(instance, fieldValue);
            }
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
