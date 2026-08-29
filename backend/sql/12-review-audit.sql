-- 12: 评价审核
USE recycle;
SET NAMES utf8mb4;

ALTER TABLE `order_review` ADD COLUMN `audit_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED' AFTER `comment`;
ALTER TABLE `order_review` ADD COLUMN `audit_remark` VARCHAR(200) DEFAULT NULL AFTER `audit_status`;
ALTER TABLE `order_review` ADD COLUMN `audited_at` DATETIME DEFAULT NULL AFTER `audit_remark`;

UPDATE `order_review` SET `audit_status` = 'APPROVED' WHERE (`comment` IS NULL OR `comment` = '');

INSERT IGNORE INTO sys_menu (id, parent_id, name, title, type, path, component, icon, perms, sort, visible) VALUES
(53, 50, 'ContentReview', '评价审核', 'MENU', 'review', 'content/review/index', 'ChatDotRound', 'content:review:list', 3, 1),
(531, 53, 'ReviewAudit', '审核评价', 'BUTTON', NULL, NULL, NULL, 'content:review:audit', 1, 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 53), (1, 531);
