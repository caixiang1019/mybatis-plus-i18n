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
 * @author Caixiang
 * @since 2017-08-15
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("art_dep")
public class ArtDep extends BaseI18nDomain {

    @TableId
    private Long id;
    @I18nField
    private String depName;
    @I18nField
    private String depCode;
    @I18nField
    private String depCountry;
    private String phone;
    private String address;
    private Integer age;
    @TableField(value = "is_deleted", strategy = FieldStrategy.NOT_NULL)
    private Boolean isDeleted;

}
