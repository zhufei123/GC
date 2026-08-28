USE recycle;
SET NAMES utf8mb4;

-- admin / Admin@123
INSERT INTO sys_admin (id, username, password, nickname, phone, status, super_admin) VALUES
(1, 'admin', '$2a$10$49SlWh25Z9Xn7lXESSEL8OBeI8QQdHRNNoIUr9OQfy0Hm1josaCji', '超级管理员', '13800000000', 1, 1);

INSERT INTO sys_role (id, code, name, remark) VALUES
(1, 'super_admin', '超级管理员', '全部权限'),
(2, 'operator', '运营专员', '分类价格内容');

INSERT INTO sys_admin_role (admin_id, role_id) VALUES (1, 1);

-- menus: id, parent, name, title, type, path, component, icon, perms, sort
INSERT INTO sys_menu (id, parent_id, name, title, type, path, component, icon, perms, sort, visible) VALUES
(1, 0, 'Dashboard', '数据看板', 'MENU', '/dashboard', 'dashboard/index', 'Odometer', 'dashboard:view', 0, 1),
(10, 0, 'Goods', '商品价格', 'DIR', '/goods', NULL, 'Goods', NULL, 10, 1),
(11, 10, 'GoodsCategory', '垃圾分类', 'MENU', 'category', 'goods/category/index', 'Menu', 'recycle:category:list', 1, 1),
(111, 11, 'CatAdd', '新增分类', 'BUTTON', NULL, NULL, NULL, 'recycle:category:add', 1, 1),
(112, 11, 'CatEdit', '编辑分类', 'BUTTON', NULL, NULL, NULL, 'recycle:category:update', 2, 1),
(113, 11, 'CatDel', '删除分类', 'BUTTON', NULL, NULL, NULL, 'recycle:category:delete', 3, 1),
(12, 10, 'GoodsSku', 'SKU管理', 'MENU', 'sku', 'goods/sku/index', 'List', 'recycle:sku:list', 2, 1),
(121, 12, 'SkuAdd', '新增SKU', 'BUTTON', NULL, NULL, NULL, 'recycle:sku:add', 1, 1),
(122, 12, 'SkuEdit', '编辑SKU', 'BUTTON', NULL, NULL, NULL, 'recycle:sku:update', 2, 1),
(123, 12, 'SkuDel', '删除SKU', 'BUTTON', NULL, NULL, NULL, 'recycle:sku:delete', 3, 1),
(13, 10, 'GoodsPrice', '价格维护', 'MENU', 'price', 'goods/price/index', 'PriceTag', 'recycle:sku:price', 3, 1),
(20, 0, 'Station', '回收站', 'DIR', '/station', NULL, 'OfficeBuilding', NULL, 20, 1),
(21, 20, 'StationAudit', '入驻审核', 'MENU', 'audit', 'station/audit/index', 'Stamp', 'store:apply:list', 1, 1),
(211, 21, 'ApplyAudit', '审核', 'BUTTON', NULL, NULL, NULL, 'store:apply:audit', 1, 1),
(22, 20, 'StationList', '门店管理', 'MENU', 'list', 'station/list/index', 'Shop', 'store:store:list', 2, 1),
(221, 22, 'StationEdit', '编辑门店', 'BUTTON', NULL, NULL, NULL, 'store:store:update', 1, 1),
(30, 0, 'Member', '用户', 'DIR', '/user', NULL, 'User', NULL, 30, 1),
(31, 30, 'UserList', '用户管理', 'MENU', 'list', 'user/list/index', 'User', 'member:user:list', 1, 1),
(311, 31, 'UserStatus', '启停用户', 'BUTTON', NULL, NULL, NULL, 'member:user:update', 1, 1),
(40, 0, 'Trade', '订单', 'DIR', '/order', NULL, 'Tickets', NULL, 40, 1),
(41, 40, 'OrderList', '订单管理', 'MENU', 'list', 'order/list/index', 'Tickets', 'trade:order:list', 1, 1),
(411, 41, 'OrderCancel', '后台取消', 'BUTTON', NULL, NULL, NULL, 'trade:order:cancel', 1, 1),
(50, 0, 'Content', '内容', 'DIR', '/content', NULL, 'Picture', NULL, 50, 1),
(51, 50, 'Banner', 'Banner', 'MENU', 'banner', 'content/banner/index', 'Picture', 'content:banner:list', 1, 1),
(511, 51, 'BannerAdd', '新增Banner', 'BUTTON', NULL, NULL, NULL, 'content:banner:add', 1, 1),
(512, 51, 'BannerEdit', '编辑Banner', 'BUTTON', NULL, NULL, NULL, 'content:banner:update', 2, 1),
(513, 51, 'BannerDel', '删除Banner', 'BUTTON', NULL, NULL, NULL, 'content:banner:delete', 3, 1),
(52, 50, 'Notice', '公告', 'MENU', 'notice', 'content/notice/index', 'ChatLineSquare', 'content:notice:list', 2, 1),
(521, 52, 'NoticeAdd', '新增公告', 'BUTTON', NULL, NULL, NULL, 'content:notice:add', 1, 1),
(60, 0, 'System', '系统管理', 'DIR', '/system', NULL, 'Setting', NULL, 60, 1),
(61, 60, 'SysAdmin', '管理员', 'MENU', 'admin', 'system/admin/index', 'UserFilled', 'system:admin:list', 1, 1),
(611, 61, 'AdminAdd', '新增管理员', 'BUTTON', NULL, NULL, NULL, 'system:admin:add', 1, 1),
(612, 61, 'AdminEdit', '编辑管理员', 'BUTTON', NULL, NULL, NULL, 'system:admin:update', 2, 1),
(62, 60, 'SysRole', '角色权限', 'MENU', 'role', 'system/role/index', 'Lock', 'system:role:list', 2, 1),
(621, 62, 'RoleAssign', '分配权限', 'BUTTON', NULL, NULL, NULL, 'system:role:assign', 1, 1),
(63, 60, 'SysMenu', '菜单管理', 'MENU', 'menu', 'system/menu/index', 'Menu', 'system:menu:list', 3, 1),
(64, 60, 'SysLog', '操作日志', 'MENU', 'log', 'system/log/index', 'Document', 'system:oplog:list', 4, 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id IN (1,10,11,111,112,12,121,122,13,50,51,511,512,52,521);

INSERT INTO category (id, parent_id, name, icon, sort, status) VALUES
(100, 0, '废纸类', '/static/cat/paper.png', 1, 1),
(101, 100, '黄板纸', NULL, 1, 1),
(102, 100, '书报纸', NULL, 2, 1),
(200, 0, '金属', '/static/cat/metal.png', 2, 1),
(201, 200, '易拉罐', NULL, 1, 1),
(202, 200, '废铁', NULL, 2, 1),
(300, 0, '塑料', '/static/cat/plastic.png', 3, 1),
(301, 300, 'PET瓶', NULL, 1, 1),
(400, 0, '电器', '/static/cat/elec.png', 4, 1),
(401, 400, '小家电', NULL, 1, 1);

INSERT INTO sku (id, category_id, name, image, unit, description, sort, status) VALUES
(1001, 101, '黄板纸（干净无胶带）', '/static/sku/cardboard.png', 'kg', '纸箱拆平，去除胶带泡沫', 1, 1),
(1002, 102, '书报纸', '/static/sku/book.png', 'kg', '干净书籍报纸', 1, 1),
(2001, 201, '易拉罐', '/static/sku/can.png', 'kg', '铝罐压扁', 1, 1),
(2002, 202, '废铁', '/static/sku/iron.png', 'kg', '不含密闭容器', 1, 1),
(3001, 301, 'PET饮料瓶', '/static/sku/pet.png', 'kg', '清空控干', 1, 1),
(4001, 401, '旧电饭煲', '/static/sku/cooker.png', 'piece', '能开机优先', 1, 1);

INSERT INTO sku_price (id, sku_id, city_code, price, effective_at, status) VALUES
(1, 1001, 'ALL', 1.20, '2026-01-01 00:00:00', 1),
(2, 1002, 'ALL', 0.80, '2026-01-01 00:00:00', 1),
(3, 2001, 'ALL', 8.50, '2026-01-01 00:00:00', 1),
(4, 2002, 'ALL', 1.50, '2026-01-01 00:00:00', 1),
(5, 3001, 'ALL', 0.60, '2026-01-01 00:00:00', 1),
(6, 4001, 'ALL', 8.00, '2026-01-01 00:00:00', 1);

INSERT INTO banner (id, title, image, link_type, sort, status, start_time, end_time) VALUES
(1, '绿色回收 随叫随到', '/static/banner/b1.png', 'NONE', 1, 1, '2026-01-01 00:00:00', '2027-12-31 23:59:59');

INSERT INTO notice (id, title, content, pinned, publish_status, publish_time) VALUES
(1, '平台结算说明', '本平台回收款项均为线下当面结算，请当面清点现金。', 1, 'published', NOW());

-- demo customer 13800000001 / sms 123456
INSERT INTO `user` (id, phone, nickname, role, status, recycler_status) VALUES
(50001, '13800000001', '张三', 'customer', 1, 'none'),
(50002, '13800000002', '李老板', 'recycler', 1, 'approved');

INSERT INTO user_address (id, user_id, receiver, phone, province, city, district, street, detail, longitude, latitude, is_default) VALUES
(60001, 50001, '张三', '13800000001', '广东省', '深圳市', '南山区', '幸福路', '幸福小区3栋2单元501', 113.9534110, 22.5370010, 1);

INSERT INTO recycle_station (id, owner_user_id, name, phone, contact_name, province, city, district, address, longitude, latitude, business_status, status, category_ids) VALUES
(3001, 50002, '幸福小区回收站', '13800000002', '李老板', '广东省', '深圳市', '南山区', '幸福路12号', 113.9500000, 22.5300000, 1, 1, '[100,200,300]');
