package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.common.core.constant.SecurityConstants;
import com.share.common.core.domain.R;
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
public class DeviceServiceImpl implements IDeviceService {

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
    public List<StationVo> nearbyStation(String longitude, String latitude, Integer radius) {
        GeoJsonPoint geoJsonPoint = new GeoJsonPoint(
                Double.parseDouble(longitude), Double.parseDouble(latitude));

        Distance distance = new Distance(radius, Metrics.KILOMETERS);

        Circle circle = new Circle(geoJsonPoint, distance);

        Query query = Query.query(Criteria.where("location").within(circle));

        List<StationLocation> stationLocations = this.mongoTemplate
                .find(query, StationLocation.class);

        if (CollectionUtils.isEmpty(stationLocations))
            return null;

        List<Long> stationIdList = stationLocations
                .stream().map(StationLocation::getStationId).toList();
        List<Station> stationList = stationService.list(
                new LambdaQueryWrapper<Station>()
                        .in(Station::getId, stationIdList)
                        .isNotNull(Station::getCabinetId));

        List<Long> CabinetIdList = stationList.stream()
                .map(Station::getCabinetId).toList();

        Map<Long, Cabinet> CabinetIdToCabinet = cabinetService.listByIds(CabinetIdList)
                .stream().collect(Collectors.toMap(Cabinet::getId, Cabinet -> Cabinet));

        //获取费用规则id列表
        List<Long> feeRuleIdList = stationList.stream()
                .map(Station::getFeeRuleId).toList();
        //获取费用规则id与费用规则信息Map
        Map<Long, FeeRule> feeRuleIdToFeeRuleMap = remoteFeeRuleService
                .getFeeRuleList(feeRuleIdList, SecurityConstants.INNER)
                .getData().stream().collect(
                        Collectors.toMap(FeeRule::getId, FeeRule -> FeeRule));

        ArrayList<StationVo> stationvoList = new ArrayList<>();

        stationList.forEach(item -> {
                    StationVo stationVo = new StationVo();
                    BeanUtils.copyProperties(item, stationVo);

                    Double d = mapService.calculateDistance(longitude, latitude,
                            item.getLongitude().toString(),
                            item.getLatitude().toString());

                    stationVo.setDistance(d);

                    Cabinet cabinet = CabinetIdToCabinet.get(item.getCabinetId());

                    if (cabinet.getAvailableNum() > 0) {
                        stationVo.setIsUsable("1");
                    } else {
                        stationVo.setIsUsable("0");
                    }
                    if (cabinet.getFreeSlots() > 0) {
                        stationVo.setIsReturn("1");
                    } else {
                        stationVo.setIsReturn("0");
                    }
                    FeeRule feeRule = feeRuleIdToFeeRuleMap.get(feeRuleIdList);


                    stationVo.setFeeRule(feeRule.getDescription());

                    stationvoList.add(stationVo);

                }
        );

        return stationvoList;

    }

    @Override
    public StationVo getStation(Long id, String latitude, String longitude) {
        Station station = stationService.getById(id);
        StationVo stationVo = new StationVo();
        BeanUtils.copyProperties(station, stationVo);
        Double d = mapService.calculateDistance(longitude, latitude,
                station.getLongitude().toString(),
                station.getLatitude().toString());

        stationVo.setDistance(d);

        Cabinet cabinet = cabinetService.getById(station.getCabinetId());

        if (cabinet.getAvailableNum() > 0) {
            stationVo.setIsUsable("1");
        } else {
            stationVo.setIsUsable("0");
        }
        if (cabinet.getFreeSlots() > 0) {
            stationVo.setIsReturn("1");
        } else {
            stationVo.setIsReturn("0");
        }

        FeeRule feeRules = remoteFeeRuleService.getFeeRule(
                        station.getFeeRuleId(), SecurityConstants.INNER)
                .getData();
        stationVo.setFeeRule(feeRules.getDescription());

        return stationVo;
    }
}