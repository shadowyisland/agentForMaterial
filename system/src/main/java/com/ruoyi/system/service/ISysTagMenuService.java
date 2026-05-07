package com.ruoyi.system.service;

/**
 * 文档标签菜单同步服务
 */
public interface ISysTagMenuService
{
    /**
     * 同步实际有文档的标签到 sys_menu
     */
    public void syncTagMenus();

    /**
     * 修改标签菜单状态
     */
    public int updateTagMenuStatus(Long tagId, String status);

    /**
     * 删除标签菜单及角色授权
     */
    public void deleteTagMenuByTagId(Long tagId);
}
