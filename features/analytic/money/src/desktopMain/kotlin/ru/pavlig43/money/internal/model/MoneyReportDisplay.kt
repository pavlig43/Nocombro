package ru.pavlig43.money.internal.model

import ru.pavlig43.database.data.money.MoneyMovementCategory

internal fun MoneyReportEntryKind.displayName(): String = when (this) {
    MoneyReportEntryKind.INCOME -> "Приход"
    MoneyReportEntryKind.EXPENSE -> "Выплата"
}

internal fun MoneyMovementCategory.displayName(): String = when (this) {
    MoneyMovementCategory.SALE_PAYMENT -> "Оплата продажи"
    MoneyMovementCategory.PURCHASE_PAYMENT -> "Закупка"
    MoneyMovementCategory.BUSINESS_EXPENSE -> "Траты бизнеса"
    MoneyMovementCategory.TAX -> "Налог"
    MoneyMovementCategory.DIVIDEND -> "Дивиденды"
    MoneyMovementCategory.OWNER_DEPOSIT -> "Вклад владельца"
    MoneyMovementCategory.REFUND -> "Возврат"
    MoneyMovementCategory.OTHER -> "Прочее"
}
