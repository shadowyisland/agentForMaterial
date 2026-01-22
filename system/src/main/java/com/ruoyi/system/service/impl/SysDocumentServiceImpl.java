package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysTag;
import com.ruoyi.system.mapper.SysTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.service.ISysDocumentService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档管理Service业务层处理
 */
@Service
public class SysDocumentServiceImpl implements ISysDocumentService {

    @Autowired
    private SysDocumentMapper documentMapper;

    @Autowired
    private SysTagMapper sysTagMapper; // 引入标签Mapper

    @Override
    public SysDocument selectDocumentById(Long documentId) {
        return documentMapper.selectDocumentById(documentId);
    }

    @Override
    public List<SysDocument> selectDocumentList(SysDocument document) {
        return documentMapper.selectDocumentList(document);
    }

    /**
     * 新增文档（包含标签处理逻辑）
     */
    @Override
    @Transactional // 开启事务，确保文档和标签同时成功
    public int insertDocument(SysDocument document) {
        document.setCreateTime(DateUtils.getNowDate());
        // 1. 保存文档基础信息
        int rows = documentMapper.insertDocument(document);

        // 2. 处理标签逻辑 (新增部分)
        if (document.getTags() != null && !document.getTags().isEmpty()) {
            Long userId = SecurityUtils.getUserId(); // 获取当前登录用户

            for (String tagName : document.getTags()) {
                if (StringUtils.isEmpty(tagName)) continue;

                // 生成标准化key (去除空格，转小写) 用于查重
                String cleanTagName = tagName.trim();
                String tagKey = cleanTagName.toLowerCase().replaceAll("\\s+", "");

                // A. 检查标签是否已存在
                SysTag tag = sysTagMapper.checkTagUnique(userId, tagKey);
                Long tagId;

                if (tag == null) {
                    // B. 不存在，创建新标签
                    tag = new SysTag();
                    tag.setOwnerUserId(userId);
                    tag.setTagName(cleanTagName);
                    tag.setTagKey(tagKey); // 记得存入 key
                    tag.setCreateBy(document.getCreateBy());
                    sysTagMapper.insertTag(tag);
                    tagId = tag.getTagId(); // 获取回填的主键
                } else {
                    // C. 存在，直接使用ID
                    tagId = tag.getTagId();
                }

                // D. 在中间表中建立关联
                sysTagMapper.insertDocTag(document.getDocumentId(), tagId);
            }
        }
        return rows;
    }

    @Override
    public int updateDocument(SysDocument document) {
        document.setUpdateTime(DateUtils.getNowDate());
        // 注意：如果修改文档时也允许修改标签，这里也需要加类似的标签处理逻辑
        // 目前版本暂只处理新增时的标签
        return documentMapper.updateDocument(document);
    }

    @Override
    public int deleteDocumentByIds(Long[] documentIds) {
        // 建议：删除文档时，最好同时也删除 sys_document_tag 表中的关联记录
        // sysTagMapper.deleteDocTagByDocIds(documentIds);
        return documentMapper.deleteDocumentByIds(documentIds);
    }

    /**
     * OCR 识别逻辑 (保留你原有的代码)
     */
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
            // 注意：SysDocument 实体类里要有 ocrTime 和 ocrError 字段，否则这里会爆红
            // 如果实体类还没加这俩字段，请先注释掉下面两行
            // doc.setOcrTime(DateUtils.getNowDate());
            // doc.setOcrError(null);

            return documentMapper.updateDocument(doc);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("OCR识别被中断");
        }
    }
}