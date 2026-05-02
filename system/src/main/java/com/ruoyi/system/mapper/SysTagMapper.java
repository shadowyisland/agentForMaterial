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
     * 获取已有文档关联的标签
     */
    public List<SysTag> selectUsedDocumentTags();

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
}
