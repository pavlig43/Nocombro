package ru.pavlig43.transaction.internal.create.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.datetime.single.datetime.DateTimeComponent
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toDto

internal class CreateTransactionSingleLineComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    observeOnItem: (TransactionEssentialsUi) -> Unit,
    componentFactory: SingleLineComponentFactory<Transact, TransactionEssentialsUi>,
    createRepository: CreateSingleItemRepository<Transact>,
) : CreateSingleLineComponent<Transact, TransactionEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = componentFactory,
    createSingleItemRepository = createRepository,
    mapperToDTO = { toDto() },
    observeOnItem = observeOnItem,
) {
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    val dialog: Value<ChildSlot<DialogConfig, DialogChild>> = childSlot(
        source = dialogNavigation,
        key = "dialog",
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::dialogChild,
    )

    /** Открывает диалог выбора даты и времени транзакции. */
    fun onOpenCreatedAtDialog() {
        dialogNavigation.activate(DialogConfig.CreatedAt)
    }

    private fun dialogChild(config: DialogConfig, context: ComponentContext): DialogChild {
        return when (config) {
            DialogConfig.CreatedAt -> {
                val item = item.value
                DialogChild.DateTime(
                    DateTimeComponent(
                        componentContext = context,
                        initDatetime = item.createdAt,
                        onChangeDate = { newDate -> onChangeItem { it.copy(createdAt = newDate) } },
                        onDismissRequest = { dialogNavigation.dismiss() },
                    ),
                )
            }
        }
    }

}

@Serializable
internal sealed interface DialogConfig {
    @Serializable
    data object CreatedAt : DialogConfig
}

internal sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
}
