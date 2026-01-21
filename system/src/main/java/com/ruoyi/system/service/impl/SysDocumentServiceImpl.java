package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.service.ISysDocumentService;
import com.ruoyi.common.exception.ServiceException;

@Service
public class SysDocumentServiceImpl implements ISysDocumentService
{
    @Autowired
    private SysDocumentMapper documentMapper;

    @Override
    public SysDocument selectDocumentById(Long documentId) {
        return documentMapper.selectDocumentById(documentId);
    }

    @Override
    public List<SysDocument> selectDocumentList(SysDocument document) {
        return documentMapper.selectDocumentList(document);
    }

    @Override
    public int insertDocument(SysDocument document) {
        document.setCreateTime(DateUtils.getNowDate());
        return documentMapper.insertDocument(document);
    }

    @Override
    public int updateDocument(SysDocument document) {
        document.setUpdateTime(DateUtils.getNowDate());
        return documentMapper.updateDocument(document);
    }

    @Override
    public int deleteDocumentByIds(Long[] documentIds) {
        return documentMapper.deleteDocumentByIds(documentIds);
    }

    @Override
    public int ocrDocument(Long documentId) {
        SysDocument doc = documentMapper.selectDocumentById(documentId);
        if (doc == null) {
            throw new ServiceException("文档不存在");
        }

        // TODO: 真正的OCR调用逻辑应在此处编写 (调用百度API等)
        // 目前阶段：直接模拟识别成功，以便前端测试

        try {
            // 模拟耗时
            Thread.sleep(500);

            // 模拟返回结果
            String mockResult = "【开发模式】这是模拟的OCR识别结果。\n" +
                    "文档ID: " + documentId + "\n" +
                    "识别时间: " + DateUtils.getTime();

            // 更新数据库状态
            doc.setIsRecognized(1); // 1表示已识别
            doc.setOcrContent(mockResult);
            doc.setOcrTime(DateUtils.getNowDate());
            doc.setOcrError(null); // 清除之前的错误

            return documentMapper.updateDocument(doc);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("OCR识别被中断");
        }
    }
}