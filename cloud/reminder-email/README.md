# Reminder Email Pipeline (YDB + Postbox)

## Scope

This package prepares cloud-side daily email delivery for reminders.
Desktop app runtime stays mostly unchanged, except reminder sync should update `reminder_email_queue`.

## What is included

- `PLAN.md` - simplified implementation plan.
- `ydb/001_email_reminder_schema.sql` - YDB schema:
  - `reminder_email_queue`
  - `reminder_recipient`
  - `reminder_email_delivery`
- `functions/send_daily_emails.py` - daily sender template:
  - loads due reminders from `reminder_email_queue`
  - resolves recipient emails from `reminder_recipient`
  - sends via SMTP (Postbox)
  - stores results in `reminder_email_delivery`
- `functions/.env.example` - required environment variables.

## Runtime topology

1. App sync writes reminder state to `reminder_email_queue`.
2. Cloud Function `send-daily-reminders` runs once per day.

## Notes

- No extra DB needed, only extra tables in existing YDB.
- Daily sender is idempotent through `reminder_email_delivery`.
- `send_daily_emails.py` still contains TODOs for concrete YDB SDK queries.
