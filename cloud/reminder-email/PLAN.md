# Plan: Daily Email Reminders (Simplified)

## Goal

Deliver reminders by email once per day from YDB without projection worker.

## Architecture decision

- Use dedicated YDB table `reminder_email_queue` as normalized source for mailing.
- Keep recipients in `reminder_recipient`.
- Keep idempotency/audit in `reminder_email_delivery`.
- Do not use `project-reminders` function.

## Phase 1. YDB schema

1. Create `reminder_email_queue`.
2. Create `reminder_recipient`.
3. Create `reminder_email_delivery`.

Result: all data needed for daily sender exists in normalized tables.

## Phase 2. Sync update in app

1. During reminder UPSERT sync, also upsert row into `reminder_email_queue` by `reminder_sync_id`.
2. During reminder DELETE sync, set `deleted_at` in `reminder_email_queue`.
3. Keep `sync_push_log` as is (optional for debugging/compatibility).

Result: queue table is always up to date from normal app sync.

## Phase 3. Daily email worker

1. Determine target date in configured timezone.
2. Select due active reminders from `reminder_email_queue`.
3. Join active recipients from `reminder_recipient`.
4. Build `delivery_id = reminder_sync_id + ':' + email + ':' + YYYY-MM-DD`.
5. Skip if delivery already exists.
6. Send via Postbox SMTP.
7. Save success/failure row to `reminder_email_delivery`.

Schedule: once per day.

## Phase 4. Operations

1. Configure env vars and secrets in Cloud Function.
2. Set logs/alerts for send failures.
3. Monitor number of failed deliveries per day.
