package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysTag;
import org.apache.ibatis.annotations.Param;

/**
 * 标签Mapper接口
 */
public interface SysTagMapper
{
    /**
     * 检查标签是否存在
     */
    public SysTag checkTagUnique(@Param("userId") Long userId, @Param("tagKey") String tagKey);

    /**
     * 新增标签
     */
    public int insertTag(SysTag sysTag);

    /**
     * 获取用户所有标签（按使用频率排序）
     */
    public List<String> selectTagsByUserId(Long userId);

    /**
     * 关联文档和标签
     */
    public int insertDocTag(@Param("documentId") Long documentId, @Param("tagId") Long tagId);

    /**
     * 根据文档ID删除标签关联
     */
    public int deleteDocTagByDocId(Long documentId);
}