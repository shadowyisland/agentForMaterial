package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysDocument;

public interface ISysDocumentService
{
    /**
     * 查询文档信息
     * * @param documentId 文档ID
     * @return 文档信息
     */
    public SysDocument selectDocumentById(Long documentId);

    /**
     * 查询文档列表
     * * @param document 文档信息
     * @return 文档集合
     */
    public List<SysDocument> selectDocumentList(SysDocument document);

    /**
     * 新增文档
     * * @param document 文档信息
     * @return 结果
     */
    public int insertDocument(SysDocument document);

    /**
     * 修改文档
     * * @param document 文档信息
     * @return 结果
     */
    public int updateDocument(SysDocument document);

    /**
     * 批量删除文档
     * * @param documentIds 需要删除的文档ID
     * @return 结果
     */
    public int deleteDocumentByIds(Long[] documentIds);

    /**
     * 执行OCR识别
     * * @param documentId 文档ID
     * @return 结果
     */
    public int ocrDocument(Long documentId);
}