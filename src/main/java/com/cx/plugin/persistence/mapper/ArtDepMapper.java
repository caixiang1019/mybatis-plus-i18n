package com.cx.plugin.persistence.mapper;


import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.cx.plugin.domain.ArtDep;

/**
 * @author Caixiang
 * @since 2017-08-15
 */
public interface ArtDepMapper extends BaseMapper<ArtDep> {

    void updateLogicDelete(Long id);

    String testId();

}