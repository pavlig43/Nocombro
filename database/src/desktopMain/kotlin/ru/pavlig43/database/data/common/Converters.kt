package ru.pavlig43.database.data.common

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.money.MoneyAccountType
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.ProductUnit
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.StockOperationReason
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
    fun toStorageLocation(value: String): StorageLocation = enumValueOf<StorageLocation>(value)

    @TypeConverter
    fun fromStorageLocation(value: StorageLocation): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = enumValueOf<TransactionType>(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name


    @TypeConverter
    fun toStockOperationReason(value: String?): StockOperationReason? =
        value?.let { enumValueOf<StockOperationReason>(it) }

    @TypeConverter
    fun fromStockOperationReason(value: StockOperationReason?): String? = value?.name
    @TypeConverter
    fun toExpenseTypeEnum(value: String): ExpenseType = enumValueOf<ExpenseType>(value)

    @TypeConverter
    fun fromExpenseTypeEnum(value: ExpenseType): String = value.name

    @TypeConverter
    fun toMoneyAccountType(value: String): MoneyAccountType = enumValueOf<MoneyAccountType>(value)

    @TypeConverter
    fun fromMoneyAccountType(value: MoneyAccountType): String = value.name

    @TypeConverter
    fun toMoneyMovementKind(value: String): MoneyMovementKind = enumValueOf<MoneyMovementKind>(value)

    @TypeConverter
    fun fromMoneyMovementKind(value: MoneyMovementKind): String = value.name

    @TypeConverter
    fun toMoneyMovementCategory(value: String?): MoneyMovementCategory? =
        value?.let { enumValueOf<MoneyMovementCategory>(it) }

    @TypeConverter
    fun fromMoneyMovementCategory(value: MoneyMovementCategory?): String? = value?.name

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String  = value.toString()
    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = value.toString()

}
