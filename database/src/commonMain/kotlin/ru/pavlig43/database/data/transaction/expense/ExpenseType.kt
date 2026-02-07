package ru.pavlig43.database.data.transaction.expense

import ru.pavlig43.core.model.ItemType

/**
 * Типы расходов
 */
sealed interface ExpenseType : ItemType {
    val enumValue: ExpenseTypeEnum

    override val displayName: String get() = enumValue.displayName

    /**
     * Транспортные расходы
     */
    sealed interface Transport : ExpenseType {
        data object Gasoline : Transport {
            override val enumValue: ExpenseTypeEnum = ExpenseTypeEnum.TRANSPORT_GASOLINE
        }

        data object Delivery : Transport {
            override val enumValue: ExpenseTypeEnum = ExpenseTypeEnum.TRANSPORT_DELIVERY
        }

        data object Depreciation : Transport {
            override val enumValue: ExpenseTypeEnum = ExpenseTypeEnum.TRANSPORT_DEPRECIATION
        }
    }

    /**
     * Канцелярия
     */
    data object Stationery : ExpenseType {
        override val enumValue: ExpenseTypeEnum = ExpenseTypeEnum.STATIONERY
    }

    /**
     * Откаты (комиссионные)
     */
    data object Commission : ExpenseType {
        override val enumValue: ExpenseTypeEnum = ExpenseTypeEnum.COMMISSION
    }

    companion object {
        val entries = listOf(
            Transport.Gasoline,
            Transport.Delivery,
            Transport.Depreciation,
            Stationery,
            Commission
        )
    }
}

/**
 * Enum для хранения в БД
 */
enum class ExpenseTypeEnum : ItemType {
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
