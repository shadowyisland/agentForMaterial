-- 用户自助注册与超级管理员审批（可重复执行）
-- 适用：MySQL 8.0；执行前请备份目标业务库。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_alter_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
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
        SELECT 1 FROM information_schema.statistics
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

CALL add_column_if_missing('sys_user', 'approval_status',
    'ALTER TABLE sys_user ADD COLUMN approval_status char(1) NOT NULL DEFAULT ''1'' COMMENT ''注册审批状态（0待审批 1已通过 2已拒绝）'' AFTER status');
CALL add_column_if_missing('sys_user', 'approval_by',
    'ALTER TABLE sys_user ADD COLUMN approval_by varchar(64) DEFAULT NULL COMMENT ''审批人'' AFTER approval_status');
CALL add_column_if_missing('sys_user', 'approval_time',
    'ALTER TABLE sys_user ADD COLUMN approval_time datetime DEFAULT NULL COMMENT ''审批时间'' AFTER approval_by');
CALL add_column_if_missing('sys_user', 'approval_remark',
    'ALTER TABLE sys_user ADD COLUMN approval_remark varchar(500) DEFAULT NULL COMMENT ''审批意见'' AFTER approval_time');
CALL add_index_if_missing('sys_user', 'idx_user_approval_status',
    'ALTER TABLE sys_user ADD INDEX idx_user_approval_status (approval_status, del_flag, create_time)');

-- 历史用户均视为已审批，避免影响现有账号。
UPDATE sys_user SET approval_status = '1'
WHERE approval_status IS NULL OR approval_status = '';

-- 现有 common 角色作为普通用户角色。
UPDATE sys_role
SET role_name = '普通用户', update_by = 'admin', update_time = NOW()
WHERE role_key = 'common' AND role_name = '普通角色';

-- 补建普通管理员角色；超级管理员仍由框架中的 admin 角色唯一表示。
INSERT INTO sys_role
    (role_name, role_key, role_sort, data_scope, menu_check_strictly,
     dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '普通管理员', 'manager', 2, '1', 1, 1, '0', '0', 'admin', NOW(),
       '协助超级管理员进行系统管理；无注册审批权限'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_key = 'manager' AND del_flag = '0'
);

-- 普通管理员拥有当前启用菜单；注册审批由后端 admin 角色校验，不授权给 manager。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_key = 'manager'
  AND r.del_flag = '0'
  AND m.status = '0';

-- 开启若依已有的注册开关。
UPDATE sys_config
SET config_value = 'true', update_by = 'admin', update_time = NOW(),
    remark = '开启自助注册；注册账号需超级管理员审批后才能登录'
WHERE config_key = 'sys.account.registerUser';

-- 若仍使用若依的 6 位默认密码，则升级为符合新规则的初始密码。
UPDATE sys_config
SET config_value = 'User@2026', update_by = 'admin', update_time = NOW()
WHERE config_key = 'sys.user.initPassword'
  AND CHAR_LENGTH(config_value) < 8;

DROP PROCEDURE add_column_if_missing;
DROP PROCEDURE add_index_if_missing;

SELECT
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'sys_user'
       AND column_name = 'approval_status') AS approval_column_count,
    (SELECT COUNT(*) FROM sys_role
     WHERE role_key = 'manager' AND del_flag = '0') AS manager_role_count,
    (SELECT config_value FROM sys_config
     WHERE config_key = 'sys.account.registerUser' LIMIT 1) AS registration_enabled;
