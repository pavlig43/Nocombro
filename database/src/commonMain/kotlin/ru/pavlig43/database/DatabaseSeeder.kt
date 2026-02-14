package ru.pavlig43.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.emptyDate
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.dao.BatchDao
import ru.pavlig43.database.data.batch.dao.BatchMovementDao
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.declaration.dao.DeclarationDao
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.expense.dao.ExpenseDao
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.FileDao
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.dao.CompositionDao
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.dao.BuyDao
import ru.pavlig43.database.data.transact.dao.TransactionDao
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.reminder.dao.ReminderDao
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.dao.VendorDao
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Suppress("LongMethod", "TooGenericExceptionCaught", "SwallowedException")
suspend fun seedDatabase(db: NocombroDatabase) {
    try {
        db.productDao.getProduct(1)
        return // База уже инициализирована
    } catch (_: Exception) {
        // База пуста, заполняем данными
    }

    // 1. === VENDOR (5 записей) ===
    val vendors = listOf(
        Vendor(displayName = "Ингре", comment = "Поставщик инградиентов", id = 1),
        Vendor(displayName = "Стоинг", comment = "Оптовый склад", id = 2),
        Vendor(displayName = "Рустарк", comment = "Российские товары", id = 3),
        Vendor(displayName = "Агро-Союз", comment = "Сельхозпродукция", id = 4),
        Vendor(displayName = "ФудМастер", comment = "Пищевые добавки", id = 5)
    )

    // 2. === PRODUCT (5 записей) ===
    val products = listOf(
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Соль",
            createdAt = LocalDate.fromEpochDays(0),
            comment = "Пищевая соль",
            id = 1
        ),
        Product(
            type = ProductType.FOOD_PF,
            displayName = "Баварские колбаски",
            createdAt = LocalDate.fromEpochDays(0),
            comment = "Полуфабрикат",
            id = 2
        ),
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Декстроза",
            createdAt = LocalDate.fromEpochDays(0),
            comment = "Глюкоза",
            id = 3
        ),
        Product(
            type = ProductType.PACK,
            displayName = "Пленка ПЭТ",
            createdAt = LocalDate.fromEpochDays(0),
            comment = "Упаковочный материал",
            id = 4
        ),
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Мука пшеничная",
            createdAt = LocalDate.fromEpochDays(0),
            comment = "Высший сорт",
            id = 5
        )
    )

    // 3. === DECLARATION (5 записей) ===
    val declarations = listOf(
        Declaration(
            displayName = "Декларация ингре",
            createdAt = LocalDate.fromEpochDays(0),
            vendorId = 1,
            vendorName = "Ингре",
            bestBefore = LocalDate.fromEpochDays(365),
            id = 1,
            observeFromNotification = true,
            bornDate = emptyDate
        ),
        Declaration(
            displayName = "Декларация стоинг",
            createdAt = LocalDate.fromEpochDays(0),
            vendorId = 2,
            vendorName = "Стоинг",
            bestBefore = LocalDate.fromEpochDays(180),
            id = 2,
            observeFromNotification = true,
            bornDate = emptyDate
        ),
        Declaration(
            displayName = "Декларация рустарк",
            createdAt = LocalDate.fromEpochDays(0),
            vendorId = 3,
            vendorName = "Рустарк",
            bestBefore = LocalDate.fromEpochDays(270),
            id = 3,
            observeFromNotification = true,
            bornDate = emptyDate
        ),
        Declaration(
            displayName = "Декларация агро",
            createdAt = LocalDate.fromEpochDays(0),
            vendorId = 4,
            vendorName = "Агро-Союз",
            bestBefore = LocalDate.fromEpochDays(90),
            id = 4,
            observeFromNotification = false,
            bornDate = emptyDate
        ),
        Declaration(
            displayName = "Декларация фуд",
            createdAt = LocalDate.fromEpochDays(0),
            vendorId = 5,
            vendorName = "ФудМастер",
            bestBefore = LocalDate.fromEpochDays(540),
            id = 5,
            observeFromNotification = true,
            bornDate = emptyDate
        )
    )

    // 4. === DOCUMENT (5 записей) ===
    val documents = listOf(
        Document(
            displayName = "ГОСТ 12345-2000",
            type = DocumentType.GOST,
            createdAt = LocalDate(2024, 1, 15),
            comment = "Государственный стандарт",
            id = 1
        ),
        Document(
            displayName = "Спецификация №001",
            type = DocumentType.SPECIFICATION,
            createdAt = LocalDate(2024, 2, 20),
            comment = "Техническая спецификация",
            id = 2
        ),
        Document(
            displayName = "ГОСТ 54321-2018",
            type = DocumentType.GOST,
            createdAt = LocalDate(2024, 3, 10),
            comment = "Стандарт качества",
            id = 3
        ),
        Document(
            displayName = "Спецификация №002",
            type = DocumentType.SPECIFICATION,
            createdAt = LocalDate(2024, 4, 5),
            comment = "Производственная спецификация",
            id = 4
        ),
        Document(
            displayName = "ГОСТ 67890-2022",
            type = DocumentType.GOST,
            createdAt = LocalDate(2024, 5, 12),
            comment = "Новый стандарт",
            id = 5
        )
    )

    // 5. === FILE (5 записей) ===
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

    // 6. === PRODUCT_DECLARATION (5 записей) ===
    val productDeclarations = listOf(
        ProductDeclarationIn(productId = 2, declarationId = 1, id = 1),
        ProductDeclarationIn(productId = 2, declarationId = 2, id = 2),
        ProductDeclarationIn(productId = 2, declarationId = 3, id = 3),
        ProductDeclarationIn(productId = 1, declarationId = 1, id = 4),
        ProductDeclarationIn(productId = 5, declarationId = 4, id = 5)
    )

    // 7. === COMPOSITION (5 записей) ===
    val compositions = listOf(
        CompositionIn(parentId = 2, productId = 1, count = 500, id = 1), // Баварские содержат 500г соли
        CompositionIn(parentId = 2, productId = 3, count = 200, id = 2), // Баварские содержат 200г декстрозы
        CompositionIn(parentId = 2, productId = 5, count = 1000, id = 3), // Баварские содержат 1кг муки
        CompositionIn(parentId = 5, productId = 1, count = 10, id = 4), // Мука содержит соль
        CompositionIn(parentId = 5, productId = 3, count = 50, id = 5) // Мука содержит декстрозу
    )

    // 8. === TRANSACT (5 записей) ===
    val transactions = listOf(
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2024, 6, 1, 10, 30),
            comment = "Закупка сырья",
            isCompleted = true,
            id = 1
        ),
        Transact(
            transactionType = TransactionType.SALE,
            createdAt = LocalDateTime(2024, 6, 5, 14, 0),
            comment = "Продажа партии",
            isCompleted = true,
            id = 2
        ),
        Transact(
            transactionType = TransactionType.WRITE_OFF,
            createdAt = LocalDateTime(2024, 6, 10, 9, 15),
            comment = "Списание испорченного товара",
            isCompleted = false,
            id = 3
        ),
        Transact(
            transactionType = TransactionType.INVENTORY,
            createdAt = LocalDateTime(2024, 6, 15, 12, 0),
            comment = "Ежемесячная инвентаризация",
            isCompleted = false,
            id = 4
        ),
        Transact(
            transactionType = TransactionType.OPZS,
            createdAt = LocalDateTime(2024, 6, 20, 16, 45),
            comment = "Отчёт для ОПЗС",
            isCompleted = true,
            id = 5
        )
    )

    // 9. === BATCH (5 записей) ===
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

    // 10. === BATCH_MOVEMENT (5 записей) ===
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

    // 11. === BUY (5 записей) ===
    val buys = listOf(
        BuyBDIn(
            transactionId = 1,
            movementId = 1,
            price = 15000, // 150.00 рублей
            comment = "Закупка соли",
            id = 1
        ),
        BuyBDIn(
            transactionId = 1,
            movementId = 2,
            price = 45000, // 450.00 рублей
            comment = "Закупка колбасок",
            id = 2
        ),
        BuyBDIn(
            transactionId = 1,
            movementId = 4,
            price = 8000, // 80.00 рублей
            comment = "Закупка декстрозы",
            id = 3
        ),
        BuyBDIn(
            transactionId = 2,
            movementId = 3,
            price = 18000, // 180.00 рублей
            comment = "Продажа оптом",
            id = 4
        ),
        BuyBDIn(
            transactionId = 3,
            movementId = 5,
            price = 2000, // 20.00 рублей
            comment = "Списание",
            id = 5
        )
    )

    // 12. === REMINDER (5 записей) ===
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

    // 13. === EXPENSE (5 записей) ===
    val expenses = listOf(
        ExpenseBD(
            transactionId = 1,
            expenseType = ExpenseType.TRANSPORT_DELIVERY,
            amount = 5000, // 50.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 1, 11, 0),
            comment = "Доставка товара",
            id = 1
        ),
        ExpenseBD(
            transactionId = 2,
            expenseType = ExpenseType.TRANSPORT_GASOLINE,
            amount = 3500, // 35.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 5, 15, 0),
            comment = "Бензин для доставки",
            id = 2
        ),
        ExpenseBD(
            transactionId = null,
            expenseType = ExpenseType.STATIONERY,
            amount = 1200, // 12.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 7, 10, 30),
            comment = "Канцелярия для офиса",
            id = 3
        ),
        ExpenseBD(
            transactionId = null,
            expenseType = ExpenseType.TRANSPORT_DEPRECIATION,
            amount = 10000, // 100.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 10, 9, 0),
            comment = "Амортизация авто за июнь",
            id = 4
        ),
        ExpenseBD(
            transactionId = 1,
            expenseType = ExpenseType.COMMISSION,
            amount = 5000, // 50.00 рублей
            expenseDateTime = LocalDateTime(2024, 6, 1, 12, 0),
            comment = "Комиссия посреднику",
            id = 5
        )
    )

    // === ЗАПИСЬ В БАЗУ В ПРАВИЛЬНОМ ПОРЯДКЕ ===

    // Сначала базовые сущности без зависимостей
    vendors.forEach { db.vendorDao.create(it) }
    products.forEach { db.productDao.create(it) }
    documents.forEach { db.documentDao.create(it) }
    transactions.forEach { db.transactionDao.create(it) }

    // Затем сущности с зависимостями
    declarations.forEach { db.declarationDao.create(it) }
    db.fileDao.upsertFiles(files)

    // Связующие таблицы
    db.productDeclarationDao.upsertProductDeclarations(productDeclarations)
    db.compositionDao.upsertComposition(compositions)

    // Партии
    batches.forEach { db.batchDao.createBatch(it) }

    // Движения партий
    db.batchMovementDao.insertMovements(batchMovements)

    // Покупки, напоминания, расходы
    buys.forEach { db.buyDao.upsertBuyBd(it) }
    db.reminderDao.upsertAll(reminders)
    db.expenseDao.upsertAll(expenses)
}
