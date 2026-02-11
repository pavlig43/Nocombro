package ru.pavlig43.transaction.internal.create.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateTimeComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.transaction.internal.TransactionField
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toDto
import ua.wwind.table.ColumnSpec

internal class CreateTransactionSingleLineComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    observeOnItem: (TransactionEssentialsUi) -> Unit,
    componentFactory: SingleLineComponentFactory<Transaction, TransactionEssentialsUi>,
    createRepository: CreateSingleItemRepository<Transaction>,
) : CreateSingleLineComponent<Transaction, TransactionEssentialsUi, TransactionField>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = componentFactory,
    createSingleItemRepository = createRepository,
    mapperToDTO = { toDto() },
    observeOnItem = observeOnItem
) {
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    val dialog: Value<ChildSlot<DialogConfig, DialogChild>> = childSlot(
        source = dialogNavigation,
        key = "dialog",
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::dialogChild
    )

    private fun dialogChild(config: DialogConfig, context: ComponentContext): DialogChild {
        return when (config) {
            DialogConfig.CreatedAt -> {
                val item = itemFields.value[0]
                DialogChild.DateTime(
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
        createTransactionColumns0(
            onOpenCreatedAtDialog = { dialogNavigation.activate(DialogConfig.CreatedAt) },
            onChangeItem = { item -> onChangeItem(item) }
        )
}

@Serializable
internal sealed interface DialogConfig {
    @Serializable
    data object CreatedAt : DialogConfig
}

internal sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
}
