package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysTag;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.mapper.SysTagMapper;
import com.ruoyi.system.service.ISysTagMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 将实际使用中的文档标签同步成二级菜单
 */
@Service
public class SysTagMenuServiceImpl implements ISysTagMenuService
{
    private static final String AUTO_TAG_MENU = "AUTO_TAG_MENU";

    @Autowired
    private SysTagMapper tagMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTagMenus()
    {
        SysMenu parentMenu = menuMapper.selectDocumentMenuDirectory();
        if (parentMenu == null)
        {
            return;
        }

        ensureTagManageMenu(parentMenu.getMenuId());

        List<SysTag> tags = tagMapper.selectUsedDocumentTags();
        List<String> activePaths = new ArrayList<String>();
        int orderNum = 3;
        for (SysTag tag : tags)
        {
            if (tag == null || tag.getTagId() == null || StringUtils.isEmpty(tag.getTagName()))
            {
                continue;
            }

            String path = "tag-" + tag.getTagId();
            activePaths.add(path);
            SysMenu menu = menuMapper.selectAutoTagMenuByPath(path);
            if (menu == null)
            {
                menu = newTagMenu(parentMenu.getMenuId(), tag, path, orderNum);
                menuMapper.insertMenu(menu);
                roleMenuMapper.copyRoleMenuByUserRoles(tag.getOwnerUserId(), menu.getMenuId());
            }
            else
            {
                String status = StringUtils.isEmpty(menu.getStatus()) ? "0" : menu.getStatus();
                fillTagMenu(menu, parentMenu.getMenuId(), tag, path, orderNum);
                menu.setStatus(status);
                menuMapper.updateMenu(menu);
            }
            orderNum++;
        }

        if (activePaths.isEmpty())
        {
            roleMenuMapper.deleteRoleMenuByAllAutoTagMenus();
            menuMapper.deleteAllAutoTagMenus();
        }
        else
        {
            roleMenuMapper.deleteRoleMenuByAutoTagMenusNotInPaths(activePaths);
            menuMapper.deleteAutoTagMenusNotInPaths(activePaths);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTagMenuStatus(Long tagId, String status)
    {
        String path = tagPath(tagId);
        int rows = menuMapper.updateAutoTagMenuStatus(path, status);
        if (rows == 0)
        {
            syncTagMenus();
            rows = menuMapper.updateAutoTagMenuStatus(path, status);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTagMenuByTagId(Long tagId)
    {
        String path = tagPath(tagId);
        roleMenuMapper.deleteRoleMenuByAutoTagMenuPath(path);
        menuMapper.deleteAutoTagMenuByPath(path);
    }

    private void ensureTagManageMenu(Long parentId)
    {
        SysMenu menu = menuMapper.selectTagManageMenu(parentId);
        if (menu == null)
        {
            menu = new SysMenu();
            menu.setMenuName("标签管理");
            menu.setParentId(parentId);
            menu.setOrderNum(2);
            menu.setPath("tag");
            menu.setComponent("system/tag/index");
            menu.setRouteName("TagManage");
            menu.setIsFrame("1");
            menu.setIsCache("0");
            menu.setMenuType("C");
            menu.setVisible("0");
            menu.setStatus("0");
            menu.setPerms("system:tag:list");
            menu.setIcon("tag");
            menu.setRemark("标签管理页面");
            menu.setCreateBy("system");
            menuMapper.insertMenu(menu);
        }
    }

    private SysMenu newTagMenu(Long parentId, SysTag tag, String path, int orderNum)
    {
        SysMenu menu = new SysMenu();
        menu.setCreateBy("system");
        fillTagMenu(menu, parentId, tag, path, orderNum);
        return menu;
    }

    private void fillTagMenu(SysMenu menu, Long parentId, SysTag tag, String path, int orderNum)
    {
        menu.setMenuName(tag.getTagName());
        menu.setParentId(parentId);
        menu.setOrderNum(orderNum);
        menu.setPath(path);
        menu.setComponent("system/document/index");
        menu.setQuery("{\"exactTag\":\"" + escapeJson(tag.getTagName()) + "\",\"tagPage\":\"1\",\"readonly\":\"1\"}");
        menu.setRouteName("Tag" + tag.getTagId());
        menu.setIsFrame("1");
        menu.setIsCache("1");
        menu.setMenuType("C");
        menu.setVisible("0");
        menu.setStatus("0");
        menu.setPerms("system:document:list");
        menu.setIcon("pdf");
        menu.setRemark(AUTO_TAG_MENU);
        menu.setUpdateBy("system");
    }

    private String tagPath(Long tagId)
    {
        return "tag-" + tagId;
    }

    private String escapeJson(String text)
    {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
