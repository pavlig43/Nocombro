from __future__ import annotations

import json
import logging
import os
import smtplib
from collections.abc import Iterable
from dataclasses import dataclass
from datetime import date, datetime, timedelta, timezone
from email.message import EmailMessage
from enum import Enum
from hashlib import sha256
from zoneinfo import ZoneInfo

import ydb

logging.basicConfig(level=logging.INFO)
LOG = logging.getLogger("send-daily-reminders")

_DRIVER = None
_POOL = None

YDB_CREDENTIAL_ENVIRONMENT_VARIABLES = (
    "YDB_METADATA_CREDENTIALS",
    "YDB_ACCESS_TOKEN_CREDENTIALS",
    "YDB_SERVICE_ACCOUNT_KEY_FILE_CREDENTIALS",
)


@dataclass(frozen=True)
class DueReminder:
    reminder_sync_id: str
    owner_label: str
    owner_subtitle: str
    reminder_text: str
    reminder_at: datetime


class ClaimResult(Enum):
    CLAIMED = "CLAIMED"
    SENT = "SENT"
    IN_PROGRESS = "IN_PROGRESS"
    FAILED = "FAILED"


@dataclass(frozen=True)
class DeliveryClaim:
    result: ClaimResult
    attempt_count: int
    final_error_type: str | None = None


class DeliveryBatchError(RuntimeError):
    def __init__(self, summary: dict) -> None:
        self.summary = summary
        super().__init__(
            "Reminder delivery failed: "
            f"status={summary['status']}, attempt_count={summary['attempt_count']}"
        )


class DeliveryClaimLostError(RuntimeError):
    pass


def parse_datetime(value: object, fallback: datetime | None = None) -> datetime:
    if isinstance(value, datetime):
        return value
    if isinstance(value, str):
        return datetime.fromisoformat(value)
    if value is None and fallback is not None:
        return fallback
    raise TypeError(f"Unsupported datetime value: {value!r}")


def utc_now() -> datetime:
    return datetime.now(timezone.utc).replace(microsecond=0)


def utc_text(value: datetime) -> str:
    return value.astimezone(timezone.utc).replace(microsecond=0).isoformat()


def parse_utc_text(value: object) -> datetime | None:
    if not isinstance(value, str) or not value.strip():
        return None
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None
    if parsed.tzinfo is None:
        return parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def positive_int_env(name: str, default: int) -> int:
    value = int(os.getenv(name, str(default)))
    if value < 1:
        raise ValueError(f"{name} must be greater than zero")
    return value


def normalize_email(value: str) -> str:
    return value.strip().casefold()


def address_hash(email: str) -> str:
    return sha256(normalize_email(email).encode("utf-8")).hexdigest()


def safe_error_text(source: str, error: Exception) -> str:
    return f"{source} error: {type(error).__name__}"


def log_final_failure(
    delivery_date: date,
    recipients: list[str],
    attempt_count: int,
    error_type: str,
) -> None:
    for email in recipients:
        LOG.error(
            "%s",
            json.dumps(
                {
                    "address_hash": address_hash(email),
                    "attempt_count": attempt_count,
                    "delivery_date": delivery_date.isoformat(),
                    "error_type": error_type,
                    "event": "reminder_delivery_final_failure",
                },
                separators=(",", ":"),
                sort_keys=True,
            ),
        )


