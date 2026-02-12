package ru.pavlig43.vendor.internal.create.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.vendor.internal.VendorField
import ru.pavlig43.vendor.internal.model.VendorEssentialsUi
import ru.pavlig43.vendor.internal.model.toDto
import ua.wwind.table.ColumnSpec

/**
 * Компонент для создания поставщика через таблицу с одной строкой.
 *
 * @param componentContext Decompose контекст компонента
 * @param onSuccessCreate Callback при успешном создании (принимает ID нового поставщика)
 * @param createVendorRepository Репозиторий для создания поставщика
 */
internal class CreateVendorSingleLineComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    observeOnItem: (VendorEssentialsUi) -> Unit,
    componentFactory: SingleLineComponentFactory<Vendor, VendorEssentialsUi>,
    createVendorRepository: CreateSingleItemRepository<Vendor>,
) : CreateSingleLineComponent<Vendor, VendorEssentialsUi, VendorField>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = componentFactory,
    createSingleItemRepository = createVendorRepository,
    mapperToDTO = VendorEssentialsUi::toDto,
    observeOnItem = observeOnItem
) {
    override val columns: ImmutableList<ColumnSpec<VendorEssentialsUi, VendorField, Unit>> =
        createVendorColumns0(
            onChangeItem = { item -> onChangeItem(item) }
        )
}
