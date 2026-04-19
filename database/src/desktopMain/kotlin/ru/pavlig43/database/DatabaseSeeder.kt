@file:Suppress("MagicNumber")
package ru.pavlig43.database

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.sale.SaleBDIn
import ru.pavlig43.database.data.vendor.Vendor
import kotlin.math.roundToLong
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

    // === РЕАЛЬНЫЕ ДАННЫЕ ИЗ ДАМПА ===

    // 1. VENDORS (7 записей)
    val vendors = listOf(
        Vendor(displayName = "Стоинг", comment = "", id = 1),
        Vendor(displayName = "Ингремарт", comment = "", id = 2),
        Vendor(displayName = "Рога и копыта", comment = "", id = 3),
        Vendor(displayName = "Оптовик", comment = "", id = 4),
        Vendor(displayName = "ИП Гармаш", comment = "", id = 5),
        Vendor(displayName = "ОЗОН", comment = "", id = 6),
        Vendor(displayName = "Высший Вкус", comment = "", id = 7)
    )

    // 2. PRODUCTS (5 записей)
    val products = listOf(
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Соль",
            secondName = "",
            createdAt = LocalDate(2025, 1, 7),
            comment = "",
            priceForSale = 3000,
            id = 1
        ),
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Декстроза",
            secondName = "",
            createdAt = LocalDate(2026, 3, 8),
            comment = "",
            priceForSale = 0,
            id = 2
        ),
        Product(
            type = ProductType.FOOD_PF,
            displayName = "Колбаски Баварские",
            secondName = "",
            createdAt = LocalDate(2026, 3, 8),
            comment = "",
            priceForSale = 120000,
            id = 3
        ),
        Product(
            type = ProductType.FOOD_BASE,
            displayName = "Ароматизатор Перец",
            secondName = "",
            createdAt = LocalDate(2026, 3, 9),
            comment = "",
            priceForSale = 150000,
            id = 4
        ),
        Product(
            type = ProductType.PACK,
            displayName = "Пакет 3кг",
            secondName = "",
            createdAt = LocalDate(2026, 3, 9),
            comment = "",
            priceForSale = 4500,
            id = 5
        )
    )

    // 3. DECLARATIONS (6 записей)
    val declarations = listOf(
        Declaration(
            displayName = "Декларация Стоинг",
            createdAt = LocalDate(2026, 3, 7),
            vendorId = 1,
            vendorName = "Стоинг",
            bestBefore = LocalDate(2027, 3, 7),
            id = 1,
            observeFromNotification = true,
            bornDate = LocalDate(2025, 3, 7)
        ),
        Declaration(
            displayName = "Ингремарт",
            createdAt = LocalDate(2026, 3, 8),
            vendorId = 2,
            vendorName = "Ингремарт",
            bestBefore = LocalDate(2027, 3, 8),
            id = 2,
            observeFromNotification = true,
            bornDate = LocalDate(2025, 2, 8)
        ),
        Declaration(
            displayName = "Декл рога и копыта ",
            createdAt = LocalDate(2026, 3, 8),
            vendorId = 3,
            vendorName = "Рога и копыта",
            bestBefore = LocalDate(2021, 2, 8),
            id = 3,
            observeFromNotification = false,
            bornDate = LocalDate(2025, 3, 8)
        ),
        Declaration(
            displayName = "Оптовик",
            createdAt = LocalDate(2026, 3, 8),
            vendorId = 4,
            vendorName = "Оптовик",
            bestBefore = LocalDate(2028, 3, 8),
            id = 4,
            observeFromNotification = true,
            bornDate = LocalDate(2025, 3, 8)
        ),
        Declaration(
            displayName = "ИП Гармаш",
            createdAt = LocalDate(2026, 3, 9),
            vendorId = 5,
            vendorName = "ИП Гармаш",
            bestBefore = LocalDate(2028, 3, 9),
            id = 5,
            observeFromNotification = true,
            bornDate = LocalDate(2025, 3, 9)
        ),
        Declaration(
            displayName = "ОЗОН",
            createdAt = LocalDate(2026, 3, 9),
            vendorId = 6,
            vendorName = "ОЗОН",
            bestBefore = LocalDate(2028, 3, 9),
            id = 6,
            observeFromNotification = true,
            bornDate = LocalDate(2026, 3, 9)
        )
    )

    // 4. PRODUCT_DECLARATIONS (5 записей)
    val productDeclarations = listOf(
        ProductDeclarationIn(productId = 1, declarationId = 1, id = 1),
        ProductDeclarationIn(productId = 2, declarationId = 2, id = 2),
        ProductDeclarationIn(productId = 4, declarationId = 4, id = 3),
        ProductDeclarationIn(productId = 5, declarationId = 6, id = 4),
        ProductDeclarationIn(productId = 3, declarationId = 5, id = 5)
    )

    // 5. SAFETY_STOCK (5 записей)
    val safetyStock = listOf(
        SafetyStock(productId = 1, reorderPoint = 30000, orderQuantity = 30000, id = 1),
        SafetyStock(productId = 2, reorderPoint = 50000, orderQuantity = 50000, id = 2),
        SafetyStock(productId = 4, reorderPoint = 10000, orderQuantity = 10000, id = 3),
        SafetyStock(productId = 5, reorderPoint = 1000000, orderQuantity = 1000000, id = 4),
        SafetyStock(productId = 3, reorderPoint = 36000, orderQuantity = 12000, id = 5)
    )

    // 6. COMPOSITIONS (4 записи)
    val compositions = listOf(
        CompositionIn(parentId = 3, productId = 1, count = 450, id = 1),
        CompositionIn(parentId = 3, productId = 2, count = 450, id = 2),
        CompositionIn(parentId = 3, productId = 4, count = 100, id = 3),
        CompositionIn(parentId = 3, productId = 5, count = 333, id = 4)
    )

    // 6.1. PRODUCT_SPECIFICATIONS (1 запись)
    val productSpecifications = listOf(
        ProductSpecification(
            productId = 3,
            dosage = "4-15 г на 1 кг фарша",
            composition = "декстроза, соль пищевая, экстракт муската, экстракт мяты.",
            shelfLifeText = "18 месяцев.",
            storageConditions = "Хранить при температуре не выше 25°C и относительной влажности 75%.",
            appearance = "Порошок.",
            physicalChemicalIndicators = """
                Массовая доля влаги, %, не более: 7,0.
                Массовая доля металлических примесей, %, не более: 0,001.
                Посторонние примеси: Не допускается.
            """.trimIndent(),
            microbiologicalIndicators = """
                КМАФАнМ, КОЕ/г, не более: 500000.
                БГКП (колиформы), в 0,01 г: не допускается.
                Патогенные, в том числе сальмонеллы, в 25 г: не допускается.
                Сульфитредуцирующие клостридии, в 0,01 г: не допускается.
                Плесени, КОЕ/г, не более: 200.
            """.trimIndent(),
            toxicElements = """
                Свинец, мг/кг, не более: 5,0.
                Мышьяк, мг/кг, не более: 3,0.
                Кадмий, мг/кг, не более: 0,2.
            """.trimIndent(),
            allergens = """
                Злаки, содержащие глютен, и их производные; нет; да
                Горчица и ее производные; нет; да
                Молоко и молочные продукты; нет; нет
                Сельдерей и его производные; нет; да
                Яйца и их производные; нет; нет
                Кунжут - семена и производные; нет; нет
                Ракообразные и продукты из них; нет; нет
                Соевые бобы и продукты из них; нет; да
                Орехи и продукты их переработки; нет; да
                Арахис и продукты его переработки; нет; нет
                Молюски и продукты их переработки; нет; нет
                Люпин и продукты его переработки; нет; нет
            """.trimIndent(),
            gmoInfo = "Данный продукт не содержит генетически модифицированных объектов (ГМО), а также не производится из генетически модифицированных источников сырья.",
            id = 1,
        )
    )

    // 7. TRANSACTIONS (11 записей)
    val transactions = listOf(
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 3, 6, 20, 17),
            comment = "",
            isCompleted = true,
            id = 1
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 3, 10, 20, 18, 33, 96344400),
            comment = "",
            isCompleted = true,
            id = 2
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 3, 2, 20, 4),
            comment = "",
            isCompleted = true,
            id = 3
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 3, 10, 20, 19, 49, 312855600),
            comment = "",
            isCompleted = true,
            id = 4
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 3, 7, 20, 20),
            comment = "",
            isCompleted = true,
            id = 5
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 3, 10, 20, 21, 12, 142261800),
            comment = "",
            isCompleted = true,
            id = 6
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 3, 10, 20, 21, 40, 848346),
            comment = "",
            isCompleted = true,
            id = 7
        ),
        Transact(
            transactionType = TransactionType.BUY,
            createdAt = LocalDateTime(2026, 2, 10, 20, 22),
            comment = "",
            isCompleted = true,
            id = 8
        ),
        Transact(
            transactionType = TransactionType.OPZS,
            createdAt = LocalDateTime(2026, 3, 8, 20, 11),
            comment = "",
            isCompleted = true,
            id = 9
        ),
        Transact(
            transactionType = TransactionType.OPZS,
            createdAt = LocalDateTime(2026, 3, 10, 20, 32, 55, 950382600),
            comment = "",
            isCompleted = true,
            id = 10
        ),
        Transact(
            transactionType = TransactionType.SALE,
            createdAt = LocalDateTime(2026, 3, 10, 21, 35, 58, 794665500),
            comment = "",
            isCompleted = true,
            id = 11
        )
    )

    // 8. BATCHES (10 записей)
    val batches = listOf(
        BatchBD(productId = 1, dateBorn = LocalDate(2026, 2, 10), declarationId = 1, id = 1),
        BatchBD(productId = 1, dateBorn = LocalDate(2026, 2, 10), declarationId = 1, id = 2),
        BatchBD(productId = 4, dateBorn = LocalDate(2026, 2, 6), declarationId = 4, id = 3),
        BatchBD(productId = 4, dateBorn = LocalDate(2026, 2, 18), declarationId = 4, id = 4),
        BatchBD(productId = 5, dateBorn = LocalDate(2025, 12, 10), declarationId = 6, id = 5),
        BatchBD(productId = 5, dateBorn = LocalDate(2026, 3, 10), declarationId = 6, id = 6),
        BatchBD(productId = 2, dateBorn = LocalDate(2026, 2, 18), declarationId = 2, id = 7),
        BatchBD(productId = 2, dateBorn = LocalDate(2026, 2, 11), declarationId = 2, id = 8),
        BatchBD(productId = 3, dateBorn = LocalDate(2026, 3, 10), declarationId = 5, id = 9),
        BatchBD(productId = 3, dateBorn = LocalDate(2026, 3, 10), declarationId = 5, id = 10)
    )

    // 9. BATCH_MOVEMENTS (22 записи)
    val batchMovements = listOf(
        BatchMovement(batchId = 1, movementType = MovementType.INCOMING, count = 30000, transactionId = 1, id = 1),
        BatchMovement(batchId = 2, movementType = MovementType.INCOMING, count = 50000, transactionId = 2, id = 2),
        BatchMovement(batchId = 3, movementType = MovementType.INCOMING, count = 9000, transactionId = 3, id = 3),
        BatchMovement(batchId = 4, movementType = MovementType.INCOMING, count = 9000, transactionId = 4, id = 4),
        BatchMovement(batchId = 5, movementType = MovementType.INCOMING, count = 1000000, transactionId = 5, id = 5),
        BatchMovement(batchId = 6, movementType = MovementType.INCOMING, count = 500000, transactionId = 6, id = 6),
        BatchMovement(batchId = 7, movementType = MovementType.INCOMING, count = 100000, transactionId = 7, id = 7),
        BatchMovement(batchId = 8, movementType = MovementType.INCOMING, count = 50000, transactionId = 8, id = 8),
        BatchMovement(batchId = 9, movementType = MovementType.INCOMING, count = 36000, transactionId = 9, id = 9),
        BatchMovement(batchId = 1, movementType = MovementType.OUTGOING, count = 16200, transactionId = 9, id = 10),
        BatchMovement(batchId = 8, movementType = MovementType.OUTGOING, count = 16200, transactionId = 9, id = 11),
        BatchMovement(batchId = 3, movementType = MovementType.OUTGOING, count = 3600, transactionId = 9, id = 12),
        BatchMovement(batchId = 5, movementType = MovementType.OUTGOING, count = 12000, transactionId = 9, id = 13),
        BatchMovement(batchId = 10, movementType = MovementType.INCOMING, count = 18000, transactionId = 10, id = 14),
        BatchMovement(batchId = 1, movementType = MovementType.OUTGOING, count = 8100, transactionId = 10, id = 15),
        BatchMovement(batchId = 8, movementType = MovementType.OUTGOING, count = 8100, transactionId = 10, id = 16),
        BatchMovement(batchId = 3, movementType = MovementType.OUTGOING, count = 1800, transactionId = 10, id = 17),
        BatchMovement(batchId = 5, movementType = MovementType.OUTGOING, count = 6000, transactionId = 10, id = 18),
        BatchMovement(batchId = 10, movementType = MovementType.OUTGOING, count = 18000, transactionId = 11, id = 19),
        BatchMovement(batchId = 9, movementType = MovementType.OUTGOING, count = 15000, transactionId = 11, id = 20),
        BatchMovement(batchId = 10, movementType = MovementType.OUTGOING, count = 18000, transactionId = 11, id = 21),
        BatchMovement(batchId = 9, movementType = MovementType.OUTGOING, count = 15000, transactionId = 11, id = 22)
    )

    // 10. BUYS (8 записей)
    val buys = listOf(
        BuyBDIn(transactionId = 1, movementId = 1, price = 3000, comment = "", id = 1),
        BuyBDIn(transactionId = 2, movementId = 2, price = 3500, comment = "", id = 2),
        BuyBDIn(transactionId = 3, movementId = 3, price = 105000, comment = "", id = 3),
        BuyBDIn(transactionId = 4, movementId = 4, price = 110000, comment = "", id = 4),
        BuyBDIn(transactionId = 5, movementId = 5, price = 2855, comment = "", id = 5),
        BuyBDIn(transactionId = 6, movementId = 6, price = 2260, comment = "", id = 6),
        BuyBDIn(transactionId = 7, movementId = 7, price = 9235, comment = "", id = 7),
        BuyBDIn(transactionId = 8, movementId = 8, price = 9530, comment = "", id = 8)
    )

    // 11. SALES (4 записи)
    val sales = listOf(
        SaleBDIn(transactionId = 11, movementId = 19, price = 120000, comment = "", clientId = 7, id = 1),
        SaleBDIn(transactionId = 11, movementId = 20, price = 120000, comment = "", clientId = 7, id = 2),
        SaleBDIn(transactionId = 11, movementId = 21, price = 120000, comment = "", clientId = 7, id = 3),
        SaleBDIn(transactionId = 11, movementId = 22, price = 120000, comment = "", clientId = 7, id = 4)
    )

    // 12. REMINDERS (1 запись)
    val reminders = listOf(
        ReminderBD(
            transactionId = 11,
            text = "деньги",
            reminderDateTime = LocalDateTime(2026, 4, 1, 0, 0),
            id = 1
        )
    )

    // 13. EXPENSES (1 запись)
    val expenses = listOf(
        ExpenseBD(
            transactionId = 11,
            expenseType = ExpenseType.TRANSPORT_GASOLINE,
            amount = 125230,
            expenseDateTime = LocalDateTime(2026, 3, 10, 21, 35, 58, 794665500),
            comment = "",
            id = 1
        )
    )

    // === ЗАПИСЬ В БАЗУ ===

    vendors.forEach { db.vendorDao.create(it) }
    products.forEach { db.productDao.create(it) }
    transactions.forEach { db.transactionDao.create(it) }
    declarations.forEach { db.declarationDao.create(it) }
    db.productDeclarationDao.upsertProductDeclarations(productDeclarations)
    db.compositionDao.upsertComposition(compositions)
    productSpecifications.forEach { db.productSpecificationDao.upsert(it) }
    safetyStock.forEach { db.safetyStockDao.upsert(it) }
    batches.forEach { db.batchDao.createBatch(it) }
    db.batchMovementDao.upsertMovements(batchMovements)
    buys.forEach { db.buyDao.upsertBuyBd(it) }
    sales.forEach { db.saleDao.upsertSaleBd(it) }
    db.expenseDao.upsertAll(expenses)
    db.reminderDao.upsertAll(reminders)
    db.batchCostDao.upsert(
        buildBatchCostPrices(
            transactions = transactions,
            batchMovements = batchMovements,
            buys = buys,
            expenses = expenses,
        )
    )
}

