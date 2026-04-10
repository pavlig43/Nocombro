# Plan: Daily Email Reminders (Simplified)

## Goal

Deliver reminders by email once per day from YDB without projection worker.

## Architecture decision

- Keep `Room` as the local source of truth in the desktop app.
- Use dedicated YDB table `reminder_email_source` as the mirrored reminder state for mailing.
- Keep a global recipient list in `reminder_recipient`.
- Keep send history in `reminder_email_delivery`.
- Do not use `project-reminders` function.

## Phase 1. YDB schema

1. Create `reminder_email_source`.
2. Create `reminder_recipient`.
3. Create `reminder_email_delivery`.

Result: all data needed for daily sender exists in normalized tables.

## Phase 2. Sync update in app

1. During reminder UPSERT sync from `Room`, also upsert row into `reminder_email_source` by `reminder_sync_id`.
2. During reminder DELETE sync, set `deleted_at` in `reminder_email_source`.
3. Keep `sync_push_log` as is (optional for debugging/compatibility).

Result: source table is always up to date from normal app sync and includes human-readable transaction context for emails.

## Phase 3. Daily email worker

1. Determine target date in configured timezone.
2. Select reminders from `reminder_email_source` where `deleted_at IS NULL` and `reminder_at <= today`.
3. Load active global recipients from `reminder_recipient`.
4. Send via Postbox SMTP on every run.
5. Save success/failure row to `reminder_email_delivery`.

Schedule: once per day.

## Phase 4. Operations

1. Configure env vars and secrets in Cloud Function.
2. Set logs/alerts for send failures.
3. Monitor number of failed deliveries per day.
