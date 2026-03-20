package ru.pavlig43.profitability.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.profitability.api.ProfitabilityDependencies
import ru.pavlig43.profitability.internal.model.ProfitabilityUi

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

    fun observeOnProducts(start: LocalDateTime, end: LocalDateTime): Flow<Result<List<ProfitabilityUi>>> {
        return flowOf(Result.success(emptyList()))
//        return dao.observeProductSales(start, end)
//            .map { sales ->
//                Result.success(sales.map { it.toUi() })
//            }
//            .catch { emit(Result.failure(it)) }
    }
}