def get_pool() -> ydb.QuerySessionPool:
    global _DRIVER, _POOL

    if _POOL is not None:
        return _POOL

    metadata_credentials = os.getenv("YDB_METADATA_CREDENTIALS", "").strip()
    if metadata_credentials and metadata_credentials != "1":
        raise RuntimeError("YDB_METADATA_CREDENTIALS must be set to 1")
    if not any(
        os.getenv(name, "").strip()
        for name in YDB_CREDENTIAL_ENVIRONMENT_VARIABLES
    ):
        raise RuntimeError(
            "YDB credentials are not configured. "
            "Set YDB_METADATA_CREDENTIALS=1 in Cloud Functions or configure "
            "YDB_ACCESS_TOKEN_CREDENTIALS / "
            "YDB_SERVICE_ACCOUNT_KEY_FILE_CREDENTIALS for a local run."
        )

    _DRIVER = ydb.Driver(
        endpoint=os.environ["YDB_ENDPOINT"],
        database=os.environ["YDB_DATABASE"],
        credentials=ydb.credentials_from_env_variables(),
    )
    _DRIVER.wait(timeout=10, fail_fast=True)
    _POOL = ydb.QuerySessionPool(_DRIVER)
    return _POOL


class DeliveryRepository:
    def list_due_reminders(self, target_date: date) -> list[DueReminder]:
        raise NotImplementedError

    def list_recipients(self) -> list[str]:
        raise NotImplementedError

    def claim_delivery(
        self,
        delivery_id: str,
        target_email: str,
        delivery_date: date,
        sending_timeout_minutes: int,
        max_attempts: int,
    ) -> DeliveryClaim:
        raise NotImplementedError

    def mark_sent(self, delivery_id: str, attempt_count: int) -> None:
        raise NotImplementedError

    def mark_failed(
        self,
        delivery_id: str,
        attempt_count: int,
        error_text: str,
    ) -> None:
        raise NotImplementedError


