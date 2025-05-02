package com.share.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.rule.domain.FeeRule;
import com.share.rule.mapper.FeeRuleMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class FeeRuleServiceImpl extends ServiceImpl<FeeRuleMapper,FeeRule>implements IFeeRuleService{

    @Autowired
    private FeeRuleMapper feeRuleMapper;

    @Override
    public List<FeeRule> selectFeeRuleList(FeeRule feeRule) {
        return feeRuleMapper.selectFeeRuleList(feeRule);
    }

    @Override
    public List<FeeRule> getALLFeeRuleList() {
        LambdaQueryWrapper<FeeRule> objectLambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<FeeRule> eq = objectLambdaQueryWrapper.eq(FeeRule::getStatus, "1");
        return feeRuleMapper.selectList(eq);
    }
}
