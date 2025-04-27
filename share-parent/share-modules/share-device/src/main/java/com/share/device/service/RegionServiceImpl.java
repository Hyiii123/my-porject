package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.Region;
import com.share.device.mapper.RegionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


import java.util.List;
@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements IRegionService {
    @Autowired
    private RegionMapper regionMapper;

    @Override
    public List<Region> treeSelect(String parentCode) {
        List<Region> regionList = regionMapper.selectList(new LambdaQueryWrapper<Region>().eq(Region::getParentCode, parentCode));
        if (!CollectionUtils.isEmpty(regionList)) {
            regionList.forEach(item -> {
                Long count = regionMapper.selectCount(new LambdaQueryWrapper<Region>().eq(Region::getParentCode, item.getParentCode()));
                if (count > 0) {
                    item.setHasChildren(true);
                } else {
                    item.setHasChildren(false);
                }
            });
        }
        return regionList;
    }

    @Override
    public String getNameByCode(String Code) {
        if (StringUtils.isEmpty(Code)) {
            return "";
        }
        Region region = regionMapper.selectOne(new LambdaQueryWrapper<Region>().eq(Region::getCode, Code).select(Region::getName));
        if (region!=null){
            return region.getName();
        }
        return "";
    }
}
