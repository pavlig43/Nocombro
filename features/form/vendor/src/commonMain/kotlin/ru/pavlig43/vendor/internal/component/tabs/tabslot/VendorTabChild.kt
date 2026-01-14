package ru.pavlig43.vendor.internal.component.tabs.tabslot

import ru.pavlig43.core.FormTabChild

internal sealed interface VendorTabChild: FormTabChild{
    class Essentials(override val component: VendorEssentialsComponent): VendorTabChild
    class Files(override val component: VendorFilesComponent): VendorTabChild
}