package ru.pavlig43.coreui.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.getOrCreate

private data class RetainedTabStateKey(
    val owner: Any,
    val name: String,
)

private class RetainedTabStateStore : InstanceKeeper.Instance {
    private val values = mutableMapOf<RetainedTabStateKey, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPut(key: RetainedTabStateKey, factory: () -> T): T {
        return values.getOrPut(key, factory) as T
    }

    override fun onDestroy() {
        values.clear()
    }
}

private val LocalRetainedTabStateStore = compositionLocalOf<RetainedTabStateStore?> { null }

@Composable
fun RetainedTabStateProvider(
    componentContext: ComponentContext,
    content: @Composable () -> Unit,
) {
    val store = remember(componentContext) {
        componentContext.instanceKeeper.getOrCreate { RetainedTabStateStore() }
    }
    CompositionLocalProvider(
        LocalRetainedTabStateStore provides store,
        content = content,
    )
}

@Composable
fun <T : Any> rememberRetainedTabState(
    owner: Any,
    name: String,
    factory: () -> T,
): T {
    val store = requireNotNull(LocalRetainedTabStateStore.current) {
        "rememberRetainedTabState must be called inside RetainedTabStateProvider"
    }
    return remember(store, owner, name) {
        store.getOrPut(
            key = RetainedTabStateKey(owner = owner, name = name),
            factory = factory,
        )
    }
}

@Composable
fun <T : Any> retainTabState(
    owner: Any,
    name: String,
    value: T,
): T {
    val store = requireNotNull(LocalRetainedTabStateStore.current) {
        "retainTabState must be called inside RetainedTabStateProvider"
    }
    return remember(store, owner, name) {
        store.getOrPut(
            key = RetainedTabStateKey(owner = owner, name = name),
            factory = { value },
        )
    }
}

@Composable
fun <T> rememberRetainedTabMutableState(
    owner: Any,
    name: String,
    initialValue: () -> T,
): MutableState<T> = rememberRetainedTabState(owner = owner, name = name) {
    mutableStateOf(initialValue())
}
