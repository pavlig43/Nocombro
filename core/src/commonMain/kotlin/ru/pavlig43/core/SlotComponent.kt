package ru.pavlig43.core

import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.StateFlow

interface SlotComponent {
    val model: StateFlow<TabModel>

    data class TabModel(
        val title: String,
    )



}


//internal class DefaultSlotComponent(
//    componentContext: ComponentContext,
//    private val tabTitle: String
//) : ComponentContext by componentContext, SlotComponent {
//    private companion object {
//        private const val KEY_STATE = "STATE"
//    }
//
//    private val handler = retainedInstance {
//        Handler(
//            initialState = stateKeeper.consume(key = KEY_STATE, strategy = State.serializer())
//                ?: State(title = "(created) $tabTitle"),
//            tabTitle = tabTitle
//        )
//    }
//
//    override val model: Value<TabModel> = handler.state.map { it.toModel() }
//
//    init {
//        lifecycle.subscribe(
//            onStart = handler::resume,
//            onStop = handler::pause,
//        )
//
//        stateKeeper.register(key = KEY_STATE, strategy = State.serializer()) { handler.state.value }
//    }
//
//    private fun State.toModel(): TabModel =
//        TabModel(
//            title = title,
//        )
//
//    @Serializable
//    private data class State(
//        val title: String,
//    )
//
//    private class Handler(initialState: State, private val tabTitle: String) : InstanceKeeper.Instance {
//        val state: MutableValue<State> = MutableValue(initialState)
//        private var job: Job? = null
//
//        fun resume() {
//            state.update { it.copy(title = "(active) $tabTitle") }
//            job?.cancel()
//
//            job = CoroutineScope(Dispatchers.Default).launch {
//                flow {
//                    while (true) {
//                        emit(Unit)
//                        delay(5000L)
//                    }
//                }
//                    .collect {
//                        state.update {it }
//                    }
//            }
//        }
//
//        fun pause() {
//            state.update { it.copy(title = "(paused) $tabTitle") }
//            job?.cancel()
//            job = null
//        }
//
//        override fun onDestroy() {
//            pause()
//        }
//    }
//}
