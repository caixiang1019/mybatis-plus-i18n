package com.cx.plugin.domain;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * Created by caixiang on 2018/2/28.
 */
@Data
@ToString
public class RuleResult {

    private List<String> originRuleList;
    private Integer count;

}
