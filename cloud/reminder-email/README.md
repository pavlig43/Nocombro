# Reminder Email Pipeline (YDB mirror + SMTP)

The daily function reads reminders from typed mirror tables:

- `reminder` joined with `transact`
- `experiment_reminder` joined with `experiment`

Legacy `reminder_email_source` and `experiment_reminder_email_source` tables are
not used and can be removed with `database/ydb/drop_legacy_sync_tables.sql`.

## Delivery model

Each date has one digest, one SMTP message, and one state row with delivery ID
`digest:{date}`. All active addresses receive that message in the `To` header.
Addresses are normalized, deduplicated, and sorted before the claim.

The function claims the digest in a serializable YDB transaction before SMTP:

- a new digest or an eligible retry becomes `SENDING`;
- every claim increments `attempt_count`;
- a fresh `SENDING` returns `IN_PROGRESS` without another SMTP call;
- a `SENDING` older than `DELIVERY_SENDING_TIMEOUT_MINUTES` may be claimed
  again;
- an SMTP error changes the row to `FAILED`;
- `FAILED` is retried while `attempt_count` is below
  `DELIVERY_MAX_ATTEMPTS`;
- successful SMTP and state persistence change the row to `SENT`;
- `SENT` and `FAILED` at the attempt limit are not sent again.

Terminal updates are fenced by `attempt_count`. A worker from an expired attempt
cannot mark a newer `SENDING` attempt as `SENT` or `FAILED`; an update that
matches no row is reported as a lost claim.

Retries use the same `Message-ID`, derived from the date and digest delivery ID.
This ID helps mail systems correlate attempts but does not guarantee duplicate
removal. If SMTP accepts the message and the connection times out before the
client receives the result, a later automatic retry can produce a duplicate.
This pipeline accepts that SMTP limitation and retries automatically.

The last failed attempt emits one JSON log event per address. The event contains
`delivery_date`, `address_hash`, `attempt_count`, and `error_type`, but no plain
address or exception text. It is diagnostic only and requires no operator
action.

## Environment

Use `.env.example` as the variable list. Delivery defaults are:

| Variable | Default | Purpose |
| --- | ---: | --- |
| `DELIVERY_SENDING_TIMEOUT_MINUTES` | `30` | Age after which `SENDING` may be retried |
| `DELIVERY_MAX_ATTEMPTS` | `3` | Maximum SMTP attempts for one digest |

Set `YDB_MIRROR_ROOT` only when desktop mirror tables live under a directory.
Leave it empty for root-level mirror tables. Table names may be overridden with
`YDB_REMINDER_RECIPIENT_TABLE` and `YDB_REMINDER_DELIVERY_TABLE`.

The function calls `ydb.credentials_from_env_variables()`:

- In Yandex Cloud Functions, set `YDB_METADATA_CREDENTIALS=1`. The function
  checks the authentication method before starting the YDB driver.
- For a local run, remove `YDB_METADATA_CREDENTIALS` and set
  `YDB_ACCESS_TOKEN_CREDENTIALS` to a short-lived IAM token.
- `YDB_SERVICE_ACCOUNT_KEY_FILE_CREDENTIALS` is supported for a local
  service-account file. Do not put keys or tokens in Git or logs.

## YDB schema and migration

`reminder_recipient` and `reminder_email_delivery` are function-owned state, not
part of desktop sync.

For a new database, apply `ydb/001_email_reminder_schema.sql`. It already
contains `attempt_count Uint64` and `status_changed_at Utf8`.

For an existing database, complete these steps before deploying the function:

1. Describe `reminder_email_delivery` in the selected YDB and check whether both
   columns exist.
2. If both columns are absent, apply `ydb/002_delivery_claim_state.sql` once with
   YDB CLI. If only one column exists, stop and inspect the partial migration;
   the one-time schema script adds both columns together.
3. In a separate YDB CLI call, apply
   `ydb/003_delivery_claim_state_backfill.sql`. YDB does not allow DDL and DML in
   the same query. The backfill sets `attempt_count` to `1` and uses
   `delivered_at`, `delivery_date`, or a fixed UTC fallback for
   `status_changed_at`.
4. Describe the table again and verify the column types.
5. Run the following check and require `rows_with_nulls = 0`:

```sql
SELECT COUNT(*) AS rows_with_nulls
FROM `reminder_email_delivery`
WHERE attempt_count IS NULL OR status_changed_at IS NULL;
```

Do not run this migration from the function at startup. Do not print the
service-account key, tokens, endpoint parameters, or database parameters while
applying it.

## Tests

From `cloud/reminder-email` run:

```powershell
$env:PYTHONDONTWRITEBYTECODE = "1"
python -m pip install -r functions/requirements.txt
python -m unittest discover -s tests -v
```
