package com.share.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.device.domain.CabinetSlot;

import java.util.List;

public interface CabinetSlotMapper extends BaseMapper<CabinetSlot> {
    public List<CabinetSlot> selectCabinetSlotList(CabinetSlot cabinetSlot);
    public List<CabinetSlot> addCabinetSlot(CabinetSlot cabinetSlot);
}
