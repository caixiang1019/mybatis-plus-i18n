package com.cx.plugin.domain;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Sql segments
 * Created by caixiang on 2017/8/23.
 */
@Data
@Builder
public class SqlSegment implements Serializable {
    private String sqlHeader;
    private String fieldsStr;
    private String middleStr;
    private String valuesStr;

}
