package com.cx.plugin.util;

import com.baomidou.mybatisplus.annotations.TableName;
import com.cx.plugin.domain.BaseI18nMetaData;
import org.junit.Test;

import java.io.Serializable;

import static com.cx.plugin.util.PackageScannerUtil.*;

/**
 * Created by caixiang on 2017/9/8.
 */
public class PackageScannerUtilTest {

    @Test
    public void testGetClassFromSuperClass() {
        getClassFromSuperClass("com.cx.plugin.domain", BaseI18nMetaData.class);
        getClassWithAnnotation("com.cx.plugin.domain", TableName.class);
        getClassImplementClass("com.cx.plugin.domain", Serializable.class);
    }

}
