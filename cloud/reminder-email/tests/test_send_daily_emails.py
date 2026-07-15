from __future__ import annotations

import json
import os
import sys
import threading
import types
import unittest
from concurrent.futures import ThreadPoolExecutor
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from unittest.mock import Mock, patch

FUNCTIONS_DIR = Path(__file__).resolve().parents[1] / "functions"
sys.path.insert(0, str(FUNCTIONS_DIR))

try:
    import ydb  # noqa: F401
except ModuleNotFoundError:
    sys.modules["ydb"] = types.ModuleType("ydb")

import send_daily_emails as delivery


class FakeRepository(delivery.DeliveryRepository):
    def __init__(self, recipients: list[str] | None = None) -> None:
        self.recipients = (
            ["owner@example.com"] if recipients is None else recipients
        )
        self.reminders = [
            delivery.DueReminder(
                reminder_sync_id="reminder-1",
                owner_label="Experiment",
                owner_subtitle="Safe retries",
                reminder_text="Check the result",
                reminder_at=datetime(2026, 7, 13, 9, 0),
            )
        ]
        self.deliveries: dict[str, dict] = {}
        self.now = datetime(2026, 7, 13, 8, 0, tzinfo=timezone.utc)
        self.claim_error: Exception | None = None
        self.mark_sent_error: Exception | None = None
        self.mark_failed_error: Exception | None = None
        self.lock = threading.Lock()

    def advance(self, minutes: int) -> None:
        self.now += timedelta(minutes=minutes)

    def list_due_reminders(self, target_date: date) -> list[delivery.DueReminder]:
        _ = target_date
        return self.reminders

    def list_recipients(self) -> list[str]:
        return self.recipients

    def claim_delivery(
        self,
        delivery_id: str,
        target_email: str,
        delivery_date: date,
        sending_timeout_minutes: int,
        max_attempts: int,
    ) -> delivery.DeliveryClaim:
        if self.claim_error is not None:
            raise self.claim_error

        with self.lock:
            row = self.deliveries.get(delivery_id)
            status = row["status"] if row is not None else None
            attempt_count = (row or {}).get("attempt_count", 0)

            if status == "SENT":
                return delivery.DeliveryClaim(
                    delivery.ClaimResult.SENT,
                    attempt_count,
                )

            if status == "SENDING":
                changed_at = row["status_changed_at"]
                if self.now - changed_at < timedelta(
                    minutes=sending_timeout_minutes
                ):
                    return delivery.DeliveryClaim(
                        delivery.ClaimResult.IN_PROGRESS,
                        attempt_count,
                    )

            if attempt_count >= max_attempts:
                final_error_type = None
                if status == "SENDING":
                    row["status"] = "FAILED"
                    row["status_changed_at"] = self.now
                    row["error_text"] = "SENDING timeout after maximum attempts"
                    final_error_type = "SendingTimeout"
                return delivery.DeliveryClaim(
                    delivery.ClaimResult.FAILED,
                    attempt_count,
                    final_error_type=final_error_type,
                )

            attempt_count += 1
            self.deliveries[delivery_id] = {
                "status": "SENDING",
                "attempt_count": attempt_count,
                "target_email": target_email,
                "delivery_date": delivery_date,
                "status_changed_at": self.now,
                "error_text": "",
            }
            return delivery.DeliveryClaim(
                delivery.ClaimResult.CLAIMED,
                attempt_count,
            )

    def mark_sent(self, delivery_id: str, attempt_count: int) -> None:
        if self.mark_sent_error is not None:
            raise self.mark_sent_error
        with self.lock:
            row = self.deliveries[delivery_id]
            if row["status"] != "SENDING" or row["attempt_count"] != attempt_count:
                raise delivery.DeliveryClaimLostError("Stale delivery attempt")
            row["status"] = "SENT"
            row["status_changed_at"] = self.now

    def mark_failed(
        self,
        delivery_id: str,
        attempt_count: int,
        error_text: str,
    ) -> None:
        if self.mark_failed_error is not None:
            raise self.mark_failed_error
        with self.lock:
            row = self.deliveries[delivery_id]
            if row["status"] != "SENDING" or row["attempt_count"] != attempt_count:
                raise delivery.DeliveryClaimLostError("Stale delivery attempt")
            row["status"] = "FAILED"
            row["status_changed_at"] = self.now
            row["error_text"] = error_text


