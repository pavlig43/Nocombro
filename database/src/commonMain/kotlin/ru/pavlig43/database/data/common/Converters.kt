package ru.pavlig43.database.data.common

import androidx.room.TypeConverter
import kotlinx.datetime.*
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.ProductUnit
import ru.pavlig43.database.data.transaction.OperationType
import ru.pavlig43.database.data.transaction.TransactionType
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
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
    fun toProductUnit(value: String): ProductUnit = enumValueOf<ProductUnit>(value)

    @TypeConverter
    fun fromProductUnit(value: ProductUnit): String = value.name

    @TypeConverter
    fun toOperationType(value: String) = enumValueOf<OperationType>(value)

    @TypeConverter
    fun fromOperationType(value: OperationType) = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = enumValueOf<TransactionType>(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name


    @TypeConverter
    fun toLocalDate(value: Long): LocalDate = Instant.fromEpochMilliseconds(value)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    @TypeConverter
    fun fromLocalDate(value: LocalDate): Long {
        val startOfDay = value.atTime(LocalTime(0,0))
        return startOfDay.toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }
    @TypeConverter
    fun toLocalDateTime(value: Long): LocalDateTime =
        Instant.fromEpochMilliseconds(value)
            .toLocalDateTime(TimeZone.currentSystemDefault())

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): Long =
        value.toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

}
