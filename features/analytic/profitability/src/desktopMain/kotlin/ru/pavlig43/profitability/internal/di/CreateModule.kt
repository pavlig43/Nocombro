package ru.pavlig43.profitability.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.analytic.profitability.ProfitabilityBD
import ru.pavlig43.profitability.api.ProfitabilityDependencies

internal fun createModule(dependencies: ProfitabilityDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single { ProfitabilityRepository(get()) }
    }
)

internal class ProfitabilityRepository(
    db: NocombroDatabase
) {
    private val dao = db.profitabilityDao

    fun observeOnProducts(start: LocalDateTime, end: LocalDateTime): Flow<Result<List<ProfitabilityBD>>> {
        return dao.observeOnProductSale(start, end)
            .map { sales ->
                Result.success(sales)
            }
            .catch { emit(Result.failure(it)) }
    }
}

