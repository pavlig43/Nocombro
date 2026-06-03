# Reminder Email Pipeline (YDB + Postbox)

## Scope

This package prepares cloud-side daily email delivery for reminders.
The desktop app keeps `Room` as the source of truth, and reminder sync mirrors the current reminder state into dedicated YDB sources for transaction and experiment reminders.

## What is included

- `ydb/001_email_reminder_schema.sql` - YDB schema:
  - `reminder_email_source`
  - `experiment_reminder_email_source`
  - `reminder_recipient`
  - `reminder_email_delivery`
- `functions/send_daily_emails.py` - daily sender template:
  - loads due reminders from both reminder source tables
  - resolves global recipient emails from `reminder_recipient`
  - sends via SMTP (Postbox)
  - stores results in `reminder_email_delivery`
- `functions/.env.example` - required environment variables.

## Runtime topology

1. App sync exports transaction reminders to `reminder_email_source` and experiment reminders to `experiment_reminder_email_source`.
2. Cloud Function `send-daily-reminders` runs once per day.
3. Every run sends all reminders where `deleted_at IS NULL` and `reminder_at <= today`.

## Notes

- No extra DB needed, only extra tables in existing YDB.
- `reminder_recipient` stores a global mailing list for all reminders.
- `reminder_email_source` stays transaction-specific and mirrors transaction type and creation time.
- `experiment_reminder_email_source` mirrors experiment title for experiment digest lines.
- `reminder_email_delivery` is a send log, not a daily send blocker.
- `send_daily_emails.py` still contains TODOs for concrete YDB SDK queries.
