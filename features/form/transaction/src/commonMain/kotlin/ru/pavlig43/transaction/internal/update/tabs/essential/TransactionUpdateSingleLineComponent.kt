package ru.pavlig43.transaction.internal.update.tabs.essential

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateTimeComponent
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.transaction.internal.TransactionField
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toDto
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec

internal class TransactionUpdateSingleLineComponent(
    componentContext: ComponentContext,
    transactionId: Int,
    updateRepository: UpdateSingleLineRepository<Transaction>,
    componentFactory: SingleLineComponentFactory<Transaction, TransactionEssentialsUi>,
    observeOnItem: (TransactionEssentialsUi) -> Unit,
    onSuccessInitData: (TransactionEssentialsUi) -> Unit,
) : UpdateSingleLineComponent<Transaction, TransactionEssentialsUi, TransactionField>(
    componentContext = componentContext,
    id = transactionId,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    observeOnItem = observeOnItem,
    onSuccessInitData = onSuccessInitData,
    mapperToDTO = { toDto() }
) {
    private val dialogNavigation = SlotNavigation<UpdateDialogConfig>()

    val dialog: Value<ChildSlot<UpdateDialogConfig, UpdateDialogChild>> = childSlot(
        source = dialogNavigation,
        key = "dialog",
        serializer = UpdateDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::dialogChild
    )

    private fun dialogChild(config: UpdateDialogConfig, context: ComponentContext): UpdateDialogChild {
        return when (config) {
            UpdateDialogConfig.CreatedAt -> {
                val item = itemFields.value[0]
                UpdateDialogChild.DateTime(
                    DateTimeComponent(
                        componentContext = context,
                        initDatetime = item.createdAt,
                        onChangeDate = { newDate -> onChangeItem(item.copy(createdAt = newDate)) },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }
        }
    }

    override val columns: ImmutableList<ColumnSpec<TransactionEssentialsUi, TransactionField, Unit>> =
        createTransactionColumns1(
            onOpenCreatedAtDialog = { dialogNavigation.activate(UpdateDialogConfig.CreatedAt) },
            onChangeItem = { item -> onChangeItem(item) }
        )

    override val errorMessages: Flow<List<String>> = errorTableMessages
}

@Serializable
internal sealed interface UpdateDialogConfig {
    @Serializable
    data object CreatedAt : UpdateDialogConfig
}

internal sealed interface UpdateDialogChild {
    class DateTime(val component: DateTimeComponent) : UpdateDialogChild
}
