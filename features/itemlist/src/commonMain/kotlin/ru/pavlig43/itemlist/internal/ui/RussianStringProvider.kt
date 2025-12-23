package ru.pavlig43.itemlist.internal.ui

import androidx.compose.runtime.Composable
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

object RussianStringProvider : StringProvider {
    @Composable
    override fun get(key: UiString): String {
        return when (key) {
// Generic
            UiString.FilterClear -> "Очистить"
            UiString.FilterApply -> "Применить"

// Placeholders
            UiString.FilterSearchPlaceholder -> "Поиск..."
            UiString.FilterEnterNumberPlaceholder -> "Введите число..."
            UiString.FilterSelectOnePlaceholder -> "Выберите одно"
            UiString.FilterSelectManyPlaceholder -> "Выберите несколько"
            UiString.FilterRangeFromPlaceholder -> "От"
            UiString.FilterRangeToPlaceholder -> "До"
            UiString.FilterRangeIconDescription -> "Диапазон"

// Date picker
            UiString.DatePickerSelectDate -> "Выбрать дату"
            UiString.DatePickerConfirm -> "Подтвердить"
            UiString.DatePickerCancel -> "Отмена"
            UiString.DatePickerClear -> "Очистить"

// Boolean
            UiString.BooleanTrueTitle -> "Да"
            UiString.BooleanFalseTitle -> "Нет"

// Constraints
            UiString.FilterConstraintEquals -> "Равно"
            UiString.FilterConstraintNotEquals -> "Не равно"
            UiString.FilterConstraintBetween -> "Между"
            UiString.FilterConstraintContains -> "Содержит"
            UiString.FilterConstraintIn -> "В списке"
            UiString.FilterConstraintStartsWith -> "Начинается с"
            UiString.FilterConstraintEndsWith -> "Заканчивается на"
            UiString.FilterConstraintNotIn -> "Не в списке"
            UiString.FilterConstraintGt -> "Больше"
            UiString.FilterConstraintGte -> "Больше или равно"
            UiString.FilterConstraintLt -> "Меньше"
            UiString.FilterConstraintLte -> "Меньше или равно"
            UiString.FilterConstraintIsNull -> "Пустое значение"
            UiString.FilterConstraintIsNotNull -> "Не пустое значение"

// Format
            UiString.FormatRules -> "Правила форматирования"
            UiString.FormatDesignTab -> "Дизайн"
            UiString.FormatConditionTab -> "Условие"
            UiString.FormatFieldTab -> "Поля для форматирования"
            UiString.FormatVerticalAlignmentTop -> "Сверху"
            UiString.FormatVerticalAlignmentCenter -> "По центру"
            UiString.FormatVerticalAlignmentBottom -> "Снизу"
            UiString.FormatHorizontalAlignmentStart -> "Слева"
            UiString.FormatHorizontalAlignmentCenter -> "По центру"
            UiString.FormatHorizontalAlignmentEnd -> "Справа"
            UiString.FormatLabelVerticalAlignment -> "Вертикальное выравнивание"
            UiString.FormatLabelHorizontalAlignment -> "Горизонтальное выравнивание"
            UiString.FormatLabelTypography -> "Стиль текста"
            UiString.FormatTextStyleNormal -> "Обычный"
            UiString.FormatTextStyleItalic -> "Курсив"
            UiString.FormatTextStyleBold -> "Жирный"
            UiString.FormatTextStyleUnderline -> "Подчёркнутый"
            UiString.FormatTextStyleStrikethrough -> "Зачёркнутый"
            UiString.FormatContentColor -> "Цвет содержимого"
            UiString.FormatBackgroundColor -> "Цвет фона"
            UiString.FormatChooseColor -> "Выбрать цвет"
            UiString.FormatResetColor -> "Сбросить цвет"
            UiString.FormatAlwaysApply -> "Всегда применять"

// Grouping menu
            UiString.GroupBy -> "Группировать по"
            UiString.Ungroup -> "Разгруппировать"

// Tooltip actions
            UiString.TooltipDismiss -> "Закрыть"


        }
    }
}