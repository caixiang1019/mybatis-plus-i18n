package com.cx.plugin.domain;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.FieldStrategy;
import com.cx.plugin.annotations.I18nField;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;


/**
 * @author Caixiang
 * @since 2017-08-15
 */
@NoArgsConstructor
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
    @TableField(strategy = FieldStrategy.NOT_NULL)
    private Boolean isDeleted;
    private DateTime createdDate = DateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDepName() {
        return depName;
    }

    public void setDepName(String depName) {
        this.depName = depName;
    }

    public String getDepCode() {
        return depCode;
    }

    public void setDepCode(String depCode) {
        this.depCode = depCode;
    }

    public String getDepCountry() {
        return depCountry;
    }

    public void setDepCountry(String depCountry) {
        this.depCountry = depCountry;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(DateTime createdDate) {
        this.createdDate = createdDate;
    }
}
