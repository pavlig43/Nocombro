package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import androidx.compose.foundation.text.input.TextFieldState
import ru.pavlig43.mutable.api.singleLine.component.SingleLineInputState
import ru.pavlig43.mutable.api.input.decimalValue
import ru.pavlig43.mutable.api.input.toDecimalInputText
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.transact.pf.PfBD

data class PfUi(
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val movementId: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val declarationId: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val countState: TextFieldState = TextFieldState(),
) : SingleLineInputState {
    val count: DecimalData3 get() = DecimalData3(countState.decimalValue(3) ?: 0L)
    override val textFields get() = listOf(countState)
    override val inputErrors get() = if (
        countState.decimalValue(3) == null
    ) listOf("Некорректное число") else emptyList()
}

internal fun PfUi.toDto(getDateBorn:()-> LocalDate): PfBD = PfBD(
    transactionId = transactionId,
    batchId = batchId,
    movementId = movementId,
    count = count.value,
    productId = productId,
    productName = productName,
    declarationId = declarationId,
    declarationName = declarationName,
    vendorName = vendorName,
    dateBorn = getDateBorn()
)

internal fun PfBD.toUi(transactionId: Int): PfUi = PfUi(
    transactionId = transactionId,
    batchId = batchId,
    movementId = movementId,
    productId = productId,
    productName = productName,
    declarationId = declarationId,
    declarationName = declarationName,
    vendorName = vendorName,
    countState = TextFieldState((DecimalData3(count)).toDecimalInputText()),
    )

