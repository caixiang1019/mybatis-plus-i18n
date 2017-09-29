package com.cx.plugin.controller;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.cx.plugin.domain.ArtCompany;
import com.cx.plugin.domain.ArtDep;
import com.cx.plugin.domain.TestArtCompany;
import com.cx.plugin.persistence.mapper.ArtCompanyMapper;
import com.cx.plugin.persistence.mapper.ArtDepMapper;
import com.cx.plugin.persistence.service.ArtCompanyService;
import com.cx.plugin.persistence.service.ArtCompanyService2;
import com.cx.plugin.persistence.service.ArtDepService;
import com.cx.plugin.service.BaseI18nService2;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Created by caixiang on 2017/8/15.
 */
@RestController
@RequestMapping(value = "test")
public class TestController {

    @Autowired
    private ArtDepMapper artDepMapper;
    @Autowired
    private ArtCompanyMapper artCompanyMapper;

    private ArtDepService artDepService;

    private ArtCompanyService artCompanyService;

    @Autowired
    private ArtCompanyService2 artCompanyService2;

    @Autowired
    private BaseI18nService2 baseI18nService2;

    public TestController(ArtDepService artDepService, ArtCompanyService artCompanyService) {
        this.artDepService = artDepService;
        this.artCompanyService = artCompanyService;
    }


    @RequestMapping(value = "convertTestArtCompany", method = RequestMethod.GET)
    public TestArtCompany convertTestArtCompanyEntity(@RequestParam(value = "id") Long id) {
        TestArtCompany testArtCompany = new TestArtCompany();
        testArtCompany.setTestArtName("testArtCompany");
        testArtCompany.setId(id);
        return baseI18nService2.convertOneByLocale(testArtCompany);
    }

    @RequestMapping(value = "convert", method = RequestMethod.GET)
    public ArtDep convertEntity(@RequestParam(value = "id") Long id) {
        ArtDep artDep2 = new ArtDep();
        artDep2.setId(id);
        return baseI18nService2.convertOneByLocale(artDep2);
    }

    @RequestMapping(value = "convertList", method = RequestMethod.GET)
    public List<ArtDep> convertEntity(@RequestParam(value = "id") Long id, @RequestParam(value = "id2") Long id2) {
        ArtDep artDep2 = new ArtDep();
        artDep2.setId(id);
        ArtDep artDep3 = new ArtDep();
        artDep3.setId(id2);
        List<ArtDep> list = new ArrayList<>();
        list.add(artDep2);
        list.add(artDep3);
        return baseI18nService2.convertListByLocale(list);
    }

    @RequestMapping(value = "getI18nInfo", method = RequestMethod.GET)
    public String getI18nInfo(@RequestParam(value = "id") Long id) {

        Map<String, ArtCompany> map = baseI18nService2.getI18nInfo(id, ArtCompany.class);
        return map.toString();
    }

    @RequestMapping(value = "selectById", method = RequestMethod.GET)
    public String selectByIdArtCompany(@RequestParam(value = "id") Long id) {
        ArtCompany artCompany = artCompanyService.selectById(id);

        return artCompany.toString();
    }

    @RequestMapping(value = "selectById2", method = RequestMethod.GET)
    public String selectByIdArtDep(@RequestParam(value = "id") Long id) {
        ArtDep artDep = artDepService.selectById(id);

        return artDep.toString();
    }

    @RequestMapping(value = "selectOne3", method = RequestMethod.GET)
    public String selectOne3(@RequestParam(value = "id") Long id) {
        ArtCompany artCompany2 = new ArtCompany();
        artCompany2.setId(id);
        artCompany2.setName("蔡翔");
        ArtCompany artCompany = artCompanyService.selectOne(new EntityWrapper<>(artCompany2));

        if (artCompany == null)
            return null;
        return artCompany.toString();
    }

    @RequestMapping(value = "selectOne", method = RequestMethod.GET)
    public String selectOne(@RequestParam(value = "id") Long id) {
        ArtCompany artCompany = artCompanyService.selectOne(new EntityWrapper<ArtCompany>().eq("id", id).eq("name", "蔡翔"));

        if (artCompany == null)
            return null;
        return artCompany.toString();
    }

