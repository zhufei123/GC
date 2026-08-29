-- 15：按微信/支付宝官方接口补齐小程序身份与配置快照
-- 微信：openid 绑定具体 appid（code2session / 商家转账 / 订阅消息）
--   https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
--   https://pay.weixin.qq.com/doc/v3/merchant/4012716434
--   https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/mp-message-management/subscribe-message/sendMessage.html
-- 支付宝：同时保留 user_id(2088) 与 open_id；新商户模板消息/转账推荐 open_id
--   https://opendocs.alipay.com/mini/84bc7352_alipay.system.oauth.token
--   https://opendocs.alipay.com/mini/6430ce5a_alipay.open.app.mini.templatemessage.send
USE recycle;
SET NAMES utf8mb4;

ALTER TABLE `user` ADD COLUMN `wx_appid` VARCHAR(32) DEFAULT NULL COMMENT '微信小程序appid，openid所属应用' AFTER `openid_wx`;
ALTER TABLE `user` ADD COLUMN `wx_session_key` VARCHAR(128) DEFAULT NULL COMMENT 'code2session会话密钥，仅服务端，勿下发前端' AFTER `unionid_wx`;
ALTER TABLE `user` ADD COLUMN `wx_session_at` DATETIME DEFAULT NULL COMMENT 'session_key更新时间' AFTER `wx_session_key`;
ALTER TABLE `user` ADD COLUMN `province` VARCHAR(64) DEFAULT NULL COMMENT '资料省份（getUserProfile）' AFTER `city`;
ALTER TABLE `user` ADD COLUMN `country` VARCHAR(64) DEFAULT NULL COMMENT '资料国家' AFTER `province`;
ALTER TABLE `user` ADD COLUMN `language` VARCHAR(16) DEFAULT NULL COMMENT '资料语言 zh_CN 等' AFTER `country`;
ALTER TABLE `user` ADD COLUMN `alipay_app_id` VARCHAR(32) DEFAULT NULL COMMENT '支付宝小程序app_id' AFTER `openid_alipay`;
ALTER TABLE `user` ADD COLUMN `alipay_user_id` VARCHAR(32) DEFAULT NULL COMMENT '支付宝user_id，2088开头16位' AFTER `alipay_app_id`;
ALTER TABLE `user` ADD COLUMN `alipay_access_token` VARCHAR(128) DEFAULT NULL COMMENT 'oauth access_token，仅服务端' AFTER `alipay_user_id`;
ALTER TABLE `user` ADD COLUMN `alipay_refresh_token` VARCHAR(128) DEFAULT NULL COMMENT 'oauth refresh_token，仅服务端' AFTER `alipay_access_token`;
ALTER TABLE `user` ADD COLUMN `alipay_token_expire_at` DATETIME DEFAULT NULL COMMENT 'access_token过期时间' AFTER `alipay_refresh_token`;
ALTER TABLE `user` ADD KEY `idx_user_alipay_uid` (`alipay_user_id`);

ALTER TABLE `payout_order` ADD COLUMN `appid` VARCHAR(32) DEFAULT NULL COMMENT '打款使用的小程序appid，须与openid同应用' AFTER `openid`;

-- 存量：把历史 2088 user_id 从 openid_alipay 回填到 alipay_user_id；未配置真实 appid 时记 mock
UPDATE `user`
SET alipay_user_id = openid_alipay
WHERE (alipay_user_id IS NULL OR alipay_user_id = '')
  AND openid_alipay IS NOT NULL
  AND openid_alipay LIKE '2088%'
  AND CHAR_LENGTH(openid_alipay) = 16;

UPDATE `user`
SET wx_appid = 'mock'
WHERE openid_wx IS NOT NULL AND openid_wx <> ''
  AND (wx_appid IS NULL OR wx_appid = '');

UPDATE `user`
SET alipay_app_id = 'mock'
WHERE (openid_alipay IS NOT NULL AND openid_alipay <> '' OR alipay_user_id IS NOT NULL AND alipay_user_id <> '')
  AND (alipay_app_id IS NULL OR alipay_app_id = '');
