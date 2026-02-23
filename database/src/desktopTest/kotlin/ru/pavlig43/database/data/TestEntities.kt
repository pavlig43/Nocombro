package ru.pavlig43.database.data

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.vendor.Vendor

/**
 * Тестовые сущности для использования в тестах.
 */
object TestEntities {

    // ==================== VENDOR ====================

    fun createTestVendor(
        id: Int = 1,
        displayName: String = "Test Vendor $id",
        comment: String = "Test comment"
    ): Vendor = Vendor(
        id = id,
        displayName = displayName,
        comment = comment
    )

    // ==================== DECLARATION ====================

    fun createTestDeclaration(
        id: Int = 1,
        displayName: String = "Test Declaration $id",
        vendorId: Int = 1,
        vendorName: String = "Test Vendor",
        createdAt: LocalDate = LocalDate(2025, 1, 1),
        bornDate: LocalDate = LocalDate(2025, 1, 1),
        bestBefore: LocalDate = LocalDate(2025, 12, 31),
        observeFromNotification: Boolean = false
    ): Declaration = Declaration(
        id = id,
        displayName = displayName,
        createdAt = createdAt,
        vendorId = vendorId,
        vendorName = vendorName,
        bornDate = bornDate,
        bestBefore = bestBefore,
        observeFromNotification = observeFromNotification
    )

    // ==================== PRODUCT ====================

    fun createTestProduct(
        id: Int = 1,
        type: ProductType = ProductType.FOOD_BASE,
        displayName: String = "Test Product $id",
        createdAt: LocalDate = LocalDate(2025, 1, 1),
        comment: String = "Test product comment",
        priceForSale: Int = 10000 // 100.00 руб
    ): Product = Product(
        id = id,
        type = type,
        displayName = displayName,
        createdAt = createdAt,
        comment = comment,
        priceForSale = priceForSale
    )

    // ==================== BATCH ====================

    fun createTestBatch(
        id: Int = 1,
        productId: Int = 1,
        declarationId: Int = 1,
        dateBorn: LocalDate = LocalDate(2025, 6, 15)
    ): BatchBD = BatchBD(
        id = id,
        productId = productId,
        dateBorn = dateBorn,
        declarationId = declarationId
    )
}
