package ru.pavlig43.money.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.money.DEFAULT_MONEY_ACCOUNT_SYNC_ID
import ru.pavlig43.database.data.money.MoneyAccount
import ru.pavlig43.database.data.money.MoneyAccountType
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.money.api.MoneyReportDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import ru.pavlig43.testkit.runOnUiThread

class MoneyReportComponentSmokeTest : DesktopMainDispatcherFunSpec({
    test("the hidden account is created automatically and accepts history from 2025") {
        withEmptyTestDatabase { db ->
            val now = getCurrentLocalDateTime()
            val past = LocalDateTime(2025, 1, 1, 0, 0)
            val lifecycle = LifecycleRegistry()
            val component = runOnUiThread {
                lifecycle.resume()
                MoneyReportComponent(
                    componentContext = DefaultComponentContext(lifecycle = lifecycle),
                    dependencies = MoneyReportDependencies(db),
                )
            }

            val account = db.moneyDao.observeAccounts().first { it.size == 1 }.single()
            account.syncId shouldBe DEFAULT_MONEY_ACCOUNT_SYNC_ID
            account.openedAt shouldBe LocalDateTime(2000, 1, 1, 0, 0)
            db.moneyDao.getAllMovements().size shouldBe 0

            val pastExpense = MoneyMovementInput(
                existing = null,
                kind = MoneyMovementKind.EXPENSE,
                category = MoneyMovementCategory.BUSINESS_EXPENSE,
                amount = 5_000,
                occurredAt = past,
                counterparty = "Поставщик",
                comment = "Расход 2025 года",
            )
            runOnUiThread {
                component.saveMovement(pastExpense)
                component.saveMovement(pastExpense)
            }
            db.moneyDao.observeMovementsUntil(now).first { it.size == 1 }
            component.actionInProgress.first { !it }

            runOnUiThread {
                component.saveMovement(
                    MoneyMovementInput(
                        existing = null,
                        kind = MoneyMovementKind.INCOME,
                        category = MoneyMovementCategory.SALE_PAYMENT,
                        amount = 20_000,
                        occurredAt = now,
                        counterparty = "Покупатель",
                        comment = "Оплата",
                    )
                )
            }

            db.moneyDao.observeMovementsUntil(now).first { it.size == 2 }
            val report = component.loadState.first {
                it is MoneyReportLoadState.Success && it.data.movementRows.size == 1
            } as MoneyReportLoadState.Success

            report.data.summary.openingBalance shouldBe -5_000L
            report.data.summary.received shouldBe 20_000L
            report.data.summary.closingBalance shouldBe 15_000L
            db.moneyDao.observeAccounts().first().size shouldBe 1
        }
    }

    test("an existing account is reused and allows earlier history") {
        withEmptyTestDatabase { db ->
            db.moneyDao.createAccount(
                MoneyAccount(
                    name = "Старый ручной счёт",
                    accountType = MoneyAccountType.BANK,
                    openedAt = LocalDateTime(2026, 7, 24, 0, 0),
                    syncId = "existing-money-account",
                )
            )
            val lifecycle = LifecycleRegistry()

            runOnUiThread {
                lifecycle.resume()
                MoneyReportComponent(
                    componentContext = DefaultComponentContext(lifecycle = lifecycle),
                    dependencies = MoneyReportDependencies(db),
                )
            }

            val account = db.moneyDao.observeAccounts().first {
                it.singleOrNull()?.openedAt == LocalDateTime(2000, 1, 1, 0, 0)
            }.single()
            account.syncId shouldBe "existing-money-account"
            db.moneyDao.getAllAccounts().size shouldBe 1
        }
    }
})
