package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.device.domain.Cabinet;
import com.share.device.domain.CabinetType;
import com.share.device.service.ICabinetTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.checkerframework.checker.units.qual.A;
import org.simpleframework.xml.core.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "柜机类型接口管理")
@RestController
@RequestMapping("/cabinetType")
public class CabinetTypeController extends BaseController
{
    @Autowired
    private ICabinetTypeService cabinetTypeService;

    /**
     * 查询柜机类型列表
     */
    @Operation(summary = "查询柜机类型列表")
    @GetMapping("/list")
    public TableDataInfo list(CabinetType cabinetType)
    {
        startPage();
        List<CabinetType> list = cabinetTypeService.selectCabinetTypeList(cabinetType);
        return getDataTable(list);
    }

    @Operation(summary = "添加柜机类型")
    @PostMapping("/add")
    public AjaxResult addCabinetType(@RequestBody @Validated CabinetType cabinetType){
        boolean save = cabinetTypeService.save(cabinetType);
        return toAjax(save);
    }

    @Operation(summary = "删除柜机类型")
    @DeleteMapping("/{ids}")
    public AjaxResult delCabinetType(@PathVariable Long[] ids){
        boolean b = cabinetTypeService.removeBatchByIds(Arrays.asList(ids));
        return toAjax(b);
    }

    @Operation(summary = "根据id查询柜机类型")
    @GetMapping("/{id}")
    public AjaxResult getCabinetTypeById(@PathVariable("id") Long id){
        CabinetType byId = cabinetTypeService.getById(id);
        return success(byId);
    }

    @Operation(summary = "修改柜机类型")
    @PutMapping("/update")
    public AjaxResult updateCabinetType(@RequestBody @Validated CabinetType cabinetType){
        boolean b = cabinetTypeService.updateById(cabinetType);
        return success(b);
//        return toAjax(b);
    }

    @Operation(summary = "查询全部柜机类型列表")
    @GetMapping("/getCabinetTypeList")
    public AjaxResult getCabinetTypeList()
    {
        return success(cabinetTypeService.list());
    }

}