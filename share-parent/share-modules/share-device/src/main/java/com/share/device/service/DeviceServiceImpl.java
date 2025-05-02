package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.device.domain.*;
import com.share.device.mapper.CabinetMapper;
import com.share.device.mapper.PowerBankMapper;
import com.share.device.mapper.StationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DeviceServiceImpl implements IDeviceService{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IStationService stationService;

    @Autowired
    private ICabinetService cabinetService;

    @Override
    public List<StationVo> nearbyStation(String latitude, String longitude, Integer radius) {
        GeoJsonPoint geoJsonPoint=new GeoJsonPoint(Double.parseDouble(longitude),Double.parseDouble(latitude));
        Distance distance = new Distance(radius, Metrics.KILOMETERS);
        Circle circle = new Circle(geoJsonPoint, distance);

        Query query = Query.query(Criteria.where("location").withinSphere(circle));
        List<StationLocation> stationLocationList = this.mongoTemplate.find(query, StationLocation.class);
        if (CollectionUtils.isEmpty(stationLocationList)){
            return null;
        }
        //获取站点id列表
        List<Long> stationIdList = stationLocationList.stream().map(StationLocation::getStationId).toList();
        //获取站点列表
        List<Station> stationList = stationService.list(
                new LambdaQueryWrapper<Station>()
                .in(Station::getId, stationService)
                .isNotNull(Station::getCabinetId));
        //获取柜机Id集合
        List<Long> CabinetIdList = stationList.stream().map(Station::getCabinetId).toList();
        //获取柜机id对柜机的映射集合，键值对，键为id，值为柜机实体；
        Map<Long, Cabinet> cabinetIdToCabinetMap= cabinetService.listByIds(CabinetIdList)
                .stream().collect(Collectors.toMap(Cabinet::getId, cabinet -> cabinet));

        // 将 stationList 中的每个 Station 对象转换为 StationVo，附带可借/可还状态
        List<StationVo> stationVoList = stationList.stream()
                .map(station -> {
                    StationVo stationVo = new StationVo();

                    // 复制基础属性
                    BeanUtils.copyProperties(station, stationVo);

                    // 根据 station 的 cabinetId 从缓存中获取对应的 Cabinet 信息
                    Cabinet cabinet = cabinetIdToCabinetMap.get(station.getCabinetId());

                    if (cabinet != null) {
                        // 可用充电宝数量 > 0，表示可借用
                        stationVo.setIsUsable(cabinet.getAvailableNum() > 0 ? "1" : "0");

                        // 空闲插槽数量 > 0，表示可归还
                        stationVo.setIsReturn(cabinet.getFreeSlots() > 0 ? "1" : "0");
                    } else {
                        // 若 cabinet 信息不存在，默认为不可借不可还
                        stationVo.setIsUsable("0");
                        stationVo.setIsReturn("0");
                    }

                    return stationVo;
                })
                .collect(Collectors.toList());  // 收集为列表



        return stationVoList;
    }

}
