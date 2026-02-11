package ru.pavlig43.declaration.internal.component.tabs.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declaration.internal.component.VendorDialogComponent
import ru.pavlig43.declaration.internal.data.DeclarationEssentialsUi
import ru.pavlig43.declaration.internal.data.toDto
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateSingleLineRepository

internal class DeclarationEssentialComponent(
    componentContext: ComponentContext,
    declarationId: Int,
    dependencies: ImmutableTableDependencies,
    tabOpener: TabOpener,
    updateRepository: UpdateSingleLineRepository<Declaration>,
    componentFactory: EssentialComponentFactory<Declaration, DeclarationEssentialsUi>,
) : UpdateEssentialsComponent<Declaration, DeclarationEssentialsUi>(
    componentContext = componentContext,
    id = declarationId,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = {toDto()}
){
    val vendorDialogComponent = VendorDialogComponent(
        parentComponentContext = componentContext,
        onChangeVendor = { id, name ->
            val declaration = itemFields.value.copy(vendorId = id, vendorName = name)
            onChangeItem(declaration)

        },
        dependencies = dependencies,
        tabOpener = tabOpener
    )
    override val errorMessages: Flow<List<String>> = flowOf(emptyList())
}