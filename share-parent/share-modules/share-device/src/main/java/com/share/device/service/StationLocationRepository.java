package com.share.device.service;

import com.baomidou.mybatisplus.extension.service.IService;

public interface StationLocationRepository extends IService {
    void deleteByStationId(Long stationId);
}
