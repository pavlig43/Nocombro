-- Add retry-safe delivery claim columns to an existing reminder_email_delivery table.
-- Apply this schema migration once, after 001_email_reminder_schema.sql.
-- Then apply 003_delivery_claim_state_backfill.sql in a separate YDB CLI call:
-- YDB does not allow DDL and DML in the same query.

ALTER TABLE `reminder_email_delivery`
    ADD COLUMN attempt_count Uint64,
    ADD COLUMN status_changed_at Utf8;
