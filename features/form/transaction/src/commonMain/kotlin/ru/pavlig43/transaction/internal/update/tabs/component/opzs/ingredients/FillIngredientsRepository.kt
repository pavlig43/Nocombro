package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transact.ingredient.IngredientBD

internal class FillIngredientsRepository(
    db: NocombroDatabase
) {
    private val compositionDao = db.compositionDao

    suspend fun getIngredientsFromComposition(productId: Int,transactionId: Int,countPf: Int): Result<List<IngredientBD>> {
        return runCatching {
            println("countpf $countPf")
            compositionDao.getIngredientsFromComposition(productId,transactionId,countPf)
        }
    }

}