class DailyDeliveryTest(unittest.TestCase):
    target_date = date(2026, 7, 13)

    def setUp(self) -> None:
        self.env = patch.dict(
            os.environ,
            {
                "SMTP_HOST": "smtp.example.com",
                "SMTP_PORT": "465",
                "SMTP_USERNAME": "sender@example.com",
                "SMTP_PASSWORD": "secret",
                "SMTP_USE_SSL": "true",
                "EMAIL_FROM": "sender@example.com",
                "DELIVERY_SENDING_TIMEOUT_MINUTES": "30",
                "DELIVERY_MAX_ATTEMPTS": "3",
            },
        )
        self.env.start()

    def tearDown(self) -> None:
        self.env.stop()

    def run_delivery(self, repo: FakeRepository) -> dict:
        return delivery.run_daily_delivery(repo, self.target_date)

    @property
    def delivery_id(self) -> str:
        return delivery.build_delivery_id(self.target_date)

    def test_sends_one_digest_to_normalized_recipient_group(self) -> None:
        repo = FakeRepository(
            [" First@Example.com ", "second@example.com", "FIRST@example.com"]
        )

        with patch.object(delivery, "send_email") as send:
            result = self.run_delivery(repo)

        self.assertEqual(1, result["sent"])
        self.assertEqual("SENT", result["status"])
        send.assert_called_once()
        self.assertEqual(
            "first@example.com, second@example.com",
            send.call_args.kwargs["email_to"],
        )
        self.assertEqual({self.delivery_id}, set(repo.deliveries))
        self.assertEqual(
            "first@example.com, second@example.com",
            repo.deliveries[self.delivery_id]["target_email"],
        )

    def test_repeat_run_skips_sent_digest(self) -> None:
        repo = FakeRepository()

        with patch.object(delivery, "send_email") as send:
            first = self.run_delivery(repo)
            second = self.run_delivery(repo)

        self.assertEqual(1, first["sent"])
        self.assertEqual("SENT", second["status"])
        self.assertEqual(1, second["skipped"])
        self.assertEqual(1, send.call_count)

    def test_parallel_fresh_sending_returns_in_progress(self) -> None:
        repo = FakeRepository()
        smtp_started = threading.Event()
        release_smtp = threading.Event()
        send_count = 0
        send_count_lock = threading.Lock()

        def blocked_send(**kwargs) -> None:
            nonlocal send_count
            _ = kwargs
            with send_count_lock:
                send_count += 1
            smtp_started.set()
            self.assertTrue(release_smtp.wait(timeout=5))

        with patch.object(delivery, "send_email", side_effect=blocked_send):
            with ThreadPoolExecutor(max_workers=1) as executor:
                first = executor.submit(self.run_delivery, repo)
                self.assertTrue(smtp_started.wait(timeout=5))
                try:
                    second = self.run_delivery(repo)
                    self.assertEqual("IN_PROGRESS", second["status"])
                finally:
                    release_smtp.set()
                self.assertEqual(1, first.result(timeout=5)["sent"])

        self.assertEqual(1, send_count)
        self.assertEqual("SENT", repo.deliveries[self.delivery_id]["status"])

    def test_expired_sending_is_retried(self) -> None:
        repo = FakeRepository()
        repo.deliveries[self.delivery_id] = {
            "status": "SENDING",
            "attempt_count": 1,
            "target_email": "owner@example.com",
            "delivery_date": self.target_date,
            "status_changed_at": repo.now - timedelta(minutes=31),
            "error_text": "",
        }

        with patch.object(delivery, "send_email") as send:
            result = self.run_delivery(repo)

        self.assertEqual("SENT", result["status"])
        self.assertEqual(2, result["attempt_count"])
        self.assertEqual(1, send.call_count)

    def test_failed_smtp_attempt_is_retried_automatically(self) -> None:
        repo = FakeRepository()

        with patch.object(
            delivery,
            "send_email",
            side_effect=[TimeoutError("SMTP timeout"), None],
        ) as send:
            with self.assertRaises(delivery.DeliveryBatchError):
                self.run_delivery(repo)
            result = self.run_delivery(repo)

        row = repo.deliveries[self.delivery_id]
        self.assertEqual("SENT", row["status"])
        self.assertEqual(2, row["attempt_count"])
        self.assertEqual(2, send.call_count)
        self.assertEqual(1, result["sent"])
        self.assertEqual(
            send.call_args_list[0].kwargs["message_id"],
            send.call_args_list[1].kwargs["message_id"],
        )

    def test_stops_after_three_smtp_failures_and_logs_address_hashes(self) -> None:
        addresses = ["private@example.com", "second@example.com"]
        repo = FakeRepository(addresses)
        smtp_error = TimeoutError("SMTP timeout for private@example.com")

        with patch.object(delivery, "send_email", side_effect=smtp_error) as send:
            for _ in range(2):
                with self.assertRaises(delivery.DeliveryBatchError):
                    self.run_delivery(repo)

            with self.assertLogs(delivery.LOG, level="ERROR") as logs:
                with self.assertRaises(delivery.DeliveryBatchError):
                    self.run_delivery(repo)

            fourth = self.run_delivery(repo)

        row = repo.deliveries[self.delivery_id]
        self.assertEqual("FAILED", row["status"])
        self.assertEqual(3, row["attempt_count"])
        self.assertEqual("FAILED", fourth["status"])
        self.assertEqual(3, send.call_count)

        events = [json.loads(record.getMessage()) for record in logs.records]
        self.assertEqual(
            {delivery.address_hash(address) for address in addresses},
            {event["address_hash"] for event in events},
        )
        for event in events:
            self.assertEqual("2026-07-13", event["delivery_date"])
            self.assertEqual(3, event["attempt_count"])
            self.assertEqual(
                "reminder_delivery_final_failure",
                event["event"],
            )
            self.assertNotIn("private@example.com", json.dumps(event))

    def test_stale_third_sending_attempt_becomes_final_failed(self) -> None:
        repo = FakeRepository()
        repo.deliveries[self.delivery_id] = {
            "status": "SENDING",
            "attempt_count": 3,
            "target_email": "owner@example.com",
            "delivery_date": self.target_date,
            "status_changed_at": repo.now - timedelta(minutes=31),
            "error_text": "",
        }

        with (
            patch.object(delivery, "send_email") as send,
            self.assertLogs(delivery.LOG, level="ERROR") as logs,
        ):
            result = self.run_delivery(repo)

        self.assertEqual("FAILED", result["status"])
        self.assertEqual("FAILED", repo.deliveries[self.delivery_id]["status"])
        send.assert_not_called()
        event = json.loads(logs.records[0].getMessage())
        self.assertEqual("SendingTimeout", event["error_type"])

    def test_sent_write_error_waits_then_retries_with_same_message_id(self) -> None:
        repo = FakeRepository()
        repo.mark_sent_error = RuntimeError("YDB unavailable")

        with patch.object(delivery, "send_email") as send:
            with self.assertRaises(delivery.DeliveryBatchError):
                self.run_delivery(repo)
            immediate = self.run_delivery(repo)
            repo.mark_sent_error = None
            repo.advance(31)
            retried = self.run_delivery(repo)

        self.assertEqual("IN_PROGRESS", immediate["status"])
        self.assertEqual("SENT", retried["status"])
        self.assertEqual(2, send.call_count)
        self.assertEqual(
            send.call_args_list[0].kwargs["message_id"],
            send.call_args_list[1].kwargs["message_id"],
        )

    def test_ydb_claim_error_fails_without_smtp(self) -> None:
        repo = FakeRepository()
        repo.claim_error = RuntimeError("YDB unavailable")

        with patch.object(delivery, "send_email") as send:
            with self.assertRaises(delivery.DeliveryBatchError) as error:
                self.run_delivery(repo)

        self.assertEqual("CLAIM_FAILED", error.exception.summary["status"])
        send.assert_not_called()

    def test_ydb_error_while_recording_smtp_failure_keeps_sending(self) -> None:
        repo = FakeRepository()
        repo.mark_failed_error = RuntimeError("YDB unavailable")

        with patch.object(
            delivery,
            "send_email",
            side_effect=RuntimeError("SMTP unavailable"),
        ) as send:
            with self.assertRaises(delivery.DeliveryBatchError):
                self.run_delivery(repo)
            immediate = self.run_delivery(repo)

        self.assertEqual("IN_PROGRESS", immediate["status"])
        self.assertEqual("SENDING", repo.deliveries[self.delivery_id]["status"])
        self.assertEqual(1, send.call_count)

    def test_handler_propagates_delivery_error(self) -> None:
        repo = FakeRepository()

        with (
            patch.object(delivery, "YdbDeliveryRepository", return_value=repo),
            patch.object(delivery, "send_email", side_effect=RuntimeError("SMTP down")),
            patch.object(delivery, "ZoneInfo", return_value=timezone.utc),
            patch.object(delivery, "datetime") as mocked_datetime,
        ):
            mocked_datetime.now.return_value = datetime(
                2026,
                7,
                13,
                tzinfo=timezone.utc,
            )
            with self.assertRaises(delivery.DeliveryBatchError):
                delivery.handler({}, object())


