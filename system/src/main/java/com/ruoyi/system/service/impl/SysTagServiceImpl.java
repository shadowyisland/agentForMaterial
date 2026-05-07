package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.SysTag;
import com.ruoyi.system.mapper.SysTagMapper;
import com.ruoyi.system.service.ISysTagMenuService;
import com.ruoyi.system.service.ISysTagService;

/**
 * 标签 服务层实现
 */
@Service
public class SysTagServiceImpl implements ISysTagService
{
    @Autowired
    private SysTagMapper tagMapper;

    @Autowired
    private ISysTagMenuService tagMenuService;

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
        tag.setCreateTime(DateUtils.getNowDate());
        return tagMapper.insertTag(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTag(SysTag tag)
    {
        tag.setUpdateTime(DateUtils.getNowDate());
        int rows = tagMapper.updateTag(tag);
        tagMenuService.syncTagMenus();
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTagById(Long tagId)
    {
        tagMapper.deleteDocumentTagByTagId(tagId);
        int rows = tagMapper.deleteTagById(tagId);
        tagMenuService.deleteTagMenuByTagId(tagId);
        tagMenuService.syncTagMenus();
        return rows;
    }

    @Override
    public int updateTagStatus(Long tagId, String status)
    {
        return tagMenuService.updateTagMenuStatus(tagId, status);
    }
}
