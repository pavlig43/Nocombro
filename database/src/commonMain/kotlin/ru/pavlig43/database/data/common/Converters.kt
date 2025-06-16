package ru.pavlig43.database.data.common

import androidx.room.TypeConverter
import ru.pavlig43.core.UTC
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType

class Converters {

    @TypeConverter
    fun toComponentType(value: String) = enumValueOf<ProductType>(value)

    @TypeConverter
    fun fromComponentType(value: ProductType) = value.name

    @TypeConverter
    fun toDocumentType(value: String) = enumValueOf<DocumentType>(value)

    @TypeConverter
    fun fromDocumentType(value: DocumentType) = value.name

    @TypeConverter
    fun toUTC(value: Long) = UTC(value)

    @TypeConverter
    fun fromUTC(value: UTC) = value.value

}