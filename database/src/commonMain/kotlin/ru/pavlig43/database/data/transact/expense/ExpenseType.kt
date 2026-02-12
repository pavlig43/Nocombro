package ru.pavlig43.database.data.transact.expense

import ru.pavlig43.core.model.ItemType

/**
 * Типы расходов
 */

/**
 * Enum для хранения в БД
 */
enum class ExpenseType : ItemType {
    TRANSPORT_GASOLINE {
        override val displayName: String = "Бензин"
    },
    TRANSPORT_DELIVERY {
        override val displayName: String = "Доставка"
    },
    TRANSPORT_DEPRECIATION {
        override val displayName: String = "Амортизация авто"
    },
    STATIONERY {
        override val displayName: String = "Канцелярия"
    },
    COMMISSION {
        override val displayName: String = "Откаты"
    }
}
