package ru.pavlig43.database.data.product

import androidx.room.Embedded
import ru.pavlig43.core.data.Item

data class ProductWithGuts(
    @Embedded
    val product: Product,



): Item by product


