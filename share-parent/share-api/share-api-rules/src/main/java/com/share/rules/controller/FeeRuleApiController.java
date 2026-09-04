package com.share.rules.legacy.controller;

import com.share.common.core.domain.R;
import com.share.common.security.annotation.InnerAuth;
import com.share.rules.FeeRuleQueryService;
import com.share.rules.domain.FeeRule;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 历史费用规则内部接口源码保留件。
 *
 * <p>原文件曾位于 API 模块，但 API 模块不应依赖业务实现模块，否则会形成
 * Maven 循环依赖。实际生产路由由 share-rule 中的同语义控制器提供；本兼容
 * 控制器仅在显式启用 {@code legacy-fee-rule-api} Profile 时注册。</p>
 */
@Profile("legacy-fee-rule-api")
@Slf4j
@RestController
@RequestMapping("/feeRule")
public class FeeRuleApiController {

    @Autowired
    private FeeRuleQueryService feeRuleService;

    @Operation(summary = "批量获取费用规则信息")
    @InnerAuth
    @PostMapping("/getFeeRuleList")
    public R<List<FeeRule>> getFeeRuleList(@RequestBody List<Long> feeRuleIdList) {
        return R.ok(feeRuleService.queryByIds(feeRuleIdList));
    }

    @Operation(summary = "获取费用规则详细信息")
    @InnerAuth
    @GetMapping("/getFeeRule/{id}")
    public R<FeeRule> getFeeRule(@PathVariable("id") Long id) {
        return R.ok(feeRuleService.queryById(id));
    }
}
