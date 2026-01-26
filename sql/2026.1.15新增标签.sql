#用户信息表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_date` datetime NULL DEFAULT NULL COMMENT '密码最后更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE KEY `uk_user_name` (`user_name`) USING BTREE,
  KEY `idx_phone` (`phonenumber`) USING BTREE,
  KEY `idx_email` (`email`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表' ROW_FORMAT=DYNAMIC;

#文档管理表
DROP TABLE IF EXISTS `sys_document`;
CREATE TABLE `sys_document` (
  `document_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `document_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '文档名称',
  `file_origin_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '原始文件名',
  `file_suffix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '文件后缀',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `mime_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT 'MIME类型',
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '本地存储路径(如有)',
  `url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '远端文件URL(下载用)',
  `is_recognized` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已OCR识别(0否1是)',
  `ocr_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'OCR识别文本内容',
  `ocr_time` datetime NULL DEFAULT NULL COMMENT 'OCR完成时间',
  `ocr_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'OCR失败原因',

  `create_user_id` bigint NULL DEFAULT NULL COMMENT '上传者用户ID(sys_user.user_id)',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '上传者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标志(0存在2删除)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`document_id`) USING BTREE,
  KEY `idx_doc_create_user_time` (`create_user_id`,`create_time`) USING BTREE,
  KEY `idx_doc_create_time` (`create_time`) USING BTREE,
  KEY `idx_doc_suffix` (`file_suffix`) USING BTREE,
  FULLTEXT KEY `ft_doc_ocr` (`ocr_content`),

  CONSTRAINT `fk_doc_user` FOREIGN KEY (`create_user_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档管理表' ROW_FORMAT=DYNAMIC;


#标签表
DROP TABLE IF EXISTS `sys_tag`;
CREATE TABLE `sys_tag` (
  `tag_id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `owner_user_id` bigint NOT NULL COMMENT '标签所属用户ID',
  `tag_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名',
  `tag_key` varchar(64)
    GENERATED ALWAYS AS (LOWER(TRIM(`tag_name`))) STORED
    COMMENT '规范化key(用于用户内去重/检索)',

  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '0' COMMENT '删除标志(0存在2删除)',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`tag_id`) USING BTREE,
  UNIQUE KEY `uk_owner_tagkey` (`owner_user_id`,`tag_key`) USING BTREE,
  KEY `idx_tag_owner` (`owner_user_id`) USING BTREE,
  KEY `idx_tag_key` (`tag_key`) USING BTREE,
  CONSTRAINT `fk_tag_user` FOREIGN KEY (`owner_user_id`) REFERENCES `sys_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私有标签表' ROW_FORMAT=DYNAMIC;

#文档-标签关联表
DROP TABLE IF EXISTS `sys_document_tag`;
CREATE TABLE `sys_document_tag` (
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建标签时间',
  PRIMARY KEY (`document_id`,`tag_id`) USING BTREE,
  KEY `idx_dt_tag_doc` (`tag_id`,`document_id`) USING BTREE,
  KEY `idx_dt_doc` (`document_id`) USING BTREE,
  CONSTRAINT `fk_dt_document` FOREIGN KEY (`document_id`) REFERENCES `sys_document` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dt_tag` FOREIGN KEY (`tag_id`) REFERENCES `sys_tag` (`tag_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档-标签关联表' ROW_FORMAT=DYNAMIC;

