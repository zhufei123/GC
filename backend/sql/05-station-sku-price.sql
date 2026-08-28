-- 门店自主报价：station_sku_price（平台 sku_price 退为指导价）
USE recycle;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `station_sku_price` (
  `id` BIGINT PRIMARY KEY,
  `station_id` BIGINT NOT NULL,
  `sku_id` BIGINT NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1报价中 0停报',
  `remark` VARCHAR(200) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY `uk_station_sku` (`station_id`,`sku_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店SKU报价';

-- 为所有已通过审核的门店按当前指导价初始化报价（已有报价的跳过）
INSERT IGNORE INTO station_sku_price (id, station_id, sku_id, price, status, remark, created_at, updated_at, deleted)
SELECT s.id * 100000 + cp.sku_id, s.id, cp.sku_id, cp.price, 1, '初始报价（同步指导价）', NOW(), NOW(), 0
FROM recycle_station s
JOIN (
  SELECT sp.sku_id, sp.price
  FROM sku_price sp
  JOIN (
    SELECT sku_id, MAX(effective_at) AS max_eff
    FROM sku_price
    WHERE status = 1 AND deleted = 0 AND effective_at <= NOW()
    GROUP BY sku_id
  ) latest ON latest.sku_id = sp.sku_id AND latest.max_eff = sp.effective_at
  WHERE sp.status = 1 AND sp.deleted = 0
) cp
WHERE s.audit_status = 'approved' AND s.deleted = 0;
