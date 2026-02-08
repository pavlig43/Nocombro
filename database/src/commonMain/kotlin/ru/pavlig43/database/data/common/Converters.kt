package ru.pavlig43.database.data.common

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.ProductUnit
import ru.pavlig43.database.data.transaction.MovementType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.database.data.transaction.expense.ExpenseType
import kotlin.time.ExperimentalTime

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
    fun fromProductType(value: ProductType): String = value.name

    @TypeConverter
    fun toProductType(value: String): ProductType  = enumValueOf<ProductType>(value)

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
    fun toExpenseTypeEnum(value: String): ExpenseType = enumValueOf<ExpenseType>(value)

    @TypeConverter
    fun fromExpenseTypeEnum(value: ExpenseType): String = value.name


    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String  = value.toString()
    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = value.toString()

}
