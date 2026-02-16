package ru.pavlig43.product.internal.update.tabs.declaration

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

/**
 * Объект для матчинга фильтров в таблице деклараций продукта.
 *
 * Поддерживает фильтрацию по полям:
 * - [ProductDeclarationField.DECLARATION_NAME] - текстовый поиск по названию декларации
 * - [ProductDeclarationField.VENDOR_NAME] - текстовый поиск по названию поставщика
 * - [ProductDeclarationField.IS_ACTUAL] - фильтр по актуальности
 *
 * Колонки SELECTION и ID не поддерживают фильтрацию.
 */
internal object ProductDeclarationFilterMatcher : FilterMatcher<FlowProductDeclarationTableUi, ProductDeclarationField>() {

    /**
     * Проверяет, соответствует ли элемент фильтру для указанной колонки.
     *
     * @param item Элемент для проверки
     * @param column Колонка, по которой применяется фильтр
     * @param stateAny Состояние фильтра
     * @return true, если элемент соответствует фильтру
     */
    override fun matchesRules(
        item: FlowProductDeclarationTableUi,
        column: ProductDeclarationField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            ProductDeclarationField.SELECTION -> true
            ProductDeclarationField.ID -> true
            ProductDeclarationField.DECLARATION_NAME -> matchesTextField(item.declarationName, stateAny)
            ProductDeclarationField.VENDOR_NAME -> matchesTextField(item.vendorName, stateAny)
            ProductDeclarationField.IS_ACTUAL -> matchesBooleanField(item.isActual, stateAny)
        }
    }
}
