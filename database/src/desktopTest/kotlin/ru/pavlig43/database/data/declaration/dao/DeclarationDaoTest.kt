package ru.pavlig43.database.data.declaration.dao

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.createTestDatabase
import ru.pavlig43.database.data.TestEntities
import ru.pavlig43.database.data.declaration.Declaration

class DeclarationDaoTest : FunSpec({

    lateinit var database: NocombroDatabase
    lateinit var declarationDao: DeclarationDao

    beforeTest {
        database = createTestDatabase()
        declarationDao = database.declarationDao
    }

    afterTest {
        database.close()
    }

    test("create should insert declaration and return generated id") {
        // Arrange - сначала создаём vendor, т.к. Declaration имеет FK на Vendor
        val vendor = TestEntities.createTestVendor(id = 0, displayName = "Test Vendor")
        database.vendorDao.create(vendor)

        val declaration = TestEntities.createTestDeclaration(
            id = 0,
            displayName = "New Declaration",
            vendorId = 1, // ссылаемся на созданный vendor
            vendorName = "Test Vendor",
            bestBefore = LocalDate(2025, 12, 31)
        )

        // Act
        val generatedId = declarationDao.create(declaration)

        // Assert
        generatedId shouldBe 1L

        val savedDeclaration = declarationDao.getDeclaration(1)
        savedDeclaration.displayName shouldBe "New Declaration"
        savedDeclaration.vendorId shouldBe 1
        savedDeclaration.bestBefore shouldBe LocalDate(2025, 12, 31)
    }

    test("deleteDeclarationsByIds should delete declarations") {
        // Arrange - создаём vendors и declarations
        val vendor = TestEntities.createTestVendor(id = 0, displayName = "Test Vendor")
        database.vendorDao.create(vendor)

        val decl1 = TestEntities.createTestDeclaration(
            id = 0, displayName = "Declaration 1", vendorId = 1, vendorName = "Test Vendor"
        )
        val decl2 = TestEntities.createTestDeclaration(
            id = 0, displayName = "Declaration 2", vendorId = 1, vendorName = "Test Vendor"
        )
        val decl3 = TestEntities.createTestDeclaration(
            id = 0, displayName = "Declaration 3", vendorId = 1, vendorName = "Test Vendor"
        )

        declarationDao.create(decl1)
        declarationDao.create(decl2)
        declarationDao.create(decl3)

        // Act
        declarationDao.deleteDeclarationsByIds(setOf(1, 3))

        // Assert
        val remainingDeclarations = mutableListOf<Declaration>()
        declarationDao.observeOnItems().test {
            remainingDeclarations.addAll(awaitItem())
            ensureAllEventsConsumed()
        }

        remainingDeclarations.size shouldBe 1
        remainingDeclarations[0].displayName shouldBe "Declaration 2"
    }

    test("isCanSave should return Success for valid declaration") {
        // Arrange - создаём vendor
        val vendor = TestEntities.createTestVendor(id = 0, displayName = "Test Vendor")
        database.vendorDao.create(vendor)

        val declaration = TestEntities.createTestDeclaration(
            id = 1,
            displayName = "Unique Name",
            vendorId = 1,
            vendorName = "Test Vendor"
        )

        // Act
        val result = declarationDao.isCanSave(declaration)

        // Assert
        result.isSuccess shouldBe true
    }
})