class YdbDeliveryRepository(DeliveryRepository):
    def __init__(self) -> None:
        mirror_root = os.getenv("YDB_MIRROR_ROOT", "").strip().strip("/")
        self.transaction_table = self._table_path(mirror_root, "transact")
        self.reminder_table = self._table_path(mirror_root, "reminder")
        self.experiment_table = self._table_path(mirror_root, "experiment")
        self.experiment_reminder_table = self._table_path(
            mirror_root,
            "experiment_reminder",
        )
        self.recipient_table = os.getenv("YDB_REMINDER_RECIPIENT_TABLE", "reminder_recipient")
        self.delivery_table = os.getenv("YDB_REMINDER_DELIVERY_TABLE", "reminder_email_delivery")

    @staticmethod
    def _table_path(root: str, table: str) -> str:
        return f"{root}/{table}" if root else table

    def list_due_reminders(self, target_date: date) -> list[DueReminder]:
        target_until = f"{target_date.isoformat()}T23:59:59"

        transaction_query = f"""
        DECLARE $target_until AS Utf8;

        SELECT
            reminder.sync_id AS reminder_sync_id,
            transact.transaction_type AS transaction_type,
            transact.created_at AS transaction_created_at,
            reminder.text AS reminder_text,
            reminder.reminder_date_time AS reminder_at
        FROM `{self.reminder_table}` AS reminder
        INNER JOIN `{self.transaction_table}` AS transact
            ON transact.sync_id = reminder.transaction_sync_id
        WHERE reminder.deleted_at IS NULL
            AND transact.deleted_at IS NULL
            AND reminder.reminder_date_time <= $target_until
        ORDER BY reminder.reminder_date_time ASC;
        """

        experiment_query = f"""
        DECLARE $target_until AS Utf8;

        SELECT
            reminder.sync_id AS reminder_sync_id,
            experiment.title AS experiment_title,
            reminder.text AS reminder_text,
            reminder.reminder_date_time AS reminder_at
        FROM `{self.experiment_reminder_table}` AS reminder
        INNER JOIN `{self.experiment_table}` AS experiment
            ON experiment.sync_id = reminder.experiment_sync_id
        WHERE reminder.deleted_at IS NULL
            AND experiment.deleted_at IS NULL
            AND reminder.reminder_date_time <= $target_until
        ORDER BY reminder.reminder_date_time ASC;
        """

        transaction_result_sets = get_pool().execute_with_retries(
            transaction_query,
            {"$target_until": target_until},
        )
        experiment_result_sets = get_pool().execute_with_retries(
            experiment_query,
            {"$target_until": target_until},
        )

        reminders: list[DueReminder] = []
        for result_set in transaction_result_sets:
            for row in result_set.rows:
                reminders.append(
                    DueReminder(
                        reminder_sync_id=row["reminder_sync_id"],
                        owner_label=row["transaction_type"] or "Транзакция",
                        owner_subtitle=parse_datetime(
                            row["transaction_created_at"],
                            fallback=parse_datetime(row["reminder_at"]),
                        ).strftime("%d.%m.%Y %H:%M"),
                        reminder_text=row["reminder_text"] or "",
                        reminder_at=parse_datetime(row["reminder_at"]),
                    )
                )

        for result_set in experiment_result_sets:
            for row in result_set.rows:
                reminders.append(
                    DueReminder(
                        reminder_sync_id=row["reminder_sync_id"],
                        owner_label="Эксперимент",
                        owner_subtitle=row["experiment_title"] or "",
                        reminder_text=row["reminder_text"] or "",
                        reminder_at=parse_datetime(row["reminder_at"]),
                    )
                )

        reminders.sort(key=lambda item: item.reminder_at)
        return reminders

    def list_recipients(self) -> list[str]:
        query = f"""
        SELECT email
        FROM `{self.recipient_table}`
        WHERE is_active = true;
        """

        result_sets = get_pool().execute_with_retries(query)
        recipients: list[str] = []

        for result_set in result_sets:
            for row in result_set.rows:
                recipients.append(row["email"])

        return recipients

    def claim_delivery(
        self,
        delivery_id: str,
        target_email: str,
        delivery_date: date,
        sending_timeout_minutes: int,
        max_attempts: int,
    ) -> DeliveryClaim:
        now = utc_now()
        status_changed_at = utc_text(now)
        sending_timeout = timedelta(minutes=sending_timeout_minutes)
        select_query = f"""
        DECLARE $delivery_id AS Utf8;

        SELECT status, attempt_count, status_changed_at
        FROM `{self.delivery_table}`
        WHERE delivery_id = $delivery_id;
        """
        claim_query = f"""
        DECLARE $delivery_id AS Utf8;
        DECLARE $delivery_date AS Utf8;
        DECLARE $target_email AS Utf8;
        DECLARE $attempt_count AS Uint64;
        DECLARE $status_changed_at AS Utf8;

        UPSERT INTO `{self.delivery_table}` (
            delivery_id,
            reminder_sync_id,
            delivery_date,
            delivered_at,
            target_email,
            status,
            error_text,
            attempt_count,
            status_changed_at
        ) VALUES (
            $delivery_id,
            "digest",
            $delivery_date,
            "",
            $target_email,
            "SENDING",
            "",
            $attempt_count,
            $status_changed_at
        );
        """
        finalize_timed_out_query = f"""
        DECLARE $delivery_id AS Utf8;
        DECLARE $status_changed_at AS Utf8;

        UPDATE `{self.delivery_table}`
        SET
            status = "FAILED",
            status_changed_at = $status_changed_at,
            error_text = "SENDING timeout after maximum attempts"
        WHERE delivery_id = $delivery_id
            AND status = "SENDING";
        """
        commit_query = "SELECT 1 AS committed;"

        def commit_read_only(tx: object) -> None:
            with tx.execute(commit_query, commit_tx=True):
                pass

        def claim(session: ydb.QuerySession) -> DeliveryClaim:
            params = {"$delivery_id": delivery_id}
            with session.transaction(ydb.QuerySerializableReadWrite()) as tx:
                rows = []
                with tx.execute(select_query, params) as result_sets:
                    for result_set in result_sets:
                        rows.extend(result_set.rows)

                status = rows[0]["status"] if rows else None
                attempt_count = int(rows[0]["attempt_count"] or 0) if rows else 0
                changed_at = parse_utc_text(rows[0]["status_changed_at"]) if rows else None

                if status == "SENT":
                    commit_read_only(tx)
                    return DeliveryClaim(ClaimResult.SENT, attempt_count)

                if status == "SENDING" and changed_at is not None:
                    if now - changed_at < sending_timeout:
                        commit_read_only(tx)
                        return DeliveryClaim(ClaimResult.IN_PROGRESS, attempt_count)

                if rows and status not in ("FAILED", "SENDING"):
                    raise RuntimeError(f"Unsupported delivery status: {status!r}")

                if attempt_count >= max_attempts:
                    if status != "SENDING":
                        commit_read_only(tx)
                        return DeliveryClaim(ClaimResult.FAILED, attempt_count)

                    with tx.execute(
                        finalize_timed_out_query,
                        {
                            "$delivery_id": delivery_id,
                            "$status_changed_at": status_changed_at,
                        },
                        commit_tx=True,
                    ):
                        pass
                    return DeliveryClaim(
                        ClaimResult.FAILED,
                        attempt_count,
                        final_error_type="SendingTimeout",
                    )

                next_attempt_count = attempt_count + 1
                with tx.execute(
                    claim_query,
                    {
                        "$delivery_id": delivery_id,
                        "$delivery_date": delivery_date.isoformat(),
                        "$target_email": target_email,
                        "$attempt_count": next_attempt_count,
                        "$status_changed_at": status_changed_at,
                    },
                    commit_tx=True,
                ):
                    pass
                return DeliveryClaim(ClaimResult.CLAIMED, next_attempt_count)

        return get_pool().retry_operation_sync(claim)

    @staticmethod
    def _require_updated_attempt(
        result_sets: Iterable[object],
        delivery_id: str,
        attempt_count: int,
    ) -> None:
        for result_set in result_sets:
            for _ in result_set.rows:
                return
        raise DeliveryClaimLostError(
            "Delivery attempt is no longer current: "
            f"delivery_id={delivery_id}, attempt_count={attempt_count}"
        )

    def mark_sent(self, delivery_id: str, attempt_count: int) -> None:
        now = utc_text(utc_now())
        query = f"""
        DECLARE $delivery_id AS Utf8;
        DECLARE $attempt_count AS Uint64;
        DECLARE $now AS Utf8;

        UPDATE `{self.delivery_table}`
        SET
            status = "SENT",
            delivered_at = $now,
            status_changed_at = $now,
            error_text = ""
        WHERE delivery_id = $delivery_id
            AND status = "SENDING"
            AND attempt_count = $attempt_count
        RETURNING delivery_id;
        """
        result_sets = get_pool().execute_with_retries(
            query,
            {
                "$delivery_id": delivery_id,
                "$attempt_count": attempt_count,
                "$now": now,
            },
        )
        self._require_updated_attempt(result_sets, delivery_id, attempt_count)

    def mark_failed(
        self,
        delivery_id: str,
        attempt_count: int,
        error_text: str,
    ) -> None:
        query = f"""
        DECLARE $delivery_id AS Utf8;
        DECLARE $attempt_count AS Uint64;
        DECLARE $error_text AS Utf8;
        DECLARE $status_changed_at AS Utf8;

        UPDATE `{self.delivery_table}`
        SET
            status = "FAILED",
            status_changed_at = $status_changed_at,
            error_text = $error_text
        WHERE delivery_id = $delivery_id
            AND status = "SENDING"
            AND attempt_count = $attempt_count
        RETURNING delivery_id;
        """
        result_sets = get_pool().execute_with_retries(
            query,
            {
                "$delivery_id": delivery_id,
                "$attempt_count": attempt_count,
                "$error_text": error_text,
                "$status_changed_at": utc_text(utc_now()),
            },
        )
        self._require_updated_attempt(result_sets, delivery_id, attempt_count)


