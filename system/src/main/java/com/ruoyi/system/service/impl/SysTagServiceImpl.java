package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.constant.DocumentConstants;
import com.ruoyi.system.domain.SysTag;
import com.ruoyi.system.mapper.SysTagMapper;
import com.ruoyi.system.service.ISysTagService;

/**
 * 标签 服务层实现
 */
@Service
public class SysTagServiceImpl implements ISysTagService
{
    @Autowired
    private SysTagMapper tagMapper;

    @Override
    public List<SysTag> selectTagList(SysTag tag)
    {
        return tagMapper.selectTagList(tag);
    }

    @Override
    public SysTag selectTagById(Long tagId)
    {
        return tagMapper.selectTagById(tagId);
    }

    @Override
    public int insertTag(SysTag tag)
    {
        normalizeScope(tag);
        tag.setTagName(tag.getTagName().trim());
        String tagKey = tag.getTagName().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (tagMapper.checkTagUnique(tag.getDocumentType(), tag.getMaterialCategory(), tagKey) != null)
        {
            throw new ServiceException("当前分类已存在同名标签");
        }
        tag.setCreateTime(DateUtils.getNowDate());
        return tagMapper.insertTag(tag);
    }

    @Override
    public int updateTag(SysTag tag)
    {
        SysTag current = tagMapper.selectTagById(tag.getTagId());
        if (current == null)
        {
            throw new ServiceException("标签不存在");
        }
        tag.setTagName(tag.getTagName().trim());
        String tagKey = tag.getTagName().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        SysTag duplicate = tagMapper.checkTagUnique(current.getDocumentType(), current.getMaterialCategory(), tagKey);
        if (duplicate != null && !duplicate.getTagId().equals(tag.getTagId()))
        {
            throw new ServiceException("当前分类已存在同名标签");
        }
        tag.setUpdateTime(DateUtils.getNowDate());
        return tagMapper.updateTag(tag);
    }

    @Override
    public int deleteTagById(Long tagId)
    {
        tagMapper.deleteDocumentTagByTagId(tagId);
        return tagMapper.deleteTagById(tagId);
    }

    @Override
    public int updateTagStatus(Long tagId, String status)
    {
        return tagMapper.updateTagStatus(tagId, status);
    }

    private void normalizeScope(SysTag tag)
    {
        if (StringUtils.isEmpty(tag.getDocumentType()))
        {
            throw new ServiceException("文档类型不能为空");
        }
        String documentType = tag.getDocumentType().toUpperCase(Locale.ROOT);
        tag.setDocumentType(documentType);
        if (DocumentConstants.TYPE_INTERNAL.equals(documentType))
        {
            if (StringUtils.isEmpty(tag.getMaterialCategory())
                    || !DocumentConstants.MATERIAL_CATEGORIES.contains(tag.getMaterialCategory()))
            {
                throw new ServiceException("材料分类不正确");
            }
        }
        else if (DocumentConstants.TYPE_EXTERNAL.equals(documentType))
        {
            tag.setMaterialCategory(null);
        }
        else
        {
            throw new ServiceException("文档类型不正确");
        }
    }
}
