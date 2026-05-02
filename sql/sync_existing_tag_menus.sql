-- 根据现有 sys_document / sys_tag / sys_document_tag 初始化标签字段二级菜单
-- 只生成 SQL，不自动执行。
-- 动态菜单统一使用 remark = 'AUTO_TAG_MENU' 标识，方便后续安全清理。

SET NAMES utf8mb4;
START TRANSACTION;

-- 1. 定位“文档管理”目录和“原材料管理”菜单
SET @documentParentId := (
    SELECT menu_id
    FROM sys_menu
    WHERE menu_name = '文档管理'
      AND menu_type = 'M'
      AND status = '0'
    ORDER BY menu_id DESC
    LIMIT 1
);

SET @materialMenuId := (
    SELECT menu_id
    FROM sys_menu
    WHERE parent_id = @documentParentId
      AND menu_name = '原材料管理'
      AND component = 'system/document/index'
      AND menu_type = 'C'
      AND status = '0'
    LIMIT 1
);

-- 如果没有找到基础菜单，直接抛错，避免插错父级
SELECT
    CASE
        WHEN @documentParentId IS NULL THEN 1 / 0
        WHEN @materialMenuId IS NULL THEN 1 / 0
        ELSE 1
    END AS check_document_menu;

-- 2. 清理旧的自动标签菜单及其角色授权
DELETE rm
FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.remark = 'AUTO_TAG_MENU';

DELETE FROM sys_menu
WHERE remark = 'AUTO_TAG_MENU';

-- 3. 插入当前已有文档关联的标签菜单
INSERT INTO sys_menu (
    menu_name,
    parent_id,
    order_num,
    path,
    component,
    `query`,
    route_name,
    is_frame,
    is_cache,
    menu_type,
    visible,
    status,
    perms,
    icon,
    create_by,
    create_time,
    remark
)
SELECT
    used_tags.tag_name AS menu_name,
    @documentParentId AS parent_id,
    used_tags.order_num AS order_num,
    CONCAT('tag-', used_tags.tag_id) AS path,
    'system/document/index' AS component,
    JSON_OBJECT(
        'exactTag', used_tags.tag_name,
        'tagPage', '1',
        'readonly', '1'
    ) AS `query`,
    CONCAT('Tag', used_tags.tag_id) AS route_name,
    1 AS is_frame,
    1 AS is_cache,
    'C' AS menu_type,
    '0' AS visible,
    '0' AS status,
    'system:document:list' AS perms,
    'pdf' AS icon,
    'system' AS create_by,
    NOW() AS create_time,
    'AUTO_TAG_MENU' AS remark
FROM (
    SELECT
        MIN(t.tag_id) AS tag_id,
        t.tag_name,
        ROW_NUMBER() OVER (ORDER BY MIN(t.tag_id)) + 1 AS order_num
    FROM sys_tag t
    INNER JOIN sys_document_tag dt ON t.tag_id = dt.tag_id
    INNER JOIN sys_document d ON dt.document_id = d.document_id
    WHERE t.del_flag = '0'
      AND d.del_flag = '0'
    GROUP BY t.tag_name
) used_tags;

-- 4. 复制“原材料管理”已有的角色菜单权限到自动标签菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.remark = 'AUTO_TAG_MENU'
WHERE rm.menu_id = @materialMenuId;

COMMIT;

