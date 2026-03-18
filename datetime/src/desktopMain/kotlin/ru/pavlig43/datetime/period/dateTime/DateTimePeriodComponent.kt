package ru.pavlig43.datetime.period.dateTime

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.serialization.Serializable
import ru.pavlig43.datetime.asStartOfMonth
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.datetime.lengthOfMonth
import ru.pavlig43.datetime.single.datetime.DateTimeComponent

class DateTimePeriodComponent(
    componentContext: ComponentContext,
    initDTPeriod: DTPeriod,
): ComponentContext by componentContext{

    private val _dateTimePeriod = MutableStateFlow(initDTPeriod)
    internal val dateTimePeriod = _dateTimePeriod.asStateFlow()

    private val _dateTimePeriodForData = MutableStateFlow(initDTPeriod)
    val dateTimePeriodForData = _dateTimePeriodForData.asStateFlow()
    private val dialogNavigation = SlotNavigation<DateTimeDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "date_time_dialog",
        serializer = DateTimeDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )
    internal fun openStartDateTimeDialog() = dialogNavigation.activate(DateTimeDialog.StartDateTime)
    internal fun openEndDateTimeDialog() = dialogNavigation.activate(DateTimeDialog.EndDateTime)
    private fun createDialogChild(dialogConfig: DateTimeDialog, context: ComponentContext): DateTimeDialogChild {
        val currentPeriod = dateTimePeriod.value
        return when (dialogConfig) {
            is DateTimeDialog.StartDateTime -> {
                DateTimeDialogChild.DateTime(
                    DateTimeComponent(
                        componentContext = context,
                        initDatetime = currentPeriod.start,
                        onChangeDate = { newDateTime ->
                            _dateTimePeriod.update { it.copy(start = newDateTime) }
                        },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }
            is DateTimeDialog.EndDateTime -> {
                DateTimeDialogChild.DateTime(
                    DateTimeComponent(
                        componentContext = context,
                        initDatetime = currentPeriod.end,
                        onChangeDate = { newDateTime ->
                            _dateTimePeriod.update { it.copy(end = newDateTime) }
                        },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }
        }
    }
    internal fun updateDateTimePeriod() {
        _dateTimePeriodForData.update { _dateTimePeriod.value }
    }


}
sealed interface DateTimeDialogChild {
    class DateTime(val component: DateTimeComponent) : DateTimeDialogChild
}

@Serializable
internal sealed interface DateTimeDialog{
    @Serializable
    data object StartDateTime : DateTimeDialog

    @Serializable
    data object EndDateTime : DateTimeDialog
}

@Serializable
data class DTPeriod(
    val start: LocalDateTime,
    val end: LocalDateTime
){
    override fun toString(): String {
        return  " ${start.format(dateTimeFormat)} - ${
            end.format(
                dateTimeFormat
            )
        }"
    }
    companion object{
        val now: DTPeriod by lazy {
            DTPeriod(getCurrentLocalDateTime(), getCurrentLocalDateTime())
        }

        val thisMonth: DTPeriod by lazy {
            val now = getCurrentLocalDateTime()
            val start = now.asStartOfMonth()
            val end = LocalDateTime(
                start.year,
                start.month,
                start.lengthOfMonth,
                23,
                59,
                59,
                999_999_999
            )
            DTPeriod(start, end)
        }
    }

}