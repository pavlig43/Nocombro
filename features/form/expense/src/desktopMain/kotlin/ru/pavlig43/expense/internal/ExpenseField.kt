package ru.pavlig43.expense.internal

/**
 * Поля для таблицы создания/редактирования расхода
 */
internal enum class ExpenseField {
    /** Тип расхода */
    EXPENSE_TYPE,
    /** Сумма в копейках */
    AMOUNT,
    /** Дата и время расхода */
    EXPENSE_DATE_TIME,
    /** Комментарий */
    COMMENT,
}
