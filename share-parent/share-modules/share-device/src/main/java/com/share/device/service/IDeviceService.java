package com.share.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.device.domain.StationVo;

import java.util.List;

public interface IDeviceService {
    List<StationVo> nearbyStation(String latitude, String longitude, Integer radius);

    StationVo getStation(Long id, String latitude, String longitude);
}
