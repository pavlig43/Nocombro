package ru.pavlig43.tablecore.ui

import androidx.compose.runtime.Composable
import ua.wwind.table.filter.data.TableFilterNode
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.strings.UiString

@Suppress("CyclomaticComplexMethod")
object RussianStringProvider : StringProvider {
    @Composable
    override fun get(key: UiString): String {
        return when (key) {
// Generic
            UiString.FilterClear -> "Очистить"
            UiString.FilterClearAll -> "Очистить всё"
            UiString.FilterApply -> "Применить"

// Placeholders
            UiString.FilterSearchPlaceholder -> "Поиск..."
            UiString.FilterEnterNumberPlaceholder -> "Введите число..."
            UiString.FilterSelectOnePlaceholder -> "Выберите одно"
            UiString.FilterSelectManyPlaceholder -> "Выберите несколько"
            UiString.FilterRangeFromPlaceholder -> "От"
            UiString.FilterRangeToPlaceholder -> "До"
            UiString.FilterRangeIconDescription -> "Диапазон"
            UiString.FilterOptionsSearchPlaceholder -> "Поиск вариантов…"
            UiString.FilterSelectAll -> "Выбрать всё"
            UiString.FilterSelectNone -> "Снять выбор"
            UiString.FilterConditionLabel -> "Условие"
            UiString.FilterValueLabel -> "Значение"
            UiString.FilterDateLabel -> "Дата"
            UiString.FilterErrorInvalidNumber -> "Введите число"
            UiString.FilterErrorRangeIncomplete -> "Введите оба значения"
            UiString.FilterErrorRangeInverted -> "Начало диапазона не должно быть больше конца"

// Date picker
            UiString.DatePickerSelectDate -> "Выбрать дату"
            UiString.DatePickerConfirm -> "Подтвердить"
            UiString.DatePickerCancel -> "Отмена"
            UiString.DatePickerClear -> "Очистить"

// Boolean
            UiString.BooleanTrueTitle -> "Да"
            UiString.BooleanFalseTitle -> "Нет"
            UiString.BooleanAnyTitle -> "Любое"

// Constraints
            UiString.FilterConstraintEquals -> "Равно"
            UiString.FilterConstraintNotEquals -> "Не равно"
            UiString.FilterConstraintBetween -> "Между"
            UiString.FilterConstraintContains -> "Содержит"
            UiString.FilterConstraintIn -> "В списке"
            UiString.FilterConstraintStartsWith -> "Начинается с"
            UiString.FilterConstraintEndsWith -> "Заканчивается на"
            UiString.FilterConstraintNotIn -> "Не в списке"
            UiString.FilterConstraintInHierarchy -> "В группе"
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
            UiString.FormatClearColor -> "Убрать цвет"
            UiString.FormatColorCancel -> "Отмена"
            UiString.FormatColorConfirm -> "ОК"
            UiString.FormatColorHex -> "Код цвета"
            UiString.FormatColorHexInvalid -> "Введите цвет в формате #RRGGBB или #AARRGGBB"
            UiString.FormatColorLowContrast -> "Низкий контраст текста и фона"
            UiString.FormatColorDeepBlue -> "Тёмно-синий"
            UiString.FormatColorSkyBlue -> "Голубой"
            UiString.FormatColorEmeraldGreen -> "Изумрудный"
            UiString.FormatColorLimeGreen -> "Салатовый"
            UiString.FormatColorSunsetOrange -> "Оранжевый"
            UiString.FormatColorCherryRed -> "Вишнёвый"
            UiString.FormatColorPurpleHaze -> "Сиреневый"
            UiString.FormatColorRosePink -> "Розовый"
            UiString.FormatColorSteelGray -> "Стальной"
            UiString.FormatColorGoldenAmber -> "Янтарный"
            UiString.FormatDeleteRuleTitle -> "Удалить это правило?"
            UiString.FormatDeleteRuleConfirm -> "Удалить"
            UiString.FormatDeleteRuleCancel -> "Отмена"
            UiString.FormatAddRule -> "Добавить правило"
            UiString.FormatClose -> "Закрыть"
            UiString.FormatRulesEmpty -> "Правил пока нет. Добавьте правило для выделения строк или ячеек."
            UiString.FormatRuleCancel -> "Отмена"
            UiString.FormatRuleSave -> "Сохранить"
            UiString.FormatRuleMoreActions -> "Другие действия"
            UiString.FormatRuleDuplicate -> "Создать копию"
            UiString.FormatRuleDelete -> "Удалить"
            UiString.FormatRuleReorder -> "Изменить порядок"
            UiString.FormatRuleMoveUp -> "Выше"
            UiString.FormatRuleMoveDown -> "Ниже"
            UiString.FormatRemoveCondition -> "Убрать фильтр"
            UiString.FormatExpandCondition -> "Развернуть"
            UiString.FormatCollapseCondition -> "Свернуть"
            UiString.FormatCustomFilterUnsupported -> "В правилах форматирования нельзя задать свой фильтр"

// Column headers and filters
            UiString.HeaderSort -> "Сортировать"
            UiString.HeaderSortedAscending -> "По возрастанию"
            UiString.HeaderSortedDescending -> "По убыванию"
            UiString.HeaderNotSorted -> "Без сортировки"
            UiString.HeaderFilter -> "Фильтр"
            UiString.HeaderFilterActive -> "Фильтр включён"
            UiString.HeaderDragColumn -> "Перетащить столбец"
            UiString.FilterChipsScrollLeft -> "Прокрутить влево"
            UiString.FilterChipsScrollRight -> "Прокрутить вправо"
            UiString.FilterOperatorAny -> "Любое"
            UiString.FilterOperatorAll -> "Все"
            UiString.FilterOperatorOfThese -> "из условий"
            UiString.FilterNot -> "НЕ"
            UiString.FilterOr -> "или"
            UiString.FilterAnd -> "и"
            UiString.FilterBack -> "Назад"
            UiString.FilterUnknownField -> "Неизвестное поле"
            UiString.FilterUnresolvedBlocked -> "Удалите неизвестные поля, чтобы применить фильтр"

// Grouping menu
            UiString.GroupBy -> "Группировать по"
            UiString.Ungroup -> "Разгруппировать"

// Column menu
            UiString.ColumnMenuSortAscending -> "По возрастанию"
            UiString.ColumnMenuSortDescending -> "По убыванию"
            UiString.ColumnMenuClearSort -> "Сбросить сортировку"
            UiString.ColumnMenuOpenFilter -> "Фильтр…"
            UiString.ColumnMenuClearFilter -> "Сбросить фильтр"
            UiString.ColumnMenuPinLeft -> "Закрепить слева"
            UiString.ColumnMenuPinRight -> "Закрепить справа"
            UiString.ColumnMenuUnpin -> "Открепить"
            UiString.ColumnMenuMoveLeft -> "Сдвинуть влево"
            UiString.ColumnMenuMoveRight -> "Сдвинуть вправо"
            UiString.ColumnMenuAutoFit -> "Подобрать ширину"
            UiString.ColumnMenuResetWidth -> "Сбросить ширину"
            UiString.ColumnMenuHide -> "Скрыть столбец"
            UiString.ColumnMenuShowHidden -> "Показать скрытые столбцы"
            UiString.ColumnMenuOptions -> "Настройки столбца"
            UiString.ColumnMenuReasonRowReorder -> "Недоступно при смене порядка строк"
            UiString.ColumnMenuReasonRowBlocks -> "Недоступно при показе блоков строк"
            UiString.ColumnMenuReasonFirst -> "Уже первый"
            UiString.ColumnMenuReasonLast -> "Уже последний"
            UiString.ColumnMenuReasonPinnedEdge -> "Уже у края закреплённой области"
            UiString.ColumnMenuReasonDefaultWidth -> "Ширина уже стандартная"
            UiString.ColumnMenuReasonNothingToFit -> "Нет данных для подбора ширины"
            UiString.ColumnMenuReasonLastVisible -> "Нельзя скрыть последний видимый столбец"
            UiString.ColumnMenuReasonLastUnpinned -> "Хотя бы один столбец должен остаться незакреплённым"

// Tooltip actions
            UiString.TooltipDismiss -> "Закрыть"
            UiString.EmptyNoData -> "Нет данных"
            UiString.EmptyNoResults -> "Нет строк по выбранным фильтрам"
            UiString.EmptyClearFilters -> "Сбросить фильтры"
            UiString.PagingLoading -> "Загрузка"
            UiString.PagingLoadError -> "Не удалось загрузить данные"
            UiString.PagingLoadMoreError -> "Не удалось загрузить часть строк"
            UiString.PagingRetry -> "Повторить"

        }
    }

    @Composable
    override fun removeFilterLabel(subject: String): String = "Убрать фильтр $subject"

    @Composable
    override fun unresolvedFilterDescription(title: String): String = "Неизвестное поле: $title"

    @Composable
    override fun removeFilterGroupLabel(columns: String): String = "Убрать группу $columns"

    @Composable
    override fun filterGroupDescription(
        operator: TableFilterNode.Operator,
        negated: Boolean,
        parts: List<String>,
    ): String {
        val sentence = when (operator) {
            TableFilterNode.Operator.OR -> "любое из условий: " + parts.joinToString(", или ")
            TableFilterNode.Operator.AND -> "все условия: " + parts.joinToString(" и ")
        }
        return if (negated) "Не ($sentence)" else sentence.replaceFirstChar(Char::uppercaseChar)
    }
}
