package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.money.MoneyAccountType
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.database.data.sync.mirror.*
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import java.math.BigDecimal
import java.text.DecimalFormat

/** Типизированная проекция деловых полей. Не читает БД и не содержит HTML. */
internal class SyncAnalysisReportData(rows: Map<MirrorSyncTable, List<MirrorSyncRow>>) {
    val rows = rows.flatMap { (table, items) -> items.map { ReportKey(table, it.syncId) to it } }.toMap()

    fun fields(row: MirrorSyncRow): List<ReportField> = when (row) {
        is VendorMirrorRow -> listOf(f("Название", row.displayName), f("Комментарий", row.comment))
        is DocumentMirrorRow -> listOf(f("Название", row.displayName), f("Тип", row.type.displayName),
            f("Дата", row.createdAt), f("Комментарий", row.comment))
        is DeclarationMirrorRow -> listOf(f("Название", row.displayName), f("Дата создания", row.createdAt),
            ref("Поставщик", MirrorSyncTable.VENDOR, row.vendorSyncId), f("Дата выпуска", row.bornDate),
            f("Годен до", row.bestBefore), f("Отслеживать срок", row.observeFromNotification))
        is ProductMirrorRow -> listOf(f("Название", row.displayName), f("Второе название", row.secondName),
            f("Тип", row.type.displayName), f("Дата создания", row.createdAt), money("Цена продажи", row.priceForSale),
            f("Срок годности", "${row.shelfLifeDays} дн."), f("НДС", "${row.recNds} %"), f("Комментарий", row.comment))
        is CompositionMirrorRow -> listOf(ref("Компонент", MirrorSyncTable.PRODUCT, row.productSyncId), qty("Количество", row.count))
        is ProductSpecificationMirrorRow -> listOf(f("Дозировка", row.dosage), f("Состав", row.composition),
            f("Срок годности", row.shelfLifeText), f("Хранение", row.storageConditions), f("Внешний вид", row.appearance),
            f("Цвет", row.color), f("Запах", row.smell), f("Вкус", row.taste),
            f("Физико-химические показатели", row.physicalChemicalIndicators), f("Микробиология", row.microbiologicalIndicators),
            f("Токсичные элементы", row.toxicElements), f("Аллергены", row.allergens), f("ГМО", row.gmoInfo))
        is SafetyStockMirrorRow -> listOf(qty("Порог заказа", row.reorderPoint), qty("Объём заказа", row.orderQuantity))
        is ProductDeclarationMirrorRow -> declarationFields(row.declarationSyncId) + f("Товар включён в декларацию", row.isProductInDeclaration)
        is BatchMirrorRow -> listOf(ref("Товар", MirrorSyncTable.PRODUCT, row.productSyncId), f("Дата партии", row.dateBorn)) +
            declarationFields(row.declarationSyncId)
        is BatchCostPriceMirrorRow -> listOf(money("Себестоимость единицы", row.costPricePerUnit))
        is BatchMovementMirrorRow -> batchFields(row.batchSyncId) + listOf(f("Приход/расход", row.movementType.displayName),
            f("Склад", row.storageLocation.displayName), qty("Количество", row.count))
        is BuyMirrorRow -> listOf(money("Цена", row.price), f("НДС", "${row.ndsPercent} %"), f("Комментарий покупки", row.comment))
        is SaleMirrorRow -> listOf(ref("Клиент", MirrorSyncTable.VENDOR, row.clientSyncId), money("Цена", row.price),
            f("НДС", "${row.ndsPercent} %"), f("Комментарий продажи", row.comment))
        is TransactionMirrorRow -> listOf(f("Тип", row.transactionType.displayName), f("Дата и время", row.createdAt),
            f("Проведена", row.isCompleted), f("Причина", row.stockOperationReason?.displayName ?: "нет"), f("Комментарий", row.comment))
        is ReminderMirrorRow -> listOf(f("Текст", row.text), f("Дата и время", row.reminderDateTime))
        is ExpenseMirrorRow -> listOf(f("Вид расхода", row.expenseType.displayName), f("Дата и время", row.expenseDateTime),
            money("Сумма", row.amount), f("Комментарий", row.comment))
        is ExperimentMirrorRow -> listOf(f("Название", row.title), f("Идея", row.ideaDescription), f("В архиве", row.isArchived))
        is ExperimentEntryMirrorRow -> listOf(f("Дата записи", row.entryDate), f("Дата и время создания", row.createdAt), f("Текст", row.content))
        is ExperimentReminderMirrorRow -> listOf(f("Текст", row.text), f("Дата и время", row.reminderDateTime))
        is MoneyAccountMirrorRow -> listOf(f("Название", row.name), f("Тип", when (row.accountType) {
            MoneyAccountType.CASH -> "Наличные"; MoneyAccountType.BANK -> "Банковский счёт"
        }), f("Дата открытия", row.openedAt), f("В архиве", row.isArchived))
        is MoneyMovementMirrorRow -> listOf(f("Вид", when (row.kind) {
            MoneyMovementKind.OPENING_BALANCE -> "Начальный остаток"; MoneyMovementKind.INCOME -> "Приход"
            MoneyMovementKind.EXPENSE -> "Расход"; MoneyMovementKind.TRANSFER -> "Перевод"
        }), f("Категория", category(row.category)), f("Дата и время", row.occurredAt),
            ref("Откуда", MirrorSyncTable.MONEY_ACCOUNT, row.fromAccountSyncId),
            ref("Куда", MirrorSyncTable.MONEY_ACCOUNT, row.toAccountSyncId), money("Сумма", row.amount),
            f("Контрагент", row.counterparty), f("Комментарий", row.comment))
        is FileMirrorRow -> listOf(f("Название файла", row.displayName))
    }

