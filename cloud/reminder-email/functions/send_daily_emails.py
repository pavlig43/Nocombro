from __future__ import annotations

import logging
import os
import smtplib
from dataclasses import dataclass
from datetime import date, datetime
from email.message import EmailMessage
from zoneinfo import ZoneInfo

logging.basicConfig(level=logging.INFO)
LOG = logging.getLogger("send-daily-reminders")


@dataclass(frozen=True)
class DueReminder:
    reminder_sync_id: str
    transaction_sync_id: str
    reminder_text: str
    reminder_at: datetime


class DeliveryRepository:
    """Storage adapter for YDB operations."""

    def list_due_reminders(self, target_date: date) -> list[DueReminder]:
        raise NotImplementedError

    def list_recipients(self, reminder_sync_id: str) -> list[str]:
        raise NotImplementedError

    def delivery_exists(self, delivery_id: str) -> bool:
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
        self.endpoint = os.environ["YDB_ENDPOINT"]
        self.database = os.environ["YDB_DATABASE"]
        self.token = os.environ.get("YDB_IAM_TOKEN")
        self.queue_table = os.getenv("YDB_REMINDER_QUEUE_TABLE", "reminder_email_queue")
        self.recipient_table = os.getenv("YDB_REMINDER_RECIPIENT_TABLE", "reminder_recipient")
        self.delivery_table = os.getenv("YDB_REMINDER_DELIVERY_TABLE", "reminder_email_delivery")

    def list_due_reminders(self, target_date: date) -> list[DueReminder]:
        raise NotImplementedError("TODO: implement YDB select due reminders from reminder_email_queue")

    def list_recipients(self, reminder_sync_id: str) -> list[str]:
        raise NotImplementedError("TODO: implement YDB select from reminder_recipient")

    def delivery_exists(self, delivery_id: str) -> bool:
        raise NotImplementedError("TODO: implement YDB select from reminder_email_delivery")

    def save_delivery(
        self,
        delivery_id: str,
        reminder_sync_id: str,
        target_email: str,
        delivery_date: date,
        status: str,
        error_text: str | None,
    ) -> None:
        raise NotImplementedError("TODO: implement YDB upsert into reminder_email_delivery")


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


def run_daily_delivery(repo: DeliveryRepository, target_date: date) -> dict:
    smtp_host = os.environ["SMTP_HOST"]
    smtp_port = int(os.getenv("SMTP_PORT", "465"))
    smtp_user = os.environ["SMTP_USERNAME"]
    smtp_password = os.environ["SMTP_PASSWORD"]
    smtp_use_ssl = os.getenv("SMTP_USE_SSL", "true").lower() == "true"
    email_from = os.environ["EMAIL_FROM"]
    subject_prefix = os.getenv("EMAIL_SUBJECT_PREFIX", "[Nocombro Reminder]")

    reminders = repo.list_due_reminders(target_date)
    sent = 0
    failed = 0
    skipped = 0

    for reminder in reminders:
        recipients = repo.list_recipients(reminder.reminder_sync_id)
        if not recipients:
            LOG.info("No recipients for reminder %s", reminder.reminder_sync_id)
            continue

        for recipient in recipients:
            delivery_id = build_delivery_id(reminder.reminder_sync_id, recipient, target_date)
            if repo.delivery_exists(delivery_id):
                skipped += 1
                continue

            subject = f"{subject_prefix} {target_date.isoformat()}"
            body = (
                f"Reminder: {reminder.reminder_text}\n"
                f"Reminder time: {reminder.reminder_at.isoformat()}\n"
                f"Transaction: {reminder.transaction_sync_id}\n"
            )

            try:
                send_email(
                    smtp_host=smtp_host,
                    smtp_port=smtp_port,
                    smtp_user=smtp_user,
                    smtp_password=smtp_password,
                    smtp_use_ssl=smtp_use_ssl,
                    email_from=email_from,
                    email_to=recipient,
                    subject=subject,
                    body=body,
                )
                repo.save_delivery(
                    delivery_id=delivery_id,
                    reminder_sync_id=reminder.reminder_sync_id,
                    target_email=recipient,
                    delivery_date=target_date,
                    status="SENT",
                    error_text=None,
                )
                sent += 1
            except Exception as exc:  # noqa: BLE001
                repo.save_delivery(
                    delivery_id=delivery_id,
                    reminder_sync_id=reminder.reminder_sync_id,
                    target_email=recipient,
                    delivery_date=target_date,
                    status="FAILED",
                    error_text=str(exc),
                )
                failed += 1

    return {
        "ok": True,
        "target_date": target_date.isoformat(),
        "sent": sent,
        "failed": failed,
        "skipped": skipped,
    }


def handler(event: dict, context: object) -> dict:
    _ = event, context
    tz = ZoneInfo(os.getenv("REMINDER_TIMEZONE", "Europe/Moscow"))
    target_date = datetime.now(tz).date()
    repo = YdbDeliveryRepository()
    return run_daily_delivery(repo=repo, target_date=target_date)


if __name__ == "__main__":
    print(handler({}, None))
