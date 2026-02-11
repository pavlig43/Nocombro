package ru.pavlig43.vendor.internal.update

import ru.pavlig43.core.FormTabChild
import ru.pavlig43.vendor.internal.update.tabs.VendorFilesComponent
import ru.pavlig43.vendor.internal.update.tabs.essential.VendorUpdateSingleLineComponent

internal sealed interface VendorTabChild : FormTabChild {
    class Essential(override val component: VendorUpdateSingleLineComponent) : VendorTabChild
    class Files(override val component: VendorFilesComponent) : VendorTabChild
}