    fun parent(row: MirrorSyncRow): ReportKey? = when (row) {
        is DeclarationMirrorRow -> ReportKey(MirrorSyncTable.VENDOR, row.vendorSyncId)
        is CompositionMirrorRow -> ReportKey(MirrorSyncTable.PRODUCT, row.parentSyncId)
        is ProductSpecificationMirrorRow -> ReportKey(MirrorSyncTable.PRODUCT, row.productSyncId)
        is SafetyStockMirrorRow -> ReportKey(MirrorSyncTable.PRODUCT, row.productSyncId)
        is ProductDeclarationMirrorRow -> ReportKey(MirrorSyncTable.PRODUCT, row.productSyncId)
        is BatchMirrorRow -> ReportKey(MirrorSyncTable.PRODUCT, row.productSyncId)
        is BatchCostPriceMirrorRow -> ReportKey(MirrorSyncTable.BATCH, row.batchSyncId)
        is BatchMovementMirrorRow -> ReportKey(MirrorSyncTable.TRANSACTION, row.transactionSyncId)
        is BuyMirrorRow -> ReportKey(MirrorSyncTable.BATCH_MOVEMENT, row.movementSyncId)
        is SaleMirrorRow -> ReportKey(MirrorSyncTable.BATCH_MOVEMENT, row.movementSyncId)
        is ReminderMirrorRow -> ReportKey(MirrorSyncTable.TRANSACTION, row.transactionSyncId)
        is ExpenseMirrorRow -> row.transactionSyncId?.let { ReportKey(MirrorSyncTable.TRANSACTION, it) }
        is ExperimentEntryMirrorRow -> ReportKey(MirrorSyncTable.EXPERIMENT, row.experimentSyncId)
        is ExperimentReminderMirrorRow -> ReportKey(MirrorSyncTable.EXPERIMENT, row.experimentSyncId)
        is FileMirrorRow -> ReportKey(when (row.ownerType) {
            OwnerType.DECLARATION -> MirrorSyncTable.DECLARATION; OwnerType.PRODUCT -> MirrorSyncTable.PRODUCT
            OwnerType.VENDOR -> MirrorSyncTable.VENDOR; OwnerType.DOCUMENT -> MirrorSyncTable.DOCUMENT
            OwnerType.TRANSACTION -> MirrorSyncTable.TRANSACTION; OwnerType.EXPENSE -> MirrorSyncTable.EXPENSE
            OwnerType.EXPERIMENT_ENTRY -> MirrorSyncTable.EXPERIMENT_ENTRY
        }, row.ownerSyncId)
        else -> null
    }

    fun label(key: ReportKey): String = rows[key]?.let { row -> when (row) {
        is VendorMirrorRow -> row.displayName
        is DocumentMirrorRow -> row.displayName
        is DeclarationMirrorRow -> row.displayName
        is ProductMirrorRow -> row.displayName
        is ExperimentMirrorRow -> row.title
        is MoneyAccountMirrorRow -> row.name
        is TransactionMirrorRow -> "${row.transactionType.displayName}, ${reportDateTime(row.createdAt)}"
        is BatchMirrorRow -> "${label(ReportKey(MirrorSyncTable.PRODUCT, row.productSyncId))}, ${row.dateBorn.format(dateFormat)}"
        is FileMirrorRow -> row.displayName
        else -> reportTableTitle(key.table)
    }.ifEmpty { "пусто" } } ?: "Связанная запись недоступна"

    private fun ref(title: String, table: MirrorSyncTable, id: String?): ReportField {
        val key = id?.let { ReportKey(table, it) }
        return ReportField(title, if (key == null) "нет" else label(key), key)
    }

