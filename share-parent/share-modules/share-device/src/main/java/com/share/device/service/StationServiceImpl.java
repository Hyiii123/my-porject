package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.Cabinet;
import com.share.device.domain.Station;
import com.share.device.domain.StationLocation;
import com.share.device.mapper.StationMapper;
import com.share.device.repository.StationLocationRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StationServiceImpl extends ServiceImpl<StationMapper, Station> implements IStationService {
    @Autowired
    private StationMapper stationMapper;

    @Autowired
    private ICabinetService iCabinetService;

    @Autowired
    private IRegionService iRegionService;

    @Autowired
    private StationLocationRepository stationLocationRepository;

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

    @Override
    public int saveStation(Station station) {
        String CityName = iRegionService.getNameByCode(station.getCityCode());
        String DistrictName = iRegionService.getNameByCode(station.getDistrictCode());
        String Province = iRegionService.getNameByCode(station.getProvinceCode());
        station.setFullAddress(CityName+DistrictName+Province+station.getAddress());
        this.save(station);
        //同步到mongodb

        StationLocation stationLocation=new StationLocation();

        stationLocation.setId(ObjectId.get().toString());

        stationLocation.setStationId(station.getId());
        stationLocation.setLocation(new GeoJsonPoint(
                station.getLatitude().doubleValue(),
                station.getLongitude().doubleValue()));

        stationLocation.setCreateTime(new Date());
        stationLocationRepository.save(stationLocation);
        return 1;
    }

    @Override
    public int updateStation(Station station) {
        String CityName = iRegionService.getNameByCode(station.getCityCode());
        String DistrictName = iRegionService.getNameByCode(station.getDistrictCode());
        String Province = iRegionService.getNameByCode(station.getProvinceCode());
        station.setFullAddress(CityName+DistrictName+Province+station.getAddress());
        this.updateById(station);

        //同步数据到mongodb
        StationLocation stationLocation = stationLocationRepository.getByStationId(station.getId());
        stationLocation.setCreateTime(new Date());
        stationLocation.setLocation(new GeoJsonPoint(
                station.getLatitude().doubleValue(),
                station.getLongitude().doubleValue()
        ));
        stationLocationRepository.save(stationLocation);
        return 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int setData(Station station) {
        this.updateById(station);

        //更正柜机使用状态
        Cabinet cabinet = iCabinetService.getById(station.getCabinetId());
        cabinet.setStatus("1");
        iCabinetService.updateById(cabinet);
        return 1;
    }

    @Override
    public void updateData() {
        List<Station> stationList = this.list();
        for (Station station:stationList){
            StationLocation stationLocation = stationLocationRepository.getByStationId(station.getId());

            if (stationLocation==null){
                stationLocation=new StationLocation();
                stationLocation.setId(ObjectId.get().toString());

                stationLocation.setStationId(station.getId());

                stationLocation.setLocation(new GeoJsonPoint(
                        station.getLatitude().doubleValue(),
                        station.getLongitude().doubleValue()
                ));

                stationLocation.setCreateTime(new Date());
                stationLocationRepository.save(stationLocation);
            }

        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeByIds(Collection<?> list) {
        for (Object id : list) {
            stationLocationRepository.deleteByStationId(Long.parseLong(id.toString()));
        }
        return super.removeByIds(list);
    }

}
