package ru.pavlig43.database.data.product.dao

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.createTestDatabase
import ru.pavlig43.database.data.TestEntities
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType

class ProductDaoTest : FunSpec({

    lateinit var database: NocombroDatabase
    lateinit var productDao: ProductDao

    beforeTest {
        database = createTestDatabase()
        productDao = database.productDao
    }

    afterTest {
        database.close()
    }

    test("create should insert product and return generated id") {
        // Arrange
        val product = TestEntities.createTestProduct(
            id = 0,
            displayName = "New Product",
            type = ProductType.FOOD_BASE,
            createdAt = LocalDate(2025, 1, 15),
            priceForSale = 15000
        )

        // Act
        val generatedId = productDao.create(product)

        // Assert
        generatedId shouldBe 1L

        val savedProduct = productDao.getProduct(1)
        savedProduct.displayName shouldBe "New Product"
        savedProduct.type shouldBe ProductType.FOOD_BASE
        savedProduct.priceForSale shouldBe 15000
    }

    test("deleteProductsByIds should delete products") {
        // Arrange
        val product1 = TestEntities.createTestProduct(id = 0, displayName = "Product 1")
        val product2 = TestEntities.createTestProduct(id = 0, displayName = "Product 2")
        val product3 = TestEntities.createTestProduct(id = 0, displayName = "Product 3")

        productDao.create(product1)
        productDao.create(product2)
        productDao.create(product3)

        // Act
        productDao.deleteProductsByIds(setOf(1, 3))

        // Assert
        val remainingProducts = mutableListOf<Product>()
        productDao.observeOnProducts().test {
            remainingProducts.addAll(awaitItem())
            ensureAllEventsConsumed()
        }

        remainingProducts.size shouldBe 1
        remainingProducts[0].displayName shouldBe "Product 2"
    }

    test("isCanSave should return Success for valid product") {
        // Arrange
        val product = TestEntities.createTestProduct(id = 1, displayName = "Unique Name")

        // Act
        val result = productDao.isCanSave(product)

        // Assert
        result.isSuccess shouldBe true
    }
})
