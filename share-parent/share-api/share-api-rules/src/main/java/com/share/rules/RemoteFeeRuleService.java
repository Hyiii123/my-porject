package com.share.rules;

import com.share.common.core.domain.R;
import com.share.rules.domain.FeeRule;
import com.share.rules.factory.RemoteFeeRuleFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.share.common.core.constant.ServiceNameConstants;
import java.util.List;

@FeignClient(contextId = "remoteFeeRuleService", value = ServiceNameConstants.FREERULE_SERVICE, fallbackFactory = RemoteFeeRuleFallbackFactory.class)
public interface RemoteFeeRuleService {

    @PostMapping(value = "/feeRule/getFeeRuleList")
    public R<List<FeeRule>> getFeeRuleList(@RequestBody List<Long> feeRuleIdList, String inner);

    @GetMapping(value = "/feeRule/getFeeRule/{id}")
    public R<FeeRule> getFeeRule(@PathVariable("id") Long id, String inner);

}