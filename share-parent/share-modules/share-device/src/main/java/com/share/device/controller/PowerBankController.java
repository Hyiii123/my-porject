package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.common.security.utils.SecurityUtils;
import com.share.device.domain.PowerBank;
import com.share.device.service.IPowerBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aspectj.weaver.loadtime.Aj;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

@Tag(name = "充电宝接口管理")
@RestController
@RequestMapping("/powerBank")
public class PowerBankController extends BaseController {
    @Autowired
    private IPowerBankService iPowerBankService;

    @Operation(summary = "查询充电宝")
    @GetMapping("/list")
    public TableDataInfo List(PowerBank powerBank){
        startPage();
        return getDataTable(iPowerBankService.selectPowerBankList(powerBank));
    }

    @Operation(summary = "增加充电宝")
    @PostMapping
    public AjaxResult add(@RequestBody PowerBank powerBank){
        powerBank.setCreateBy(SecurityUtils.getUsername());
        powerBank.setCreateTime(new Date());
        return toAjax(iPowerBankService.savePowerBank(powerBank));
    }

    @Operation(summary = "删除充电宝")
    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable Long[] ids){
        return toAjax(iPowerBankService.removeBatchByIds(Arrays.asList(ids)));
    }

    @Operation(summary = "修改充电宝")
    @PutMapping
    public AjaxResult edit(@RequestBody PowerBank powerBank){
        powerBank.setUpdateBy(SecurityUtils.getUsername());
        powerBank.setUpdateTime(new Date());
        return toAjax(iPowerBankService.updatePowerBank(powerBank));
    }
    @Operation(summary = "查询充电宝详细信息")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable("id") Long id){
        return success(iPowerBankService.getById(id));
    }
}
