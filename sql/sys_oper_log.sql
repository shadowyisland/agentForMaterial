/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : aiforscience

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 21/01/2026 21:26:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '模块标题',
  `business_type` int NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '请求方式',
  `operator_type` int NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '返回参数',
  `status` int NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint NULL DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`) USING BTREE,
  INDEX `idx_sys_oper_log_bt`(`business_type` ASC) USING BTREE,
  INDEX `idx_sys_oper_log_s`(`status` ASC) USING BTREE,
  INDEX `idx_sys_oper_log_ot`(`oper_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 113 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------
INSERT INTO `sys_oper_log` VALUES (100, '文档管理', 1, 'com.ruoyi.web.controller.system.SysDocumentController.add()', 'POST', 1, 'admin', '研发部门', '/system/document', '127.0.0.1', '内网IP', '{\"createBy\":\"admin\",\"createTime\":\"2025-12-24 19:44:42\",\"documentId\":1,\"documentName\":\"基金申报书\",\"fileOriginName\":\"基金申报书.pdf\",\"filePath\":\"/profile/upload/2025/12/24/基金申报书_20251224194439A001.pdf\",\"fileSuffix\":\"pdf\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2025-12-24 19:44:42', 12);
INSERT INTO `sys_oper_log` VALUES (101, '文档管理', 3, 'com.ruoyi.web.controller.system.SysDocumentController.remove()', 'DELETE', 1, 'admin', '研发部门', '/system/document/1', '127.0.0.1', '内网IP', '[1] ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2025-12-24 21:19:25', 10);
INSERT INTO `sys_oper_log` VALUES (102, '文档管理', 1, 'com.ruoyi.web.controller.system.SysDocumentController.add()', 'POST', 1, 'admin', NULL, '/system/document', '127.0.0.1', '内网IP', '{\"createBy\":\"admin\",\"createTime\":\"2025-12-25 16:05:37\",\"documentId\":2,\"documentName\":\"病案[2025129268][2025-07-14](2)\",\"fileOriginName\":\"病案[2025129268][2025-07-14](2).pdf\",\"filePath\":\"/profile/upload/2025/12/25/病案[2025129268][2025-07-14](2)_20251225160445A001.pdf\",\"fileSuffix\":\"pdf\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2025-12-25 16:05:37', 45);
INSERT INTO `sys_oper_log` VALUES (103, '用户管理', 1, 'com.ruoyi.web.controller.system.SysUserController.add()', 'POST', 1, 'admin', '研发部门', '/system/user', '127.0.0.1', '内网IP', '{\"admin\":false,\"createBy\":\"admin\",\"deptId\":103,\"nickName\":\"123\",\"params\":{},\"postIds\":[],\"roleIds\":[],\"status\":\"0\",\"userId\":100,\"userName\":\"123\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-07 16:28:26', 162);
INSERT INTO `sys_oper_log` VALUES (104, '文档管理', 1, 'com.ruoyi.web.controller.system.SysDocumentController.add()', 'POST', 1, 'admin', '研发部门', '/system/document', '127.0.0.1', '内网IP', '{\"createBy\":\"admin\",\"createTime\":\"2026-01-07 16:40:48\",\"documentId\":3,\"documentName\":\"Huang_et_al-2025-Scientific_Reports - \",\"fileOriginName\":\"Huang_et_al-2025-Scientific_Reports.pdf\",\"filePath\":\"/profile/upload/2026/01/07/Huang_et_al-2025-Scientific_Reports_20260107163043A001.pdf\",\"fileSuffix\":\"pdf\",\"params\":{},\"remark\":\"paper\"} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-07 16:40:48', 35);
INSERT INTO `sys_oper_log` VALUES (105, 'OCR识别', 2, 'com.ruoyi.web.controller.system.SysDocumentController.ocr()', 'POST', 1, 'admin', '研发部门', '/system/document/ocr/3', '127.0.0.1', '内网IP', '3 ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-21 17:02:08', 513);
INSERT INTO `sys_oper_log` VALUES (106, 'OCR识别', 2, 'com.ruoyi.web.controller.system.SysDocumentController.ocr()', 'POST', 1, 'admin', '研发部门', '/system/document/ocr/2', '127.0.0.1', '内网IP', '2 ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-21 20:20:02', 522);
INSERT INTO `sys_oper_log` VALUES (107, '文档管理', 1, 'com.ruoyi.web.controller.system.SysDocumentController.add()', 'POST', 1, 'admin', '研发部门', '/system/document', '127.0.0.1', '内网IP', '{\"createBy\":\"admin\",\"createTime\":\"2026-01-21 20:20:41\",\"documentId\":4,\"documentName\":\"病案[2025129268][2025-07-14]\",\"fileOriginName\":\"病案[2025129268][2025-07-14].pdf\",\"filePath\":\"/profile/upload/2026/01/21/病案[2025129268][2025-07-14]_20260121202039A001.pdf\",\"fileSuffix\":\"pdf\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-21 20:20:41', 10);
INSERT INTO `sys_oper_log` VALUES (108, 'OCR识别', 2, 'com.ruoyi.web.controller.system.SysDocumentController.ocr()', 'POST', 1, 'admin', '研发部门', '/system/document/ocr/4', '127.0.0.1', '内网IP', '4 ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-21 20:20:45', 508);
INSERT INTO `sys_oper_log` VALUES (109, '文档管理', 1, 'com.ruoyi.web.controller.system.SysDocumentController.add()', 'POST', 1, 'admin', '研发部门', '/system/document', '127.0.0.1', '内网IP', '{\"createBy\":\"admin\",\"createTime\":\"2026-01-21 20:22:08\",\"documentId\":5,\"documentName\":\"dzfp_25342000000033118687_安徽磐众信息科技有限公司_20250228085536\",\"fileOriginName\":\"dzfp_25342000000033118687_安徽磐众信息科技有限公司_20250228085536.pdf\",\"filePath\":\"/profile/upload/2026/01/21/dzfp_25342000000033118687_安徽磐众信息科技有限公司_20250228085536_20260121202207A003.pdf\",\"fileSuffix\":\"pdf\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-21 20:22:08', 5);
INSERT INTO `sys_oper_log` VALUES (110, 'OCR识别', 2, 'com.ruoyi.web.controller.system.SysDocumentController.ocr()', 'POST', 1, 'admin', '研发部门', '/system/document/ocr/5', '127.0.0.1', '内网IP', '5 ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-21 20:22:11', 510);
INSERT INTO `sys_oper_log` VALUES (111, '文档管理', 1, 'com.ruoyi.web.controller.system.SysDocumentController.add()', 'POST', 1, 'admin', '研发部门', '/system/document', '127.0.0.1', '内网IP', '{\"createBy\":\"admin\",\"createTime\":\"2026-01-21 20:28:35\",\"documentId\":6,\"documentName\":\"病案[2025129268][2025-07-14]\",\"fileOriginName\":\"病案[2025129268][2025-07-14].pdf\",\"filePath\":\"/profile/upload/2026/01/21/病案[2025129268][2025-07-14]_20260121202834A001.pdf\",\"fileSuffix\":\"pdf\",\"params\":{}} ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-21 20:28:35', 83);
INSERT INTO `sys_oper_log` VALUES (112, 'OCR识别', 2, 'com.ruoyi.web.controller.system.SysDocumentController.ocr()', 'POST', 1, 'admin', '研发部门', '/system/document/ocr/6', '127.0.0.1', '内网IP', '6 ', '{\"msg\":\"操作成功\",\"code\":200}', 0, NULL, '2026-01-21 20:28:38', 511);

SET FOREIGN_KEY_CHECKS = 1;
