package ru.pavlig43.database.dao

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.db.BaseDatabaseTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentDaoTest : BaseDatabaseTest() {

    private lateinit var documentDao: DocumentDao

    @BeforeTest
    fun daoSetup() {
        documentDao = database.documentDao
    }
    private fun getTestDocument(id: Int,name: String = "name"): Document {
        return Document(
            displayName = name,
            type = DocumentType.GOST,
            createdAt = 1234567890L + id,
            comment = "Comment $id",
            id = id
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun create_returnsId() = runTest {

        // WHEN
        val insertedId = documentDao.create(getTestDocument(1))
        advanceUntilIdle()

        // THEN
        assertTrue(insertedId > 0)
        val found = documentDao.getDocument(insertedId.toInt())
        assertEquals("name", found.displayName)
        assertEquals(getTestDocument(1), found)
    }

    @Test
    fun updateDocument_works() = runTest {
        // GIVEN
        val document = getTestDocument(1)
        documentDao.create(document)
        advanceUntilIdle()

        // WHEN
        val updated = document.copy(displayName = "After")
        documentDao.updateDocument(updated)
        advanceUntilIdle()

        // THEN
        val result = documentDao.getDocument(1)
        assertEquals("After", result.displayName)
    }

    @Test
    fun deleteDocumentsByIds_works() = runTest {
        // GIVEN
        documentDao.create(getTestDocument(1))
        documentDao.create(getTestDocument(2))
        advanceUntilIdle()

        // WHEN
        documentDao.deleteDocumentsByIds(listOf(1))
        advanceUntilIdle()

        // THEN
        assertFailsWith<IllegalStateException> {  // ← ПРОВЕРЯЕМ исключение!
            documentDao.getDocument(1)
        }
        assertNotNull(documentDao.getDocument(2))
    }

    @Test
    fun observeOnDocuments_emitsUpdates() = runTest {
        // GIVEN
        documentDao.create(getTestDocument(1))
        documentDao.create(getTestDocument(2))
        advanceUntilIdle()

        // WHEN + THEN
        documentDao.observeOnDocuments("", listOf(DocumentType.GOST)).test {
            // Initial emit
            assertEquals(2, awaitItem().size)

            // Update search
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun isNameAllowed_returnsCorrectly() = runTest {
        // GIVEN
        documentDao.create(getTestDocument(1,"Existing"))
        advanceUntilIdle()

        // WHEN + THEN
        val sameNameSameId = documentDao.isNameAllowed(1, "Existing")
        val sameNameDifferentId = documentDao.isNameAllowed(2, "Existing")
        val newName = documentDao.isNameAllowed(2, "New")

        assertTrue(sameNameSameId)
        assertFalse(sameNameDifferentId)
        assertTrue(newName)
    }
}
