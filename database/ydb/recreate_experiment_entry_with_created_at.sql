-- Manual YDB maintenance for experiment_entry.
-- This drops old remote experiment entries. Run only when losing old remote
-- experiment_entry data is acceptable.

DROP TABLE IF EXISTS `experiment_entry`;

CREATE TABLE `experiment_entry` (
    sync_id Utf8,
    experiment_sync_id Utf8,
    entry_date Utf8,
    created_at Utf8,
    content Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (sync_id)
);
