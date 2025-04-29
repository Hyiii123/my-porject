package com.share.device.service;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.common.security.utils.SecurityUtils;
import com.share.device.domain.Cabinet;
import com.share.device.domain.CabinetType;
import com.share.device.domain.PowerBank;
import com.share.device.mapper.CabinetMapper;
import com.share.device.mapper.CabinetTypeMapper;
import com.share.device.mapper.CabinetSlotMapper;
import com.share.device.service.ICabinetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.share.device.domain.CabinetSlot;
import javax.sql.rowset.serial.SerialException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CabinetServiceImpl extends ServiceImpl<CabinetMapper, Cabinet> implements ICabinetService
{

    @Autowired
    private CabinetMapper cabinetMapper;
    private CabinetTypeMapper cabinetTypeMapper;
    private CabinetSlotMapper cabinetSlotMapper;
    private IPowerBankService powerBankService;

    //初始化柜机
    public void setCabinet(Cabinet cabinet,CabinetType cabinetType){
        cabinet.setTotalSlots(cabinetType.getTotalSlots());
        cabinet.setFreeSlots(cabinetType.getTotalSlots());
        cabinet.setUsedSlots(0);
        cabinet.setAvailableNum(0);
        cabinet.setCreateTime(new Date());
        cabinet.setCreateBy(SecurityUtils.getUsername());
        this.save(cabinet);
    }
    //创建插槽
    public void createCabinet(Cabinet cabinet,CabinetType cabinetType){
        int size =cabinetType.getTotalSlots();
        for (int i=0;i<size;i++){
            CabinetSlot cabinetSlot =new CabinetSlot();
            cabinetSlot.setCabinetId(cabinet.getId());
            cabinetSlot.setSlotNo(i+1+"");
            cabinetSlot.setCreateTime(new Date());
            cabinet.setCreateBy(SecurityUtils.getUsername());
            cabinetSlotMapper.insert(cabinetSlot);
        }
    }

    @Override
    public List<Cabinet> selectCabinetList(Cabinet cabinet)
    {
        return cabinetMapper.selectCabinetList(cabinet);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    //新增柜机
    public int saveCabinet(Cabinet cabinet) {
        //检查柜机编号是否已经存在
        long count=this.count(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getCabinetNo,cabinet.getCabinetNo()));
        if (count>0){
            throw new ServiceException("该柜机编号已存在");
        }

        //根据柜机类型id查询柜机类型
        CabinetType cabinetType = cabinetTypeMapper.selectById(cabinet.getCabinetTypeId());

        //设置柜机的总插槽数和可用插槽数
        cabinet.setTotalSlots(cabinetType.getTotalSlots());
        cabinet.setFreeSlots(cabinetType.getTotalSlots());
        cabinet.setUsedSlots(0);
        cabinet.setAvailableNum(0);
        cabinet.setCreateTime(new Date());
        cabinet.setCreateBy(SecurityUtils.getUsername());
        this.save(cabinet);


        //为柜机创建插槽
        int size =cabinetType.getTotalSlots();
        for (int i=0;i<size;i++){
            CabinetSlot cabinetSlot =new CabinetSlot();
            cabinetSlot.setCabinetId(cabinet.getId());
            cabinetSlot.setSlotNo(i+1+"");
            cabinetSlot.setCreateTime(new Date());
            cabinet.setCreateBy(SecurityUtils.getUsername());
            cabinetSlotMapper.insert(cabinetSlot);
        }

        return 1;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    //修改柜机
    public int updateCabinet(Cabinet cabinet) {
        //0未投放 1使用中 -1故障
        Cabinet oldCabinet =this.getById(cabinet.getStatus()); //获取旧的柜机数据
        if (oldCabinet!=null&&!"0".equals(oldCabinet.getStatus())){
            throw new ServiceException("该柜机已投放，无法修改");
        }

        if (!oldCabinet.getCabinetNo().equals(cabinet.getCabinetNo())){
            long count = this.count(new LambdaQueryWrapper<Cabinet>().eq(Cabinet::getCabinetNo, cabinet.getCabinetNo()));
            if (count>0){
                throw new ServiceException("该编号已存在，无法修改");
            }
        }
        if (!oldCabinet.getCabinetTypeId().equals(cabinet.getCabinetTypeId())){
            //获取新柜机类型数据
            CabinetType cabinetType = cabinetTypeMapper.selectById(cabinet.getId());
            //初始化柜机的插槽
            cabinet.setTotalSlots(cabinetType.getTotalSlots());
            cabinet.setFreeSlots(cabinetType.getTotalSlots());
            cabinet.setUsedSlots(0);
            cabinet.setAvailableNum(0);
            cabinet.setCreateTime(new Date());
            cabinet.setCreateBy(SecurityUtils.getUsername());
            this.updateById(cabinet);

            //删除旧的插槽数据
//            CabinetSlot cabinetSlot=cabinetSlotMapper.selectById(cabinet.getId());
//            cabinetSlotMapper.delete(new LambdaQueryWrapper<CabinetSlot>().eq(CabinetSlot::getCabinetId,cabinetSlot.getCabinetId()));
            cabinetSlotMapper.delete(new LambdaQueryWrapper<CabinetSlot>().eq(CabinetSlot::getCabinetId,cabinet.getId()));
            //创建插槽
            createCabinet(cabinet,cabinetType);
        }else {
            //如果柜机类型没有更改，直接修改
            cabinet.setCreateTime(new Date());
            cabinet.setCreateBy(SecurityUtils.getUsername());
            this.updateById(cabinet);
        }
        return 1;
    }

    @Override
    public int removeCabinet(List<Long> idList) {
        return 0;
    }

    @Override
    public List<Cabinet> searchNoUseList(String keyword) {
        return cabinetMapper.selectList(new LambdaQueryWrapper<Cabinet>()
                .like(Cabinet::getCabinetNo, keyword)
                .eq(Cabinet::getStatus, "0")
        );
    }

    @Override
    public Map<String, Object> getAllInfo(Long id) {
        // 查询柜机信息
        Cabinet cabinet = this.getById(id);

        // 查询插槽信息
        List<CabinetSlot> cabinetSlotList = cabinetSlotMapper.selectList(new LambdaQueryWrapper<CabinetSlot>().eq(CabinetSlot::getCabinetId, cabinet.getId()));
        // 获取可用充电宝id列表
        List<Long> powerBankIdList = cabinetSlotList.stream().filter(item -> null != item.getPowerBankId()).map(CabinetSlot::getPowerBankId).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(powerBankIdList)) {
            List<PowerBank> powerBankList = powerBankService.listByIds(powerBankIdList);
            Map<Long,PowerBank> powerBankIdToPowerBankMap = powerBankList.stream().collect(Collectors.toMap(PowerBank::getId, PowerBank -> PowerBank));
            cabinetSlotList.forEach(item -> item.setPowerBank(powerBankIdToPowerBankMap.get(item.getPowerBankId())));
        }

        Map<String, Object> result = Map.of("cabinet", cabinet, "cabinetSlotList", cabinetSlotList);
        return result;
    }
}