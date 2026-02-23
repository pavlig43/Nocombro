package ru.pavlig43.database.data.batch.dao

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.createTestDatabase
import ru.pavlig43.database.data.TestEntities

class BatchDaoTest : FunSpec({

    lateinit var database: NocombroDatabase
    lateinit var batchDao: BatchDao

    beforeTest {
        database = createTestDatabase()
        batchDao = database.batchDao
    }

    afterTest {
        database.close()
    }

    test("createBatch should insert batch and return generated id") {
        // Arrange - создаём vendor, declaration, product
        val vendor = TestEntities.createTestVendor(id = 0, displayName = "Test Vendor")
        database.vendorDao.create(vendor)

        val declaration = TestEntities.createTestDeclaration(
            id = 0,
            displayName = "Test Declaration",
            vendorId = 1,
            vendorName = "Test Vendor"
        )
        database.declarationDao.create(declaration)

        val product = TestEntities.createTestProduct(id = 0, displayName = "Test Product")
        database.productDao.create(product)

        val batch = TestEntities.createTestBatch(
            id = 0,
            productId = 1,
            declarationId = 1,
            dateBorn = LocalDate(2025, 6, 15)
        )

        // Act
        val generatedId = batchDao.createBatch(batch)

        // Assert
        generatedId shouldBe 1L
    }
})
