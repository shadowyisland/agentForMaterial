package com.ruoyi.system.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.constant.DocumentConstants;

/**
 * 根据文档分类选择网页与下载使用的 Word 模版。
 */
@Service
public class DocumentTemplateResolver
{
    private static final String MSDS_TEMPLATE = "templates/document/msds/MSDS-COMMON.docx";

    private static final Map<String, String> TDS_TEMPLATES;

    static
    {
        Map<String, String> templates = new HashMap<String, String>();
        templates.put("ACRYLIC", "templates/document/tds/ACRYLIC.docx");
        templates.put("EPOXY", "templates/document/tds/EPOXY.docx");
        templates.put("OTHER_RESIN", "templates/document/tds/OTHER_RESIN.docx");
        templates.put("FILLER", "templates/document/tds/FILLER.docx");
        templates.put("SILICONE", "templates/document/tds/SILICONE.docx");
        templates.put("ADDITIVE", "templates/document/tds/ADDITIVE.docx");
        TDS_TEMPLATES = Collections.unmodifiableMap(templates);
    }

    public Resource resolve(String materialCategory, String documentKind)
    {
        if (DocumentConstants.KIND_MSDS.equals(documentKind))
        {
            return loadTemplate(MSDS_TEMPLATE);
        }
        if (!DocumentConstants.KIND_TDS.equals(documentKind))
        {
            throw new ServiceException("文档资料类型不正确");
        }
        String path = TDS_TEMPLATES.get(materialCategory);
        if (path == null)
        {
            throw new ServiceException("当前材料分类未配置 TDS Word 模版");
        }
        return loadTemplate(path);
    }

    private Resource loadTemplate(String path)
    {
        Resource resource = new ClassPathResource(path);
        if (!resource.exists())
        {
            throw new ServiceException("Word 模版不存在: " + path);
        }
        return resource;
    }

    /** 原始 Word 的固定版式预览，与带字段标记的下载模板分开维护。 */
    public Resource resolvePreview(String materialCategory, String documentKind)
    {
        String fileName = resolve(materialCategory, documentKind).getFilename();
        String folder = DocumentConstants.KIND_MSDS.equals(documentKind) ? "msds/" : "tds/";
        Resource preview = new ClassPathResource("templates/document/preview/" + folder
                + fileName.replace(".docx", ".pdf"));
        if (!preview.exists())
        {
            throw new ServiceException("当前分类的模版预览不存在");
        }
        return preview;
    }

    public boolean supports(String materialCategory, String documentKind)
    {
        return DocumentConstants.KIND_MSDS.equals(documentKind)
                || (DocumentConstants.KIND_TDS.equals(documentKind) && TDS_TEMPLATES.containsKey(materialCategory));
    }
}
