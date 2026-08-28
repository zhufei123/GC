-- 幂等键按用户隔离，允许不同用户使用相同 requestId
ALTER TABLE recycle_order
  DROP INDEX uk_order_req,
  ADD UNIQUE KEY uk_order_req (user_id, request_id);
