package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.Cabinet;
import com.share.device.domain.Station;
import com.share.device.mapper.StationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StationServiceImpl extends ServiceImpl<StationMapper, Station> implements IStationService {
    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private ICabinetService iCabinetService;

    @Override
    public List<Station> selectStationList(Station station) {
        //查询所有站点，用集合list存储
        List<Station> list = stationMapper.selectStationList(station);
        //去除cabinetId这一列存储在集合cabintIdList中
        List<Long> cabinetIdList = list.stream().map(Station::getCabinetId).collect(Collectors.toList());
        //用于存储cabinetId到cabinetNo的map映射
        Map<Long,String> cabinetIdTocabinetNoMap=new HashMap<>();

        if (!CollectionUtils.isEmpty(cabinetIdList)){

            List<Cabinet> cabinetList = iCabinetService.list(new LambdaQueryWrapper<Cabinet>().in(Cabinet::getId, cabinetIdList));
            cabinetIdTocabinetNoMap =cabinetList.stream().collect(Collectors.toMap(Cabinet::getId,Cabinet::getCabinetNo));
        }

        for (Station item:list){
            item.setCabinetNo(cabinetIdTocabinetNoMap.get(item.getCabinetId()));
        }


        return list;
    }
}
