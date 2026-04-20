package com.ruoyi.system.domain.dto;

public class DocumentTagDto {
    /** 文档ID */
    private Long documentId;
    /** 选中的标签名称 */
    private String tagName;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}