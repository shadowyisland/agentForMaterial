package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 文档 AI 抽取结果。
 */
public class SysDocumentExtract extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long extractId;
    private Long documentId;
    private String materialType;
    private String documentKind;
    private String modelName;
    private String promptVersion;
    private String aiJson;
    private String finalJson;
    private String status;

    public Long getExtractId()
    {
        return extractId;
    }

    public void setExtractId(Long extractId)
    {
        this.extractId = extractId;
    }

    public Long getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(Long documentId)
    {
        this.documentId = documentId;
    }

    public String getMaterialType()
    {
        return materialType;
    }

    public void setMaterialType(String materialType)
    {
        this.materialType = materialType;
    }

    public String getDocumentKind()
    {
        return documentKind;
    }

    public void setDocumentKind(String documentKind)
    {
        this.documentKind = documentKind;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getPromptVersion()
    {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion)
    {
        this.promptVersion = promptVersion;
    }

    public String getAiJson()
    {
        return aiJson;
    }

    public void setAiJson(String aiJson)
    {
        this.aiJson = aiJson;
    }

    public String getFinalJson()
    {
        return finalJson;
    }

    public void setFinalJson(String finalJson)
    {
        this.finalJson = finalJson;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