class MessageIdTest(unittest.TestCase):
    def test_smtp_message_contains_stable_message_id(self) -> None:
        messages = []

        class FakeSmtp:
            def __init__(self, **kwargs) -> None:
                self.kwargs = kwargs

            def __enter__(self):
                return self

            def __exit__(self, exc_type, exc_value, traceback) -> None:
                _ = exc_type, exc_value, traceback

            def login(self, username: str, password: str) -> None:
                _ = username, password

            def send_message(self, message) -> None:
                messages.append(message)

        message_id = "<digest.2026-07-13.stable@nocombro.local>"
        with patch.object(delivery.smtplib, "SMTP_SSL", FakeSmtp):
            delivery.send_email(
                smtp_host="smtp.example.com",
                smtp_port=465,
                smtp_user="sender@example.com",
                smtp_password="secret",
                smtp_use_ssl=True,
                email_from="sender@example.com",
                email_to="owner@example.com",
                subject="Digest",
                body="Body",
                message_id=message_id,
            )

        self.assertEqual(message_id, messages[0]["Message-ID"])


class FakeQueryResultContext:
    def __init__(self, rows: list[dict] | None = None) -> None:
        self.result_sets = [types.SimpleNamespace(rows=rows or [])]

    def __enter__(self):
        return self.result_sets

    def __exit__(self, exc_type, exc_value, traceback) -> None:
        _ = exc_type, exc_value, traceback


