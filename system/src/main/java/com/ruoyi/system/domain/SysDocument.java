package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 文档管理对象 sys_document
 */
public class SysDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private Long documentId;

    /** 文档名称 */
    private String documentName;

    /** 文件路径 */
    private String filePath;

    /** 原始文件名 */
    private String fileOriginName;

    /** 文件后缀 */
    private String fileSuffix;

    /** 是否已OCR识别(0否1是) */
    private Integer isRecognized;

    /** OCR识别文本内容 */
    private String ocrContent;

    /** OCR完成时间 */
    private Date ocrTime;

    /** OCR失败原因 */
    private String ocrError;

    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getDocumentId() { return documentId; }

    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public String getDocumentName() { return documentName; }

    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFilePath() { return filePath; }

    public void setFileOriginName(String fileOriginName) { this.fileOriginName = fileOriginName; }
    public String getFileOriginName() { return fileOriginName; }

    public void setFileSuffix(String fileSuffix) { this.fileSuffix = fileSuffix; }
    public String getFileSuffix() { return fileSuffix; }

    public Integer getIsRecognized() { return isRecognized; }
    public void setIsRecognized(Integer isRecognized) { this.isRecognized = isRecognized; }

    public String getOcrContent() { return ocrContent; }
    public void setOcrContent(String ocrContent) { this.ocrContent = ocrContent; }

    public Date getOcrTime() { return ocrTime; }
    public void setOcrTime(Date ocrTime) { this.ocrTime = ocrTime; }

    public String getOcrError() { return ocrError; }
    public void setOcrError(String ocrError) { this.ocrError = ocrError; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("documentId", getDocumentId())
                .append("documentName", getDocumentName())
                .append("filePath", getFilePath())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("remark", getRemark())
                .append("isRecognized", getIsRecognized())
                .append("ocrContent", getOcrContent())
                .append("ocrTime", getOcrTime())
                .toString();
    }
}