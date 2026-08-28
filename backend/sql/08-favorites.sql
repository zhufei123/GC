-- C端收藏回收站：user_favorite_station（物理删除，取消收藏后可重新收藏）
USE recycle;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_favorite_station` (
  `id` BIGINT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `station_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_fav_user_station` (`user_id`,`station_id`),
  KEY `idx_fav_station` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏回收站';