class FakeTransaction:
    def __init__(self, row: dict | None) -> None:
        self.row = row

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback) -> None:
        _ = exc_type, exc_value, traceback

    def execute(
        self,
        query: str,
        params: dict | None = None,
        commit_tx: bool = False,
    ) -> FakeQueryResultContext:
        _ = commit_tx
        normalized_query = " ".join(query.split())
        params = params or {}

        if "SELECT status, attempt_count, status_changed_at" in normalized_query:
            return FakeQueryResultContext([dict(self.row)] if self.row else [])

        if normalized_query.startswith("SELECT 1 AS committed"):
            return FakeQueryResultContext()

        if "UPSERT INTO" in normalized_query:
            self.row = {
                "status": "SENDING",
                "attempt_count": params["$attempt_count"],
                "status_changed_at": params["$status_changed_at"],
                "target_email": params["$target_email"],
            }
            return FakeQueryResultContext()

        if "SENDING timeout after maximum attempts" in normalized_query:
            self.row["status"] = "FAILED"
            self.row["status_changed_at"] = params["$status_changed_at"]
            return FakeQueryResultContext()

        if 'SET status = "SENT"' in normalized_query:
            if (
                self.row is not None
                and self.row["status"] == "SENDING"
                and self.row["attempt_count"] == params["$attempt_count"]
            ):
                self.row["status"] = "SENT"
                self.row["status_changed_at"] = params["$now"]
                return FakeQueryResultContext(
                    [{"delivery_id": params["$delivery_id"]}]
                )
            return FakeQueryResultContext()

        if 'SET status = "FAILED"' in normalized_query:
            if (
                self.row is not None
                and self.row["status"] == "SENDING"
                and self.row["attempt_count"] == params["$attempt_count"]
            ):
                self.row["status"] = "FAILED"
                self.row["status_changed_at"] = params["$status_changed_at"]
                self.row["error_text"] = params["$error_text"]
                return FakeQueryResultContext(
                    [{"delivery_id": params["$delivery_id"]}]
                )
            return FakeQueryResultContext()

        raise AssertionError(f"Unexpected query: {normalized_query}")


