package com.ruoyi.system.service;

import com.ruoyi.system.constant.DocumentConstants;

/**
 * 文档抽取提示词资源定义。
 */
public enum DocumentExtractPrompt
{
    ACRYLIC_TDS("ACRYLIC", DocumentConstants.KIND_TDS, "ACRYLIC-TDS-prompt.txt"),
    ACRYLIC_MSDS("ACRYLIC", DocumentConstants.KIND_MSDS, "ACRYLIC-MSDS-prompt.txt"),
    EPOXY_TDS("EPOXY", DocumentConstants.KIND_TDS, "EPOXY-TDS-prompt.txt"),
    EPOXY_MSDS("EPOXY", DocumentConstants.KIND_MSDS, "EPOXY-MSDS-prompt.txt"),
    OTHER_RESIN_TDS("OTHER_RESIN", DocumentConstants.KIND_TDS, "OTHER_RESIN-TDS-prompt.txt"),
    OTHER_RESIN_MSDS("OTHER_RESIN", DocumentConstants.KIND_MSDS, "OTHER_RESIN-MSDS-prompt.txt"),
    FILLER_TDS("FILLER", DocumentConstants.KIND_TDS, "FILLER-TDS-prompt.txt"),
    FILLER_MSDS("FILLER", DocumentConstants.KIND_MSDS, "FILLER-MSDS-prompt.txt"),
    SILICONE_TDS("SILICONE", DocumentConstants.KIND_TDS, "SILICONE-TDS-prompt.txt"),
    SILICONE_MSDS("SILICONE", DocumentConstants.KIND_MSDS, "SILICONE-MSDS-prompt.txt"),
    ADDITIVE_TDS("ADDITIVE", DocumentConstants.KIND_TDS, "ADDITIVE-TDS-prompt.txt"),
    ADDITIVE_MSDS("ADDITIVE", DocumentConstants.KIND_MSDS, "ADDITIVE-MSDS-prompt.txt");

    private final String materialCategory;
    private final String documentKind;
    private final String fileName;

    DocumentExtractPrompt(String materialCategory, String documentKind, String fileName)
    {
        this.materialCategory = materialCategory;
        this.documentKind = documentKind;
        this.fileName = fileName;
    }

    public String getFileName()
    {
        return fileName;
    }

    public static DocumentExtractPrompt find(String materialCategory, String documentKind)
    {
        for (DocumentExtractPrompt prompt : values())
        {
            if (prompt.materialCategory.equals(materialCategory) && prompt.documentKind.equals(documentKind))
            {
                return prompt;
            }
        }
        return null;
    }
}
