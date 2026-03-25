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
import ru.pavlig43.profitability.internal.model.ProfitabilityBatchDetails
import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import kotlin.Int
import kotlin.math.roundToInt

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


    fun observeOnProducts(start: LocalDateTime, end: LocalDateTime): Flow<Result<AllProfitability>> {
        return combine(
            expenseDao.observeMainExpense(start, end), dao.observeOnSale(start, end)
        ) { expenses, sales ->
            runCatching {
                val products =
                    sales.groupBy { it.movementOut.batchOut.product.id }.values.mapParallel(Dispatchers.IO) { sales ->
                            val product = sales.first().movementOut.batchOut.product
                            val productName = product.displayName
                            val productId = product.id
                            var quantity = 0
                            var allRevenue = 0
                            var productExpenses = 0
                            var totalProfit = 0

                            val details = sales.map { sale ->
                                val expensesOnSale = sale.expenses.sumOf { it.amount } // кп
                                val batchesCost = sale.movementOut.batchOut.costPrice
                                val saleQuantity = sale.movementOut.movement.count

                                val costPrice = (batchesCost?.costPricePerUnit ?: 0) * saleQuantity / 1000

                                // Итого расходы( стоимость партий + расходы на продажу)
                                val expenses = costPrice + expensesOnSale

                                val revenue = (sale.sale.price * (saleQuantity.toDouble()/1000)).roundToInt()
                                println("saleQuantity: $saleQuantity")
                                println("price ${sale.sale.price}")
                                println("revenue: $revenue")
                                val profit = revenue - expenses
                                val expensesOnOneKg = (expenses * 1000 / saleQuantity.toDouble())
                                val margin = profit.toDouble() / expenses * 100
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
                                    expenses = expenses.let {
                                        productExpenses += it
                                        DecimalData2(it)
                                    },
                                    expensesOnOneKg = expensesOnOneKg,
                                    profit = profit.let {
                                        totalProfit += it
                                        DecimalData2(it)
                                    },
                                    margin = margin,
                                    profitability = profitability
                                )
                            }
                            ProfitabilityProduct(
                                productId = productId,
                                productName = productName,
                                quantity = DecimalData3(quantity),
                                revenue = DecimalData2(allRevenue),
                                totalExpenses = DecimalData2(productExpenses),
                                expensesOnOneKg = 0.0,
                                profit = DecimalData2(totalProfit),
                                margin = 0.0,
                                profitability = 0.0,
                                details = details
                            )

                        }
                val totalMainExpenses: Int = expenses.sumOf { it.amount }
                val totalQuantity = products.sumOf { it.quantity.value }
                val productsWithMainExpenses = products.map { product ->
                    // Распределяем общие расходы пропорционально quantity
                    val mainExpenseShare =  (totalMainExpenses * product.quantity.value) / totalQuantity

                    // Итого расходы с учётом общих
                    val totalExpenses = product.totalExpenses.value + mainExpenseShare

                    // Пересчитываем показатели
                    val profit = product.revenue.value - totalExpenses
                    val expensesOnOneKg = (totalExpenses.toDouble() * 1000) / product.quantity.value
                    val margin = (profit.toDouble() / totalExpenses * 100)
                    val profitability = (profit.toDouble() / product.revenue.value * 100)

                    product.copy(
                        totalExpenses = DecimalData2(totalExpenses),
                        expensesOnOneKg = expensesOnOneKg,
                        profit = DecimalData2(profit),
                        margin = margin,
                        profitability = profitability
                    )
                }
                AllProfitability(
                    mainExpenses = totalMainExpenses, products = productsWithMainExpenses
                )
            }


        }
    }
}



