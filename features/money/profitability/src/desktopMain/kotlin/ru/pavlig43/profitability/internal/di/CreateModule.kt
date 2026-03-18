package ru.pavlig43.profitability.internal.di

import kotlinx.datetime.LocalDateTime
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.profitability.api.ProfitabilityDependencies
import ru.pavlig43.profitability.internal.model.ProfitabilityUi
import kotlinx.coroutines.flow.Flow

internal fun createModule(dependencies: ProfitabilityDependencies) = listOf(
    module {

    }
)
internal class ProfitabilityRepository(
    db: NocombroDatabase
){
    private val productDao = db.profitabilityDao

    fun observeOnAllProduct(start: LocalDateTime,end:LocalDateTime):Flow<List<ProfitabilityUi>>{
    }
}