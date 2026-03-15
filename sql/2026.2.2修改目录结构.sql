-- ===================================================================
-- 脚本说明：
-- 重构菜单：将[文档管理]提升为一级目录，原页面改名为[原材料管理]
-- ===================================================================

-- 1. 清理旧数据：防止重复插入
-- 删除可能存在的旧“文档管理”、“原材料管理”菜单及其子按钮
DELETE FROM sys_menu WHERE menu_name IN ('文档管理', '原材料管理');
-- 删除可能存在的旧组件路径记录（双重保险）
DELETE FROM sys_menu WHERE component = 'system/document/index';
-- 删除可能存在的旧路由路径记录
DELETE FROM sys_menu WHERE path = 'material' OR path = '/material';


-- 2. 插入一级目录：【文档管理】
-- 注意：path 必须以 '/' 开头 (如 '/material')，否则 Vue Router 会报 404 警告
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
    (
        '文档管理',             -- 菜单名称
        0,                     -- 0 代表一级菜单
        0,                     -- 排序 0 (排在最前面，系统管理之前)
        '/material',           -- 【关键修复】路由地址必须带斜杠 /
        'Layout',              -- 一级目录组件必须是 Layout
        0, 0, 'M', '0', '0',   -- 类型 M=目录
        NULL,                  -- 目录无需权限标识
        'documentation',       -- 图标
        'admin', NOW(), '一级业务目录'
    );

-- 获取刚才插入的一级目录 ID，用于给子菜单做父ID
SET @parentId = LAST_INSERT_ID();


-- 3. 插入二级菜单：【原材料管理】
-- 这是实际的功能页面
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
    (
        '原材料管理',           -- 菜单名称
        @parentId,             -- 挂载到上面的目录ID
        1,                     -- 排序 1
        'document',            -- 子路由地址 (不需要斜杠)
        'system/document/index', -- 指向 Vue 文件路径
        0, 0, 'C', '0', '0',   -- 类型 C=菜单
        'system:document:list',-- 权限标识
        'pdf',                 -- 图标
        'admin', NOW(), '原材料管理页面'
    );

-- 获取刚才插入的二级菜单 ID，用于给按钮做父ID
SET @menuId = LAST_INSERT_ID();


-- 4. 插入页面内的按钮权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
    ('文档查询', @menuId, 1, '#', '', 0, 0, 'F', '0', '0', 'system:document:query', '#', 'admin', NOW(), ''),
    ('文档新增', @menuId, 2, '#', '', 0, 0, 'F', '0', '0', 'system:document:add',   '#', 'admin', NOW(), ''),
    ('文档修改', @menuId, 3, '#', '', 0, 0, 'F', '0', '0', 'system:document:edit',  '#', 'admin', NOW(), ''),
    ('文档删除', @menuId, 4, '#', '', 0, 0, 'F', '0', '0', 'system:document:remove','#', 'admin', NOW(), ''),
    ('文档导出', @menuId, 5, '#', '', 0, 0, 'F', '0', '0', 'system:document:export','#', 'admin', NOW(), '');