    private fun declarationFields(id: String): List<ReportField> {
        val declaration = rows[ReportKey(MirrorSyncTable.DECLARATION, id)] as? DeclarationMirrorRow
        return listOf(ref("Декларация", MirrorSyncTable.DECLARATION, id), declaration?.let {
            ref("Поставщик", MirrorSyncTable.VENDOR, it.vendorSyncId)
        } ?: f("Поставщик", "Связанная запись недоступна"))
    }

    private fun batchFields(id: String): List<ReportField> {
        val batch = rows[ReportKey(MirrorSyncTable.BATCH, id)] as? BatchMirrorRow
        return if (batch != null) fields(batch) else listOf(
            f("Товар", "Связанная запись недоступна"), f("Дата партии", "Недоступно"),
            f("Декларация", "Связанная запись недоступна"), f("Поставщик", "Связанная запись недоступна"))
    }
}

internal data class ReportKey(val table: MirrorSyncTable, val id: String)
internal data class ReportField(
    val title: String,
    val text: String,
    val reference: ReportKey? = null,
    val value: Any = text,
)

private fun f(title: String, value: Any): ReportField = ReportField(title, when (value) {
    is Boolean -> if (value) "да" else "нет"
    is LocalDate -> value.format(dateFormat)
    is LocalDateTime -> reportDateTime(value)
    else -> value.toString().ifEmpty { "пусто" }
}, value = value)

private fun reportDateTime(value: LocalDateTime): String = value.format(dateTimeFormat) +
    if (value.second == 0 && value.nanosecond == 0) "" else ":${value.second.toString().padStart(2, '0')}" +
        if (value.nanosecond == 0) "" else ".${value.nanosecond.toString().padStart(9, '0').trimEnd('0')}"

/** Тот же шаблон DecimalData2/3, но BigDecimal сохраняет все разряды Long. */
private fun scaled(value: DecimalData): String = DecimalFormat("#,##0.###")
    .format(BigDecimal.valueOf(value.value, value.countDecimal))
private fun money(title: String, value: Long) = f(title, "${scaled(DecimalData2(value))} ₽")
private fun qty(title: String, value: Long) = f(title, "${scaled(DecimalData3(value))} кг")
private fun category(value: MoneyMovementCategory?): String = when (value) {
    null -> "нет"; MoneyMovementCategory.SALE_PAYMENT -> "Оплата продажи"; MoneyMovementCategory.PURCHASE_PAYMENT -> "Оплата покупки"
    MoneyMovementCategory.BUSINESS_EXPENSE -> "Расходы бизнеса"; MoneyMovementCategory.TAX -> "Налог"
    MoneyMovementCategory.DIVIDEND -> "Дивиденды"; MoneyMovementCategory.OWNER_DEPOSIT -> "Взнос владельца"
    MoneyMovementCategory.REFUND -> "Возврат"; MoneyMovementCategory.OTHER -> "Прочее"
}

internal fun reportTableTitle(table: MirrorSyncTable): String = when (table) {
    MirrorSyncTable.VENDOR -> "Поставщик / клиент"; MirrorSyncTable.DOCUMENT -> "Документ"
    MirrorSyncTable.DECLARATION -> "Декларация"; MirrorSyncTable.PRODUCT -> "Товар"
    MirrorSyncTable.TRANSACTION -> "Операция"; MirrorSyncTable.EXPERIMENT -> "Опыт"
    MirrorSyncTable.PRODUCT_SPECIFICATION -> "Спецификация"; MirrorSyncTable.SAFETY_STOCK -> "Запас товара"
    MirrorSyncTable.EXPERIMENT_ENTRY -> "Запись опыта"; MirrorSyncTable.EXPERIMENT_REMINDER -> "Напоминание об опыте"
    MirrorSyncTable.PRODUCT_DECLARATION -> "Товар в декларации"; MirrorSyncTable.COMPOSITION -> "Состав товара"
    MirrorSyncTable.BATCH -> "Партия"; MirrorSyncTable.BATCH_COST_PRICE -> "Себестоимость партии"
    MirrorSyncTable.BATCH_MOVEMENT -> "Движение партии"; MirrorSyncTable.REMINDER -> "Напоминание"
    MirrorSyncTable.EXPENSE -> "Расход"; MirrorSyncTable.BUY -> "Покупка"; MirrorSyncTable.SALE -> "Продажа"
    MirrorSyncTable.MONEY_ACCOUNT -> "Счёт"; MirrorSyncTable.MONEY_MOVEMENT -> "Движение денег"; MirrorSyncTable.FILE -> "Файл"
}
