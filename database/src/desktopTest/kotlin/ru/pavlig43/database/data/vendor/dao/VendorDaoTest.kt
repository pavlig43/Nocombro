package ru.pavlig43.database.data.vendor.dao

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.createTestDatabase
import ru.pavlig43.database.data.TestEntities
import ru.pavlig43.database.data.vendor.Vendor

class VendorDaoTest : FunSpec({

    lateinit var database: NocombroDatabase
    lateinit var vendorDao: VendorDao

    beforeTest {
        database = createTestDatabase()
        vendorDao = database.vendorDao
    }

    afterTest {
        database.close()
    }

    test("create should insert vendor and return generated id") {
        // Arrange
        val vendor = TestEntities.createTestVendor(
            id = 0,
            displayName = "New Vendor",
            comment = "Test comment"
        )

        // Act
        val generatedId = vendorDao.create(vendor)

        // Assert
        generatedId shouldBe 1L

        val savedVendor = vendorDao.getVendor(1)
        savedVendor.displayName shouldBe "New Vendor"
        savedVendor.comment shouldBe "Test comment"
    }

    test("getVendor should return vendor by id") {
        // Arrange
        val vendor = TestEntities.createTestVendor(
            id = 0,
            displayName = "Test Vendor"
        )
        vendorDao.create(vendor)

        // Act
        val retrievedVendor = vendorDao.getVendor(1)

        // Assert
        retrievedVendor.displayName shouldBe "Test Vendor"
    }

    test("deleteVendorsByIds should delete vendors") {
        // Arrange
        val vendor1 = TestEntities.createTestVendor(id = 0, displayName = "Vendor 1")
        val vendor2 = TestEntities.createTestVendor(id = 0, displayName = "Vendor 2")
        val vendor3 = TestEntities.createTestVendor(id = 0, displayName = "Vendor 3")

        vendorDao.create(vendor1)
        vendorDao.create(vendor2)
        vendorDao.create(vendor3)

        // Act
        vendorDao.deleteVendorsByIds(setOf(1, 3))

        // Assert
        val remainingVendors = mutableListOf<Vendor>()
        vendorDao.observeOnVendors().test {
            remainingVendors.addAll(awaitItem())
            ensureAllEventsConsumed()
        }

        remainingVendors.size shouldBe 1
        remainingVendors[0].displayName shouldBe "Vendor 2"
    }

    test("isCanSave should return Success for valid vendor") {
        // Arrange
        val vendor = TestEntities.createTestVendor(id = 1, displayName = "Unique Name")

        // Act
        val result = vendorDao.isCanSave(vendor)

        // Assert
        result.isSuccess shouldBe true
    }
})