class FakeSession:
    def __init__(self, transaction: FakeTransaction) -> None:
        self.fake_transaction = transaction

    def transaction(self, mode) -> FakeTransaction:
        _ = mode
        return self.fake_transaction


class FakePool:
    def __init__(self, row: dict | None) -> None:
        self.transaction = FakeTransaction(row)

    def retry_operation_sync(self, operation):
        return operation(FakeSession(self.transaction))

    def execute_with_retries(
        self,
        query: str,
        params: dict,
    ) -> list[types.SimpleNamespace]:
        return self.transaction.execute(query, params).result_sets


class YdbClaimTest(unittest.TestCase):
    target_date = date(2026, 7, 13)
    now = datetime(2026, 7, 13, 8, 0, tzinfo=timezone.utc)

    def claim(self, row: dict | None) -> tuple[delivery.DeliveryClaim, FakePool]:
        pool = FakePool(row)
        repo = delivery.YdbDeliveryRepository()
        with (
            patch.object(delivery, "get_pool", return_value=pool),
            patch.object(delivery, "utc_now", return_value=self.now),
            patch.object(
                delivery.ydb,
                "QuerySerializableReadWrite",
                return_value=object(),
                create=True,
            ),
        ):
            result = repo.claim_delivery(
                delivery_id="digest:2026-07-13",
                target_email="owner@example.com",
                delivery_date=self.target_date,
                sending_timeout_minutes=30,
                max_attempts=3,
            )
        return result, pool

    def test_fresh_sending_is_in_progress(self) -> None:
        result, pool = self.claim(
            {
                "status": "SENDING",
                "attempt_count": 1,
                "status_changed_at": delivery.utc_text(
                    self.now - timedelta(minutes=29)
                ),
            }
        )

        self.assertEqual(delivery.ClaimResult.IN_PROGRESS, result.result)
        self.assertEqual(1, pool.transaction.row["attempt_count"])

    def test_stale_sending_is_claimed_again(self) -> None:
        result, pool = self.claim(
            {
                "status": "SENDING",
                "attempt_count": 1,
                "status_changed_at": delivery.utc_text(
                    self.now - timedelta(minutes=31)
                ),
            }
        )

        self.assertEqual(delivery.ClaimResult.CLAIMED, result.result)
        self.assertEqual(2, result.attempt_count)
        self.assertEqual(2, pool.transaction.row["attempt_count"])

    def test_failed_at_limit_is_not_claimed(self) -> None:
        result, pool = self.claim(
            {
                "status": "FAILED",
                "attempt_count": 3,
                "status_changed_at": delivery.utc_text(self.now),
            }
        )

        self.assertEqual(delivery.ClaimResult.FAILED, result.result)
        self.assertEqual(3, pool.transaction.row["attempt_count"])

    def test_stale_sending_at_limit_becomes_failed(self) -> None:
        result, pool = self.claim(
            {
                "status": "SENDING",
                "attempt_count": 3,
                "status_changed_at": delivery.utc_text(
                    self.now - timedelta(minutes=31)
                ),
            }
        )

        self.assertEqual(delivery.ClaimResult.FAILED, result.result)
        self.assertEqual("SendingTimeout", result.final_error_type)
        self.assertEqual("FAILED", pool.transaction.row["status"])

    def test_stale_worker_cannot_close_new_attempt(self) -> None:
        result, pool = self.claim(
            {
                "status": "SENDING",
                "attempt_count": 1,
                "status_changed_at": delivery.utc_text(
                    self.now - timedelta(minutes=31)
                ),
            }
        )
        self.assertEqual(2, result.attempt_count)
        repo = delivery.YdbDeliveryRepository()

        with (
            patch.object(delivery, "get_pool", return_value=pool),
            patch.object(delivery, "utc_now", return_value=self.now),
        ):
            with self.assertRaises(delivery.DeliveryClaimLostError):
                repo.mark_sent("digest:2026-07-13", attempt_count=1)
            with self.assertRaises(delivery.DeliveryClaimLostError):
                repo.mark_failed(
                    "digest:2026-07-13",
                    attempt_count=1,
                    error_text="old worker failed",
                )

            self.assertEqual("SENDING", pool.transaction.row["status"])
            self.assertEqual(2, pool.transaction.row["attempt_count"])
            repo.mark_sent("digest:2026-07-13", attempt_count=2)

        self.assertEqual("SENT", pool.transaction.row["status"])

    def test_no_op_terminal_update_is_reported(self) -> None:
        pool = FakePool(
            {
                "status": "SENT",
                "attempt_count": 2,
                "status_changed_at": delivery.utc_text(self.now),
            }
        )
        repo = delivery.YdbDeliveryRepository()

        with (
            patch.object(delivery, "get_pool", return_value=pool),
            patch.object(delivery, "utc_now", return_value=self.now),
            self.assertRaises(delivery.DeliveryClaimLostError),
        ):
            repo.mark_sent("digest:2026-07-13", attempt_count=2)

        self.assertEqual("SENT", pool.transaction.row["status"])