    @RequestMapping(value = "selectOne2", method = RequestMethod.GET)
    public String selectOne2(@RequestParam(value = "id") Long id) {
        ArtCompany artCompany = new ArtCompany();
        artCompany.setName("蔡翔");
        artCompany = artCompanyMapper.selectOne(artCompany);
        if (artCompany == null)
            return null;
        return artCompany.toString();
    }

    @RequestMapping(value = "selectList", method = RequestMethod.GET)
    public String selectArtDep(@RequestParam(value = "age") Integer age) {
        ArtDep artDep = new ArtDep();
        artDep.setAge(age);
        List<ArtDep> artDepList = artDepService.selectList(new EntityWrapper<>(artDep).eq("is_deleted", false).where("is_deleted = false"));

        return artDepList.toString();
    }

    //selectCount，selectMaps，selectObjs
    @RequestMapping(value = "selectCount", method = RequestMethod.GET)
    public Integer selectCount(@RequestParam(value = "id") Long id) {
        int t = artDepService.selectCount(new EntityWrapper<ArtDep>().eq("age", 34).eq("id", id));

        return t;
    }

    @RequestMapping(value = "selectMaps", method = RequestMethod.GET)
    public String selectMaps(@RequestParam(value = "id") Long id) {
        List<Map<String, Object>> list = artDepService.selectMaps(new EntityWrapper<ArtDep>().eq("age", 34).eq("id", id));

        return list.toString();
    }

    @RequestMapping(value = "selectObjs", method = RequestMethod.GET)
    public String selectObjs(@RequestParam(value = "id") Long id) {
        List<Object> objectList = artDepService.selectObjs(new EntityWrapper<ArtDep>().eq("age", 34).eq("id", id));

        return objectList.toString();
    }


    @RequestMapping(value = "selectAll2", method = RequestMethod.GET)
    public String select2ArtDep() {
        List<ArtDep> artDepList = artDepService.selectList(new EntityWrapper<ArtDep>().eq("name", "caixiang"));

        return artDepList.toString();
    }

    @RequestMapping(value = "add", method = RequestMethod.POST)
    public Boolean addArtCompany(@RequestBody ArtCompany artCompany) {
        artCompany.setDeleted(false);
        return artCompanyService.insert(artCompany);
    }

    @RequestMapping(value = "addAll", method = RequestMethod.POST)
    public Boolean addAllArtCompany(@RequestBody ArtCompany artCompany) {
        artCompany.setDeleted(false);
        return artCompanyService.insertAllColumn(artCompany);
    }

    @RequestMapping(value = "addArtDep", method = RequestMethod.POST)
    public Boolean addArtDep(@RequestBody ArtDep artDep) {
        artDep.setDeleted(false);
        artDep.setCreatedDate(DateTime.now());
        return artDepService.insert(artDep);
    }

    @RequestMapping(value = "add2", method = RequestMethod.POST)
    public Boolean addArtCompany2(@RequestBody ArtCompany artCompany) {
        return artCompanyService.insert(artCompany);
    }

    @RequestMapping(value = "update", method = RequestMethod.PUT)
    public Boolean updateArtCompany(@RequestBody ArtCompany artCompany) {
        artCompany.setDeleted(false);
        return artCompanyService.updateAllColumnById(artCompany);
    }

    @RequestMapping(value = "updateArtDep2", method = RequestMethod.PUT)
    public Boolean updateArtDep2(@RequestBody ArtDep artDep) {
        return artDepService.updateById(artDep);
    }

    @RequestMapping(value = "updateArtDep", method = RequestMethod.PUT)
    public Boolean updateArtDep(@RequestBody ArtDep artDep) {
        return artDepService.updateAllColumnById(artDep);
    }

    @RequestMapping(value = "update2", method = RequestMethod.PUT)
    public Boolean updateArtCompany2(@RequestBody ArtCompany artCompany) {
        artCompany.setDeleted(false);
        ArtCompany artCompany1 = new ArtCompany();
        artCompany1.setAge(34);
        artCompany1.setName("蔡翔");
        artCompany1.setDeleted(false);
        return artCompanyService.update(artCompany, new EntityWrapper<>(artCompany1));
    }
    @RequestMapping(value = "update5", method = RequestMethod.PUT)
    public Boolean updateArtCompany5(@RequestBody ArtCompany artCompany) {
        artCompany.setDeleted(false);
        ArtCompany artCompany1 = new ArtCompany();
        artCompany1.setAge(34);
        artCompany1.setName("蔡翔");
        return artCompanyService.update(artCompany, new EntityWrapper<>(artCompany1).eq("is_deleted", false));
    }

