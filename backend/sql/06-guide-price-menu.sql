-- 管理端文案：平台 sku_price 改为「指导价」
USE recycle;
SET NAMES utf8mb4;

UPDATE sys_menu SET title = '分类与指导价' WHERE id = 10 AND title = '商品价格';
UPDATE sys_menu SET title = '指导价' WHERE id = 13 AND title IN ('价格维护', '价格管理');
