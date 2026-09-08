package com.ruoyi.system.service;

import java.io.OutputStream;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.constant.DocumentConstants;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.domain.SysDocumentExtract;
import com.ruoyi.system.mapper.SysDocumentExtractMapper;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.service.impl.QwenExtractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文档 OCR 内容的大模型抽取、保存和 Word 导出。
 */
@Service
public class DocumentExtractService
{
    @Autowired
    private SysDocumentMapper documentMapper;

    @Autowired
    private SysDocumentExtractMapper extractMapper;

    @Autowired
    private DocumentExtractPromptService promptService;

    @Autowired
    private QwenExtractService qwenExtractService;

    @Autowired
    private DocumentWordExportService documentWordExportService;

    public SysDocumentExtract extractDocument(Long documentId)
    {
        SysDocument document = getExtractableDocument(documentId);
        String prompt = promptService.getPrompt(document.getMaterialCategory(), document.getDocumentKind());
        String aiJson = qwenExtractService.extractJson(prompt, document.getOcrContent());

        SysDocumentExtract extract = new SysDocumentExtract();
        extract.setDocumentId(documentId);
        extract.setDocumentKind(document.getDocumentKind());
        extract.setMaterialType(document.getMaterialCategory());
        extract.setModelName(qwenExtractService.getModelName());
        extract.setPromptVersion("V1");
        extract.setAiJson(aiJson);
        extract.setStatus("draft");
        extract.setCreateBy(SecurityUtils.getUsername());
        extract.setCreateTime(DateUtils.getNowDate());
        extractMapper.insertExtract(extract);
        return extract;
    }

    public SysDocumentExtract selectLatest(Long documentId)
    {
        SysDocument document = getDocument(documentId);
        requireInternalDocument(document);
        requireDocumentKind(document);
        SysDocumentExtract extract = extractMapper.selectLatestByDocumentId(documentId, document.getDocumentKind());
        if (extract != null)
        {
            extract.setDocumentKind(document.getDocumentKind());
        }
        return extract;
    }

    public int saveFinalJson(Long documentId, Long extractId, String finalJson, boolean confirmed)
    {
        SysDocument document = getDocument(documentId);
        requireInternalDocument(document);
        requireDocumentKind(document);
        validateJson(finalJson);
        SysDocumentExtract extract = extractMapper.selectById(documentId, extractId, document.getDocumentKind());
        if (extract == null)
        {
            throw new ServiceException("抽取记录不存在");
        }
        extract.setDocumentKind(document.getDocumentKind());
        extract.setFinalJson(JSON.toJSONString(JSON.parseObject(finalJson)));
        extract.setStatus(confirmed ? "confirmed" : "draft");
        extract.setUpdateBy(SecurityUtils.getUsername());
        extract.setUpdateTime(DateUtils.getNowDate());
        return extractMapper.updateFinalJson(extract);
    }

    public void download(Long documentId, Long extractId, String finalJson, OutputStream outputStream)
    {
        SysDocument document = getDocument(documentId);
        requireInternalDocument(document);
        documentWordExportService.validateTemplate(document);
        saveFinalJson(documentId, extractId, finalJson, true);
        documentWordExportService.writeDocument(document, finalJson, outputStream);
    }

    public void validateDownload(Long documentId)
    {
        SysDocument document = getDocument(documentId);
        requireInternalDocument(document);
        documentWordExportService.validateTemplate(document);
    }

    public void deleteByDocumentIds(Long[] documentIds)
    {
        if (documentIds == null || documentIds.length == 0)
        {
            return;
        }
        extractMapper.deleteTdsByDocumentIds(documentIds);
        extractMapper.deleteMsdsByDocumentIds(documentIds);
    }

    private SysDocument getExtractableDocument(Long documentId)
    {
        SysDocument document = getDocument(documentId);
        requireInternalDocument(document);
        requireDocumentKind(document);
        if (StringUtils.isEmpty(document.getOcrContent()))
        {
            throw new ServiceException("请先执行 OCR");
        }
        return document;
    }

    private SysDocument getDocument(Long documentId)
    {
        SysDocument document = documentMapper.selectDocumentById(documentId);
        if (document == null)
        {
            throw new ServiceException("文档不存在");
        }
        return document;
    }

    private void requireDocumentKind(SysDocument document)
    {
        if (StringUtils.isEmpty(document.getDocumentKind()))
        {
            throw new ServiceException("请先维护文档类型");
        }
        if (!DocumentConstants.DOCUMENT_KINDS.contains(document.getDocumentKind()))
        {
            throw new ServiceException("文档类型不正确");
        }
    }

    private void requireInternalDocument(SysDocument document)
    {
        if (!DocumentConstants.TYPE_INTERNAL.equals(document.getDocumentType()))
        {
            throw new ServiceException("外部文档只执行 OCR，不进行 AI 提取或 Word 模板导出");
        }
    }

    private void validateJson(String json)
    {
        try
        {
            JSONObject object = JSON.parseObject(json);
            if (object == null)
            {
                throw new ServiceException("最终 JSON 必须是对象");
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("最终 JSON 格式不正确");
        }
    }
}
