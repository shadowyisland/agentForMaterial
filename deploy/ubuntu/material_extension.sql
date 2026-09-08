-- Safe deployment extension for the material/document features.
-- Run after sql/ry_20250522.sql and sql/quartz.sql on a fresh database.

SET NAMES utf8mb4;
START TRANSACTION;

CREATE TABLE IF NOT EXISTS `sys_document` (
  `document_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `document_type` varchar(16) NOT NULL DEFAULT 'INTERNAL' COMMENT '文档类型',
  `material_category` varchar(32) DEFAULT NULL COMMENT '材料分类',
  `document_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '文档名称',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '产品名称',
  `product_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '产品型号',
  `internal_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '内部编号',
  `source_name` varchar(200) DEFAULT NULL COMMENT '来源单位或网站',
  `source_url` varchar(1000) DEFAULT NULL COMMENT '原始链接',
  `publish_date` date DEFAULT NULL COMMENT '发布日期',
  `file_origin_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '原始文件名',
  `file_suffix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '文件后缀',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT 'MIME类型',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '本地存储路径',
  `url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '远程文件URL',
  `is_recognized` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已OCR识别',
  `ocr_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'OCR识别文本',
  `ocr_time` datetime DEFAULT NULL COMMENT 'OCR完成时间',
  `ocr_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'OCR失败原因',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '状态',
  `create_user_id` bigint DEFAULT NULL COMMENT '上传者用户ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`document_id`),
  KEY `idx_doc_create_user_time` (`create_user_id`, `create_time`),
  KEY `idx_doc_create_time` (`create_time`),
  KEY `idx_doc_suffix` (`file_suffix`),
  KEY `idx_doc_status` (`status`),
  KEY `idx_doc_del_flag` (`del_flag`),
  KEY `idx_doc_scope_del` (`document_type`, `material_category`, `del_flag`),
  CONSTRAINT `fk_doc_user` FOREIGN KEY (`create_user_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档管理表';

CREATE TABLE IF NOT EXISTS `sys_tag` (
  `tag_id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `document_type` varchar(16) NOT NULL DEFAULT 'INTERNAL' COMMENT '文档类型',
  `material_category` varchar(32) DEFAULT NULL COMMENT '材料分类',
  `owner_user_id` bigint NOT NULL COMMENT '标签所属用户ID',
  `tag_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名',
  `tag_key` varchar(64)
    GENERATED ALWAYS AS (LOWER(REPLACE(TRIM(`tag_name`), ' ', ''))) STORED
    COMMENT '规范化key',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标志',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_tag_scope` (`document_type`, `material_category`, `tag_key`),
  KEY `idx_tag_scope_status` (`document_type`, `material_category`, `status`, `del_flag`),
  KEY `idx_tag_owner` (`owner_user_id`),
  KEY `idx_tag_key` (`tag_key`),
  CONSTRAINT `fk_tag_user` FOREIGN KEY (`owner_user_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私有标签表';

CREATE TABLE IF NOT EXISTS `sys_document_tag` (
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`document_id`, `tag_id`),
  KEY `idx_dt_tag_doc` (`tag_id`, `document_id`),
  KEY `idx_dt_doc` (`document_id`),
  CONSTRAINT `fk_dt_document` FOREIGN KEY (`document_id`) REFERENCES `sys_document` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dt_tag` FOREIGN KEY (`tag_id`) REFERENCES `sys_tag` (`tag_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档-标签关联表';

CREATE TABLE IF NOT EXISTS `sys_document_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `usage_content` varchar(4000) DEFAULT NULL COMMENT '使用内容',
  `remark` varchar(4000) DEFAULT NULL COMMENT '备注',
  `create_user_id` bigint NOT NULL COMMENT '添加人ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '添加人姓名',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新人姓名',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标记',
  PRIMARY KEY (`record_id`),
  KEY `idx_record_document_del` (`document_id`, `del_flag`, `create_time`),
  KEY `idx_record_creator` (`create_user_id`),
  CONSTRAINT `fk_record_document` FOREIGN KEY (`document_id`) REFERENCES `sys_document` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_record_user` FOREIGN KEY (`create_user_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档使用记录表';

DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.remark = 'AUTO_TAG_MENU'
   OR m.remark LIKE 'BUSINESS_DOCUMENT_%'
   OR m.component IN ('system/document/index', 'system/document/search', 'system/tag/index')
   OR (m.menu_name = '文档管理' AND m.parent_id = 0);

DELETE FROM sys_menu
WHERE remark = 'AUTO_TAG_MENU'
   OR remark LIKE 'BUSINESS_DOCUMENT_%'
   OR component IN ('system/document/index', 'system/document/search', 'system/tag/index')
   OR (menu_name = '文档管理' AND parent_id = 0);

INSERT INTO sys_menu
  (menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
VALUES
  ('文档管理',0,0,'documents','Layout',NULL,'DocumentManagement',1,0,'M','0','0','','documentation','admin',NOW(),'BUSINESS_DOCUMENT_ROOT');
SET @documentRootId = LAST_INSERT_ID();

INSERT INTO sys_menu
  (menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
VALUES
  ('内部文档',@documentRootId,1,'internal','ParentView',NULL,'InternalDocument',1,0,'M','0','0','','nested','admin',NOW(),'BUSINESS_DOCUMENT_INTERNAL');
SET @internalRootId = LAST_INSERT_ID();

INSERT INTO sys_menu
  (menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
VALUES
  ('原材料',@internalRootId,1,'raw-material','system/document/index','{"documentType":"INTERNAL","materialCategory":"RAW_MATERIAL"}','RawMaterialDocument',1,0,'C','0','0','system:document:list','pdf','admin',NOW(),'BUSINESS_DOCUMENT_CATEGORY'),
  ('丙烯酸原材料',@internalRootId,2,'acrylic','system/document/index','{"documentType":"INTERNAL","materialCategory":"ACRYLIC"}','AcrylicDocument',1,0,'C','0','0','system:document:list','pdf','admin',NOW(),'BUSINESS_DOCUMENT_CATEGORY'),
  ('环氧原材料',@internalRootId,3,'epoxy','system/document/index','{"documentType":"INTERNAL","materialCategory":"EPOXY"}','EpoxyDocument',1,0,'C','0','0','system:document:list','pdf','admin',NOW(),'BUSINESS_DOCUMENT_CATEGORY'),
  ('其它树脂原材料',@internalRootId,4,'other-resin','system/document/index','{"documentType":"INTERNAL","materialCategory":"OTHER_RESIN"}','OtherResinDocument',1,0,'C','0','0','system:document:list','pdf','admin',NOW(),'BUSINESS_DOCUMENT_CATEGORY'),
  ('填料类原材料',@internalRootId,5,'filler','system/document/index','{"documentType":"INTERNAL","materialCategory":"FILLER"}','FillerDocument',1,0,'C','0','0','system:document:list','pdf','admin',NOW(),'BUSINESS_DOCUMENT_CATEGORY'),
  ('有机硅原材料',@internalRootId,6,'silicone','system/document/index','{"documentType":"INTERNAL","materialCategory":"SILICONE"}','SiliconeDocument',1,0,'C','0','0','system:document:list','pdf','admin',NOW(),'BUSINESS_DOCUMENT_CATEGORY'),
  ('助剂类原材料',@internalRootId,7,'additive','system/document/index','{"documentType":"INTERNAL","materialCategory":"ADDITIVE"}','AdditiveDocument',1,0,'C','0','0','system:document:list','pdf','admin',NOW(),'BUSINESS_DOCUMENT_CATEGORY'),
  ('外部文档',@documentRootId,2,'external','system/document/index','{"documentType":"EXTERNAL"}','ExternalDocument',1,0,'C','0','0','system:document:list','international','admin',NOW(),'BUSINESS_DOCUMENT_EXTERNAL'),
  ('文档检索',@documentRootId,3,'search','system/document/search',NULL,'DocumentSearch',1,0,'C','0','0','system:document:search','search','admin',NOW(),'BUSINESS_DOCUMENT_SEARCH');

INSERT INTO sys_menu
  (menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
VALUES
  ('文档查询',@documentRootId,10,'#','',NULL,'',1,0,'F','0','0','system:document:query','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('文档新增',@documentRootId,11,'#','',NULL,'',1,0,'F','0','0','system:document:add','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('文档修改',@documentRootId,12,'#','',NULL,'',1,0,'F','0','0','system:document:edit','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('文档删除',@documentRootId,13,'#','',NULL,'',1,0,'F','0','0','system:document:remove','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('文档导出',@documentRootId,14,'#','',NULL,'',1,0,'F','0','0','system:document:export','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('记录查看',@documentRootId,20,'#','',NULL,'',1,0,'F','0','0','system:document:record:list','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('记录新增',@documentRootId,21,'#','',NULL,'',1,0,'F','0','0','system:document:record:add','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('记录修改',@documentRootId,22,'#','',NULL,'',1,0,'F','0','0','system:document:record:edit','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('记录删除',@documentRootId,23,'#','',NULL,'',1,0,'F','0','0','system:document:record:remove','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('标签查看',@documentRootId,30,'#','',NULL,'',1,0,'F','0','0','system:tag:list','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('标签新增',@documentRootId,31,'#','',NULL,'',1,0,'F','0','0','system:tag:add','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('标签修改',@documentRootId,32,'#','',NULL,'',1,0,'F','0','0','system:tag:edit','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION'),
  ('标签删除',@documentRootId,33,'#','',NULL,'',1,0,'F','0','0','system:tag:remove','#','admin',NOW(),'BUSINESS_DOCUMENT_PERMISSION');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE remark LIKE 'BUSINESS_DOCUMENT_%';

UPDATE sys_menu
SET parent_id=0, order_num=2, path='users', route_name='UserManagement', update_by='admin', update_time=NOW()
WHERE menu_id=100;

DELETE rm FROM sys_role_menu rm
WHERE rm.role_id <> 1
  AND rm.menu_id IN (100,1001,1002,1003,1004,1005);

COMMIT;
