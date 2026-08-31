-- 材料智能体文档管理升级（只向前迁移，不删除用户表或文档表）
-- 适用：MySQL 8.0；请在目标业务库中执行一次。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
DROP PROCEDURE IF EXISTS drop_index_if_exists;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_alter_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @migration_sql = p_alter_sql;
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;
END$$

CREATE PROCEDURE add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_alter_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @migration_sql = p_alter_sql;
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;
END$$

CREATE PROCEDURE drop_index_if_exists(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_alter_sql TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @migration_sql = p_alter_sql;
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;
END$$
DELIMITER ;

-- 1. 文档类型、材料分类和外部来源信息
CALL add_column_if_missing('sys_document', 'document_type',
    'ALTER TABLE sys_document ADD COLUMN document_type varchar(16) NOT NULL DEFAULT ''INTERNAL'' COMMENT ''文档类型'' AFTER document_id');
CALL add_column_if_missing('sys_document', 'material_category',
    'ALTER TABLE sys_document ADD COLUMN material_category varchar(32) DEFAULT NULL COMMENT ''材料分类'' AFTER document_type');
CALL add_column_if_missing('sys_document', 'source_name',
    'ALTER TABLE sys_document ADD COLUMN source_name varchar(200) DEFAULT NULL COMMENT ''来源单位或网站'' AFTER internal_code');
CALL add_column_if_missing('sys_document', 'source_url',
    'ALTER TABLE sys_document ADD COLUMN source_url varchar(1000) DEFAULT NULL COMMENT ''原始链接'' AFTER source_name');
CALL add_column_if_missing('sys_document', 'publish_date',
    'ALTER TABLE sys_document ADD COLUMN publish_date date DEFAULT NULL COMMENT ''发布日期'' AFTER source_url');

UPDATE sys_document SET document_type = 'INTERNAL'
WHERE document_type IS NULL OR document_type = '';
UPDATE sys_document SET material_category = 'RAW_MATERIAL'
WHERE document_type = 'INTERNAL' AND (material_category IS NULL OR material_category = '');
UPDATE sys_document SET material_category = NULL
WHERE document_type = 'EXTERNAL';
ALTER TABLE sys_document MODIFY COLUMN material_category varchar(32) DEFAULT NULL COMMENT '材料分类';

CALL add_index_if_missing('sys_document', 'idx_doc_scope_del',
    'ALTER TABLE sys_document ADD INDEX idx_doc_scope_del (document_type, material_category, del_flag)');

-- 2. 文档使用记录与备注
CREATE TABLE IF NOT EXISTS sys_document_record (
    record_id bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    document_id bigint NOT NULL COMMENT '文档ID',
    usage_content varchar(4000) DEFAULT NULL COMMENT '使用内容',
    remark varchar(4000) DEFAULT NULL COMMENT '备注',
    create_user_id bigint NOT NULL COMMENT '添加人ID',
    create_by varchar(64) DEFAULT '' COMMENT '添加人姓名',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by varchar(64) DEFAULT '' COMMENT '更新人姓名',
    update_time datetime DEFAULT NULL COMMENT '修改时间',
    del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标记',
    PRIMARY KEY (record_id),
    KEY idx_record_document_del (document_id, del_flag, create_time),
    KEY idx_record_creator (create_user_id),
    CONSTRAINT fk_record_document FOREIGN KEY (document_id)
        REFERENCES sys_document (document_id) ON DELETE CASCADE,
    CONSTRAINT fk_record_user FOREIGN KEY (create_user_id)
        REFERENCES sys_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档使用记录表';

-- 3. 标签变为分类内共享，并迁移旧标签状态
CALL add_column_if_missing('sys_tag', 'document_type',
    'ALTER TABLE sys_tag ADD COLUMN document_type varchar(16) NOT NULL DEFAULT ''INTERNAL'' COMMENT ''文档类型'' AFTER tag_id');
CALL add_column_if_missing('sys_tag', 'material_category',
    'ALTER TABLE sys_tag ADD COLUMN material_category varchar(32) DEFAULT NULL COMMENT ''材料分类'' AFTER document_type');
CALL add_column_if_missing('sys_tag', 'status',
    'ALTER TABLE sys_tag ADD COLUMN status char(1) NOT NULL DEFAULT ''0'' COMMENT ''状态（0启用 1停用）'' AFTER tag_key');

UPDATE sys_tag SET document_type = 'INTERNAL'
WHERE document_type IS NULL OR document_type = '';
UPDATE sys_tag SET material_category = 'RAW_MATERIAL'
WHERE document_type = 'INTERNAL' AND (material_category IS NULL OR material_category = '');
UPDATE sys_tag SET material_category = NULL
WHERE document_type = 'EXTERNAL';
ALTER TABLE sys_tag MODIFY COLUMN material_category varchar(32) DEFAULT NULL COMMENT '材料分类';

UPDATE sys_tag t
INNER JOIN sys_menu m
    ON m.remark = 'AUTO_TAG_MENU'
   AND JSON_VALID(m.`query`) = 1
   AND CAST(JSON_UNQUOTE(JSON_EXTRACT(m.`query`, '$.tagId')) AS UNSIGNED) = t.tag_id
SET t.status = m.status;

DROP TEMPORARY TABLE IF EXISTS tmp_tag_merge;
CREATE TEMPORARY TABLE tmp_tag_merge AS
SELECT tag_id AS old_tag_id,
       FIRST_VALUE(tag_id) OVER (
           PARTITION BY document_type, material_category, tag_key
           ORDER BY CASE WHEN del_flag = '0' THEN 0 ELSE 1 END, tag_id
       ) AS keep_tag_id
FROM sys_tag;
ALTER TABLE tmp_tag_merge ADD PRIMARY KEY (old_tag_id), ADD INDEX idx_keep_tag_id (keep_tag_id);

INSERT IGNORE INTO sys_document_tag (document_id, tag_id, create_time)
SELECT dt.document_id, tm.keep_tag_id, dt.create_time
FROM sys_document_tag dt
INNER JOIN tmp_tag_merge tm ON tm.old_tag_id = dt.tag_id
WHERE tm.old_tag_id <> tm.keep_tag_id;

DELETE dt
FROM sys_document_tag dt
INNER JOIN tmp_tag_merge tm ON tm.old_tag_id = dt.tag_id
WHERE tm.old_tag_id <> tm.keep_tag_id;

DELETE t
FROM sys_tag t
INNER JOIN tmp_tag_merge tm ON tm.old_tag_id = t.tag_id
WHERE tm.old_tag_id <> tm.keep_tag_id;

DROP TEMPORARY TABLE tmp_tag_merge;
CALL drop_index_if_exists('sys_tag', 'uk_owner_tagkey',
    'ALTER TABLE sys_tag DROP INDEX uk_owner_tagkey');
CALL add_index_if_missing('sys_tag', 'uk_tag_scope',
    'ALTER TABLE sys_tag ADD UNIQUE INDEX uk_tag_scope (document_type, material_category, tag_key)');
CALL add_index_if_missing('sys_tag', 'idx_tag_scope_status',
    'ALTER TABLE sys_tag ADD INDEX idx_tag_scope_status (document_type, material_category, status, del_flag)');

-- 4. 记录旧角色权限，以便菜单重建后原样继承文档操作能力
DROP TEMPORARY TABLE IF EXISTS tmp_document_role_access;
CREATE TEMPORARY TABLE tmp_document_role_access AS
SELECT rm.role_id,
       MAX(m.perms = 'system:document:list') AS can_list,
       MAX(m.perms = 'system:document:query') AS can_query,
       MAX(m.perms = 'system:document:add') AS can_add,
       MAX(m.perms = 'system:document:edit') AS can_edit,
       MAX(m.perms = 'system:document:remove') AS can_remove,
       MAX(m.perms = 'system:document:export') AS can_export
FROM sys_role_menu rm
INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
GROUP BY rm.role_id;
ALTER TABLE tmp_document_role_access ADD PRIMARY KEY (role_id);

-- 找出旧文档菜单、标签菜单及自动标签菜单的完整子树
DROP TEMPORARY TABLE IF EXISTS tmp_old_document_menu;
CREATE TEMPORARY TABLE tmp_old_document_menu (menu_id bigint PRIMARY KEY);
INSERT IGNORE INTO tmp_old_document_menu (menu_id)
WITH RECURSIVE old_menu AS (
    SELECT menu_id
    FROM sys_menu
    WHERE remark = 'AUTO_TAG_MENU'
       OR remark LIKE 'BUSINESS_DOCUMENT_%'
       OR component IN ('system/document/index', 'system/document/search', 'system/tag/index')
       OR (menu_name = '文档管理' AND parent_id = 0)
    UNION ALL
    SELECT child.menu_id
    FROM sys_menu child
    INNER JOIN old_menu parent ON child.parent_id = parent.menu_id
)
SELECT menu_id FROM old_menu;

DELETE rm
FROM sys_role_menu rm
INNER JOIN tmp_old_document_menu old_menu ON old_menu.menu_id = rm.menu_id;
DELETE m
FROM sys_menu m
INNER JOIN tmp_old_document_menu old_menu ON old_menu.menu_id = m.menu_id;
DROP TEMPORARY TABLE tmp_old_document_menu;

-- 文档管理一级目录
INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, `query`, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, remark)
VALUES
    ('文档管理', 0, 0, 'documents', 'Layout', NULL, 'DocumentManagement',
     1, 0, 'M', '0', '0', '', 'documentation',
     'admin', NOW(), 'BUSINESS_DOCUMENT_ROOT');
SET @document_root_id = LAST_INSERT_ID();

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, `query`, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, remark)
VALUES
    ('内部文档', @document_root_id, 1, 'internal', 'ParentView', NULL, 'InternalDocument',
     1, 0, 'M', '0', '0', '', 'nested', 'admin', NOW(), 'BUSINESS_DOCUMENT_INTERNAL');
