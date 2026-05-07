package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysTag;

/**
 * 标签 服务层
 */
public interface ISysTagService
{
    /**
     * 查询标签列表
     *
     * @param tag 标签信息
     * @return 标签集合
     */
    public List<SysTag> selectTagList(SysTag tag);

    /**
     * 查询标签详情
     *
     * @param tagId 标签ID
     * @return 标签信息
     */
    public SysTag selectTagById(Long tagId);

    /**
     * 新增标签
     *
     * @param tag 标签信息
     * @return 结果
     */
    public int insertTag(SysTag tag);

    /**
     * 修改标签
     *
     * @param tag 标签信息
     * @return 结果
     */
    public int updateTag(SysTag tag);

    /**
     * 删除标签
     *
     * @param tagId 标签ID
     * @return 结果
     */
    public int deleteTagById(Long tagId);

    /**
     * 修改标签页面开关状态
     *
     * @param tagId 标签ID
     * @param status 状态
     * @return 结果
     */
    public int updateTagStatus(Long tagId, String status);
}
