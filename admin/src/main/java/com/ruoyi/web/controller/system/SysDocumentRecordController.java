package com.ruoyi.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysDocumentRecord;
import com.ruoyi.system.service.ISysDocumentRecordService;

/**
 * 文档使用记录与备注接口。
 */
@RestController
@RequestMapping("/system/document")
public class SysDocumentRecordController extends BaseController
{
    @Autowired
    private ISysDocumentRecordService recordService;

    @PreAuthorize("@ss.hasPermi('system:document:record:list')")
    @GetMapping("/{documentId}/records")
    public AjaxResult list(@PathVariable Long documentId)
    {
        List<SysDocumentRecord> records = recordService.selectRecordList(documentId);
        return success(records);
    }

    @PreAuthorize("@ss.hasPermi('system:document:record:add')")
    @Log(title = "文档使用记录", businessType = BusinessType.INSERT)
    @PostMapping("/{documentId}/records")
    public AjaxResult add(@PathVariable Long documentId, @RequestBody SysDocumentRecord record)
    {
        record.setDocumentId(documentId);
        return toAjax(recordService.insertRecord(record));
    }

    @PreAuthorize("@ss.hasPermi('system:document:record:edit')")
    @Log(title = "文档使用记录", businessType = BusinessType.UPDATE)
    @PutMapping("/records/{recordId}")
    public AjaxResult edit(@PathVariable Long recordId, @RequestBody SysDocumentRecord record)
    {
        record.setRecordId(recordId);
        return toAjax(recordService.updateRecord(record));
    }

    @PreAuthorize("@ss.hasPermi('system:document:record:remove')")
    @Log(title = "文档使用记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/records/{recordId}")
    public AjaxResult remove(@PathVariable Long recordId)
    {
        return toAjax(recordService.deleteRecord(recordId));
    }
}
