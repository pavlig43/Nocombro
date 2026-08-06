package ru.pavlig43.nocombro.mobile.warehouse

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class MobileWarehouseComposeContractTest {
    @Test
    fun `screen stays a non-interactive one-column phone list`() {
        val source = locateScreenSource().readText()

        assertContains(source, "Scaffold(")
        assertContains(source, "LazyColumn(")
        assertContains(source, "key = MobileWarehouseUiProduct::productSyncId")
        assertContains(source, "Modifier.weight(1f)")
        assertContains(source, "maxLines = 2")
        assertContains(source, "overflow = TextOverflow.Ellipsis")
        assertFalse(source.contains("LazyVerticalGrid"))
        assertFalse(source.contains("BoxWithConstraints"))
        assertFalse(source.contains("WarehouseProductCard(product, state.searchQuery)\n                .clickable"))
    }

    private fun locateScreenSource(): File {
        val relative = "src/main/kotlin/ru/pavlig43/nocombro/mobile/warehouse/MobileWarehouseScreen.kt"
        return sequenceOf(File(relative), File("app/nocombroMobile/$relative"))
            .first(File::isFile)
    }
}
