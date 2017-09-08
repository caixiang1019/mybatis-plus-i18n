package com.cx.plugin.domain;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.FieldStrategy;
import com.cx.plugin.annotations.I18nField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


/**
 * Created by caixiang on 2017/8/16.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("art_company")
public class ArtCompany extends BaseI18nDomain {

    @TableId
    private Long id;
    @I18nField
    private String name;
    @I18nField
    private String code;
    private String phone;
    private String address;
    private Integer age;
    @TableField(value = "is_deleted", validate = FieldStrategy.NOT_NULL)
    private Boolean isDeleted;

}
