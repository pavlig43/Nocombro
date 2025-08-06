package ru.pavlig43.database.data.product

import androidx.room.Embedded
import androidx.room.Relation
import ru.pavlig43.core.data.Item
import ru.pavlig43.database.data.document.declaration.Declaration
import ru.pavlig43.database.data.document.declaration.DeclarationWithDocuments

data class ProductWithGuts(
    @Embedded
    val product: Product,
    @Relation(
        entity = Declaration::class,
        parentColumn = "id",
        entityColumn = "product_id"
    )
    val declarations:List<DeclarationWithDocuments>

): Item by product


