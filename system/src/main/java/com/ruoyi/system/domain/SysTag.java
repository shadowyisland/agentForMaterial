package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 标签对象 sys_tag
 */
public class SysTag extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long tagId;

    private Long ownerUserId;

    private String tagName;

    private String tagKey;

    public void setTagId(Long tagId)
    {
        this.tagId = tagId;
    }

    public Long getTagId()
    {
        return tagId;
    }
    public void setOwnerUserId(Long ownerUserId)
    {
        this.ownerUserId = ownerUserId;
    }

    public Long getOwnerUserId()
    {
        return ownerUserId;
    }
    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public String getTagName()
    {
        return tagName;
    }
    public void setTagKey(String tagKey)
    {
        this.tagKey = tagKey;
    }

    public String getTagKey()
    {
        return tagKey;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("tagId", getTagId())
                .append("ownerUserId", getOwnerUserId())
                .append("tagName", getTagName())
                .append("tagKey", getTagKey())
                .toString();
    }
}