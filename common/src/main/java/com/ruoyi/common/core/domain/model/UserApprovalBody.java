package com.ruoyi.common.core.domain.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 注册申请审批对象
 */
public class UserApprovalBody
{
    /** 用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 是否通过；false 表示拒绝 */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /** 审批通过时分配的角色ID */
    private Long roleId;

    /** 审批意见 */
    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String remark;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Boolean getApproved()
    {
        return approved;
    }

    public void setApproved(Boolean approved)
    {
        this.approved = approved;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
