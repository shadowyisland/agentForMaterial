package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysRoleMenu;
import org.apache.ibatis.annotations.Param;

/**
 * 角色与菜单关联表 数据层
 * 
 * @author ruoyi
 */
public interface SysRoleMenuMapper
{
    /**
     * 查询菜单使用数量
     * 
     * @param menuId 菜单ID
     * @return 结果
     */
    public int checkMenuExistRole(Long menuId);

    /**
     * 通过角色ID删除角色和菜单关联
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    public int deleteRoleMenuByRoleId(Long roleId);

    /**
     * 通过菜单ID删除角色和菜单关联
     */
    public int deleteRoleMenuByMenuId(Long menuId);

    /**
     * 批量删除角色菜单关联信息
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteRoleMenu(Long[] ids);

    /**
     * 批量新增角色菜单信息
     * 
     * @param roleMenuList 角色菜单列表
     * @return 结果
     */
    public int batchRoleMenu(List<SysRoleMenu> roleMenuList);

    /**
     * 复制已有菜单的角色授权到新菜单
     */
    public int copyRoleMenuBySourceMenu(@Param("sourceMenuId") Long sourceMenuId, @Param("targetMenuId") Long targetMenuId);

    /**
     * 复制用户所属角色到新菜单
     */
    public int copyRoleMenuByUserRoles(@Param("userId") Long userId, @Param("targetMenuId") Long targetMenuId);

    /**
     * 删除全部自动标签菜单的角色授权
     */
    public int deleteRoleMenuByAllAutoTagMenus();

    /**
     * 删除不再使用的自动标签菜单角色授权
     */
    public int deleteRoleMenuByAutoTagMenusNotInPaths(List<String> paths);

    /**
     * 根据自动标签菜单路由删除角色授权
     */
    public int deleteRoleMenuByAutoTagMenuPath(String path);
}
