-- 逻辑删除后允许重用角色编码
ALTER TABLE sys_role
  DROP INDEX uk_role_code,
  ADD UNIQUE KEY uk_role_code (code, deleted);
