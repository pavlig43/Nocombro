package ru.pavlig43.nocombro.mobile.warehouse

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.math.BigDecimal
import java.util.Locale
import kotlinx.datetime.LocalDateTime

enum class MobileWarehouseLocation {
    MAIN,
    EXPERIMENTAL,
}

data class MobileWarehouseProduct(
    val productSyncId: String,
    val displayName: String,
    val mainBalance: Long,
    val experimentalBalance: Long,
) {
    fun balanceAt(location: MobileWarehouseLocation): Long = when (location) {
        MobileWarehouseLocation.MAIN -> mainBalance
        MobileWarehouseLocation.EXPERIMENTAL -> experimentalBalance
    }
}

data class MobileWarehouseSnapshot(
    val updatedAt: LocalDateTime,
    val products: List<MobileWarehouseProduct>,
)

fun formatMobileWarehouseBalance(value: Long): String {
    return DecimalFormat(
        "#,##0.000",
        DecimalFormatSymbols(Locale.forLanguageTag("ru-RU")),
    ).format(BigDecimal.valueOf(value, 3))
}
