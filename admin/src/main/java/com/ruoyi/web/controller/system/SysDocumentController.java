package com.ruoyi.web.controller.system;

import com.ruoyi.system.domain.dto.DocumentTagDto;
import com.ruoyi.system.domain.SysDocumentExtract;
import com.ruoyi.system.service.DocumentExtractService;
import com.ruoyi.system.service.DocumentPreviewService;
import com.ruoyi.system.service.DocumentTemplateResolver;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.StringUtils;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    @Autowired
    private DocumentExtractService documentExtractService;

    @Autowired
    private DocumentPreviewService documentPreviewService;

    @Autowired
    private DocumentTemplateResolver documentTemplateResolver;

    @PreAuthorize("@ss.hasPermi('system:document:extract:query')")
    @GetMapping("/{documentId}/preview/template")
    public void previewTemplate(@PathVariable Long documentId,
                                @RequestParam(defaultValue = "1") int page,
                                HttpServletResponse response) throws Exception
    {
        SysDocument document = previewDocument(documentId);
        response.setContentType("image/png");
        documentPreviewService.writeTemplatePage(documentTemplateResolver
                .resolvePreview(document.getMaterialCategory(), document.getDocumentKind()), page, response.getOutputStream());
    }

    @PreAuthorize("@ss.hasPermi('system:document:extract:query')")
    @GetMapping("/{documentId}/preview/template/info")
    public AjaxResult templatePreviewInfo(@PathVariable Long documentId) throws Exception
    {
        SysDocument document = previewDocument(documentId);
        return success(documentPreviewService.getTemplateInfo(documentTemplateResolver
                .resolvePreview(document.getMaterialCategory(), document.getDocumentKind())));
    }

    @PreAuthorize("@ss.hasPermi('system:document:extract:query')")
    @GetMapping("/{documentId}/preview/upload/info")
    public AjaxResult uploadPreviewInfo(@PathVariable Long documentId) throws Exception
    {
        return success(documentPreviewService.getUploadInfo(previewDocument(documentId)));
    }

    @PreAuthorize("@ss.hasPermi('system:document:extract:query')")
    @GetMapping("/{documentId}/preview/upload")
    public void previewUpload(@PathVariable Long documentId,
                              @RequestParam(defaultValue = "1") int page,
                              HttpServletResponse response) throws Exception
    {
        SysDocument document = previewDocument(documentId);
        response.setContentType("image/png");
        documentPreviewService.writeUploadPage(document, page, response.getOutputStream());
    }

    private SysDocument previewDocument(Long documentId)
    {
        SysDocument document = sysDocumentService.selectDocumentById(documentId);
        if (document == null)
        {
            throw new ServiceException("文档不存在");
        }
        return document;
    }

    /**
     * 获取常用标签列表（Top）
     */
    @PreAuthorize("@ss.hasPermi('system:document:list')")
    @GetMapping("/tags/top")
    public AjaxResult getTopTags(@RequestParam String documentType,
                                 @RequestParam(required = false) String materialCategory)
    {
        List<String> list = sysTagMapper.selectTagsByScope(documentType, materialCategory);
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
     * 跨内部/外部文档检索OCR正文与使用记录。
     */
    @PreAuthorize("@ss.hasPermi('system:document:search')")
    @GetMapping("/search")
    public TableDataInfo search(SysDocument sysDocument)
    {
        startPage();
        List<SysDocument> list = sysDocumentService.searchDocumentList(sysDocument);
        return getDataTable(list);
    }

    /**
     * 获取当前文档类型/分类统计。
     */
    @PreAuthorize("@ss.hasPermi('system:document:list')")
    @GetMapping("/stats")
    public AjaxResult stats(SysDocument sysDocument)
    {
        Map<String, Object> stats = sysDocumentService.selectDocumentStats(sysDocument);
        return success(stats);
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
        int rows = sysDocumentService.insertDocument(sysDocument);
        return rows > 0 ? AjaxResult.success(sysDocument.getDocumentId()) : AjaxResult.error("新增文档失败");
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
     * 手动重新执行 AI 抽取。
     */
    @PreAuthorize("@ss.hasPermi('system:document:extract')")
    @Log(title = "文档 AI 抽取", businessType = BusinessType.UPDATE)
    @PostMapping("/{documentId}/extract")
    public AjaxResult extract(@PathVariable("documentId") Long documentId)
    {
        return success(documentExtractService.extractDocument(documentId));
    }

    /**
     * 获取文档最新的 AI 抽取结果。
     */
    @PreAuthorize("@ss.hasPermi('system:document:extract:query')")
    @GetMapping("/{documentId}/extract/latest")
    public AjaxResult latestExtract(@PathVariable("documentId") Long documentId)
    {
        return success(documentExtractService.selectLatest(documentId));
    }

    /**
     * 保存用户修改后的抽取 JSON。
     */
    @PreAuthorize("@ss.hasPermi('system:document:extract:edit')")
    @Log(title = "文档解析结果", businessType = BusinessType.UPDATE)
    @PutMapping("/{documentId}/extract/{extractId}/final")
    public AjaxResult saveExtractFinal(@PathVariable("documentId") Long documentId,
                                       @PathVariable("extractId") Long extractId,
                                       @RequestBody SysDocumentExtract extract)
    {
        return toAjax(documentExtractService.saveFinalJson(documentId, extractId, extract.getFinalJson(), false));
    }

    /**
     * 保存最终 JSON 并下载回填后的 TDS Word。
     */
    @PreAuthorize("@ss.hasPermi('system:document:extract:download')")
    @Log(title = "文档解析结果下载", businessType = BusinessType.EXPORT)
    @PostMapping("/{documentId}/extract/{extractId}/download")
    public void downloadExtract(@PathVariable("documentId") Long documentId,
                                @PathVariable("extractId") Long extractId,
                                @RequestBody SysDocumentExtract extract,
                                HttpServletResponse response) throws Exception
    {
        SysDocument document = sysDocumentService.selectDocumentById(documentId);
        if (document == null)
        {
            throw new IllegalArgumentException("文档不存在");
        }
        documentExtractService.validateDownload(documentId);
        String fileName = StringUtils.defaultString(document.getDocumentName())
                .replaceAll("[\\\\/:*?\"<>|]", "_") + "-" + document.getDocumentKind() + ".docx";
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        FileUtils.setAttachmentResponseHeader(response, fileName);
        documentExtractService.download(documentId, extractId, extract.getFinalJson(), response.getOutputStream());
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

    /**
     * OCR选中文本：新增标签
     */
    @PreAuthorize("@ss.hasPermi('system:document:edit')")
    @Log(title = "文档管理-新增标签", businessType = BusinessType.INSERT)
    @PostMapping("/addTag")
    public AjaxResult addTag(@RequestBody DocumentTagDto dto)
    {
        if (dto.getDocumentId() == null || StringUtils.isEmpty(dto.getTagName())) {
            return AjaxResult.error("参数不完整：文档ID或标签内容不能为空");
        }
        return toAjax(sysDocumentService.addDocumentTag(dto.getDocumentId(), dto.getTagName(), getUserId()));
    }

    /**
     * OCR选中文本：替换标签 (清空当前文档旧标签，设置新标签)
     */
    @PreAuthorize("@ss.hasPermi('system:document:edit')")
    @Log(title = "文档管理-替换标签", businessType = BusinessType.UPDATE)
    @PostMapping("/replaceTag")
    public AjaxResult replaceTag(@RequestBody DocumentTagDto dto)
    {
        if (dto.getDocumentId() == null || StringUtils.isEmpty(dto.getTagName())) {
            return AjaxResult.error("参数不完整：文档ID或标签内容不能为空");
        }
        return toAjax(sysDocumentService.replaceDocumentTag(dto.getDocumentId(), dto.getTagName(), getUserId()));
    }
}
