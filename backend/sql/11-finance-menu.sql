-- 11: 管理端财务菜单（打款单）
USE recycle;
SET NAMES utf8mb4;

INSERT IGNORE INTO sys_menu (id, parent_id, name, title, type, path, component, icon, perms, sort, visible) VALUES
(70, 0, 'Finance', '财务', 'DIR', '/finance', NULL, 'Wallet', NULL, 45, 1),
(71, 70, 'FinancePayout', '打款单', 'MENU', 'payout', 'finance/payout/index', 'Wallet', 'finance:payout:list', 1, 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 70), (1, 71);
