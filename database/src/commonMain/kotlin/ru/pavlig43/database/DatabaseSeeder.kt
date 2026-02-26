@file:Suppress("MagicNumber")
package ru.pavlig43.database

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import ru.pavlig43.core.emptyDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.vendor.Vendor
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Suppress("LongMethod", "TooGenericExceptionCaught", "SwallowedException")
suspend fun seedDatabase(db: NocombroDatabase) {
    try {
        db.productDao.getProduct(1)
        return //
    } catch (_: Exception) {
        //
    }

    // 1. ===
    val vendors = listOf(
        Vendor(displayName = "NOCOMBRO", comment = "", id = 1),
        Vendor(displayName = "Стоинг", comment = "", id = 2),
        Vendor(displayName = "Рустарк", comment = "", id = 3),
        Vendor(displayName = "Агро-Союз", comment = "", id = 4),
        Vendor(displayName = "Ingre", comment = "", id = 5)
    )

    // 2. ===
    val products = listOf(
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Соль",
            createdAt = getCurrentLocalDate(),
            comment = "",
            id = 2
        ),
        Product(
            type = ProductType.FOOD_PF,
            displayName = "Баварские колбаски",
            createdAt = getCurrentLocalDate(),
            comment = "",
            id = 1
        ),
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Декстроза",
            createdAt = getCurrentLocalDate(),
            comment = "",
            id = 3
        ),
        Product(
            type = ProductType.PACK,
            displayName = "Пленка ПЭТ",
            createdAt = getCurrentLocalDate(),
            comment = "",
            id = 4
        ),
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Ароматизатор мясо",
            createdAt = getCurrentLocalDate(),
            comment = "",
            id = 5
        )
    )

    // 3. ===
    val declarations = listOf(
        Declaration(
            displayName = "Декларация ингре",
            createdAt = getCurrentLocalDate(),
            vendorId = 1,
            vendorName = "Ингре",
            bestBefore = getCurrentLocalDate().plus(10, DateTimeUnit.DAY),
            id = 1,
            observeFromNotification = true,
            bornDate = getCurrentLocalDate().minus(5, DateTimeUnit.DAY)
        ),
        Declaration(
            displayName = "Декларация стоинг",
            createdAt = getCurrentLocalDate(),
            vendorId = 2,
            vendorName = "Стоинг",
            bestBefore = getCurrentLocalDate().plus(10, DateTimeUnit.DAY),
            id = 2,
            observeFromNotification = true,
            bornDate = getCurrentLocalDate().minus(5, DateTimeUnit.DAY)
        ),
        Declaration(
            displayName = "Декларация рустарк",
            createdAt = getCurrentLocalDate(),
            vendorId = 3,
            vendorName = "Рустарк",
            bestBefore = getCurrentLocalDate().plus(10, DateTimeUnit.DAY),
            id = 3,
            observeFromNotification = true,
            bornDate = getCurrentLocalDate().minus(5, DateTimeUnit.DAY)
        ),
        Declaration(
            displayName = "Декларация агро",
            createdAt = getCurrentLocalDate(),
            vendorId = 4,
            vendorName = "Агро-Союз",
            bestBefore = getCurrentLocalDate().plus(10, DateTimeUnit.DAY),
            id = 4,
            observeFromNotification = false,
            bornDate = getCurrentLocalDate().minus(5, DateTimeUnit.DAY)
        ),
        Declaration(
            displayName = "Декларация фуд",
            createdAt = getCurrentLocalDate(),
            vendorId = 5,
            vendorName = "ФудМастер",
            bestBefore = getCurrentLocalDate().plus(10, DateTimeUnit.DAY),
            id = 5,
            observeFromNotification = true,
            bornDate = getCurrentLocalDate().minus(5, DateTimeUnit.DAY)
        )
    )

    // 4. ===
    val documents = listOf(
        Document(
            displayName = "ГОСТ 12345-2000",
            type = DocumentType.GOST,
            createdAt = LocalDate(2026, 1, 15),
            comment = "",
            id = 1
        ),
        Document(
            displayName = "Спецификация №001",
            type = DocumentType.SPECIFICATION,
            createdAt = LocalDate(2026, 2, 20),
            comment = "",
            id = 2
        ),
        Document(
            displayName = "ГОСТ 54321-2018",
            type = DocumentType.GOST,
            createdAt = LocalDate(2026, 1, 10),
            comment = "",
            id = 3
        ),
        Document(
            displayName = "Спецификация №002",
            type = DocumentType.SPECIFICATION,
            createdAt = LocalDate(2026, 1, 5),
            comment = "",
            id = 4
        ),
        Document(
            displayName = "ГОСТ 67890-2022",
            type = DocumentType.GOST,
            createdAt = LocalDate(2026, 2, 1),
            comment = "",
            id = 5
        )
    )

    // 5. ===
    val files = listOf(
        FileBD(
            ownerId = 1,
            ownerFileType = OwnerType.PRODUCT,
            path = "/storage/images/salt.jpg",
            id = 1
        ),
        FileBD(
            ownerId = 1,
            ownerFileType = OwnerType.DECLARATION,
            path = "/storage/docs/decl1.pdf",
            id = 2
        ),
        FileBD(
            ownerId = 3,
            ownerFileType = OwnerType.VENDOR,
            path = "/storage/vendor/rustark_cert.pdf",
            id = 3
        ),
        FileBD(
            ownerId = 1,
            ownerFileType = OwnerType.DOCUMENT,
            path = "/storage/docs/gost_12345.pdf",
            id = 4
        ),
        FileBD(
            ownerId = 2,
            ownerFileType = OwnerType.PRODUCT,
            path = "/storage/images/sausage.jpg",
            id = 5
        )
    )

    // 6. ===
    val productDeclarations = listOf(
        ProductDeclarationIn(productId = 2, declarationId = 1, id = 1),
        ProductDeclarationIn(productId = 2, declarationId = 2, id = 2),
        ProductDeclarationIn(productId = 2, declarationId = 3, id = 3),
        ProductDeclarationIn(productId = 1, declarationId = 1, id = 4),
        ProductDeclarationIn(productId = 5, declarationId = 4, id = 5)
    )

    // 7. ===
    val compositions = listOf(
        CompositionIn(parentId = 1, productId = 2, count = 700, id = 1),
        CompositionIn(parentId = 1, productId = 3, count = 250, id = 2),
        CompositionIn(parentId = 1, productId = 5, count = 50, id = 3)
    )

    // 8. ===
    val transactions = listOf(
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 1, 10, 9, 0),
            comment = "",
            isCompleted = true,
            id = 1
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 1, 15, 10, 30),
            comment = "",
            isCompleted = true,
            id = 2
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 1, 20, 11, 15),
            comment = "",
            isCompleted = true,
            id = 3
        ),
        Transact(
            transactionType = TransactionType.OPZS,
            createdAt = LocalDateTime(2026, 2, 6, 10, 0),
            comment = "",
            isCompleted = true,
            id = 4
        ),
        Transact(
            transactionType = TransactionType.OPZS,
            createdAt = LocalDateTime(2026, 2, 10, 10, 0),
            comment = "",
            isCompleted = true,
            id = 5
        ),
        Transact(
            transactionType = TransactionType.OPZS,
            createdAt = LocalDateTime(2026, 2, 15, 10, 0),
            comment = "",
            isCompleted = true,
            id = 6
        ),
        Transact(
            transactionType = TransactionType.SALE,
            createdAt = LocalDateTime(2026, 2, 20, 14, 0),
            comment = "",
            isCompleted = true,
            id = 7
        ),
        Transact(
            transactionType = TransactionType.WRITE_OFF,
            createdAt = LocalDateTime(2026, 2, 25, 9, 15),
            comment = "",
            isCompleted = false,
            id = 8
        )
    )

    // 9. ===
    val batches = listOf(
        BatchBD(productId = 2, dateBorn = LocalDate(2026, 1, 5), declarationId = 1, id = 1),
        BatchBD(productId = 2, dateBorn = LocalDate(2026, 1, 6), declarationId = 1, id = 2),
        BatchBD(productId = 2, dateBorn = LocalDate(2026, 1, 7), declarationId = 1, id = 3),
        BatchBD(productId = 3, dateBorn = LocalDate(2026, 1, 8), declarationId = 1, id = 4),
        BatchBD(productId = 3, dateBorn = LocalDate(2026, 1, 9), declarationId = 1, id = 5),
        BatchBD(productId = 3, dateBorn = LocalDate(2026, 1, 10), declarationId = 1, id = 6),
        BatchBD(productId = 5, dateBorn = LocalDate(2026, 1, 11), declarationId = 4, id = 7),
        BatchBD(productId = 5, dateBorn = LocalDate(2026, 1, 12), declarationId = 4, id = 8),
        BatchBD(productId = 5, dateBorn = LocalDate(2026, 1, 13), declarationId = 4, id = 9),
        BatchBD(productId = 1, dateBorn = LocalDate(2026, 2, 6), declarationId = 1, id = 10),
        BatchBD(productId = 1, dateBorn = LocalDate(2026, 2, 10), declarationId = 1, id = 11),
        BatchBD(productId = 1, dateBorn = LocalDate(2026, 2, 15), declarationId = 1, id = 12)
    )

    // 10. ===
    val batchMovements = listOf(
        BatchMovement(batchId = 1, movementType = MovementType.INCOMING, count = 25000, transactionId = 1, id = 1),
        BatchMovement(batchId = 2, movementType = MovementType.INCOMING, count = 30000, transactionId = 1, id = 2),
        BatchMovement(batchId = 3, movementType = MovementType.INCOMING, count = 27500, transactionId = 1, id = 3),
        BatchMovement(batchId = 4, movementType = MovementType.INCOMING, count = 22000, transactionId = 2, id = 4),
        BatchMovement(batchId = 5, movementType = MovementType.INCOMING, count = 25000, transactionId = 2, id = 5),
        BatchMovement(batchId = 6, movementType = MovementType.INCOMING, count = 24000, transactionId = 2, id = 6),
        BatchMovement(batchId = 7, movementType = MovementType.INCOMING, count = 21000, transactionId = 3, id = 7),
        BatchMovement(batchId = 8, movementType = MovementType.INCOMING, count = 23000, transactionId = 3, id = 8),
        BatchMovement(batchId = 9, movementType = MovementType.INCOMING, count = 20050, transactionId = 3, id = 9),
        BatchMovement(batchId = 10, movementType = MovementType.INCOMING, count = 25000, transactionId = 4, id = 10),
        BatchMovement(batchId = 1, movementType = MovementType.OUTGOING, count = 17500, transactionId = 4, id = 11),
        BatchMovement(batchId = 4, movementType = MovementType.OUTGOING, count = 6250, transactionId = 4, id = 12),
        BatchMovement(batchId = 7, movementType = MovementType.OUTGOING, count = 1250, transactionId = 4, id = 13),
        BatchMovement(batchId = 11, movementType = MovementType.INCOMING, count = 30000, transactionId = 5, id = 14),
        BatchMovement(batchId = 2, movementType = MovementType.OUTGOING, count = 21000, transactionId = 5, id = 15),
        BatchMovement(batchId = 5, movementType = MovementType.OUTGOING, count = 7500, transactionId = 5, id = 16),
        BatchMovement(batchId = 8, movementType = MovementType.OUTGOING, count = 1500, transactionId = 5, id = 17),
        BatchMovement(batchId = 12, movementType = MovementType.INCOMING, count = 22000, transactionId = 6, id = 18),
        BatchMovement(batchId = 3, movementType = MovementType.OUTGOING, count = 15400, transactionId = 6, id = 19),
        BatchMovement(batchId = 6, movementType = MovementType.OUTGOING, count = 5500, transactionId = 6, id = 20),
        BatchMovement(batchId = 9, movementType = MovementType.OUTGOING, count = 1100, transactionId = 6, id = 21)
    )

    // 11. ===
    val buys = listOf(
        BuyBDIn(transactionId = 1, movementId = 1, price = 250000, comment = "", id = 1),
        BuyBDIn(transactionId = 1, movementId = 2, price = 300000, comment = "", id = 2),
        BuyBDIn(transactionId = 1, movementId = 3, price = 275000, comment = "", id = 3),
        BuyBDIn(transactionId = 2, movementId = 4, price = 180000, comment = "", id = 4),
        BuyBDIn(transactionId = 2, movementId = 5, price = 200000, comment = "", id = 5),
        BuyBDIn(transactionId = 2, movementId = 6, price = 192000, comment = "", id = 6),
        BuyBDIn(transactionId = 3, movementId = 7, price = 504000, comment = "", id = 7),
        BuyBDIn(transactionId = 3, movementId = 8, price = 552000, comment = "", id = 8),
        BuyBDIn(transactionId = 3, movementId = 9, price = 481200, comment = "", id = 9)
    )

    // 12. ===
    val reminders = listOf(
        ReminderBD(
            transactionId = 1,
            text = "",
            reminderDateTime = LocalDateTime(2026, 1, 11, 9, 0),
            id = 1
        ),
        ReminderBD(
            transactionId = 2,
            text = "",
            reminderDateTime = LocalDateTime(2026, 1, 16, 10, 0),
            id = 2
        ),
        ReminderBD(
            transactionId = 3,
            text = "",
            reminderDateTime = LocalDateTime(2026, 1, 21, 11, 0),
            id = 3
        ),
        ReminderBD(
            transactionId = 4,
            text = "",
            reminderDateTime = LocalDateTime(2026, 2, 7, 9, 0),
            id = 4
        ),
        ReminderBD(
            transactionId = 5,
            text = "",
            reminderDateTime = LocalDateTime(2026, 2, 11, 9, 0),
            id = 5
        )
    )

    // 13. ===
    val expenses = listOf(
        ExpenseBD(
            transactionId = 1,
            expenseType = ExpenseType.TRANSPORT_DELIVERY,
            amount = 5000,
            expenseDateTime = LocalDateTime(2026, 1, 10, 10, 0),
            comment = "",
            id = 1
        ),
        ExpenseBD(
            transactionId = 2,
            expenseType = ExpenseType.TRANSPORT_DELIVERY,
            amount = 3500,
            expenseDateTime = LocalDateTime(2026, 1, 15, 11, 0),
            comment = "",
            id = 2
        ),
        ExpenseBD(
            transactionId = 3,
            expenseType = ExpenseType.TRANSPORT_DELIVERY,
            amount = 2000,
            expenseDateTime = LocalDateTime(2026, 1, 20, 12, 0),
            comment = "",
            id = 3
        ),
        ExpenseBD(
            transactionId = null,
            expenseType = ExpenseType.STATIONERY,
            amount = 1200,
            expenseDateTime = LocalDateTime(2026, 2, 5, 10, 0),
            comment = "",
            id = 4
        )
    )

    // === ЗАПИСЬ В БАЗУ ===

    //
    vendors.forEach { db.vendorDao.create(it) }
    products.forEach { db.productDao.create(it) }
    documents.forEach { db.documentDao.create(it) }
    transactions.forEach { db.transactionDao.create(it) }

    //
    declarations.forEach { db.declarationDao.create(it) }
    db.fileDao.upsertFiles(files)

    //
    db.productDeclarationDao.upsertProductDeclarations(productDeclarations)
    db.compositionDao.upsertComposition(compositions)

    //
    batches.forEach { db.batchDao.createBatch(it) }

    //
    db.batchMovementDao.upsertMovements(batchMovements)

    //
    buys.forEach { db.buyDao.upsertBuyBd(it) }
    db.reminderDao.upsertAll(reminders)
    db.expenseDao.upsertAll(expenses)
}
