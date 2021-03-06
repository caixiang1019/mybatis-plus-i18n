package com.cx.plugin.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.MybatisConfiguration;
import com.baomidou.mybatisplus.MybatisXMLLanguageDriver;
import com.baomidou.mybatisplus.entity.GlobalConfiguration;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.plugins.PerformanceInterceptor;
import com.baomidou.mybatisplus.plugins.SqlExplainInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.cx.plugin.annotations.I18nDomainScan;
import com.cx.plugin.plugins.I18nSqlProcessInterceptor;
import com.cx.plugin.web.resolver.HeliosLocaleResolver;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.LocaleResolver;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by caixiang on 2017/8/15.
 */
@Configuration
@I18nDomainScan("com.cx.plugin.domain")
@MapperScan("com.cx.plugin.persistence.mapper")
public class MybatisI18NConfig {

    private final DataSourceProperties dataSourceProperties;
    private final MybatisProperties properties;
    private final ResourceLoader resourceLoader;

    public MybatisI18NConfig(DataSourceProperties dataSourceProperties, MybatisProperties properties, ResourceLoader resourceLoader) {
        this.dataSourceProperties = dataSourceProperties;
        this.properties = properties;
        this.resourceLoader = (resourceLoader == null) ? new DefaultResourceLoader() : resourceLoader;
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource, DataSourceProperties dataSourceProperties,
                                     LiquibaseProperties liquibaseProperties) {

        // Use liquibase.integration.spring.SpringLiquibase if you don't want Liquibase to start asynchronously
        SpringLiquibase liquibase = new AsyncSpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:liquibase/master.xml");
        liquibase.setContexts(liquibaseProperties.getContexts());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        liquibase.setShouldRun(liquibaseProperties.isEnabled());
        return liquibase;
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/i18n_db?useUnicode=true&characterEncoding=utf8");
        dataSource.setUsername("root");
        dataSource.setPassword("123456");
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setMaxActive(100);
        dataSource.setInitialSize(10);
        dataSource.setMaxWait(2000);
        dataSource.setTimeBetweenEvictionRunsMillis(10000);
        dataSource.setTestOnBorrow(true);
        dataSource.setTestOnReturn(true);
        dataSource.setValidationQuery("SELECT 1");
        return dataSource;
    }

    @Bean
    public MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean() throws Exception {
        MybatisSqlSessionFactoryBean mybatisSqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        mybatisSqlSessionFactoryBean.setDataSource(dataSource());
        mybatisSqlSessionFactoryBean.setVfs(SpringBootVFS.class);
        mybatisSqlSessionFactoryBean.setConfiguration(null);
        mybatisSqlSessionFactoryBean.setPlugins(getInterceptors());
        GlobalConfiguration globalConfig = new GlobalConfiguration();
        globalConfig.setIdType(2);
        globalConfig.setDbColumnUnderline(true);
        globalConfig.setDbType("mysql");
        mybatisSqlSessionFactoryBean.setGlobalConfig(globalConfig);
        MybatisConfiguration mybatisConfiguration = new MybatisConfiguration();
        mybatisConfiguration.setDefaultScriptingLanguage(MybatisXMLLanguageDriver.class);
        mybatisSqlSessionFactoryBean.setConfiguration(mybatisConfiguration);
        mybatisSqlSessionFactoryBean.setTypeAliasesPackage("com.cx.plugin");
        mybatisSqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("com/cx/plugin/persistence/mapper/*Mapper.xml"));
        mybatisSqlSessionFactoryBean.setTypeHandlersPackage("com.cx.plugin.persistence.typehandler");
        return mybatisSqlSessionFactoryBean;
    }

    @Bean
    public Interceptor[] getInterceptors() {
        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        paginationInterceptor.setDialectType("mysql");
        interceptors.add(paginationInterceptor);

        // Disabled on Production and Stage env
//        // Performance Analysis Plugins
        Interceptor performanceInterceptor = new PerformanceInterceptor();
        Properties performanceInterceptorProps = new Properties();
        performanceInterceptorProps.setProperty("maxTime", "100000000");
        performanceInterceptorProps.setProperty("format", "true");
        performanceInterceptor.setProperties(performanceInterceptorProps);
        interceptors.add(performanceInterceptor);

        Interceptor sqlExplainInterceptor = new SqlExplainInterceptor();
        Properties sqlExplainInterceptorProps = new Properties();
        sqlExplainInterceptorProps.setProperty("stopProceed", "false");
        sqlExplainInterceptor.setProperties(sqlExplainInterceptorProps);
        interceptors.add(sqlExplainInterceptor);
        I18nSqlProcessInterceptor i18NSqlProcessInterceptor = new I18nSqlProcessInterceptor();
        interceptors.add(i18NSqlProcessInterceptor);
        return interceptors.toArray(new Interceptor[]{});
    }

//    @Bean
//    public LocaleResolver localeResolver(){
//        return new HeliosLocaleResolver();
//    }

}
