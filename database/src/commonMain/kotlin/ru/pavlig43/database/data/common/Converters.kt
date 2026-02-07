package ru.pavlig43.database.data.common

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.ProductUnit
import ru.pavlig43.database.data.transaction.MovementType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.database.data.transaction.expense.ExpenseTypeEnum
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("TooManyFunctions")
@OptIn(ExperimentalTime::class)
class Converters {

    @TypeConverter
    fun fromOwnerType(type: OwnerType): String {
        return type.name
    }
    @TypeConverter
    fun toOwnerType(value: String) = enumValueOf<OwnerType>(value)

    @TypeConverter
    fun fromProductType(productType: ProductType?): String {
        return productType?.enumValue?.name ?: ProductType.Pack.enumValue.name

    }

    @TypeConverter
    fun toProductType(value: String?): ProductType {
        if (value.isNullOrBlank()) {
            return ProductType.Pack // Значение по умолчанию для null/пустых значений
        }
        return ProductType.entries.firstOrNull { it.enumValue.name == value }
            ?: ProductType.Pack // Fallback на Pack если не найдено
    }

    @TypeConverter
    fun toDocumentType(value: String) = enumValueOf<DocumentType>(value)

    @TypeConverter
    fun fromDocumentType(value: DocumentType) = value.name

    @TypeConverter
    fun toProductUnit(value: String): ProductUnit = enumValueOf<ProductUnit>(value)

    @TypeConverter
    fun fromProductUnit(value: ProductUnit): String = value.name

    @TypeConverter
    fun toOperationType(value: String) = enumValueOf<MovementType>(value)

    @TypeConverter
    fun fromOperationType(value: MovementType) = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = enumValueOf<TransactionType>(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toExpenseTypeEnum(value: String): ExpenseTypeEnum = enumValueOf<ExpenseTypeEnum>(value)

    @TypeConverter
    fun fromExpenseTypeEnum(value: ExpenseTypeEnum): String = value.name


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
