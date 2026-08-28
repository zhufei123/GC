-- C端订单评价：order_review（订单完成后可评 1-5 星 + 评论，一单一评）
USE recycle;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `order_review` (
  `id` BIGINT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `station_id` BIGINT DEFAULT NULL COMMENT '冗余门店 id，便于统计门店均分',
  `rating` TINYINT NOT NULL COMMENT '1-5 星',
  `comment` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY `uk_review_order` (`order_id`),
  KEY `idx_review_station` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单评价';
