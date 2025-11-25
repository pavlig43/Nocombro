package ru.pavlig43.transactionform.internal.data

import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.database.data.transaction.OperationType
import ru.pavlig43.database.data.transaction.TransactionType

internal data class ProductTransactionUi(


    val transactionType: TransactionType? = null,

    val operationType: OperationType? = null,

    val date: Long = 0,

    val comment: String = "",

    val rows:List<TransactionRowUi> = emptyList(),

    val id: Int = 0,

)

internal data class TransactionRowUi(
    val  composeKey:Int,

    val productId: Int,

    val displayName:String,

    val dateBorn:Long?,

//    val dateExpire: Long,

    val declarationWithVendorName: String?,

    val batchNumber:Int?,

    val id:Int = 0
)