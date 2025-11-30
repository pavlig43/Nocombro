package ru.pavlig43.manageitem.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import org.koin.core.qualifier.named
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.manageitem.api.UpsertEssentialsDependencies
import ru.pavlig43.manageitem.api.DeclarationFactoryParam
import ru.pavlig43.manageitem.api.DocumentFactoryParam
import ru.pavlig43.manageitem.api.ProductFactoryParam
import ru.pavlig43.manageitem.api.UpsertEssentialsFactoryParam
import ru.pavlig43.manageitem.api.VendorFactoryParam
import ru.pavlig43.manageitem.internal.component.DeclarationComponent
import ru.pavlig43.manageitem.internal.component.DocumentComponent
import ru.pavlig43.manageitem.internal.component.EssentialsComponent
import ru.pavlig43.manageitem.internal.component.ProductComponent
import ru.pavlig43.manageitem.internal.component.VendorComponent
import ru.pavlig43.manageitem.internal.data.ItemEssentialsUi
import ru.pavlig43.manageitem.internal.di.moduleFactory
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UpsertEssentialsFactoryComponent(
    componentContext: ComponentContext,
    upsertEssentialsFactoryParam: UpsertEssentialsFactoryParam,
    upsertEssentialsDependencies: UpsertEssentialsDependencies,
    val closeFormScreen: () -> Unit
) : ComponentContext by componentContext, FormTabSlot {
    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(moduleFactory(upsertEssentialsDependencies))


    internal val essentialsComponent: EssentialsComponent<out GenericItem, out ItemEssentialsUi> =
        when (val param = upsertEssentialsFactoryParam) {

            is DocumentFactoryParam -> DocumentComponent(
                componentContext = componentContext,
                param = param,
                createEssentialsRepository = scope.get(named(param.createItemType))
            )

            is ProductFactoryParam -> ProductComponent(
                componentContext = componentContext,
                param = param,
                createEssentialsRepository = scope.get(named(param.createItemType))
            )

            is VendorFactoryParam -> VendorComponent(
                componentContext = componentContext,
                param = param,
                createEssentialsRepository = scope.get(named(param.createItemType))
            )

            is DeclarationFactoryParam -> DeclarationComponent(
                componentContext = componentContext,
                param = param,
                createEssentialsRepository = scope.get(named(param.createItemType)),
            )
        }
    override val title: String = "Основная информация"

    override suspend fun onUpdate() {

    }

}
