-- 14: payout_order 查询索引（对账巡检按 status+create_time 扫描；财务/用户维度筛选按 user_id）
USE recycle;

ALTER TABLE payout_order ADD KEY idx_payout_status_time (status, create_time);
ALTER TABLE payout_order ADD KEY idx_payout_user (user_id);