private fun buildBatchCostPrices(
    transactions: List<Transact>,
    batchMovements: List<BatchMovement>,
    buys: List<BuyBDIn>,
    expenses: List<ExpenseBD>,
): List<BatchCostPriceEntity> {
    val movementById = batchMovements.associateBy { it.id }
    val expensesByTransactionId = expenses.groupBy { it.transactionId }
    val costByBatchId = mutableMapOf<Int, Long>()

    val buyCosts = transactions
        .filter { it.transactionType == TransactionType.BUY }
        .flatMap { transaction ->
            val transactionBuys = buys.filter { it.transactionId == transaction.id }
            if (transactionBuys.isEmpty()) return@flatMap emptyList()

            val transactionExpenses = expensesByTransactionId[transaction.id].orEmpty().sumOf { it.amount }
            val quantityAmount = transactionBuys.sumOf { buy ->
                movementById[buy.movementId]?.count ?: 0L
            }
            val expenseOnOneKg = if (quantityAmount > 0) {
                (transactionExpenses / quantityAmount.toDouble()) * 1000
            } else 0.0

            transactionBuys.mapNotNull { buy ->
                val movement = movementById[buy.movementId] ?: return@mapNotNull null
                val cost = (buy.price + expenseOnOneKg).roundToLong()
                costByBatchId[movement.batchId] = cost
                BatchCostPriceEntity(
                    batchId = movement.batchId,
                    costPricePerUnit = cost,
                )
            }
        }

    val opzsTransactions = transactions.filter { it.transactionType == TransactionType.OPZS }
    val opzsCosts = buildList {
        val remaining = opzsTransactions.toMutableList()
        while (remaining.isNotEmpty()) {
            var progress = false
            val iterator = remaining.iterator()

            while (iterator.hasNext()) {
                val transaction = iterator.next()
                val transactionMovements = batchMovements.filter { it.transactionId == transaction.id }
                val incomingMovements = transactionMovements.filter { it.movementType == MovementType.INCOMING }
                val ingredientMovements = transactionMovements.filter { it.movementType == MovementType.OUTGOING }
                if (incomingMovements.isEmpty()) {
                    iterator.remove()
                    progress = true
                    continue
                }
                if (ingredientMovements.any { it.batchId !in costByBatchId }) continue

                val incomingMovement = incomingMovements.first()
                val totalCost = ingredientMovements.sumOf { ingredient ->
                    val costPerKg = costByBatchId[ingredient.batchId] ?: 0L
                    (costPerKg * ingredient.count) / 1000.0
                }
                val costPricePerUnit = if (incomingMovement.count > 0) {
                    ((totalCost / incomingMovement.count.toDouble()) * 1000).roundToLong()
                } else 0L

                costByBatchId[incomingMovement.batchId] = costPricePerUnit
                add(
                    BatchCostPriceEntity(
                        batchId = incomingMovement.batchId,
                        costPricePerUnit = costPricePerUnit,
                    )
                )
                iterator.remove()
                progress = true
            }

            if (!progress) {
                error("Не удалось вычислить себестоимость для всех OPZS-партий в seedDatabase.")
            }
        }
    }

    return buyCosts + opzsCosts
}
