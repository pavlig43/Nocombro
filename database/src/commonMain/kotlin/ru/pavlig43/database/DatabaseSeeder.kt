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
            createdAt = LocalDate(2024, 1, 15),
            comment = "",
            id = 1
        ),
        Document(
            displayName = "Спецификация №001",
            type = DocumentType.SPECIFICATION,
            createdAt = LocalDate(2024, 2, 20),
            comment = "",
            id = 2
        ),
        Document(
            displayName = "ГОСТ 54321-2018",
            type = DocumentType.GOST,
            createdAt = LocalDate(2024, 3, 10),
            comment = "",
            id = 3
        ),
        Document(
            displayName = "Спецификация №002",
            type = DocumentType.SPECIFICATION,
            createdAt = LocalDate(2024, 4, 5),
            comment = "",
            id = 4
        ),
        Document(
            displayName = "ГОСТ 67890-2022",
            type = DocumentType.GOST,
            createdAt = LocalDate(2024, 5, 12),
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
        CompositionIn(parentId = 2, productId = 1, count = 700, id = 1), //
        CompositionIn(parentId = 2, productId = 3, count = 250, id = 2), //
        CompositionIn(parentId = 2, productId = 5, count = 50, id = 3), //
    )

    // 8. ===
    val transactions = listOf(
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2024, 6, 1, 10, 30),
            comment = "",
            isCompleted = true,
            id = 1
        ),
        Transact(
            transactionType = TransactionType.SALE,
            createdAt = LocalDateTime(2024, 6, 5, 14, 0),
            comment = "",
            isCompleted = true,
            id = 2
        ),
        Transact(
            transactionType = TransactionType.WRITE_OFF,
            createdAt = LocalDateTime(2024, 6, 10, 9, 15),
            comment = "",
            isCompleted = false,
            id = 3
        ),
        Transact(
            transactionType = TransactionType.INVENTORY,
            createdAt = LocalDateTime(2024, 6, 15, 12, 0),
            comment = "",
            isCompleted = false,
            id = 4
        ),
        Transact(
            transactionType = TransactionType.OPZS,
            createdAt = LocalDateTime(2024, 6, 20, 16, 45),
            comment = "",
            isCompleted = true,
            id = 5
        )
    )

    // 9. ===
    val batches = listOf(
        BatchBD(
            productId = 1,
            dateBorn = LocalDate(2024, 5, 15),
            declarationId = 1,
            id = 1
        ),
        BatchBD(
            productId = 2,
            dateBorn = LocalDate(2024, 5, 20),
            declarationId = 2,
            id = 2
        ),
        BatchBD(
            productId = 3,
            dateBorn = LocalDate(2024, 5, 25),
            declarationId = 1,
            id = 3
        ),
        BatchBD(
            productId = 4,
            dateBorn = LocalDate(2024, 6, 1),
            declarationId = 5,
            id = 4
        ),
        BatchBD(
            productId = 5,
            dateBorn = LocalDate(2024, 6, 5),
            declarationId = 4,
            id = 5
        )
    )

    // 10. ===
    val batchMovements = listOf(
        BatchMovement(
            batchId = 1,
            movementType = MovementType.INCOMING,
            count = 100,
            transactionId = 1,
            id = 1
        ),
        BatchMovement(
            batchId = 2,
            movementType = MovementType.INCOMING,
            count = 50,
            transactionId = 1,
            id = 2
        ),
        BatchMovement(
            batchId = 2,
            movementType = MovementType.OUTGOING,
            count = 20,
            transactionId = 2,
            id = 3
        ),
        BatchMovement(
            batchId = 3,
            movementType = MovementType.INCOMING,
            count = 200,
            transactionId = 1,
            id = 4
        ),
        BatchMovement(
            batchId = 1,
            movementType = MovementType.OUTGOING,
            count = 10,
            transactionId = 3,
            id = 5
        )
    )

    // 11. ===
    val buys = listOf(
        BuyBDIn(
            transactionId = 1,
            movementId = 1,
            price = 15000, // 150.00 рублей
            comment = "",
            id = 1
        ),
        BuyBDIn(
            transactionId = 1,
            movementId = 2,
            price = 45000, // 450.00 рублей
            comment = "",
            id = 2
        ),
        BuyBDIn(
            transactionId = 1,
            movementId = 4,
            price = 8000, // 80.00 рублей
            comment = "",
            id = 3
        ),
        BuyBDIn(
            transactionId = 2,
            movementId = 3,
            price = 18000, // 180.00 рублей
            comment = "",
            id = 4
        ),
        BuyBDIn(
            transactionId = 3,
            movementId = 5,
            price = 2000, // 20.00 рублей
            comment = "",
            id = 5
        )
    )

    // 12. ===
    val reminders = listOf(
        ReminderBD(
            transactionId = 1,
            text = "Проверить качество поставки",
            reminderDateTime = LocalDateTime(2024, 6, 2, 10, 0),
            id = 1
        ),
        ReminderBD(
            transactionId = 2,
            text = "Оплатить счёт поставщику",
            reminderDateTime = LocalDateTime(2024, 6, 6, 14, 0),
            id = 2
        ),
        ReminderBD(
            transactionId = 3,
            text = "Подать акт списания",
            reminderDateTime = LocalDateTime(2024, 6, 11, 9, 0),
            id = 3
        ),
        ReminderBD(
            transactionId = 4,
            text = "Завершить инвентаризацию",
            reminderDateTime = LocalDateTime(2024, 6, 16, 18, 0),
            id = 4
        ),
        ReminderBD(
            transactionId = 5,
            text = "Отправить отчёт в налоговую",
            reminderDateTime = LocalDateTime(2024, 6, 25, 12, 0),
            id = 5
        )
    )

    // 13. ===
    val expenses = listOf(
        ExpenseBD(
            transactionId = 1,
            expenseType = ExpenseType.TRANSPORT_DELIVERY,
            amount = 5000, // 50.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 1, 11, 0),
            comment = "",
            id = 1
        ),
        ExpenseBD(
            transactionId = 2,
            expenseType = ExpenseType.TRANSPORT_GASOLINE,
            amount = 3500, // 35.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 5, 15, 0),
            comment = "",
            id = 2
        ),
        ExpenseBD(
            transactionId = null,
            expenseType = ExpenseType.STATIONERY,
            amount = 1200, // 12.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 7, 10, 30),
            comment = "",
            id = 3
        ),
        ExpenseBD(
            transactionId = null,
            expenseType = ExpenseType.TRANSPORT_DEPRECIATION,
            amount = 10000, // 100.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 10, 9, 0),
            comment = "",
            id = 4
        ),
        ExpenseBD(
            transactionId = 1,
            expenseType = ExpenseType.COMMISSION,
            amount = 5000, // 50.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 1, 12, 0),
            comment = "",
            id = 5
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
