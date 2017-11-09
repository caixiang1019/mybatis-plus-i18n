package com.cx.plugin.domain;

import com.baomidou.mybatisplus.annotations.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caixiang on 2017/8/15.
 */
@Data
@NoArgsConstructor
public class BaseI18nDomain {
    @TableField(exist = false)
    private Map<String, List<Map<String, String>>> i18n;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseI18nDomain that = (BaseI18nDomain) o;

        return i18n != null ? i18n.equals(that.i18n) : that.i18n == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (i18n != null ? i18n.hashCode() : 0);
        return result;
    }
}
