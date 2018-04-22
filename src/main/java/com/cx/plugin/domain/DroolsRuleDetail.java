package com.cx.plugin.domain;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by caixiang on 2018/2/28.
 */
@TableName("atl_drools_rule_detail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DroolsRuleDetail {

    @TableId
    private Long id;

    @TableField(value = "drools_rule_detail_oid")
    private UUID droolsRuleDetailOID;

    @TableField(value = "rule_condition_oid")
    private UUID ruleConditionOID;

    @TableField(value = "rule_condition_approver_oid")
    private UUID ruleConditionApproverOID;

    @TableField(value = "drools_rule_detail_value")
    private String droolsRuleDetailValue;
}
