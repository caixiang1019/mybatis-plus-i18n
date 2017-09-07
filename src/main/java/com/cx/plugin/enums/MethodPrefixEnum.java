package com.cx.plugin.enums;

/**
 * Created by caixiang on 2017/8/16.
 */
public enum MethodPrefixEnum {

    GET("get", "get方法前缀"),
    SET("set", "set方法前缀");

    MethodPrefixEnum(String prefix, String desc) {
        this.prefix = prefix;
        this.desc = desc;
    }

    private String prefix;

    private String desc;

    public void setPrefix(String prefix, String desc) {
        this.prefix = prefix;
        this.desc = desc;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDesc() {
        return desc;
    }
}
