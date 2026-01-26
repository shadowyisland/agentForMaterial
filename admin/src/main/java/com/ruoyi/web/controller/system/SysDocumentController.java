package com.ruoyi.web.controller.system;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import com.ruoyi.system.mapper.SysTagMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.service.ISysDocumentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 文档管理Controller
 */
@RestController
@RequestMapping("/system/document")
public class SysDocumentController extends BaseController
{
    @Autowired
    private ISysDocumentService sysDocumentService;

    @Autowired
    private SysTagMapper sysTagMapper;

    /**
     * 获取常用标签列表（Top）
     */
    @PreAuthorize("@ss.hasPermi('system:document:list')")
    @GetMapping("/tags/top")
    public AjaxResult getTopTags()
    {
        List<String> list = sysTagMapper.selectTagsByUserId(getUserId());
        return success(list);
    }

    /**
     * 查询文档管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDocument sysDocument)
    {
        startPage();
        List<SysDocument> list = sysDocumentService.selectDocumentList(sysDocument);
        return getDataTable(list);
    }

    /**
     * 导出文档管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:document:export')")
    @Log(title = "文档管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDocument sysDocument)
    {
        List<SysDocument> list = sysDocumentService.selectDocumentList(sysDocument);
        ExcelUtil<SysDocument> util = new ExcelUtil<SysDocument>(SysDocument.class);
        util.exportExcel(response, list, "文档管理数据");
    }

    /**
     * 获取文档管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:document:query')")
    @GetMapping(value = "/{documentId}")
    public AjaxResult getInfo(@PathVariable("documentId") Long documentId)
    {
        return success(sysDocumentService.selectDocumentById(documentId));
    }

    /**
     * 新增文档管理
     */
    @PreAuthorize("@ss.hasPermi('system:document:add')")
    @Log(title = "文档管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysDocument sysDocument)
    {
        sysDocument.setCreateBy(getUsername());
        return toAjax(sysDocumentService.insertDocument(sysDocument));
    }

    /**
     * OCR 识别接口
     */
    @PreAuthorize("@ss.hasPermi('system:document:edit')")
    @Log(title = "OCR识别", businessType = BusinessType.UPDATE)
    @PostMapping("/ocr/{documentId}")
    public AjaxResult ocr(@PathVariable("documentId") Long documentId)
    {
        return toAjax(sysDocumentService.ocrDocument(documentId));
    }

    /**
     * 修改文档管理
     */
    @PreAuthorize("@ss.hasPermi('system:document:edit')")
    @Log(title = "文档管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysDocument sysDocument)
    {
        sysDocument.setUpdateBy(getUsername());
        return toAjax(sysDocumentService.updateDocument(sysDocument));
    }

    /**
     * 删除文档管理
     */
    @PreAuthorize("@ss.hasPermi('system:document:remove')")
    @Log(title = "文档管理", businessType = BusinessType.DELETE)
    @DeleteMapping(value = "/{documentIds}")
    public AjaxResult remove(@PathVariable Long[] documentIds)
    {
        return toAjax(sysDocumentService.deleteDocumentByIds(documentIds));
    }
}