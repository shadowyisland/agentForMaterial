package com.ruoyi.system.mapper;

import java.util.Date;
import java.util.List;
import com.ruoyi.system.domain.SysTag;
import org.apache.ibatis.annotations.Param;

/**
 * 标签Mapper接口
 */
public interface SysTagMapper
{
    /**
     * 查询标签列表
     */
    public List<SysTag> selectTagList(SysTag tag);

    /**
     * 查询标签详情
     */
    public SysTag selectTagById(Long tagId);

    /**
     * 检查标签是否存在
     */
    public SysTag checkTagUnique(@Param("userId") Long userId, @Param("tagKey") String tagKey);

    /**
     * 新增标签
     */
    public int insertTag(SysTag sysTag);

    /**
     * 修改标签
     */
    public int updateTag(SysTag sysTag);

    /**
     * 根据标签ID删除标签
     */
    public int deleteTagById(Long tagId);

    /**
     * 根据标签ID查询关联文档名称
     */
    public List<String> selectDocumentNamesByTagId(Long tagId);

    /**
     * 获取用户所有标签（按使用频率排序）
     */
    public List<String> selectTagsByUserId(Long userId);

    /**
     * 获取已有文档关联的标签
     */
    public List<SysTag> selectUsedDocumentTags();

    /**
     * 查询文档关联的标签ID
     */
    public List<Long> selectTagIdsByDocumentId(Long documentId);

    /**
     * 查询多个文档关联的标签ID
     */
    public List<Long> selectTagIdsByDocumentIds(Long[] documentIds);

    /**
     * 查询已无有效文档关联的标签ID
     */
    public List<Long> selectUnusedTagIds(@Param("tagIds") List<Long> tagIds);

    /**
     * 批量删除标签
     */
    public int deleteTagByIds(@Param("tagIds") List<Long> tagIds);

    /**
     * 关联文档和标签
     */
    public int insertDocTag(@Param("documentId") Long documentId, @Param("tagId") Long tagId);

    /**
     * 根据文档ID删除标签关联
     */
    public int deleteDocTagByDocId(Long documentId);

    /**
     * 根据文档ID批量删除标签关联
     */
    public int deleteDocTagByDocIds(Long[] documentIds);

    /** 查询用户私有同名标签 */
    SysTag selectTagByNameAndUserId(@Param("tagName") String tagName, @Param("userId") Long userId);


    /** 检查文档与某标签是否已有绑定 */
    int checkDocumentTag(@Param("documentId") Long documentId, @Param("tagId") Long tagId);

    /** 插入文档与标签的关联 */
    int insertDocumentTag(@Param("documentId") Long documentId, @Param("tagId") Long tagId, @Param("createTime") Date createTime);

    /** 清空某一文档的全部标签关联 */
    int deleteDocumentTagByDocumentId(Long documentId);

    /** 清空某一标签的全部文档关联 */
    int deleteDocumentTagByTagId(Long tagId);

    /** 批量清空标签的全部文档关联 */
    int deleteDocumentTagByTagIds(@Param("tagIds") List<Long> tagIds);
}
