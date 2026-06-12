# Reminder Email Pipeline (YDB mirror + SMTP)

The daily function reads reminders directly from typed mirror tables:

- `reminder` joined with `transact`
- `experiment_reminder` joined with `experiment`

Legacy `reminder_email_source` and `experiment_reminder_email_source` tables are
not used and can be removed with `database/ydb/drop_legacy_sync_tables.sql`.

`reminder_recipient` and `reminder_email_delivery` remain function-owned state,
not part of the desktop synchronization transport. Their DDL is in
`ydb/001_email_reminder_schema.sql`.

Set `YDB_MIRROR_ROOT` only when desktop mirror tables are stored under a
directory. Leave it empty for root-level mirror tables.
