package com.cx.plugin.domain;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.FieldStrategy;
import com.cx.plugin.annotations.I18nField;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;


/**
 * @author Caixiang
 * @since 2017-08-15
 */
@NoArgsConstructor
@TableName("art_dep")
public class ArtDep extends BaseI18nDomain {

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
    protected ZonedDateTime createdDate;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String toString() {
        return "ArtDep{" +
                "id=" + id +
                ", depName='" + depName + '\'' +
                ", depCode='" + depCode + '\'' +
                ", depCountry='" + depCountry + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", age=" + age +
                ", isDeleted=" + isDeleted +
                ", createdDate=" + createdDate +
                '}';
    }
}
