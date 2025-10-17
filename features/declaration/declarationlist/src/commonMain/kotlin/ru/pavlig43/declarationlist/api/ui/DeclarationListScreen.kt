import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.DeleteState
import ru.pavlig43.core.convertToDate
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.coreui.itemlist.AnyItemListBody
import ru.pavlig43.coreui.itemlist.Cell
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.coreui.itemlist.ItemListBox
import ru.pavlig43.coreui.itemlist.SearchTextField
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.declarationlist.api.component.DeclarationListComponent
import ru.pavlig43.declarationlist.internal.data.DeclarationItemUi
import ru.pavlig43.declarationlist.internal.data.DeclarationListState

@Composable
fun DeclarationListScreen(
    component: DeclarationListComponent,
    modifier: Modifier = Modifier,
) {
    val declarationListState by component.declarationListState.collectAsState()
    val deleteState by component.deleteState.collectAsState()
    val selectedItemIds = component.selectedItemIds
    val searchText by component.searchField.collectAsState()

    DeclarationListScreenState(
        state = declarationListState,
        onCreate = { component.onCreate() },
        actionInSelectedItemIds = component::actionInSelectedItemIds,
        selectedItemIds = selectedItemIds,
        deleteItems = component::deleteItems,
        shareItems = component::shareItems,
        deleteState = deleteState,
        onItemClick = {component.onItemClick((it as DeclarationItemUi))},
        searchText = searchText,
        onSearchChange = component::onSearchChange,
        withCheckbox = component.withCheckbox,
        modifier = modifier
    )
}

@Suppress("LongParameterList")
@Composable
private fun DeclarationListScreenState(
    state: DeclarationListState,
    selectedItemIds: List<Int>,
    actionInSelectedItemIds: (Boolean, Int) -> Unit,
    deleteState: DeleteState,
    onCreate: () -> Unit,
    deleteItems: (List<Int>) -> Unit,
    shareItems: (List<Int>) -> Unit,
    onItemClick: (IItemUi) -> Unit,
    searchText: String,
    onSearchChange: (String) -> Unit,
    withCheckbox: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is DeclarationListState.Error -> ErrorScreen(state.message, modifier)
        is DeclarationListState.Initial -> LoadingScreen(modifier)
        is DeclarationListState.Loading -> LoadingScreen(modifier)
        is DeclarationListState.Success -> ItemList(
            itemList = state.data,
            selectedItemIds = selectedItemIds,
            actionInSelectedItemIds = actionInSelectedItemIds,
            onCreate = onCreate,
            deleteItems = deleteItems,
            shareItems = shareItems,
            deleteState = deleteState,
            onItemClick = onItemClick,
            withCheckbox = withCheckbox,
            searchText = searchText,
            onSearchChange = onSearchChange,
            modifier = modifier
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ItemList(
    itemList: List<DeclarationItemUi>,
    selectedItemIds: List<Int>,
    actionInSelectedItemIds: (Boolean, Int) -> Unit,
    deleteState: DeleteState,
    onCreate: () -> Unit,
    deleteItems: (List<Int>) -> Unit,
    shareItems: (List<Int>) -> Unit,
    onItemClick: (IItemUi) -> Unit,
    searchText: String,
    onSearchChange: (String) -> Unit,
    withCheckbox: Boolean,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()


    ItemListBox(
        verticalScrollState = verticalScrollState,
        horizontalScrollState = horizontalScrollState,
        modifier = modifier
    ) {
        AnyItemListBody(
            itemList = itemList,
            withCheckbox = withCheckbox,
            selectedItemIds = selectedItemIds,
            actionInSelectedItemIds = actionInSelectedItemIds,
            deleteState = deleteState,
            deleteItems = deleteItems,
            shareItems = shareItems,
            horizontalScrollState = horizontalScrollState,
            verticalScrollState = verticalScrollState,
            onClickItem = onItemClick,
            searchText = searchText,
            settingRow = {
                SettingsRow(
                    onCreate = onCreate,
                    searchText = searchText,
                    onSearchChange = onSearchChange,
                )
            },
            baseCells = baseCells,
            checkBoxWidth = CHECKBOX_WIDTH,
            mainCellFactory = { (it as DeclarationItemUi).toCell() },
        )

    }

}

internal const val CHECKBOX_WIDTH = 48
internal const val ID_WIDTH = 40
internal const val NAME_WIDTH = 500
internal const val BEST_BEFORE_AT_WIDTH = 300

internal const val ID = "ИД"
internal const val NAME = "Название"
internal const val VENDOR = "Поставщик"
internal const val BEST_BEFORE_AT = "Годен до"


private val baseCells = listOf(
    Cell(ID, ID_WIDTH),
    Cell(NAME, NAME_WIDTH),
    Cell(VENDOR, NAME_WIDTH),
    Cell(BEST_BEFORE_AT, BEST_BEFORE_AT_WIDTH),
)

private fun DeclarationItemUi.toCell(): List<Cell> = listOf(

    Cell(id.toString(), ID_WIDTH),
    Cell(displayName, NAME_WIDTH),
    Cell(vendorName, NAME_WIDTH),
    Cell(bestBefore.convertToDate(), BEST_BEFORE_AT_WIDTH),
)

@Suppress("LongParameterList")
@Composable
internal fun SettingsRow(
    onCreate: () -> Unit,
    searchText:String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().height(32.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButtonToolTip(
            tooltipText = "Создать декларацию",
            onClick = onCreate,
            icon = Icons.Filled.AddCircle,
        )
        SearchTextField(
            value = searchText,
            onValueChange = onSearchChange
        )


    }
}

