-- recycle platform schema (MySQL 8.x, utf8mb4)
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS recycle DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE recycle;

DROP TABLE IF EXISTS sys_log;
DROP TABLE IF EXISTS notify_log;
DROP TABLE IF EXISTS payout_order;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_admin_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_admin;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS recycle_order;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS sku_price_log;
DROP TABLE IF EXISTS station_sku_price;
DROP TABLE IF EXISTS sku_price;
DROP TABLE IF EXISTS sku;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS station_apply;
DROP TABLE IF EXISTS recycle_station;
DROP TABLE IF EXISTS banner;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS wallet_ledger;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id            BIGINT PRIMARY KEY,
  openid_wx     VARCHAR(64)  DEFAULT NULL,
  unionid_wx    VARCHAR(64)  DEFAULT NULL,
  openid_alipay VARCHAR(64)  DEFAULT NULL,
  phone         VARCHAR(20)  DEFAULT NULL,
  nickname      VARCHAR(64)  DEFAULT NULL,
  avatar        VARCHAR(512) DEFAULT NULL,
  role          VARCHAR(20)  NOT NULL DEFAULT 'customer' COMMENT 'customer/recycler',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  balance       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '钱包预留',
  recycler_status VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT 'none/pending/approved/rejected',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_phone (phone),
  UNIQUE KEY uk_user_wx (openid_wx),
  UNIQUE KEY uk_user_alipay (openid_alipay)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C/B端用户';

