package com.share.device.service;


import com.alibaba.fastjson2.JSONObject;
import com.share.common.core.exception.ServiceException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements IMapService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${tencent.map.key}")
    private String key;

    @Override
    public JSONObject calculateLatLng(String keyword) {
        String url = "https://apis.map.qq.com/ws/geocoder/v1/?address={address}&key={key}";

        Map<String, String> map = new HashMap<>();
        map.put("address", keyword);
        map.put("key", key);

        JSONObject response = restTemplate.getForObject(url, JSONObject.class, map);
        if (response.getIntValue("status") != 0) {
            throw new ServiceException("地图解析异常");
        }

        //返回第一条最佳线路
        JSONObject result = response.getJSONObject("result");
        System.out.println(result.toJSONString());
        return result.getJSONObject("location");
    }

}