def send_email(
    smtp_host: str,
    smtp_port: int,
    smtp_user: str,
    smtp_password: str,
    smtp_use_ssl: bool,
    email_from: str,
    email_to: str,
    subject: str,
    body: str,
    message_id: str,
) -> None:
    msg = EmailMessage()
    msg["From"] = email_from
    msg["To"] = email_to
    msg["Subject"] = subject
    msg["Message-ID"] = message_id
    msg.set_content(body)

    if smtp_use_ssl:
        with smtplib.SMTP_SSL(host=smtp_host, port=smtp_port, timeout=20) as smtp:
            smtp.login(smtp_user, smtp_password)
            smtp.send_message(msg)
    else:
        with smtplib.SMTP(host=smtp_host, port=smtp_port, timeout=20) as smtp:
            smtp.starttls()
            smtp.login(smtp_user, smtp_password)
            smtp.send_message(msg)


def build_delivery_id(target_date: date) -> str:
    return f"digest:{target_date.isoformat()}"


def build_message_id(delivery_id: str, target_date: date) -> str:
    source = f"{target_date.isoformat()}:{delivery_id}"
    digest_hash = sha256(source.encode("utf-8")).hexdigest()[:32]
    return f"<digest.{target_date.isoformat()}.{digest_hash}@nocombro.local>"


