package ru.pavlig43.tablecore.utils

import ua.wwind.table.state.SortState
/**
 * Интерфейс для сортировки элементов по заданным критериям.
 *
 * Позволяет реализовать различные стратегии сортировки для разных типов данных.
 * Использует обобщенные типы для гибкости применения к различным доменным моделям.
 *
 * ## Типовые параметры:
 * @param I Тип сортируемых элементов
 * @param C Тип критериев сортировки
 *
 * ## Пример реализации:
 * ```kotlin
 * class UserSorter : SortMatcher<User, UserSortField> {
 *     override fun sort(items: List<User>, sort: SortState<UserSortField>?): List<User> {
 *             if (sort == null) {
 *             return items
 *         }
 *         return when (sort.column) {
 *             UserSortField.NAME -> items.sortedBy { it.name }
 *             UserSortField.AGE -> items.sortedBy { it.age }
 *             null -> items
 *         }.let { if (sort?.ascending == false) it.reversed() else it }
 *     }
 *             return if (sort.order == SortOrder.DESCENDING) {
 *             sortedList.asReversed()
 *         } else {
 *             sortedList
 *         }
 * }
 * @see SortState Состояние сортировки (поле и направление)
 */
interface SortMatcher<I, C> {
    fun sort(items: List<I>, sort: SortState<C>?): List<I>
}