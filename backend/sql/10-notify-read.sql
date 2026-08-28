-- 10: 站内通知已读标记
USE recycle;

ALTER TABLE notify_log ADD COLUMN read_at DATETIME NULL AFTER error;
