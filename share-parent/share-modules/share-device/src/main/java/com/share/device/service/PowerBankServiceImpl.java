package com.share.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.exception.ServiceException;
import com.share.device.domain.PowerBank;
import com.share.device.mapper.PowerBankMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PowerBankServiceImpl extends ServiceImpl<PowerBankMapper,PowerBank> implements IPowerBankService{

    @Autowired
    private PowerBankMapper powerBankMapper;

    @Override
    public List<PowerBank> selectPowerBankList(PowerBank powerBank) {

        return powerBankMapper.selectPowerBankList(powerBank);
    }

    @Override
    public PowerBank getByPowerBankNo(String powerBankNo) {
        return powerBankMapper.selectOne(new LambdaQueryWrapper<PowerBank>().eq(PowerBank::getPowerBankNo,powerBankNo));
    }

    @Override
    public int savePowerBank(PowerBank powerBank) {
        long count=this.count(new LambdaQueryWrapper<PowerBank>().eq(PowerBank::getPowerBankNo,powerBank.getPowerBankNo()));
        if (count>0){
            throw new ServiceException("该充电宝编号已存在");
        }
        powerBankMapper.insert(powerBank);
        return 1;
    }

    @Override
    public int updatePowerBank(PowerBank powerBank) {
        PowerBank oldPowerBank = this.getById(powerBank.getId());
        if (oldPowerBank!=null&& !"0".equals(powerBank.getStatus())){
            throw new ServiceException("该充电宝已投放，无法修改");
        }

        if (oldPowerBank != null && !Objects.equals(oldPowerBank.getPowerBankNo(), powerBank.getPowerBankNo())) {
            long count = this.count(new LambdaQueryWrapper<PowerBank>().eq(PowerBank::getPowerBankNo, powerBank.getPowerBankNo()));
            if (count > 0) {
                throw new ServiceException("该充电宝编号已存在");
            }
        }

        powerBankMapper.updateById(powerBank);
        return 1;
    }
}