def build_digest_body(reminders: list[DueReminder]) -> str:
    parts = []

    for index, reminder in enumerate(reminders, start=1):
        parts.append(
            (
                f"{index}.\n"
                "Время напоминания\n"
                f"{reminder.reminder_at.strftime('%d.%m.%Y %H:%M')}\n\n"
                "Напоминание\n"
                f"{reminder.reminder_text}\n\n"
                f"{reminder.owner_label}\n"
                f"{reminder.owner_subtitle}\n"
            )
        )

    return "\n\n".join(parts)


def run_daily_delivery(repo: DeliveryRepository, target_date: date) -> dict:
    smtp_host = os.environ["SMTP_HOST"]
    smtp_port = int(os.getenv("SMTP_PORT", "465"))
    smtp_user = os.environ["SMTP_USERNAME"]
    smtp_password = os.environ["SMTP_PASSWORD"]
    smtp_use_ssl = os.getenv("SMTP_USE_SSL", "true").lower() == "true"
    email_from = os.environ["EMAIL_FROM"]
    subject_prefix = os.getenv("EMAIL_SUBJECT_PREFIX", "[Nocombro Reminder]")
    sending_timeout_minutes = positive_int_env(
        "DELIVERY_SENDING_TIMEOUT_MINUTES",
        30,
    )
    max_attempts = positive_int_env("DELIVERY_MAX_ATTEMPTS", 3)

    reminders = repo.list_due_reminders(target_date)
    recipients = sorted(
        {
            normalized
            for email in repo.list_recipients()
            if (normalized := normalize_email(email))
        }
    )

    if not recipients:
        LOG.info("No global recipients configured")
        return {
            "ok": True,
            "target_date": target_date.isoformat(),
            "status": "NO_RECIPIENTS",
            "attempt_count": 0,
            "sent": 0,
            "failed": 0,
            "skipped": 0,
        }

    if not reminders:
        LOG.info("No reminders due for %s", target_date.isoformat())
        return {
            "ok": True,
            "target_date": target_date.isoformat(),
            "status": "NO_REMINDERS",
            "attempt_count": 0,
            "sent": 0,
            "failed": 0,
            "skipped": 0,
        }

    subject = f"{subject_prefix} {target_date.isoformat()}"
    body = build_digest_body(reminders)
    joined_recipients = ", ".join(recipients)
    delivery_id = build_delivery_id(target_date)
    message_id = build_message_id(delivery_id, target_date)

    try:
        claim = repo.claim_delivery(
            delivery_id=delivery_id,
            target_email=joined_recipients,
            delivery_date=target_date,
            sending_timeout_minutes=sending_timeout_minutes,
            max_attempts=max_attempts,
        )
    except Exception as exc:
        summary = {
            "ok": False,
            "target_date": target_date.isoformat(),
            "status": "CLAIM_FAILED",
            "attempt_count": 0,
            "sent": 0,
            "failed": 1,
            "skipped": 0,
        }
        LOG.error("Delivery claim failed error_type=%s", type(exc).__name__)
        raise DeliveryBatchError(summary) from exc

    if claim.result is ClaimResult.SENT:
        return {
            "ok": True,
            "target_date": target_date.isoformat(),
            "status": ClaimResult.SENT.value,
            "attempt_count": claim.attempt_count,
            "sent": 0,
            "failed": 0,
            "skipped": 1,
        }

    if claim.result is ClaimResult.IN_PROGRESS:
        return {
            "ok": True,
            "target_date": target_date.isoformat(),
            "status": ClaimResult.IN_PROGRESS.value,
            "attempt_count": claim.attempt_count,
            "sent": 0,
            "failed": 0,
            "skipped": 1,
        }

    if claim.result is ClaimResult.FAILED:
        if claim.final_error_type is not None:
            log_final_failure(
                target_date,
                recipients,
                claim.attempt_count,
                claim.final_error_type,
            )
        return {
            "ok": False,
            "target_date": target_date.isoformat(),
            "status": ClaimResult.FAILED.value,
            "attempt_count": claim.attempt_count,
            "sent": 0,
            "failed": 1,
            "skipped": 1,
        }

    try:
        send_email(
            smtp_host=smtp_host,
            smtp_port=smtp_port,
            smtp_user=smtp_user,
            smtp_password=smtp_password,
            smtp_use_ssl=smtp_use_ssl,
            email_from=email_from,
            email_to=joined_recipients,
            subject=subject,
            body=body,
            message_id=message_id,
        )
    except Exception as exc:
        failure_status = ClaimResult.FAILED.value
        try:
            repo.mark_failed(
                delivery_id=delivery_id,
                attempt_count=claim.attempt_count,
                error_text=safe_error_text("SMTP", exc),
            )
        except Exception as mark_exc:
            failure_status = "SENDING"
            LOG.error(
                "Failed to persist SMTP failure error_type=%s",
                type(mark_exc).__name__,
            )
        else:
            if claim.attempt_count >= max_attempts:
                log_final_failure(
                    target_date,
                    recipients,
                    claim.attempt_count,
                    type(exc).__name__,
                )

        summary = {
            "ok": False,
            "target_date": target_date.isoformat(),
            "status": failure_status,
            "attempt_count": claim.attempt_count,
            "sent": 0,
            "failed": 1,
            "skipped": 0,
        }
        raise DeliveryBatchError(summary) from exc

    try:
        repo.mark_sent(delivery_id, claim.attempt_count)
    except Exception as exc:
        summary = {
            "ok": False,
            "target_date": target_date.isoformat(),
            "status": "SENDING",
            "attempt_count": claim.attempt_count,
            "sent": 0,
            "failed": 1,
            "skipped": 0,
        }
        LOG.error("Failed to mark delivery SENT error_type=%s", type(exc).__name__)
        raise DeliveryBatchError(summary) from exc

    return {
        "ok": True,
        "target_date": target_date.isoformat(),
        "status": ClaimResult.SENT.value,
        "attempt_count": claim.attempt_count,
        "sent": 1,
        "failed": 0,
        "skipped": 0,
    }


def handler(event: dict, context: object) -> dict:
    _ = event, context
    tz = ZoneInfo(os.getenv("REMINDER_TIMEZONE", "Europe/Moscow"))
    target_date = datetime.now(tz).date()
    repo = YdbDeliveryRepository()
    return run_daily_delivery(repo=repo, target_date=target_date)
