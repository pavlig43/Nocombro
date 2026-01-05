package ru.pavlig43.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SlotComponent {
    val model: StateFlow<TabModel>

    data class TabModel(
        val title: String,
    )

}
interface FormTabSlot {
    val title: String
    suspend fun onUpdate(): Result<Unit>

    val errorMessages: Flow<List<String>>


}


