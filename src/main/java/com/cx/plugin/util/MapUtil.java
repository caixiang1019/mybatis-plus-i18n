package com.cx.plugin.util;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by caixiang on 2017/9/25.
 */
public class MapUtil {

    /**
     * 使用 Map按key进行排序
     *
     * @param map
     * @return
     */
    public static Map<String, String> sortMapByKey(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return map;
        }

        Map<String, String> sortMap = new TreeMap<>(String::compareTo);

        sortMap.putAll(map);

        return sortMap;
    }

}
