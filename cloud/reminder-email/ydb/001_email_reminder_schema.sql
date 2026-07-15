-- Reminder email delivery state.
-- Reminder sources are the typed mirror tables created by mirror_sync_v1.sql.

CREATE TABLE IF NOT EXISTS `reminder_recipient` (
    email Utf8,
    is_active Bool,
    updated_at Utf8,
    PRIMARY KEY (email)
);

CREATE TABLE IF NOT EXISTS `reminder_email_delivery` (
    delivery_id Utf8,
    reminder_sync_id Utf8,
    delivery_date Utf8,
    delivered_at Utf8,
    target_email Utf8,
    status Utf8,
    error_text Utf8,
    attempt_count Uint64,
    status_changed_at Utf8,
    PRIMARY KEY (delivery_id)
);