class YdbCredentialsTest(unittest.TestCase):
    def test_pool_uses_credentials_from_environment(self) -> None:
        credentials = object()
        pool = object()

        class FakeDriver:
            def __init__(self, **kwargs) -> None:
                self.kwargs = kwargs

            def wait(self, **kwargs) -> None:
                self.wait_kwargs = kwargs

        fake_ydb = types.SimpleNamespace(
            Driver=FakeDriver,
            QuerySessionPool=Mock(return_value=pool),
            credentials_from_env_variables=Mock(return_value=credentials),
        )

        original_driver = delivery._DRIVER
        original_pool = delivery._POOL
        delivery._DRIVER = None
        delivery._POOL = None
        try:
            with (
                patch.object(delivery, "ydb", fake_ydb),
                patch.dict(
                    os.environ,
                    {
                        "YDB_ENDPOINT": "endpoint",
                        "YDB_DATABASE": "database",
                        "YDB_METADATA_CREDENTIALS": "1",
                    },
                    clear=True,
                ),
            ):
                self.assertIs(pool, delivery.get_pool())
                fake_ydb.credentials_from_env_variables.assert_called_once_with()
                self.assertIs(credentials, delivery._DRIVER.kwargs["credentials"])
        finally:
            delivery._DRIVER = original_driver
            delivery._POOL = original_pool

    def test_pool_rejects_missing_credentials_before_driver_start(self) -> None:
        fake_ydb = types.SimpleNamespace(
            Driver=Mock(),
            QuerySessionPool=Mock(),
            credentials_from_env_variables=Mock(),
        )

        original_driver = delivery._DRIVER
        original_pool = delivery._POOL
        delivery._DRIVER = None
        delivery._POOL = None
        try:
            with (
                patch.object(delivery, "ydb", fake_ydb),
                patch.dict(
                    os.environ,
                    {"YDB_ENDPOINT": "endpoint", "YDB_DATABASE": "database"},
                    clear=True,
                ),
                self.assertRaisesRegex(
                    RuntimeError,
                    "YDB credentials are not configured",
                ),
            ):
                delivery.get_pool()

            fake_ydb.Driver.assert_not_called()
            fake_ydb.credentials_from_env_variables.assert_not_called()
        finally:
            delivery._DRIVER = original_driver
            delivery._POOL = original_pool


if __name__ == "__main__":
    unittest.main()
