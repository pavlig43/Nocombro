package ru.pavlig43.database.data.common

import androidx.room.TypeConverter
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.vendor.VendorType

class Converters {

    @TypeConverter
    fun toProductType(value: String) = enumValueOf<ProductType>(value)

    @TypeConverter
    fun fromProductType(value: ProductType) = value.name

    @TypeConverter
    fun toDocumentType(value: String) = enumValueOf<DocumentType>(value)

    @TypeConverter
    fun fromDocumentType(value: DocumentType) = value.name

    @TypeConverter
    fun toVendorType(value: String) = enumValueOf<VendorType>(value)

    @TypeConverter
    fun fromVendorType(value: VendorType) = value.name

}