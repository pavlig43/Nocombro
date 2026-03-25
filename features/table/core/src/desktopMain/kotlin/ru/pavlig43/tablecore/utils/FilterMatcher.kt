package ru.pavlig43.tablecore.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.core.model.toStartDoubleFormat
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.NumberFilterDelegate


@Suppress("UNCHECKED_CAST")
/**
 * @param I сам объект (какой-нибудь ITableUi)
 * @param C sealed который содержит названия колонок
 */
abstract class FilterMatcher<I, C> {


    protected abstract fun matchesRules(item:I,column:C,stateAny:  TableFilterState<*>): Boolean


    /**
     * @param item какой-то объект на вход
     * @param filters
     * мапа <Колонка, САМ фильтр(в котором ограничение [FilterConstraint] и значения(строка или пара чисел))>
     * в зависимости от [FilterConstraint] идет сравнение со значениями в мапе
     * Если объект прошел все фильтр, то возвращается true
     */
    @Suppress("ComplexCondition")
    fun matchesItem(
        item: I,
        filters: Map<C, TableFilterState<*>>,
    ): Boolean {
        for ((column, stateAny) in filters) {
            /**
             * Если constraint == null(не знаю как такое возможно) или
             * (список значений null, но при этом ограничения не проверяют на нулабельность
             * - значений быть не может, ограничение закладывает с constraint),
             * то эта колонка с фильтрами пропускается.
             *
             */
            if (stateAny.constraint == null ||
                (
                        stateAny.values == null &&
                                stateAny.constraint != FilterConstraint.IS_NULL &&
                                stateAny.constraint != FilterConstraint.IS_NOT_NULL
                        )
            ) {
                continue
            }

            val matches = matchesRules(item,column,stateAny)

            if (!matches) return false
        }
        return true
    }
    protected fun matchesTextField(
        value: String,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<String>
        val query = st.values?.firstOrNull().orEmpty()
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
            else -> true
        }
    }

@Suppress("CyclomaticComplexMethod")
    protected fun matchesIntField(
        value: Int,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<Int>
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.EQUALS -> value == (st.values?.getOrNull(0) ?: value)
            FilterConstraint.NOT_EQUALS -> value != (st.values?.getOrNull(0) ?: value)
            FilterConstraint.BETWEEN -> {
                val from = st.values?.getOrNull(0) ?: value
                val to = st.values?.getOrNull(1) ?: value
                value in from..to
            }

            else -> true
        }
    }
    @Suppress("CyclomaticComplexMethod")
    protected fun matchesLongField(
        value: Long,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<Long>
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.EQUALS -> value == (st.values?.getOrNull(0) ?: value)
            FilterConstraint.NOT_EQUALS -> value != (st.values?.getOrNull(0) ?: value)
            FilterConstraint.BETWEEN -> {
                val from = st.values?.getOrNull(0) ?: value
                val to = st.values?.getOrNull(1) ?: value
                value in from..to
            }

            else -> true
        }
    }


    fun <I>matchesTypeField(value: I, state: TableFilterState<*>,): Boolean {
        val constraint = state.constraint ?: return true

        @Suppress("UNCHECKED_CAST")
        val selectedValues = (state.values as? List<I>) ?: emptyList()

        return when (constraint) {
            FilterConstraint.IN -> selectedValues.isEmpty() || selectedValues.contains(value)
            FilterConstraint.NOT_IN -> selectedValues.isEmpty() || !selectedValues.contains(value)
            FilterConstraint.EQUALS -> selectedValues.firstOrNull() == value
            FilterConstraint.NOT_EQUALS -> selectedValues.firstOrNull() != value
            else -> true
        }
    }
    protected fun matchesBooleanField(
        value: Boolean,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<Boolean>
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.EQUALS -> st.values?.firstOrNull()?.let { v -> value == v } ?: true
            FilterConstraint.NOT_EQUALS -> st.values?.firstOrNull()?.let { v -> value != v } ?: true
            else -> true
        }
    }


    private fun <T> matchesComparableField(
        value: T,
        state: TableFilterState<*>,
    ): Boolean where T : Comparable<T> {
        @Suppress("UNCHECKED_CAST")
        val st = state as TableFilterState<T>
        val constraint = st.constraint ?: return true

        val v0 = st.values?.getOrNull(0) ?: value
        val v1 = st.values?.getOrNull(1) ?: value

        return when (constraint) {
            FilterConstraint.GT         -> value >  v0
            FilterConstraint.GTE        -> value >= v0
            FilterConstraint.LT         -> value <  v0
            FilterConstraint.LTE        -> value <= v0
            FilterConstraint.EQUALS     -> value == v0
            FilterConstraint.NOT_EQUALS -> value != v0
            FilterConstraint.BETWEEN    -> value in v0..v1
            else                        -> true
        }
    }
    protected fun matchesDateField(
        value: LocalDate,
        state: TableFilterState<*>,
    ): Boolean = matchesComparableField(value, state)

    protected fun matchesDateTimeField(
        value: LocalDateTime,
        state: TableFilterState<*>,
    ): Boolean = matchesComparableField(value, state)

    protected fun matchesDecimalField(
        value: DecimalData,
        state: TableFilterState<*>,
    ): Boolean = matchesComparableField(value, state)

    protected fun matchesDoubleField(
        value: Double,
        state: TableFilterState<*>,
    ): Boolean = matchesComparableField(value, state)




}

@Suppress("MagicNumber")
object DataDecimalDelegate2:NumberFilterDelegate<DecimalData2> {
    override val regex: Regex
        get() = Regex("^-?\\d*(\\.\\d{0,2})?$")
    override val default: DecimalData2
        get() = DecimalData2(0)


    override fun parse(input: String): DecimalData2? {
        val doubleValue = input.toDoubleOrNull() ?: return null
        val longValue = (doubleValue * 100).toLong()
        return DecimalData2(longValue)
    }

    override fun format(value: DecimalData2): String = value.toStartDoubleFormat()

    override fun toSliderValue(value: DecimalData2): Float = (value.value / 100.0f)

    override fun fromSliderValue(value: Float): DecimalData2 = DecimalData2((value * 100).toLong())

    override fun compare(
        a: DecimalData2,
        b: DecimalData2
    ): Boolean {
        return a.value <= b.value
    }

}
@Suppress("MagicNumber")
object DataDecimalDelegate3:NumberFilterDelegate<DecimalData3> {
    override val regex: Regex
        get() = Regex("^-?\\d*(\\.\\d{0,3})?$")
    override val default: DecimalData3
        get() = DecimalData3(0)

    override fun parse(input: String): DecimalData3? {
        val doubleValue = input.toDoubleOrNull() ?: return null
        val longValue = (doubleValue * 1000).toLong()
        return DecimalData3(longValue)
    }

    override fun format(value: DecimalData3): String = value.toStartDoubleFormat()

    override fun toSliderValue(value: DecimalData3): Float = (value.value / 1000.0f)

    override fun fromSliderValue(value: Float): DecimalData3 = DecimalData3((value * 1000).toLong())

    override fun compare(
        a: DecimalData3,
        b: DecimalData3
    ): Boolean {
        return a.value <= b.value
    }

}
//externalState?.values?.firstOrNull()?.let { filter.delegate.format(it) } ?: ""



