package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.domain.SysDocument;
import com.ruoyi.system.service.ISysDocumentService;

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
    public int deleteDocumentByIds(Long[] documentIds) {
        return documentMapper.deleteDocumentByIds(documentIds);
    }
}