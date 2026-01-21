package com.ruoyi.web.controller.system;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.service.ISysDocumentService;

@RestController
@RequestMapping("/system/document")
public class SysDocumentController extends BaseController
{
    @Autowired
    private ISysDocumentService documentService;

    @PreAuthorize("@ss.hasPermi('system:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDocument document)
    {
        startPage();
        List<SysDocument> list = documentService.selectDocumentList(document);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:document:add')")
    @Log(title = "文档管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysDocument document)
    {
        document.setCreateBy(getUsername());
        return toAjax(documentService.insertDocument(document));
    }

    // 【新增】OCR 识别接口
    @PreAuthorize("@ss.hasPermi('system:document:edit')") // 借用 edit 权限，或新增权限字符
    @Log(title = "OCR识别", businessType = BusinessType.UPDATE)
    @PostMapping("/ocr/{documentId}")
    public AjaxResult ocr(@PathVariable Long documentId)
    {
        return toAjax(documentService.ocrDocument(documentId));
    }

    @PreAuthorize("@ss.hasPermi('system:document:remove')")
    @Log(title = "文档管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{documentIds}")
    public AjaxResult remove(@PathVariable Long[] documentIds)
    {
        return toAjax(documentService.deleteDocumentByIds(documentIds));
    }

    /**
     * 获取文档管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:document:query')")
    @GetMapping(value = "/{documentId}")
    public AjaxResult getInfo(@PathVariable("documentId") Long documentId)
    {
        return AjaxResult.success(documentService.selectDocumentById(documentId));
    }
}