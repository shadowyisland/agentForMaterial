package com.ruoyi.system.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 文档管理对象 sys_document
 */
public class SysDocument extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private Long documentId;

    /** 文档名称 */
    @Excel(name = "文档名称")
    private String documentName;

    /** 产品名称 */
    @Excel(name = "产品名称")
    private String productName;

    /** 产品型号 */
    @Excel(name = "产品型号")
    private String productModel;

    /** 内部编号 */
    @Excel(name = "内部编号")
    private String internalCode;

    /** 文件路径 */
    @Excel(name = "文件路径")
    private String filePath;

    /** 原文件名 */
    @Excel(name = "原文件名")
    private String fileOriginName;

    /** 文件后缀 */
    @Excel(name = "文件后缀")
    private String fileSuffix;

    /** 文件大小 */
    @Excel(name = "文件大小")
    private Long fileSize;

    /** MIME类型 */
    @Excel(name = "MIME类型")
    private String mimeType;

    /** OCR识别内容 */
    private String ocrContent;

    /** OCR是否完成 */
    private Integer isRecognized;

    /** OCR完成时间 */
    private Date ocrTime;

    /** OCR失败原因 */
    private String ocrError;

    /** 文档类型：INTERNAL / EXTERNAL */
    private String documentType;

    /** 内部材料分类 */
    private String materialCategory;

    /** 文档资料类型：TDS / MSDS */
    private String documentKind;

    /** 外部文档来源单位或网站 */
    private String sourceName;

    /** 外部文档原始链接 */
    private String sourceUrl;

    /** 外部文档发布日期 */
    private Date publishDate;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 上传者用户ID */
    private Long createUserId;

    private String searchTag;

    private String exactTag;

    /** 全局检索关键词（空格分词） */
    private String keyword;

    /** 检索范围：all / ocr / record */
    private String searchScope;

    /** 已转义的检索词，仅供Mapper使用 */
    @JsonIgnore
    private List<String> keywords;

    /** 命中来源与摘要（非数据库字段） */
    private String matchSource;

    private String matchSnippet;

    /** 标签列表（非数据库字段，用于接收前端传参） */
    private List<String> tags;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductModel() {
        return productModel;
    }

    public void setProductModel(String productModel) {
        this.productModel = productModel;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileOriginName() {
        return fileOriginName;
    }

    public void setFileOriginName(String fileOriginName) {
        this.fileOriginName = fileOriginName;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOcrContent() {
        return ocrContent;
    }

    public void setOcrContent(String ocrContent) {
        this.ocrContent = ocrContent;
    }

    public Integer getIsRecognized() {
        return isRecognized;
    }

    public void setIsRecognized(Integer isRecognized) {
        this.isRecognized = isRecognized;
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

    public String getStatus() {
        return status;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getMaterialCategory() {
        return materialCategory;
    }

    public void setMaterialCategory(String materialCategory) {
        this.materialCategory = materialCategory;
    }

    public String getDocumentKind() {
        return documentKind;
    }

    public void setDocumentKind(String documentKind) {
        this.documentKind = documentKind;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSearchTag() {
        return searchTag;
    }

    public void setSearchTag(String searchTag) {
        this.searchTag = searchTag;
    }

    public String getExactTag() {
        return exactTag;
    }

    public void setExactTag(String exactTag) {
        this.exactTag = exactTag;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getSearchScope() {
        return searchScope;
    }

    public void setSearchScope(String searchScope) {
        this.searchScope = searchScope;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getMatchSource() {
        return matchSource;
    }

    public void setMatchSource(String matchSource) {
        this.matchSource = matchSource;
    }

    public String getMatchSnippet() {
        return matchSnippet;
    }

    public void setMatchSnippet(String matchSnippet) {
        this.matchSnippet = matchSnippet;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("documentId", getDocumentId())
                .append("documentName", getDocumentName())
                .append("productName", getProductName())
                .append("productModel", getProductModel())
                .append("internalCode", getInternalCode())
                .append("filePath", getFilePath())
                .append("fileOriginName", getFileOriginName())
                .append("fileSuffix", getFileSuffix())
                .append("fileSize", getFileSize())
                .append("mimeType", getMimeType())
                .append("ocrContent", getOcrContent())
                .append("isRecognized", getIsRecognized())
                .append("documentType", getDocumentType())
                .append("materialCategory", getMaterialCategory())
                .append("documentKind", getDocumentKind())
                .append("sourceName", getSourceName())
                .append("sourceUrl", getSourceUrl())
                .append("publishDate", getPublishDate())
                .append("status", getStatus())
                .append("createUserId", getCreateUserId())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .append("exactTag", getExactTag())
                .append("tags", getTags())
                .toString();
    }
}
