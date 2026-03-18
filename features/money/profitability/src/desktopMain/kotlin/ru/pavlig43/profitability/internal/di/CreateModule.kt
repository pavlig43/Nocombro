package ru.pavlig43.profitability.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.money.profitability.ProductSalesBD
import ru.pavlig43.profitability.api.ProfitabilityDependencies
import ru.pavlig43.profitability.internal.model.ProfitabilityUi

internal fun createModule(dependencies: ProfitabilityDependencies) = listOf(
    module {
        single { dependencies }
        single { ProfitabilityRepository(get()) }
    }
)

internal class ProfitabilityRepository(
    dependencies: ProfitabilityDependencies
) {
    private val dao = dependencies.db.profitabilityDao

    fun observeOnProducts(start: LocalDateTime, end: LocalDateTime): Flow<Result<List<ProfitabilityUi>>> {
        return dao.observeProductSales(start, end)
            .map { sales ->
                Result.success(sales.map { it.toUi() })
            }
            .catch { emit(Result.failure(it)) }
    }
}

private fun ProductSalesBD.toUi(): ProfitabilityUi {
    val expenses = 0
    val expensesOnOneKg = if (quantity > 0) expenses * 1000 / quantity else 0
    val profit = revenue - expenses
    val margin = if (expenses > 0) (profit.toDouble() / expenses * 100) else 0.0
    val profitability = if (revenue > 0) (profit.toDouble() / revenue * 100) else 0.0

    return ProfitabilityUi(
        productId = productId,
        productName = productName,
        quantity = quantity,
        revenue = revenue,
        expenses = expenses,
        expensesOnOneKg = expensesOnOneKg,
        profit = profit,
        margin = margin,
        profitability = profitability
    )
}