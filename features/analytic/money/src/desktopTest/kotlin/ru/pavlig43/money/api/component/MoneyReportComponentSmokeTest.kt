package ru.pavlig43.money.api.component

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.money.api.MoneyReportDependencies
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.runOnUiThread

class MoneyReportComponentSmokeTest : DesktopMainDispatcherFunSpec({
    test("empty database stays empty without creating a second money source") {
        withEmptyTestDatabase { db ->
            val lifecycle = LifecycleRegistry()
            val component = runOnUiThread {
                lifecycle.resume()
                MoneyReportComponent(
                    componentContext = DefaultComponentContext(lifecycle = lifecycle),
                    dependencies = MoneyReportDependencies(db),
                )
            }

            val report = component.loadState.first { it is MoneyReportLoadState.Success }
                as MoneyReportLoadState.Success
            report.data.summary shouldBe ru.pavlig43.money.internal.model.MoneySummary()
            db.moneyDao.getAllAccounts().size shouldBe 0
            db.moneyDao.getAllMovements().size shouldBe 0
        }
    }

    test("seeded sales purchases and expenses are the report source") {
        withSeededTestDatabase { db ->
            val march = DTPeriod(
                LocalDateTime(2026, 3, 1, 0, 0),
                LocalDateTime(2026, 3, 31, 23, 59, 59),
            )
            val report = MoneyReportRepository(db).observe(march).first()
                as MoneyReportLoadState.Success

            report.data.summary.openingBalance shouldBe -476_500L
            report.data.summary.received shouldBe 7_920_000L
            report.data.summary.spent shouldBe 7_233_730L
            report.data.summary.netChange shouldBe 686_270L
            report.data.summary.closingBalance shouldBe 209_770L
            report.data.movementRows.shouldHaveSize(9)
            report.data.incomeByCategory.single().apply {
                category shouldBe MoneyMovementCategory.SALE_PAYMENT
                amount shouldBe 7_920_000L
            }
            report.data.expensesByCategory.map { it.category to it.amount } shouldBe listOf(
                MoneyMovementCategory.PURCHASE_PAYMENT to 7_108_500L,
                MoneyMovementCategory.BUSINESS_EXPENSE to 125_230L,
            )
            val sale = report.data.movementRows.single {
                it.entry.category == MoneyMovementCategory.SALE_PAYMENT
            }
            val linkedExpense = report.data.movementRows.single {
                it.entry.category == MoneyMovementCategory.BUSINESS_EXPENSE
            }
            linkedExpense.entry.operationGroupKey shouldBe sale.entry.operationGroupKey
            linkedExpense.entry.operationGroupLabel shouldBe "Продажа №11"
        }
    }
})
