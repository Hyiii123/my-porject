package com.share.rules.factory;

import com.share.common.core.domain.R;
import com.share.rules.RemoteFeeRuleService;
import com.share.rules.domain.FeeRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class RemoteFeeRuleFallbackFactory implements FallbackFactory<RemoteFeeRuleService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteFeeRuleFallbackFactory.class);

    @Override
    public RemoteFeeRuleService create(Throwable throwable) {
        log.error("规则服务调用失败:{}", throwable.getMessage());
        return new RemoteFeeRuleService() {

            @Override
            public R<List<FeeRule>> getFeeRuleList(List<Long> feeRuleIdList, String inner) {
                return R.fail("操作失败"+throwable.getMessage());
            }

            @Override
            public R<FeeRule> getFeeRule(Long id, String inner) {
                return R.fail("操作失败"+throwable.getMessage());
            }
        };
    }
}