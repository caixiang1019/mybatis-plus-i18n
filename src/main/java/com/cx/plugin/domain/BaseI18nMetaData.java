package com.cx.plugin.domain;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by caixiang on 2017/8/22.
 */

@Data
@Builder
public class BaseI18nMetaData implements Serializable {

    private String field;
    private String language;
    private String value;

}
