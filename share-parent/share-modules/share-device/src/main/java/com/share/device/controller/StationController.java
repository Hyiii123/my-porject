package com.share.device.controller;

import com.share.common.core.web.controller.BaseController;
import com.share.common.core.web.domain.AjaxResult;
import com.share.common.core.web.page.TableDataInfo;
import com.share.device.domain.Station;
import com.share.device.service.IStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aspectj.weaver.loadtime.Aj;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Tag(name = "站点信息接口管理")
@RestController
@RequestMapping("/station")
public class StationController extends BaseController {
    @Autowired
    private IStationService iStationService;


    @Operation(summary = "新增站点")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Station station){
        return toAjax(iStationService.saveStation(station));
    }

    @Operation(summary = "修改站点")
    @PutMapping("/edit")
    public AjaxResult edit(@RequestBody Station station){
        return toAjax(iStationService.updateStation(station));
    }

    @Operation(summary = "查询站点列表")
    @GetMapping("/list")
    public TableDataInfo list(Station station){
        startPage();
        List<Station> stations = iStationService.selectStationList(station);
        return getDataTable(stations);
    }

    @Operation(summary = "获取站点详细信息")
    @GetMapping("/{id}")
    public AjaxResult selectStation(@PathVariable("id") Long id){
        return success(iStationService.getById(id));
    }

    @Operation(summary = "删除站点")
    @DeleteMapping("/{ids}")
    public AjaxResult deleteStation(@PathVariable Long[] ids){
        return toAjax(iStationService.removeBatchByIds(Arrays.asList(ids)));
    }
    @Operation(summary = "站点设置关联数据")
    @PostMapping("/setData")
    public AjaxResult setData(@RequestBody Station station)
    {
        return toAjax(iStationService.setData(station));
    }

    @Operation(summary = "获取站点详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(iStationService.getById(id));
    }

    @Operation(summary = "删除站点")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(iStationService.removeBatchByIds(Arrays.asList(ids)));
    }


}
