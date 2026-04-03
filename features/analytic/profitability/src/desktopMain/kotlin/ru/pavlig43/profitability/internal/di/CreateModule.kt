@file:Suppress("MagicNumber")
package ru.pavlig43.profitability.internal.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDateTime
import org.koin.dsl.module
import ru.pavlig43.core.mapParallel
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.profitability.api.ProfitabilityDependencies
import ru.pavlig43.profitability.internal.model.AllProfitability
import ru.pavlig43.profitability.internal.model.ExpenseByType
import ru.pavlig43.profitability.internal.model.ProfitabilityBatchDetails
import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import ru.pavlig43.profitability.internal.model.ProfitabilitySummary
import kotlin.math.roundToLong

internal fun createModule(dependencies: ProfitabilityDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single { ProfitabilityRepository(get()) }
    })

internal class ProfitabilityRepository(
    db: NocombroDatabase
) {
    private val dao = db.profitabilityDao
    private val expenseDao = db.expenseDao

@Suppress("LongMethod")
    fun observeOnProducts(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<Result<AllProfitability>> {
        return combine(
            expenseDao.observeMainExpense(start, end), dao.observeOnSale(start, end)
        ) { expenses, sales ->
            runCatching {
                // Группируем продажи по транзакциям для распределения расходов транзакции
                val quantityFromTransaction =
                    sales.groupBy { it.transaction.id }.mapValues { (_, sales) ->
                        sales.sumOf { it.movementOut.movement.count }
                    }

                val products =
                    sales.groupBy { it.movementOut.batchOut.product.id }.values.mapParallel(
                        Dispatchers.IO
                    ) { sales ->
                        val product = sales.first().movementOut.batchOut.product
                        val productName = product.displayName
                        val productId = product.id
                        var quantity = 0L
                        var allRevenue = 0L
                        var productExpenses = 0L

                        val details = sales.map { sale ->
                            val expensesOnSale = sale.expenses.sumOf { it.amount }
                            val saleQuantity = sale.movementOut.movement.count
                            // Расходы транзакции распределяются по кг внутри транзакции
                            val expenseOnSaleOnAllPositionInSale =
                                if ((quantityFromTransaction[sale.transaction.id] ?: 0) != 0L) {
                                    ((expensesOnSale.toDouble() * 1000) / (quantityFromTransaction[sale.transaction.id]!!))
                                } else 0.0
                            val expenseOnOneRowInSale = (expenseOnSaleOnAllPositionInSale * saleQuantity) / 1000

                            val batchesCost = sale.movementOut.batchOut.costPrice
                            val costPrice =
                                (batchesCost?.costPricePerUnit ?: 0) * saleQuantity / 1000

                            val itemExpenses =
                                (costPrice + expenseOnOneRowInSale).roundToLong()

                            val revenue =
                                (sale.sale.price * (saleQuantity.toDouble() / 1000)).roundToLong()
                            val profit = revenue - itemExpenses
                            val expensesOnOneKg =
                                (itemExpenses * 1000 / saleQuantity.toDouble()).roundToLong()
                            val margin = profit.toDouble() / itemExpenses * 100
                            val profitability = profit.toDouble() / revenue * 100

                            ProfitabilityBatchDetails(
                                contrAgentId = sale.client.id,
                                contrAgentName = sale.client.displayName,
                                date = sale.transaction.createdAt.date,
                                quantity = saleQuantity.let {
                                    quantity += it
                                    DecimalData3(it)
                                },
                                revenue = revenue.let {
                                    allRevenue += it
                                    DecimalData2(it)
                                },
                                expenses = itemExpenses.let {
                                    productExpenses += it
                                    DecimalData2(it)
                                },
                                expensesOnOneKg = DecimalData2(expensesOnOneKg),
                                profit = profit.let { DecimalData2(it) },
                                margin = margin,
                                profitability = profitability
                            )
                        }
                        val profit = allRevenue - productExpenses
                        val expensesOnOneKg = if (quantity != 0L) {
                            (productExpenses.toDouble() * 1000 / quantity).roundToLong()
                        } else 0L
                        val margin = if (productExpenses != 0L) {
                            profit.toDouble() / productExpenses * 100
                        } else 0.0
                        val profitability = if (allRevenue != 0L) {
                            profit.toDouble() / allRevenue * 100
                        } else 0.0

                        ProfitabilityProduct(
                            productId = productId,
                            productName = productName,
                            quantity = DecimalData3(quantity),
                            revenue = DecimalData2(allRevenue),
                            totalExpenses = DecimalData2(productExpenses),
                            expensesOnOneKg = DecimalData2(expensesOnOneKg),
                            profit = DecimalData2(profit),
                            margin = margin,
                            profitability = profitability,
                            details = details
                        )
                    }

                val totalRevenue = products.sumOf { it.revenue.value }
                val batchExpenses = products.sumOf { it.totalExpenses.value }
                val totalMainExpenses = expenses.sumOf { it.amount }
                val profit = totalRevenue - batchExpenses - totalMainExpenses
                val mainExpensesByType = expenses
                    .groupBy { it.expenseType }
                    .map { (type, list) ->
                        ExpenseByType(type, DecimalData2(list.sumOf { it.amount }))
                    }

                val summary = ProfitabilitySummary(
                    totalRevenue = DecimalData2(totalRevenue),
                    batchExpenses = DecimalData2(batchExpenses),
                    mainExpenses = DecimalData2(totalMainExpenses),
                    profit = DecimalData2(profit),
                    mainExpensesByType = mainExpensesByType
                )
                AllProfitability(summary = summary, products = products)
            }


        }
    }
}



