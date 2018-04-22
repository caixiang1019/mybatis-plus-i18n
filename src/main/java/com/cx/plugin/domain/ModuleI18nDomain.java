package com.cx.plugin.domain;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.enums.FieldStrategy;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * I18n基类
 * Created by caixiang on 2017/9/12.
 */

public abstract class ModuleI18nDomain implements Serializable {
    @TableId
    @JsonSerialize(
            using = ToStringSerializer.class
    )
    protected Long id;
    @TableField(
            value = "is_enabled",
            strategy = FieldStrategy.NOT_NULL
    )
    protected Boolean isEnabled;
    @TableField(
            value = "is_deleted",
            strategy = FieldStrategy.NOT_NULL
    )
    protected Boolean isDeleted;
    protected ZonedDateTime createdDate;
    protected Long createdBy;
    protected ZonedDateTime lastUpdatedDate;
    protected Long lastUpdatedBy;
    protected Integer versionNumber;
    
    public ModuleI18nDomain() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getIsEnabled() {
        return this.isEnabled;
    }

    public Boolean getIsDeleted() {
        return this.isDeleted;
    }

    public ZonedDateTime getCreatedDate() {
        return this.createdDate;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public ZonedDateTime getLastUpdatedDate() {
        return this.lastUpdatedDate;
    }

    public Long getLastUpdatedBy() {
        return this.lastUpdatedBy;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void setCreatedDate(ZonedDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setLastUpdatedDate(ZonedDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public void setLastUpdatedBy(Long lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
    
    public void setVersionNumber(Integer versionNumber){
        this.versionNumber = versionNumber;
    }
    
    public Integer getVersionNumber(){
        return this.versionNumber;
    }

    public String toString() {
        return "ModuleI18nDomain(id=" + this.getId()+", isEnabled=" + this.getIsEnabled() + ", isDeleted=" + this.getIsDeleted() + ", createdDate=" + this.getCreatedDate() + ", createdBy=" + this.getCreatedBy() + ", lastUpdatedDate=" + this.getLastUpdatedDate() + ", lastUpdatedBy=" + this.getLastUpdatedBy() + ", versionNumber=" + this.getVersionNumber() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof ModuleI18nDomain)) {
            return false;
        } else {
            ModuleI18nDomain other = (ModuleI18nDomain)o;
            if(!other.canEqual(this)) {
                return false;
            } else if(!super.equals(o)) {
                return false;
            } else {
                Object this$id = this.getId();
                Object other$id = other.getId();
                if(this$id == null) {
                    if(other$id != null) {
                        return false;
                    }
                } else if(!this$id.equals(other$id)) {
                    return false;
                }
                Object this$isEnabled = this.getIsEnabled();
                Object other$isEnabled = other.getIsEnabled();
                if(this$isEnabled == null) {
                    if(other$isEnabled != null) {
                        return false;
                    }
                } else if(!this$isEnabled.equals(other$isEnabled)) {
                    return false;
                }

                Object this$isDeleted = this.getIsDeleted();
                Object other$isDeleted = other.getIsDeleted();
                if(this$isDeleted == null) {
                    if(other$isDeleted != null) {
                        return false;
                    }
                } else if(!this$isDeleted.equals(other$isDeleted)) {
                    return false;
                }

                label71: {
                    Object this$createdDate = this.getCreatedDate();
                    Object other$createdDate = other.getCreatedDate();
                    if(this$createdDate == null) {
                        if(other$createdDate == null) {
                            break label71;
                        }
                    } else if(this$createdDate.equals(other$createdDate)) {
                        break label71;
                    }

                    return false;
                }

                label64: {
                    Object this$createdBy = this.getCreatedBy();
                    Object other$createdBy = other.getCreatedBy();
                    if(this$createdBy == null) {
                        if(other$createdBy == null) {
                            break label64;
                        }
                    } else if(this$createdBy.equals(other$createdBy)) {
                        break label64;
                    }

                    return false;
                }

                Object this$lastUpdatedDate = this.getLastUpdatedDate();
                Object other$lastUpdatedDate = other.getLastUpdatedDate();
                if(this$lastUpdatedDate == null) {
                    if(other$lastUpdatedDate != null) {
                        return false;
                    }
                } else if(!this$lastUpdatedDate.equals(other$lastUpdatedDate)) {
                    return false;
                }

                Object this$lastUpdatedBy = this.getLastUpdatedBy();
                Object other$lastUpdatedBy = other.getLastUpdatedBy();
                if(this$lastUpdatedBy == null) {
                    if(other$lastUpdatedBy != null) {
                        return false;
                    }
                } else if(!this$lastUpdatedBy.equals(other$lastUpdatedBy)) {
                    return false;
                }
                Object this$versionNumber = this.getVersionNumber();
                Object other$versionNumber = other.getVersionNumber();
                if(this$versionNumber == null) {
                    if(other$versionNumber != null) {
                        return false;
                    }
                } else if(!this$versionNumber.equals(other$versionNumber)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof ModuleI18nDomain;
    }

    @Override
    public int hashCode() {
//        int PRIME = true;
        int result = 1;
        result = result * 59 + super.hashCode();
        Object $id = this.getId();
        result = result * 59 + ($id == null?43:$id.hashCode());
        Object $isEnabled = this.getIsEnabled();
        result = result * 59 + ($isEnabled == null?43:$isEnabled.hashCode());
        Object $isDeleted = this.getIsDeleted();
        result = result * 59 + ($isDeleted == null?43:$isDeleted.hashCode());
        Object $createdDate = this.getCreatedDate();
        result = result * 59 + ($createdDate == null?43:$createdDate.hashCode());
        Object $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null?43:$createdBy.hashCode());
        Object $lastUpdatedDate = this.getLastUpdatedDate();
        result = result * 59 + ($lastUpdatedDate == null?43:$lastUpdatedDate.hashCode());
        Object $lastUpdatedBy = this.getLastUpdatedBy();
        result = result * 59 + ($lastUpdatedBy == null?43:$lastUpdatedBy.hashCode());
        Object $versionNumber = this.getVersionNumber();
        result = result * 59 + ($versionNumber == null?43:$versionNumber.hashCode());
        return result;
    }
}
