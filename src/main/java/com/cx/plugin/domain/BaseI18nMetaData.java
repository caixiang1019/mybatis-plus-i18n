package com.cx.plugin.domain;

import lombok.Builder;
import lombok.Data;

/**
 * Created by caixiang on 2017/8/22.
 */

@Data
@Builder
public class BaseI18nMetaData {

    private String field;
    private String language;
    private String value;

}
