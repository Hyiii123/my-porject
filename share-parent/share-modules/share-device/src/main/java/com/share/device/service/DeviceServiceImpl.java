package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.SecurityConstants;
import com.share.device.domain.*;
import com.share.rules.RemoteFeeRuleService;
import com.share.rules.domain.FeeRule;
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


    @Autowired
    private RemoteFeeRuleService remoteFeeRuleService;

    @Autowired
    private IMapService mapService;

    @Override
    public List<StationVo> nearbyStation(String latitude, String longitude, Integer radius) {
        //坐标，确定中心点
        // GeoJsonPoint(double x, double y) x 表示经度，y 表示纬度。
        GeoJsonPoint geoJsonPoint = new GeoJsonPoint(
                Double.parseDouble(longitude),
                Double.parseDouble(latitude));
        //画圈的半径,50km范围
        Distance d = new Distance(radius, Metrics.KILOMETERS);
        //画了一个圆圈
        Circle circle = new Circle(geoJsonPoint, d);
        //条件排除自己
        Query query = Query.query(Criteria.where("location").withinSphere(circle));
        List<StationLocation> stationLocationList = this.mongoTemplate.find(
                query, StationLocation.class);
        if (CollectionUtils.isEmpty(stationLocationList)) return null;

        //组装数据
        List<Long> stationIdList =stationLocationList
                .stream()
                .map(StationLocation::getStationId)
                .collect(Collectors.toList());
        //获取站点列表
        List<Station> stationList = stationService
                .list(new LambdaQueryWrapper<Station>()
                .in(Station::getId, stationIdList)
                .isNotNull(Station::getCabinetId));

        //获取柜机id列表
        List<Long> cabinetIdList = stationList
                .stream()
                .map(Station::getCabinetId)
                .collect(Collectors.toList());
        //获取柜机id与柜机信息Map
        Map<Long, Cabinet> cabinetIdToCabinetMap = cabinetService
                .listByIds(cabinetIdList)
                .stream().collect(Collectors
                .toMap(Cabinet::getId, Cabinet -> Cabinet));

        //获取柜机id列表
        List<Long> feeRuleIdList = stationList.stream()
                .map(Station::getFeeRuleId)
                .collect(Collectors.toList());
        //获取柜机id与柜机信息Map
        Map<Long, FeeRule> feeRuleIdToFeeRuleMap = remoteFeeRuleService
                .getFeeRuleList(feeRuleIdList, SecurityConstants.INNER)
                .getData().stream()
                .collect(Collectors
                .toMap(FeeRule::getId, FeeRule -> FeeRule));

        List<StationVo> stationVoList = new ArrayList<>();
        stationList.forEach(item -> {
            StationVo stationVo = new StationVo();
            BeanUtils.copyProperties(item, stationVo);
            // 计算距离
            Double distance = mapService.calculateDistance(longitude, latitude,
                            item.getLongitude().toString(),
                            item.getLatitude().toString());
            stationVo.setDistance(distance);

            // 获取柜机信息
            Cabinet cabinet = cabinetIdToCabinetMap.get(item.getCabinetId());
            //可用充电宝数量大于0，可借用
            if(cabinet.getAvailableNum() > 0) {
                stationVo.setIsUsable("1");
            } else {
                stationVo.setIsUsable("0");
            }
            // 获取空闲插槽数量大于0，可归还
            if (cabinet.getFreeSlots() > 0) {
                stationVo.setIsReturn("1");
            } else {
                stationVo.setIsReturn("0");
            }

            // 获取费用规则
            FeeRule feeRule = feeRuleIdToFeeRuleMap.get(item.getFeeRuleId());
            stationVo.setFeeRule(feeRule.getDescription());

            stationVoList.add(stationVo);
        });
        return stationVoList;
    }
    @Override
    public StationVo getStation(Long id, String latitude, String longitude) {
        Station station = stationService.getById(id);
        StationVo stationVo = new StationVo();
        BeanUtils.copyProperties(station, stationVo);
        // 计算距离
        Double distance = mapService.calculateDistance(
                longitude, latitude,
                station.getLongitude().toString(),
                station.getLatitude().toString());
        stationVo.setDistance(distance);

        // 获取柜机信息
        Cabinet cabinet = cabinetService.getById(station.getCabinetId());
        //可用充电宝数量大于0，可借用
        if(cabinet.getAvailableNum() > 0) {
            stationVo.setIsUsable("1");
        } else {
            stationVo.setIsUsable("0");
        }
        // 获取空闲插槽数量大于0，可归还
        if (cabinet.getFreeSlots() > 0) {
            stationVo.setIsReturn("1");
        } else {
            stationVo.setIsReturn("0");
        }

        // 获取费用规则
        FeeRule feeRule = remoteFeeRuleService.getFeeRule(
                station.getFeeRuleId(), SecurityConstants.INNER)
                .getData();
        stationVo.setFeeRule(feeRule.getDescription());
        return stationVo;
    }
}