SET @internal_root_id = LAST_INSERT_ID();

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, `query`, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, remark)
VALUES
    ('原材料', @internal_root_id, 1, 'raw-material', 'system/document/index', '{"documentType":"INTERNAL","materialCategory":"RAW_MATERIAL"}', 'RawMaterialDocument', 1, 0, 'C', '0', '0', 'system:document:list', 'pdf', 'admin', NOW(), 'BUSINESS_DOCUMENT_CATEGORY'),
    ('丙烯酸原材料', @internal_root_id, 2, 'acrylic', 'system/document/index', '{"documentType":"INTERNAL","materialCategory":"ACRYLIC"}', 'AcrylicDocument', 1, 0, 'C', '0', '0', 'system:document:list', 'pdf', 'admin', NOW(), 'BUSINESS_DOCUMENT_CATEGORY'),
    ('环氧原材料', @internal_root_id, 3, 'epoxy', 'system/document/index', '{"documentType":"INTERNAL","materialCategory":"EPOXY"}', 'EpoxyDocument', 1, 0, 'C', '0', '0', 'system:document:list', 'pdf', 'admin', NOW(), 'BUSINESS_DOCUMENT_CATEGORY'),
    ('其它树脂原材料', @internal_root_id, 4, 'other-resin', 'system/document/index', '{"documentType":"INTERNAL","materialCategory":"OTHER_RESIN"}', 'OtherResinDocument', 1, 0, 'C', '0', '0', 'system:document:list', 'pdf', 'admin', NOW(), 'BUSINESS_DOCUMENT_CATEGORY'),
    ('填料类原材料', @internal_root_id, 5, 'filler', 'system/document/index', '{"documentType":"INTERNAL","materialCategory":"FILLER"}', 'FillerDocument', 1, 0, 'C', '0', '0', 'system:document:list', 'pdf', 'admin', NOW(), 'BUSINESS_DOCUMENT_CATEGORY'),
    ('有机硅原材料', @internal_root_id, 6, 'silicone', 'system/document/index', '{"documentType":"INTERNAL","materialCategory":"SILICONE"}', 'SiliconeDocument', 1, 0, 'C', '0', '0', 'system:document:list', 'pdf', 'admin', NOW(), 'BUSINESS_DOCUMENT_CATEGORY'),
    ('助剂类原材料', @internal_root_id, 7, 'additive', 'system/document/index', '{"documentType":"INTERNAL","materialCategory":"ADDITIVE"}', 'AdditiveDocument', 1, 0, 'C', '0', '0', 'system:document:list', 'pdf', 'admin', NOW(), 'BUSINESS_DOCUMENT_CATEGORY');

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, `query`, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, remark)
VALUES
    ('外部文档', @document_root_id, 2, 'external', 'system/document/index', '{"documentType":"EXTERNAL"}', 'ExternalDocument', 1, 0, 'C', '0', '0', 'system:document:list', 'international', 'admin', NOW(), 'BUSINESS_DOCUMENT_EXTERNAL');
