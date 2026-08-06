package ru.pavlig43.nocombro.mobile.warehouse

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MobileWarehouseComponentStateTest {
    private val snapshot = MobileWarehouseSnapshot(
        updatedAt = LocalDateTime.parse("2026-08-06T14:32:00"),
        products = listOf(
            MobileWarehouseProduct("3", "Яблоко", 1_500, 25),
            MobileWarehouseProduct("1", "абрикос", 0, -120),
            MobileWarehouseProduct("2", "Банан банан", 6_250, 0),
        ),
    )

    @Test
    fun `switches balances and sorts names with Russian collation`() {
        val main = buildMobileWarehouseUiState(snapshot, MobileWarehouseLocation.MAIN, "")
        val experimental = buildMobileWarehouseUiState(
            snapshot,
            MobileWarehouseLocation.EXPERIMENTAL,
            "",
        )

        assertEquals(listOf("абрикос", "Банан банан", "Яблоко"), main.products.map { it.displayName })
        assertEquals("1,500", main.products.single { it.productSyncId == "3" }.formattedBalance)
        assertEquals("-0,120", formatMobileWarehouseBalance(-120))
        assertEquals(-120, experimental.products.single { it.productSyncId == "1" }.balance)
    }

    @Test
    fun `searches displayed name and selected formatted balance with trim and ignore case`() {
        val byName = buildMobileWarehouseUiState(snapshot, MobileWarehouseLocation.MAIN, "  БАНАН ")
        val byMainBalance = buildMobileWarehouseUiState(snapshot, MobileWarehouseLocation.MAIN, "1,500")
        val byExperimentalBalance = buildMobileWarehouseUiState(
            snapshot,
            MobileWarehouseLocation.EXPERIMENTAL,
            "-0,120",
        )

        assertEquals(listOf("Банан банан"), byName.products.map { it.displayName })
        assertEquals(listOf("Яблоко"), byMainBalance.products.map { it.displayName })
        assertEquals(listOf("абрикос"), byExperimentalBalance.products.map { it.displayName })
        assertTrue(buildMobileWarehouseUiState(snapshot, MobileWarehouseLocation.MAIN, "нет").products.isEmpty())
    }

    @Test
    fun `finds every non-overlapping match and leaves empty query unhighlighted`() {
        assertEquals(listOf(0..4, 6..10), findMobileWarehouseSearchMatches("Банан банан", "банан"))
        assertTrue(findMobileWarehouseSearchMatches("Банан", "   ").isEmpty())

        val annotated = buildWarehouseHighlightedText(
            text = "Банан банан",
            query = "банан",
            background = Color.Yellow,
            foreground = Color.Black,
        )
        assertEquals(2, annotated.spanStyles.size)
    }

    @Test
    fun `distinguishes absent snapshot from an empty search result`() {
        val absent = buildMobileWarehouseUiState(null, MobileWarehouseLocation.MAIN, "")
        val emptySearch = buildMobileWarehouseUiState(snapshot, MobileWarehouseLocation.MAIN, "нет")

        assertFalse(absent.hasSnapshot)
        assertTrue(absent.products.isEmpty())
        assertTrue(emptySearch.hasSnapshot)
        assertTrue(emptySearch.products.isEmpty())
        assertEquals("Обновлено 6 августа, 14:32", formatWarehouseUpdatedAt(snapshot.updatedAt))
    }
}