    @RequestMapping(value = "update6", method = RequestMethod.PUT)
    public Boolean updateArtCompany6(@RequestBody ArtCompany artCompany) {
        artCompany.setDeleted(false);
        return artCompanyService.update(artCompany, new EntityWrapper<ArtCompany>().eq("age",34).eq("is_deleted", false));
    }

    @RequestMapping(value = "update4", method = RequestMethod.PUT)
    public Boolean updateArtCompany4(@RequestBody ArtCompany artCompany) {
        artCompany.setDeleted(false);
        return artCompanyService.update(artCompany, new EntityWrapper<ArtCompany>().eq("age", 34).eq("name", "蔡翔"));
    }

    @RequestMapping(value = "update3", method = RequestMethod.PUT)
    public Boolean updateArtDep3(@RequestBody ArtDep artDep) {
        return artDepService.updateById(artDep);
    }


    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public Boolean deleteArtCompany(@RequestParam(value = "id") Long id) {
        return artCompanyService.deleteById(id);
    }

    @RequestMapping(value = "delete2", method = RequestMethod.DELETE)
    public Boolean deleteArtDep2(@RequestBody ArtDep artDep) {
        return artDepService.delete(new EntityWrapper<>(artDep));
    }

    @RequestMapping(value = "delete3", method = RequestMethod.DELETE)
    public Boolean deleteByMap() {
        Map m = new HashMap<String, String>();

        m.put("dep_code", "代号133");
        m.put("dep_name", "蔡翔");
        return artDepService.deleteByMap(m);
    }

    @RequestMapping(value = "logicDelete", method = RequestMethod.DELETE)
    public Boolean logicDelete(@RequestParam(value = "id") Long id) {
        artDepMapper.updateLogicDelete(id);
        return true;
    }


    @RequestMapping(value = "add3", method = RequestMethod.POST)
    public Boolean addArtCompany3(@RequestBody ArtCompany artCompany) {
        return artCompanyService2.insert(artCompany);
    }

    @GetMapping(value = "selectPage")
    public List<ArtDep> selectPage(@RequestParam Integer age){
        ArtDep artDep = new ArtDep();
        artDep.setAge(age);
        artDep.setCreatedDate(null);
        Page page = new Page(2,3);
        return artDepMapper.selectPage(page, new EntityWrapper<>(artDep));
    }

    @GetMapping(value = "selectPage2")
    public List<ArtDep> selectPage2(@RequestParam Integer age){
        Page page = new Page(2,3);
        return artDepMapper.selectPage(page, new EntityWrapper<ArtDep>().where("is_deleted = false"));
    }

    @GetMapping(value = "selectPageMap")
    public String selectPage3(@RequestParam Integer age){
        ArtDep artDep = new ArtDep();
        artDep.setAge(age);
        artDep.setDepCode("XZB");
        artDep.setCreatedDate(null);
        Page page = new Page(2,3);

        List<Map<String,Object>> list = artDepMapper.selectMapsPage(page, new EntityWrapper<>(artDep).eq("is_deleted", false).where("is_deleted = false"));
        return list.toString();
    }

    @GetMapping(value = "noneParameterTest")
    public String noneParameterTest(){
        Locale locale = LocaleContextHolder.getLocale();
        System.out.println(locale.toString());
        return artDepMapper.testId();
    }

    @GetMapping(value ="selectListBaseTableInfoWithI18n")
    public String selectListBaseTableInfoWithI18n(){
        List<Long> idList = new ArrayList<>();
        idList.add(908508483712397314L);
        idList.add(908508667691347970L);
        List<ArtCompany> artCompanyList = baseI18nService2.selectListBaseTableInfoWithI18n(idList, ArtCompany.class);
        return artCompanyList.toString();
    }

    @GetMapping(value ="selectOneBaseTableInfoWithI18n")
    public String selectOneBaseTableInfoWithI18n(){
        ArtCompany artCompany = baseI18nService2.selectOneBaseTableInfoWithI18n(908508483712397314L, ArtCompany.class);
        return artCompany.toString();
    }

}
