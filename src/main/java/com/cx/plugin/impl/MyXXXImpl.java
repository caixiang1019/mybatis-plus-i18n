package com.cx.plugin.impl;

import com.cx.plugin.domain.ArtCompany;
import com.cx.plugin.domain.ArtDep;
import com.cx.plugin.persistence.service.ArtCompanyService;
import com.cx.plugin.persistence.service.ArtDepService;
import com.cx.plugin.service.BaseI18nService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caixiang on 2017/11/15.
 */
@Service
public class MyXXXImpl implements MyXXXX {

    @Autowired
    ArtCompanyService artCompanyService;
    @Autowired
    ArtDepService artDepService;
    @Autowired
    BaseI18nService2 baseI18nService2;

    @Override
    @Transactional
    public String test(ArtCompany artCompany) {
        artCompany.setDeleted(false);
        artCompany.setId(908508667691347970L);
//        artCompanyService.update(artCompany, new EntityWrapper<ArtCompany>().eq("age", 34).eq("name", "蔡翔"));
        artCompanyService.updateById(artCompany);
        baseI18nService2.insertOrUpdateI18n(artCompany.getI18n(), artCompany.getClass(), artCompany.getId());
        return "XXX";
    }

    @Override
    public String test2(List<ArtCompany> artCompanyList) {
        Map map = new HashMap<String, Integer>();
        for(ArtCompany artCompany : artCompanyList){
            try{

            }catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
        return null;
    }
}
