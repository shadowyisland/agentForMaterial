-- 文档资料类型与 AI 抽取结果表。
-- 请手动执行本文件；应用不会自动修改数据库结构。

ALTER TABLE `sys_document`
    ADD COLUMN `document_kind` varchar(10) DEFAULT NULL COMMENT '文档资料类型：TDS/MSDS' AFTER `material_category`;

CREATE TABLE IF NOT EXISTS `sys_document_tds_extract` (
    `extract_id` bigint NOT NULL AUTO_INCREMENT COMMENT '抽取ID',
    `document_id` bigint NOT NULL COMMENT '文档ID',
    `material_type` varchar(32) NOT NULL COMMENT '材料分类',
    `model_name` varchar(128) DEFAULT NULL COMMENT '模型名称',
    `prompt_version` varchar(64) DEFAULT NULL COMMENT '提示词版本',
    `ai_json` longtext COMMENT 'AI 原始 JSON',
    `final_json` longtext COMMENT '用户最终 JSON',
    `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/confirmed',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`extract_id`),
    KEY `idx_tds_extract_document` (`document_id`, `extract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档 TDS AI 抽取结果';

CREATE TABLE IF NOT EXISTS `sys_document_msds_extract` (
    `extract_id` bigint NOT NULL AUTO_INCREMENT COMMENT '抽取ID',
    `document_id` bigint NOT NULL COMMENT '文档ID',
    `material_type` varchar(32) NOT NULL COMMENT '材料分类',
    `model_name` varchar(128) DEFAULT NULL COMMENT '模型名称',
    `prompt_version` varchar(64) DEFAULT NULL COMMENT '提示词版本',
    `ai_json` longtext COMMENT 'AI 原始 JSON',
    `final_json` longtext COMMENT '用户最终 JSON',
    `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/confirmed',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`extract_id`),
    KEY `idx_msds_extract_document` (`document_id`, `extract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档 MSDS AI 抽取结果';

-- AI 抽取接口权限。按钮菜单不会出现在左侧导航，只会出现在角色管理的菜单权限树中。
-- 以当前“文档管理”目录作为父级；重复执行时会先清理旧记录。
SET @document_root_id := (
    SELECT `menu_id`
    FROM `sys_menu`
    WHERE `menu_name` = '文档管理' AND `menu_type` = 'M'
    ORDER BY `menu_id` DESC
    LIMIT 1
);

DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (
    SELECT `menu_id`
    FROM `sys_menu`
    WHERE `perms` IN (
        'system:document:extract',
        'system:document:extract:query',
        'system:document:extract:edit',
        'system:document:extract:download'
    )
);

DELETE FROM `sys_menu`
WHERE `perms` IN (
    'system:document:extract',
    'system:document:extract:query',
    'system:document:extract:edit',
    'system:document:extract:download'
);

INSERT INTO `sys_menu`
    (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
     `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 'AI抽取', @document_root_id, 30, '#', '', 1, 0,
       'F', '0', '0', 'system:document:extract', '#', 'admin', NOW(), 'DOCUMENT_EXTRACT_PERMISSION'
WHERE @document_root_id IS NOT NULL
UNION ALL
SELECT '解析结果查看', @document_root_id, 31, '#', '', 1, 0,
       'F', '0', '0', 'system:document:extract:query', '#', 'admin', NOW(), 'DOCUMENT_EXTRACT_PERMISSION'
WHERE @document_root_id IS NOT NULL
UNION ALL
SELECT '解析结果保存', @document_root_id, 32, '#', '', 1, 0,
       'F', '0', '0', 'system:document:extract:edit', '#', 'admin', NOW(), 'DOCUMENT_EXTRACT_PERMISSION'
WHERE @document_root_id IS NOT NULL
UNION ALL
SELECT '解析结果下载', @document_root_id, 33, '#', '', 1, 0,
       'F', '0', '0', 'system:document:extract:download', '#', 'admin', NOW(), 'DOCUMENT_EXTRACT_PERMISSION'
WHERE @document_root_id IS NOT NULL;

-- 保持已有角色的能力：具备文档查询的角色可查看解析结果；具备文档修改的角色可抽取、保存和下载。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT role_menu.`role_id`, extract_menu.`menu_id`
FROM `sys_role_menu` role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.`menu_id` = role_menu.`menu_id`
INNER JOIN `sys_menu` extract_menu ON extract_menu.`perms` = 'system:document:extract:query'
WHERE source_menu.`perms` = 'system:document:query';

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT role_menu.`role_id`, extract_menu.`menu_id`
FROM `sys_role_menu` role_menu
INNER JOIN `sys_menu` source_menu ON source_menu.`menu_id` = role_menu.`menu_id`
INNER JOIN `sys_menu` extract_menu ON extract_menu.`perms` IN (
    'system:document:extract',
    'system:document:extract:edit',
    'system:document:extract:download'
)
WHERE source_menu.`perms` = 'system:document:edit';
