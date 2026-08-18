package ru.pavlig43.nocombro.mobile.warehouse

import com.arkivanov.decompose.ComponentContext
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.pavlig43.core.componentCoroutineScope

class MobileWarehouseComponent(
    componentContext: ComponentContext,
    snapshotStore: MobileWarehouseSnapshotStore,
) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()
    private val selectedLocation = MutableStateFlow(MobileWarehouseLocation.MAIN)
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<MobileWarehouseUiState> = combine(
        snapshotStore.observeSnapshot(),
        selectedLocation,
        searchQuery,
        ::buildMobileWarehouseUiState,
    ).stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MobileWarehouseUiState(),
    )

    fun selectLocation(location: MobileWarehouseLocation) {
        selectedLocation.value = location
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
}

data class MobileWarehouseUiProduct(
    val productSyncId: String,
    val displayName: String,
    val balance: Long,
    val formattedBalance: String,
)

data class MobileWarehouseUiState(
    val selectedLocation: MobileWarehouseLocation = MobileWarehouseLocation.MAIN,
    val searchQuery: String = "",
    val updatedAt: kotlinx.datetime.LocalDateTime? = null,
    val products: List<MobileWarehouseUiProduct> = emptyList(),
    val hasSnapshot: Boolean = false,
)

internal fun buildMobileWarehouseUiState(
    snapshot: MobileWarehouseSnapshot?,
    selectedLocation: MobileWarehouseLocation,
    searchQuery: String,
): MobileWarehouseUiState {
    val normalizedQuery = searchQuery.trim()
    val products = snapshot?.products.orEmpty()
        .mapNotNull { product ->
            val balance = product.balanceAt(selectedLocation)
            if (balance == 0L) return@mapNotNull null
            MobileWarehouseUiProduct(
                productSyncId = product.productSyncId,
                displayName = product.displayName,
                balance = balance,
                formattedBalance = formatMobileWarehouseBalance(balance),
            )
        }
        .filter { product ->
            normalizedQuery.isEmpty() ||
                product.displayName.contains(normalizedQuery, ignoreCase = true) ||
                product.formattedBalance.contains(normalizedQuery, ignoreCase = true)
        }
        .sortedWith { left, right -> RUSSIAN_COLLATOR.compare(left.displayName, right.displayName) }

    return MobileWarehouseUiState(
        selectedLocation = selectedLocation,
        searchQuery = searchQuery,
        updatedAt = snapshot?.updatedAt,
        products = products,
        hasSnapshot = snapshot != null,
    )
}

internal fun findMobileWarehouseSearchMatches(
    text: String,
    query: String,
): List<IntRange> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()
    return buildList {
        var startIndex = 0
        while (startIndex <= text.length - normalizedQuery.length) {
            val matchIndex = text.indexOf(normalizedQuery, startIndex, ignoreCase = true)
            if (matchIndex < 0) break
            add(matchIndex until (matchIndex + normalizedQuery.length))
            startIndex = matchIndex + normalizedQuery.length
        }
    }
}

private val RUSSIAN_COLLATOR: Collator = Collator.getInstance(Locale.forLanguageTag("ru-RU")).apply {
    strength = Collator.PRIMARY
}
