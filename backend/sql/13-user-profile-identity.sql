-- 13：三方登录身份资料补全（模板/订阅消息依赖 openid，画像依赖头像/性别/城市）
USE recycle;
SET NAMES utf8mb4;

-- gender/city：uni.getUserProfile / my.getOpenUserInfo 可选授权字段
ALTER TABLE `user` ADD COLUMN `gender` TINYINT DEFAULT NULL COMMENT '0未知 1男 2女' AFTER `avatar`;
ALTER TABLE `user` ADD COLUMN `city` VARCHAR(64) DEFAULT NULL COMMENT '资料城市（授权时）' AFTER `gender`;

-- 订阅消息授权标记：用户在小程序 requestSubscribeMessage 至少接受过一次则置 1
ALTER TABLE `user` ADD COLUMN `subscribe_wx` TINYINT NOT NULL DEFAULT 0 COMMENT '1=曾授权微信订阅消息' AFTER `unionid_wx`;
ALTER TABLE `user` ADD COLUMN `subscribe_alipay` TINYINT NOT NULL DEFAULT 0 COMMENT '1=曾授权支付宝订阅消息' AFTER `subscribe_wx`;
