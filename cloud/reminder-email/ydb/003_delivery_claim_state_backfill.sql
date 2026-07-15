-- Backfill retry-safe delivery state after 002_delivery_claim_state.sql.
-- Keep this DML in a separate YDB CLI call from the schema migration.

UPDATE `reminder_email_delivery`
SET
    attempt_count = COALESCE(attempt_count, CAST(1 AS Uint64)),
    status_changed_at = COALESCE(status_changed_at, delivered_at, delivery_date, "1970-01-01T00:00:00"u)
WHERE attempt_count IS NULL OR status_changed_at IS NULL;
