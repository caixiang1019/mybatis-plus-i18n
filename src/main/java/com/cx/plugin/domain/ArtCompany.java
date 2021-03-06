package com.cx.plugin.domain;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.FieldStrategy;
import com.cx.plugin.annotations.I18nField;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.ZonedDateTime;


/**
 * Created by caixiang on 2017/8/16.
 */
@Data
@ToString(callSuper = true)
@NoArgsConstructor
@TableName("art_company")
public class ArtCompany extends BaseI18nDomain {

    private Long id;
    @I18nField
    private String name;
    @I18nField
    private String code;
    private String phone;
    private String address;
    private Integer age;
    @TableField(strategy = FieldStrategy.NOT_NULL)
    private boolean isDeleted;
    protected ZonedDateTime createdDate;
}
