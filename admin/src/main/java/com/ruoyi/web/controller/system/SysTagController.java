package com.ruoyi.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysTag;
import com.ruoyi.system.service.ISysTagService;

/**
 * 标签管理Controller
 */
@RestController
@RequestMapping("/system/tag")
public class SysTagController extends BaseController
{
    @Autowired
    private ISysTagService tagService;

    /**
     * 查询标签列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysTag tag)
    {
        startPage();
        List<SysTag> list = tagService.selectTagList(tag);
        return getDataTable(list);
    }

    /**
     * 获取标签详细信息
     */
    @GetMapping(value = "/{tagId}")
    public AjaxResult getInfo(@PathVariable Long tagId)
    {
        return success(tagService.selectTagById(tagId));
    }

    /**
     * 新增标签
     */
    @Log(title = "标签管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysTag tag)
    {
        if (StringUtils.isEmpty(tag.getTagName()))
        {
            return error("标签名称不能为空");
        }
        tag.setOwnerUserId(getUserId());
        tag.setCreateBy(getUsername());
        return toAjax(tagService.insertTag(tag));
    }

    /**
     * 修改标签
     */
    @Log(title = "标签管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysTag tag)
    {
        if (tag.getTagId() == null)
        {
            return error("标签ID不能为空");
        }
        if (StringUtils.isEmpty(tag.getTagName()))
        {
            return error("标签名称不能为空");
        }
        tag.setUpdateBy(getUsername());
        return toAjax(tagService.updateTag(tag));
    }

    /**
     * 删除标签
     */
    @Log(title = "标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tagId}")
    public AjaxResult remove(@PathVariable Long tagId)
    {
        return toAjax(tagService.deleteTagById(tagId));
    }

    /**
     * 修改标签页面开关状态
     */
    @Log(title = "标签管理-状态切换", businessType = BusinessType.UPDATE)
    @PutMapping("/{tagId}/status")
    public AjaxResult changeStatus(@PathVariable Long tagId, @RequestParam String status)
    {
        if (!"0".equals(status) && !"1".equals(status))
        {
            return error("状态值只能是0或1");
        }
        return toAjax(tagService.updateTagStatus(tagId, status));
    }
}
