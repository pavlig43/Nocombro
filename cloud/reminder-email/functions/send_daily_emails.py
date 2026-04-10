from __future__ import annotations

import logging
import os
import smtplib
from dataclasses import dataclass
from datetime import date, datetime, timezone
from email.message import EmailMessage
from zoneinfo import ZoneInfo

import ydb

logging.basicConfig(level=logging.INFO)
LOG = logging.getLogger("send-daily-reminders")

_DRIVER = None
_POOL = None


@dataclass(frozen=True)
class DueReminder:
    reminder_sync_id: str
    transaction_sync_id: str
    transaction_type: str
    transaction_created_at: datetime
    reminder_text: str
    reminder_at: datetime


def parse_datetime(value: object, fallback: datetime | None = None) -> datetime:
    if isinstance(value, datetime):
        return value
    if isinstance(value, str):
        return datetime.fromisoformat(value)
    if value is None and fallback is not None:
        return fallback
    raise TypeError(f"Unsupported datetime value: {value!r}")


def get_pool() -> ydb.QuerySessionPool:
    global _DRIVER, _POOL

    if _POOL is not None:
        return _POOL

    key_json = os.environ["YDB_SERVICE_ACCOUNT_KEY"]

    _DRIVER = ydb.Driver(
        endpoint=os.environ["YDB_ENDPOINT"],
        database=os.environ["YDB_DATABASE"],
        credentials=ydb.iam.ServiceAccountCredentials.from_content(key_json),
    )
    _DRIVER.wait(timeout=10, fail_fast=True)
    _POOL = ydb.QuerySessionPool(_DRIVER)
    return _POOL


class DeliveryRepository:
    def list_due_reminders(self, target_date: date) -> list[DueReminder]:
        raise NotImplementedError

    def list_recipients(self) -> list[str]:
        raise NotImplementedError

    def save_delivery(
        self,
        delivery_id: str,
        reminder_sync_id: str,
        target_email: str,
        delivery_date: date,
        status: str,
        error_text: str | None,
    ) -> None:
        raise NotImplementedError


class YdbDeliveryRepository(DeliveryRepository):
    def __init__(self) -> None:
        self.source_table = os.getenv("YDB_REMINDER_SOURCE_TABLE", "reminder_email_source")
        self.recipient_table = os.getenv("YDB_REMINDER_RECIPIENT_TABLE", "reminder_recipient")
        self.delivery_table = os.getenv("YDB_REMINDER_DELIVERY_TABLE", "reminder_email_delivery")

    def list_due_reminders(self, target_date: date) -> list[DueReminder]:
        target_until = f"{target_date.isoformat()}T23:59:59"

        query = f"""
        DECLARE $target_until AS Utf8;

        SELECT
            reminder_sync_id,
            transaction_sync_id,
            transaction_type,
            transaction_created_at,
            reminder_text,
            reminder_at
        FROM `{self.source_table}`
        WHERE deleted_at IS NULL
            AND reminder_at IS NOT NULL
            AND reminder_at <= $target_until
        ORDER BY reminder_at ASC;
        """

        result_sets = get_pool().execute_with_retries(
            query,
            {"$target_until": target_until},
        )

        reminders: list[DueReminder] = []
        for result_set in result_sets:
            for row in result_set.rows:
                reminders.append(
                    DueReminder(
                        reminder_sync_id=row["reminder_sync_id"],
                        transaction_sync_id=row["transaction_sync_id"],
                        transaction_type=row["transaction_type"] or "",
                        transaction_created_at=parse_datetime(
                            row["transaction_created_at"],
                            fallback=parse_datetime(row["reminder_at"]),
                        ),
                        reminder_text=row["reminder_text"] or "",
                        reminder_at=parse_datetime(row["reminder_at"]),
                    )
                )

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

    def save_delivery(
        self,
        delivery_id: str,
        reminder_sync_id: str,
        target_email: str,
        delivery_date: date,
        status: str,
        error_text: str | None,
    ) -> None:
        delivered_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat()

        query = f"""
        DECLARE $delivery_id AS Utf8;
        DECLARE $reminder_sync_id AS Utf8;
        DECLARE $delivery_date AS Utf8;
        DECLARE $delivered_at AS Utf8;
        DECLARE $target_email AS Utf8;
        DECLARE $status AS Utf8;
        DECLARE $error_text AS Utf8;

        UPSERT INTO `{self.delivery_table}` (
            delivery_id,
            reminder_sync_id,
            delivery_date,
            delivered_at,
            target_email,
            status,
            error_text
        ) VALUES (
            $delivery_id,
            $reminder_sync_id,
            $delivery_date,
            $delivered_at,
            $target_email,
            $status,
            $error_text
        );
        """

        get_pool().execute_with_retries(
            query,
            {
                "$delivery_id": delivery_id,
                "$reminder_sync_id": reminder_sync_id,
                "$delivery_date": delivery_date.isoformat(),
                "$delivered_at": delivered_at,
                "$target_email": target_email,
                "$status": status,
                "$error_text": error_text or "",
            },
        )


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
) -> None:
    msg = EmailMessage()
    msg["From"] = email_from
    msg["To"] = email_to
    msg["Subject"] = subject
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


def build_delivery_id(reminder_sync_id: str, target_email: str, target_date: date) -> str:
    return f"{reminder_sync_id}:{target_email}:{target_date.isoformat()}"


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
                f"{reminder.transaction_type}\n"
                f"{reminder.transaction_created_at.strftime('%d.%m.%Y %H:%M')}\n"
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

    reminders = repo.list_due_reminders(target_date)
    recipients = repo.list_recipients()

    sent = 0
    failed = 0

    if not recipients:
        LOG.info("No global recipients configured")
        return {
            "ok": True,
            "target_date": target_date.isoformat(),
            "sent": 0,
            "failed": 0,
        }

    if not reminders:
        LOG.info("No reminders due for %s", target_date.isoformat())
        return {
            "ok": True,
            "target_date": target_date.isoformat(),
            "sent": 0,
            "failed": 0,
        }

    subject = f"{subject_prefix} {target_date.isoformat()}"
    body = build_digest_body(reminders)
    joined_recipients = ", ".join(recipients)
    delivery_id = f"digest:{target_date.isoformat()}:{joined_recipients}"

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
        )
        repo.save_delivery(
            delivery_id=delivery_id,
            reminder_sync_id="digest",
            target_email=joined_recipients,
            delivery_date=target_date,
            status="SENT",
            error_text=None,
        )
        sent = 1
    except Exception as exc:
        repo.save_delivery(
            delivery_id=delivery_id,
            reminder_sync_id="digest",
            target_email=joined_recipients,
            delivery_date=target_date,
            status="FAILED",
            error_text=str(exc),
        )
        failed = 1

    return {
        "ok": True,
        "target_date": target_date.isoformat(),
        "sent": sent,
        "failed": failed,
    }


def handler(event: dict, context: object) -> dict:
    _ = event, context
    tz = ZoneInfo(os.getenv("REMINDER_TIMEZONE", "Europe/Moscow"))
    target_date = datetime.now(tz).date()
    repo = YdbDeliveryRepository()
    return run_daily_delivery(repo=repo, target_date=target_date)