SET @external_document_id = LAST_INSERT_ID();

INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, `query`, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, remark)
VALUES
    ('文档检索', @document_root_id, 3, 'search', 'system/document/search', NULL, 'DocumentSearch', 1, 0, 'C', '0', '0', 'system:document:search', 'search', 'admin', NOW(), 'BUSINESS_DOCUMENT_SEARCH');
SET @document_search_id = LAST_INSERT_ID();

-- 页面通用按钮权限集中挂载在文档管理目录下
INSERT INTO sys_menu
    (menu_name, parent_id, order_num, path, component, `query`, route_name,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_by, create_time, remark)
VALUES
    ('文档查询', @document_root_id, 10, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:query', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('文档新增', @document_root_id, 11, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:add', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('文档修改', @document_root_id, 12, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:edit', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('文档删除', @document_root_id, 13, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:remove', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('文档导出', @document_root_id, 14, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:export', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('记录查看', @document_root_id, 20, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:record:list', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('记录新增', @document_root_id, 21, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:record:add', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('记录修改', @document_root_id, 22, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:record:edit', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('记录删除', @document_root_id, 23, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:document:record:remove', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('标签查看', @document_root_id, 30, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:tag:list', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('标签新增', @document_root_id, 31, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:tag:add', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('标签修改', @document_root_id, 32, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:tag:edit', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION'),
    ('标签删除', @document_root_id, 33, '#', '', NULL, '', 1, 0, 'F', '0', '0', 'system:tag:remove', '#', 'admin', NOW(), 'BUSINESS_DOCUMENT_PERMISSION');

-- 原先拥有文档查看权限的角色可访问全部分类、检索、标签和自己的记录
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT access.role_id, menu.menu_id
FROM tmp_document_role_access access
INNER JOIN sys_menu menu
    ON menu.remark IN ('BUSINESS_DOCUMENT_ROOT', 'BUSINESS_DOCUMENT_INTERNAL',
                       'BUSINESS_DOCUMENT_CATEGORY', 'BUSINESS_DOCUMENT_EXTERNAL',
                       'BUSINESS_DOCUMENT_SEARCH')
WHERE access.can_list = 1;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT access.role_id, menu.menu_id
FROM tmp_document_role_access access
INNER JOIN sys_menu menu ON menu.perms IN (
    'system:document:query', 'system:document:search',
    'system:document:record:list', 'system:document:record:add',
    'system:document:record:edit', 'system:tag:list'
)
WHERE access.can_list = 1;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT access.role_id, menu.menu_id
FROM tmp_document_role_access access
INNER JOIN sys_menu menu
    ON (menu.perms = 'system:document:add' AND access.can_add = 1)
    OR (menu.perms = 'system:document:edit' AND access.can_edit = 1)
    OR (menu.perms = 'system:document:remove' AND access.can_remove = 1)
    OR (menu.perms = 'system:document:export' AND access.can_export = 1);

-- 超级管理员通常通过框架自动拥有全部权限；保留显式映射便于后台查看角色菜单。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id
FROM sys_menu
WHERE remark LIKE 'BUSINESS_DOCUMENT_%';

DROP TEMPORARY TABLE tmp_document_role_access;

-- 5. 用户管理提升为独立一级菜单，并对非管理员角色移除其菜单/按钮授权
UPDATE sys_menu
SET menu_name = '用户管理', parent_id = 0, order_num = 2,
    path = 'users', route_name = 'UserManagement',
    update_by = 'admin', update_time = NOW()
WHERE menu_id = 100;

DROP TEMPORARY TABLE IF EXISTS tmp_user_menu_tree;
CREATE TEMPORARY TABLE tmp_user_menu_tree (menu_id bigint PRIMARY KEY);
INSERT IGNORE INTO tmp_user_menu_tree (menu_id)
WITH RECURSIVE user_menu AS (
    SELECT menu_id FROM sys_menu WHERE menu_id = 100
    UNION ALL
    SELECT child.menu_id
    FROM sys_menu child
    INNER JOIN user_menu parent ON child.parent_id = parent.menu_id
)
SELECT menu_id FROM user_menu;

DELETE rm
FROM sys_role_menu rm
INNER JOIN tmp_user_menu_tree user_menu ON user_menu.menu_id = rm.menu_id
WHERE rm.role_id <> 1;
DROP TEMPORARY TABLE tmp_user_menu_tree;

DROP PROCEDURE add_column_if_missing;
DROP PROCEDURE add_index_if_missing;
DROP PROCEDURE drop_index_if_exists;

-- 验收辅助结果：迁移前后的业务数量应保持一致（若存在重复标签，仅重复项会被合并）。
SELECT
    (SELECT COUNT(*) FROM sys_document WHERE del_flag = '0') AS active_document_count,
    (SELECT COUNT(*) FROM sys_tag WHERE del_flag = '0') AS active_tag_count,
    (SELECT COUNT(*) FROM sys_user WHERE del_flag = '0') AS active_user_count,
    (SELECT COUNT(*) FROM sys_document_record WHERE del_flag = '0') AS active_record_count;
