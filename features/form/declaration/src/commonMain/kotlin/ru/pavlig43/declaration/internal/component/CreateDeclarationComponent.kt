package ru.pavlig43.declaration.internal.component

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.create.component.CreateEssentialsComponent
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declaration.internal.data.DeclarationEssentialsUi
import ru.pavlig43.declaration.internal.data.toDto
import ru.pavlig43.immutable.api.ImmutableTableDependencies


internal class CreateDeclarationComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    createDeclarationRepository: CreateEssentialsRepository<Declaration>,
    dependencies: ImmutableTableDependencies,
    tabOpener: TabOpener,
    componentFactory: EssentialComponentFactory<Declaration, DeclarationEssentialsUi>
) : CreateEssentialsComponent<Declaration, DeclarationEssentialsUi>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    createEssentialsRepository = createDeclarationRepository,
    componentFactory = componentFactory,
    mapperToDTO = { toDto() },
) {
    val vendorDialog = VendorDialogComponent(
        parentComponentContext = componentContext,
        onChangeVendor = { id, name ->
            val declaration = itemFields.value.copy(vendorId = id, vendorName = name)
            onChangeItem(declaration)
        },
        dependencies = dependencies,
        tabOpener = tabOpener
    )


}

