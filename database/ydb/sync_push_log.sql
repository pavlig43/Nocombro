-- Минимальная remote-таблица для serverless YDB.
-- Хранит последнее известное состояние каждой sync-сущности.
CREATE TABLE IF NOT EXISTS `sync_push_log` (
    device_id Utf8,
    entity_table Utf8,
    entity_sync_id Utf8,
    change_type Utf8,
    payload_json Utf8,
    source_queue_ids Utf8,
    last_queued_at Utf8,
    reserved_at Utf8,
    pushed_at Utf8,
    remote_cursor Utf8,
    change_cursor Utf8,
    PRIMARY KEY (entity_table, entity_sync_id)
);
