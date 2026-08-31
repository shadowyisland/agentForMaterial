package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysDocumentRecord;
import com.ruoyi.system.mapper.SysDocumentMapper;
import com.ruoyi.system.mapper.SysDocumentRecordMapper;
import com.ruoyi.system.service.ISysDocumentRecordService;

/**
 * 文档使用记录服务实现。
 */
@Service
public class SysDocumentRecordServiceImpl implements ISysDocumentRecordService
{
    @Autowired
    private SysDocumentRecordMapper recordMapper;

    @Autowired
    private SysDocumentMapper documentMapper;

    @Override
    public List<SysDocumentRecord> selectRecordList(Long documentId)
    {
        ensureDocumentExists(documentId);
        return recordMapper.selectRecordListByDocumentId(documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRecord(SysDocumentRecord record)
    {
        validateContent(record);
        ensureDocumentExists(record.getDocumentId());
        record.setCreateUserId(SecurityUtils.getUserId());
        record.setCreateBy(SecurityUtils.getUsername());
        record.setCreateTime(DateUtils.getNowDate());
        return recordMapper.insertRecord(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRecord(SysDocumentRecord record)
    {
        validateContent(record);
        SysDocumentRecord current = recordMapper.selectRecordById(record.getRecordId());
        if (current == null)
        {
            throw new ServiceException("使用记录不存在");
        }
        Long userId = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin(userId) && !userId.equals(current.getCreateUserId()))
        {
            throw new ServiceException("只能修改本人添加的使用记录");
        }
        record.setUpdateBy(SecurityUtils.getUsername());
        record.setUpdateTime(DateUtils.getNowDate());
        return recordMapper.updateRecord(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRecord(Long recordId)
    {
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()))
        {
            throw new ServiceException("只有管理员可以删除使用记录");
        }
        SysDocumentRecord current = recordMapper.selectRecordById(recordId);
        if (current == null)
        {
            throw new ServiceException("使用记录不存在");
        }
        current.setUpdateBy(SecurityUtils.getUsername());
        current.setUpdateTime(DateUtils.getNowDate());
        return recordMapper.softDeleteRecord(current);
    }

    private void ensureDocumentExists(Long documentId)
    {
        if (documentId == null || documentMapper.selectDocumentById(documentId) == null)
        {
            throw new ServiceException("文档不存在");
        }
    }

    private void validateContent(SysDocumentRecord record)
    {
        if (record == null || (StringUtils.isEmpty(record.getUsageContent()) && StringUtils.isEmpty(record.getRemark())))
        {
            throw new ServiceException("使用内容和备注至少填写一项");
        }
        if (record.getUsageContent() != null && record.getUsageContent().length() > 4000)
        {
            throw new ServiceException("使用内容不能超过4000个字符");
        }
        if (record.getRemark() != null && record.getRemark().length() > 4000)
        {
            throw new ServiceException("备注不能超过4000个字符");
        }
    }
}