CREATE TABLE user_address (
  id          BIGINT PRIMARY KEY,
  user_id     BIGINT       NOT NULL,
  receiver    VARCHAR(32)  NOT NULL,
  phone       VARCHAR(20)  NOT NULL,
  province    VARCHAR(32)  NOT NULL,
  city        VARCHAR(32)  NOT NULL,
  district    VARCHAR(32)  NOT NULL,
  street      VARCHAR(64)  DEFAULT NULL,
  detail      VARCHAR(128) NOT NULL,
  longitude   DECIMAL(10,7) DEFAULT NULL,
  latitude    DECIMAL(10,7) DEFAULT NULL,
  is_default  TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  KEY idx_addr_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址';

CREATE TABLE recycle_station (
  id               BIGINT PRIMARY KEY,
  owner_user_id    BIGINT       NOT NULL,
  name             VARCHAR(64)  NOT NULL,
  photos           JSON         DEFAULT NULL,
  phone            VARCHAR(20)  NOT NULL,
  contact_name     VARCHAR(32)  DEFAULT NULL,
  province         VARCHAR(32)  DEFAULT NULL,
  city             VARCHAR(32)  DEFAULT NULL,
  district         VARCHAR(32)  DEFAULT NULL,
  address          VARCHAR(255) NOT NULL,
  longitude        DECIMAL(10,7) DEFAULT NULL,
  latitude         DECIMAL(10,7) DEFAULT NULL,
  business_hours   VARCHAR(64)  DEFAULT '09:00-18:00',
  business_status  TINYINT      NOT NULL DEFAULT 1 COMMENT '1营业 0休息',
  audit_status     VARCHAR(20)  NOT NULL DEFAULT 'approved',
  status           TINYINT      NOT NULL DEFAULT 1 COMMENT '账号启停',
  category_ids     JSON         DEFAULT NULL,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_station_owner (owner_user_id),
  KEY idx_station_geo (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收站';

CREATE TABLE station_apply (
  id             BIGINT PRIMARY KEY,
  user_id        BIGINT       NOT NULL,
  store_name     VARCHAR(64)  NOT NULL,
  contact_name   VARCHAR(32)  NOT NULL,
  contact_phone  VARCHAR(20)  NOT NULL,
  province       VARCHAR(32)  DEFAULT NULL,
  city           VARCHAR(32)  DEFAULT NULL,
  district       VARCHAR(32)  DEFAULT NULL,
  detail         VARCHAR(255) NOT NULL,
  longitude      DECIMAL(10,7) DEFAULT NULL,
  latitude       DECIMAL(10,7) DEFAULT NULL,
  license_image  VARCHAR(512) DEFAULT NULL,
  store_images   JSON         DEFAULT NULL,
  category_ids   JSON         DEFAULT NULL,
  audit_status   VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  audit_remark   VARCHAR(255) DEFAULT NULL,
  auditor_id     BIGINT       DEFAULT NULL,
  audit_time     DATETIME     DEFAULT NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT      NOT NULL DEFAULT 0,
  KEY idx_apply_user (user_id),
  KEY idx_apply_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入驻申请';

CREATE TABLE category (
  id          BIGINT PRIMARY KEY,
  parent_id   BIGINT       NOT NULL DEFAULT 0,
  name        VARCHAR(32)  NOT NULL,
  icon        VARCHAR(512) DEFAULT NULL,
  sort        INT          NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_cat_parent_name (parent_id, name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='两级分类';

CREATE TABLE sku (
  id          BIGINT PRIMARY KEY,
  category_id BIGINT       NOT NULL,
  name        VARCHAR(64)  NOT NULL,
  image       VARCHAR(512) DEFAULT NULL,
  unit        VARCHAR(8)   NOT NULL DEFAULT 'kg' COMMENT 'kg/piece',
  description VARCHAR(255) DEFAULT NULL,
  sort        INT          NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  KEY idx_sku_cat (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收SKU';

CREATE TABLE sku_price (
  id            BIGINT PRIMARY KEY,
  sku_id        BIGINT        NOT NULL,
  city_code     VARCHAR(16)   NOT NULL DEFAULT 'ALL',
  price         DECIMAL(10,2) NOT NULL,
  effective_at  DATETIME      NOT NULL,
  status        TINYINT       NOT NULL DEFAULT 1,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT       NOT NULL DEFAULT 0,
  KEY idx_price_sku_time (sku_id, city_code, effective_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU指导价';

CREATE TABLE station_sku_price (
  id          BIGINT PRIMARY KEY,
  station_id  BIGINT        NOT NULL,
  sku_id      BIGINT        NOT NULL,
  price       DECIMAL(10,2) NOT NULL,
  status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1报价中 0停报',
  remark      VARCHAR(200)  DEFAULT NULL,
  created_at  DATETIME      NOT NULL,
  updated_at  DATETIME      NOT NULL,
  deleted     TINYINT       NOT NULL DEFAULT 0,
  UNIQUE KEY uk_station_sku (station_id, sku_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店SKU报价';

CREATE TABLE sku_price_log (
  id            BIGINT PRIMARY KEY,
  sku_id        BIGINT        NOT NULL,
  old_price     DECIMAL(10,2) DEFAULT NULL,
  new_price     DECIMAL(10,2) NOT NULL,
  effective_at  DATETIME      NOT NULL,
  reason        VARCHAR(128)  DEFAULT NULL,
  operator_id   BIGINT        DEFAULT NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_plog_sku (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调价记录';

CREATE TABLE recycle_order (
  id               BIGINT PRIMARY KEY,
  order_no         VARCHAR(32)   NOT NULL,
  user_id          BIGINT        NOT NULL,
  station_id       BIGINT        DEFAULT NULL,
  type             VARCHAR(16)   NOT NULL DEFAULT 'PICKUP' COMMENT 'PICKUP/DROPOFF',
  status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  receiver         VARCHAR(32)   DEFAULT NULL,
  phone            VARCHAR(20)   DEFAULT NULL,
  address          VARCHAR(255)  DEFAULT NULL,
  longitude        DECIMAL(10,7) DEFAULT NULL,
  latitude         DECIMAL(10,7) DEFAULT NULL,
  appoint_date     DATE          DEFAULT NULL,
  appoint_period   VARCHAR(32)   DEFAULT NULL,
  estimate_amount  DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  actual_amount    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  photos_customer  JSON          DEFAULT NULL,
  photos_weigh     JSON          DEFAULT NULL,
  remark           VARCHAR(255)  DEFAULT NULL,
  cancel_by        VARCHAR(16)   DEFAULT NULL,
  cancel_reason    VARCHAR(255)  DEFAULT NULL,
  request_id       VARCHAR(64)   DEFAULT NULL,
  pay_method       VARCHAR(16)   NOT NULL DEFAULT 'OFFLINE' COMMENT 'OFFLINE/WX_TRANSFER/ALIPAY_TRANSFER/WALLET',
  paid_at          DATETIME      DEFAULT NULL,
  payout_status    VARCHAR(24)   DEFAULT NULL COMMENT 'SUCCESS/PROCESSING/WAIT_USER_CONFIRM/FAILED',
  accepted_at      DATETIME      DEFAULT NULL,
  served_at        DATETIME      DEFAULT NULL,
  weighed_at       DATETIME      DEFAULT NULL,
  completed_at     DATETIME      DEFAULT NULL,
  cancelled_at     DATETIME      DEFAULT NULL,
  create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          TINYINT       NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_order_req (user_id, request_id),
  KEY idx_order_user (user_id, status),
  KEY idx_order_station (station_id, status),
  KEY idx_order_status (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收订单';

CREATE TABLE order_item (
  id          BIGINT PRIMARY KEY,
  order_id    BIGINT        NOT NULL,
  item_type   VARCHAR(16)   NOT NULL COMMENT 'ESTIMATE/ACTUAL',
  sku_id      BIGINT        NOT NULL,
  sku_name    VARCHAR(64)   NOT NULL,
  unit        VARCHAR(8)    NOT NULL DEFAULT 'kg',
  weight      DECIMAL(10,3) NOT NULL DEFAULT 0.000,
  price       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_oi_order (order_id, item_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细(预估/实收)';

CREATE TABLE banner (
  id          BIGINT PRIMARY KEY,
  title       VARCHAR(64)  DEFAULT NULL,
  image       VARCHAR(512) NOT NULL,
  link_type   VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PAGE/RICH',
  link_url    VARCHAR(255) DEFAULT NULL,
  sort        INT          NOT NULL DEFAULT 0,
  start_time  DATETIME     DEFAULT NULL,
  end_time    DATETIME     DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Banner';

CREATE TABLE notice (
  id            BIGINT PRIMARY KEY,
  title         VARCHAR(80)  NOT NULL,
  content       MEDIUMTEXT,
  pinned        TINYINT      NOT NULL DEFAULT 0,
  publish_status VARCHAR(16) NOT NULL DEFAULT 'published' COMMENT 'draft/published/offline',
  publish_time  DATETIME     DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告';

CREATE TABLE wallet_ledger (
  id          BIGINT PRIMARY KEY,
  user_id     BIGINT        NOT NULL,
  amount      DECIMAL(12,2) NOT NULL,
  biz_type    VARCHAR(32)   NOT NULL,
  biz_id      BIGINT        DEFAULT NULL,
  remark      VARCHAR(128)  DEFAULT NULL,
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wallet_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水';

CREATE TABLE payout_order (
  id              BIGINT PRIMARY KEY,
  payout_no       VARCHAR(32)   NOT NULL,
  order_id        BIGINT        NOT NULL,
  user_id         BIGINT        NOT NULL,
  station_id      BIGINT        NOT NULL,
  channel         VARCHAR(24)   NOT NULL COMMENT 'OFFLINE/WX_TRANSFER/ALIPAY_TRANSFER/WALLET',
  amount          DECIMAL(12,2) NOT NULL,
  openid          VARCHAR(64)   DEFAULT NULL,
  status          VARCHAR(24)   NOT NULL COMMENT 'SUCCESS/PROCESSING/WAIT_USER_CONFIRM/FAILED',
  channel_bill_no VARCHAR(64)   DEFAULT NULL,
  package_info    VARCHAR(512)  DEFAULT NULL,
  fail_reason     VARCHAR(200)  DEFAULT NULL,
  create_time     DATETIME      NOT NULL,
  update_time     DATETIME      NOT NULL,
  deleted         TINYINT       NOT NULL DEFAULT 0,
  UNIQUE KEY uk_payout_no (payout_no),
  UNIQUE KEY uk_payout_order (order_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打款单(站点付客户)';

CREATE TABLE notify_log (
  id           BIGINT PRIMARY KEY,
  user_id      BIGINT       NOT NULL,
  channel      VARCHAR(16)  NOT NULL COMMENT 'WX/ALIPAY/INAPP',
  template_key VARCHAR(64)  NOT NULL,
  biz_type     VARCHAR(32)  NOT NULL,
  biz_id       BIGINT       NOT NULL,
  title        VARCHAR(80)  DEFAULT NULL,
  content      VARCHAR(500) DEFAULT NULL,
  status       VARCHAR(16)  NOT NULL DEFAULT 'SENT',
  error        VARCHAR(200) DEFAULT NULL,
  read_at      DATETIME     DEFAULT NULL,
  create_time  DATETIME     NOT NULL,
  deleted      TINYINT      DEFAULT 0,
  KEY idx_notify_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录';

CREATE TABLE sys_admin (
  id          BIGINT PRIMARY KEY,
  username    VARCHAR(32)  NOT NULL,
  password    VARCHAR(100) NOT NULL,
  nickname    VARCHAR(32)  DEFAULT NULL,
  phone       VARCHAR(20)  DEFAULT NULL,
  avatar      VARCHAR(512) DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  super_admin TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员';

CREATE TABLE sys_role (
  id          BIGINT PRIMARY KEY,
  code        VARCHAR(32)  NOT NULL,
  name        VARCHAR(32)  NOT NULL,
  remark      VARCHAR(128) DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_role_code (code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE sys_menu (
  id          BIGINT PRIMARY KEY,
  parent_id   BIGINT       NOT NULL DEFAULT 0,
  name        VARCHAR(64)  NOT NULL COMMENT '路由name',
  title       VARCHAR(64)  NOT NULL COMMENT '显示名',
  type        VARCHAR(16)  NOT NULL COMMENT 'DIR/MENU/BUTTON',
  path        VARCHAR(128) DEFAULT NULL,
  component   VARCHAR(128) DEFAULT NULL,
  icon        VARCHAR(64)  DEFAULT NULL,
  perms       VARCHAR(64)  DEFAULT NULL,
  sort        INT          NOT NULL DEFAULT 0,
  visible     TINYINT      NOT NULL DEFAULT 1,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限';

CREATE TABLE sys_admin_role (
  admin_id BIGINT NOT NULL,
  role_id  BIGINT NOT NULL,
  PRIMARY KEY (admin_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_log (
  id          BIGINT PRIMARY KEY,
  module      VARCHAR(32)  DEFAULT NULL,
  type        VARCHAR(16)  DEFAULT NULL,
  description VARCHAR(128) DEFAULT NULL,
  operator    VARCHAR(32)  DEFAULT NULL,
  operator_id BIGINT       DEFAULT NULL,
  method      VARCHAR(16)  DEFAULT NULL,
  path        VARCHAR(255) DEFAULT NULL,
  params      TEXT,
  result_code INT          DEFAULT NULL,
  cost_ms     INT          DEFAULT NULL,
  ip          VARCHAR(64)  DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

SET FOREIGN_KEY_CHECKS = 1;
