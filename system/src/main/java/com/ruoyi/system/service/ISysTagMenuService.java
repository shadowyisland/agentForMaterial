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
}
