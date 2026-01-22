package ru.pavlig43.vendor.internal.component.tabs

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.vendor.internal.component.tabs.component.VendorEssentialsComponent
import ru.pavlig43.vendor.internal.component.tabs.component.VendorFilesComponent

internal sealed interface VendorTabChild: FormTabChild {
    class Essentials(override val component: VendorEssentialsComponent): VendorTabChild
    class Files(override val component: VendorFilesComponent): VendorTabChild
}