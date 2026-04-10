-- Reminder email pipeline schema for YDB (single daily sender flow).

CREATE TABLE IF NOT EXISTS `reminder_email_queue` (
    reminder_sync_id Utf8,
    transaction_sync_id Utf8,
    reminder_text Utf8,
    reminder_at Utf8,
    updated_at Utf8,
    deleted_at Utf8,
    PRIMARY KEY (reminder_sync_id)
);

CREATE TABLE IF NOT EXISTS `reminder_recipient` (
    reminder_sync_id Utf8,
    email Utf8,
    is_active Bool,
    updated_at Utf8,
    PRIMARY KEY (reminder_sync_id, email)
);

CREATE TABLE IF NOT EXISTS `reminder_email_delivery` (
    delivery_id Utf8,
    reminder_sync_id Utf8,
    delivery_date Utf8,
    delivered_at Utf8,
    target_email Utf8,
    status Utf8,
    error_text Utf8,
    PRIMARY KEY (delivery_id)
);
