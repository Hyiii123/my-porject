package com.share.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.rule.mapper.FeeRuleMapper;
import com.share.rules.FeeRuleQueryService;
import com.share.rules.domain.FeeRule;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class FeeRuleServiceImpl extends ServiceImpl<FeeRuleMapper,FeeRule>
        implements IFeeRuleService, FeeRuleQueryService {

    @Autowired
    private FeeRuleMapper feeRuleMapper;

    @Override
    public List<FeeRule> queryByIds(Collection<? extends Serializable> ids) {
        return listByIds(ids);
    }

    @Override
    public FeeRule queryById(Serializable id) {
        return getById(id);
    }

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
