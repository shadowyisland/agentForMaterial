package com.ruoyi.system.domain;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 文档管理对象 sys_document
 */
public class SysDocument extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 文档名称
     */
    @Excel(name = "文档名称")
    private String documentName;

    /**
     * 文件路径
     */
    @Excel(name = "文件路径")
    private String filePath;

    /**
     * 原文件名
     */
    @Excel(name = "原文件名")
    private String fileOriginName;

    /**
     * 文件后缀
     */
    @Excel(name = "文件后缀")
    private String fileSuffix;

    /**
     * 文件大小
     */
    @Excel(name = "文件大小")
    private Long fileSize;

    /**
     * MIME类型
     */
    @Excel(name = "MIME类型")
    private String mimeType;

    /**
     * OCR识别内容
     */
    private String ocrContent;

    /**
     * OCR是否完成
     */
    private Integer isRecognized;

    /**
     * OCR完成时间
     */
    private Date ocrTime;

    /**
     * OCR失败原因
     */
    private String ocrError;

    /**
     * 状态
     */
    @Excel(name = "状态")
    private String status;

    // --- 新增代码 Start ---
    /**
     * 标签列表（非数据库字段，用于接收前端传参）
     */
    private List<String> tags;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    // --- 新增代码 End ---

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFileOriginName(String fileOriginName) {
        this.fileOriginName = fileOriginName;
    }

    public String getFileOriginName() {
        return fileOriginName;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setOcrContent(String ocrContent) {
        this.ocrContent = ocrContent;
    }

    public String getOcrContent() {
        return ocrContent;
    }

    public void setIsRecognized(Integer isRecognized) {
        this.isRecognized = isRecognized;
    }

    public Integer getIsRecognized() {
        return isRecognized;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public Date getOcrTime() {
        return ocrTime;
    }

    public void setOcrTime(Date ocrTime) {
        this.ocrTime = ocrTime;
    }

    public String getOcrError() {
        return ocrError;
    }

    public void setOcrError(String ocrError) {
        this.ocrError = ocrError;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("documentId", getDocumentId())
                .append("documentName", getDocumentName())
                .append("filePath", getFilePath())
                .append("fileOriginName", getFileOriginName())
                .append("fileSuffix", getFileSuffix())
                .append("fileSize", getFileSize())
                .append("mimeType", getMimeType())
                .append("ocrContent", getOcrContent())
                .append("isRecognized", getIsRecognized())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .append("tags", getTags())
                .toString();
    }
}