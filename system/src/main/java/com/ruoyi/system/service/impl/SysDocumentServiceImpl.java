package com.ruoyi.system.service.impl;

import java.util.List;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.service.ISysDocumentService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.ocr.BaiduOcrService;

@Service
public class SysDocumentServiceImpl implements ISysDocumentService
{
    @Autowired
    private SysDocumentMapper documentMapper;

    // 注入百度OCR服务
    @Autowired
    private BaiduOcrService baiduOcrService;

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

        // 1. 获取文件的本地绝对路径
        String filePath = doc.getFilePath();
        if (StringUtils.isEmpty(filePath)) {
            throw new ServiceException("文件路径为空，无法识别");
        }

        // 将虚拟路径 /profile/upload/2026/... 转换为本地绝对路径 D:/ruoyi/uploadPath/upload/2026/...
        // Constants.RESOURCE_PREFIX 默认为 "/profile"
        String localPath = RuoYiConfig.getProfile() + StringUtils.substringAfter(filePath, Constants.RESOURCE_PREFIX);

        // 2. 调用百度OCR服务
        // 注意：OCR识别可能较慢，建议在生产环境中使用异步任务处理，这里为了演示直接同步调用
        String result = baiduOcrService.recognizeGeneral(localPath);

        // 3. 更新数据库状态
        doc.setIsRecognized(1); // 标记为已识别
        doc.setOcrContent(result);
        doc.setOcrTime(DateUtils.getNowDate());
        // 如果识别结果包含"失败"字样，你也可以选择记录到 Error 字段
        // doc.setOcrError(...);

        return documentMapper.updateDocument(doc);
    }
}