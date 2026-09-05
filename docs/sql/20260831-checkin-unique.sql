USE wenji;

-- 仅用于已经初始化过旧版 schema.sql 的本地数据库；全新数据库无需执行。
-- 执行前应先清理同一 user_id + resource_id 的重复记录。
ALTER TABLE checkins
    ADD CONSTRAINT uk_checkin_user_resource UNIQUE (user_id, resource_id);
