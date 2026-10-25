package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.money.*
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.sync.SyncAnalysisHtmlReportFormatter
import ru.pavlig43.database.data.sync.SyncAnalysisReportData
import ru.pavlig43.database.data.sync.mirror.*
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import java.text.DecimalFormat
import java.math.BigDecimal

class SyncAnalysisHtmlReportTest : DesktopMainDispatcherFunSpec({
    test("all 22 typed tables appear once in both directions including full additions and deletions") {
        val all = reportFixture()
        all.size shouldBe 22
        for (push in listOf(true, false)) {
            val added = reportHtml(if (push) all else emptyMap(), if (push) emptyMap() else all)
            all.keys.forEach { table -> added.countText("data-record=\"${table.tableName}\"") shouldBe 1 }
            added.countText("Добавление\n") shouldBe 22
            added shouldContain "class=\"added\""
            added shouldNotContain "<td class=\"removed\">"
            val tombstones = all.mapValues { (_, rows) -> rows.map { it.markDeleted(newTime) } }
            val deleted = reportHtml(if (push) tombstones else all, if (push) all else tombstones)
            all.keys.forEach { table -> deleted.countText("data-record=\"${table.tableName}\"") shouldBe 1 }
            deleted.countText("Удаление\n") shouldBe 22
            deleted shouldContain "<td class=\"removed\">"
            deleted shouldContain "Название файла"
            deleted shouldContain "состав 1"
            deleted shouldContain "Физико-химические показатели"
        }
    }

    test("both sides resolve renamed relationships separately and only changed cells are yellow") {
        val old = reportFixture()
        val changed = old.toMutableMap().apply {
            put(MirrorSyncTable.VENDOR, listOf((old.getValue(MirrorSyncTable.VENDOR).single() as VendorMirrorRow).copy(displayName = "Новое имя", updatedAt = newTime)))
            put(MirrorSyncTable.BUY, listOf((old.getValue(MirrorSyncTable.BUY).single() as BuyMirrorRow).copy(price = 12345, updatedAt = newTime)))
        }
        val html = reportHtml(changed, old)
        val row = html.tables().single { "Комментарий покупки" in it }
        row shouldContain "Поставщик 1"
        row shouldContain "Новое имя"
        row shouldContain "<td class=\"changed\">"
        // Количество принадлежит неизменённому движению; цена — изменённой покупке.
        Regex("<td[^>]*>[^<]*кг</td>").findAll(row).all { "class=" !in it.value } shouldBe true
        row shouldNotContain "Движение партии: Изменение"
        html.countText("data-record=\"buy\"") shouldBe 1
        html.countText("data-record=\"transact\"") shouldBe 0
    }

    test("parent edit never marks children changed and child edit shows a neutral parent") {
        val old = reportFixture()
        val product = old.getValue(MirrorSyncTable.PRODUCT).single() as ProductMirrorRow
        val onlyProduct = old + (MirrorSyncTable.PRODUCT to listOf(product.copy(comment = "Правка", updatedAt = newTime)))
        val html = reportHtml(onlyProduct, old)
        html.countText("data-record=") shouldBe 1
        html shouldNotContain "<caption>Партия"
        val spec = old.getValue(MirrorSyncTable.PRODUCT_SPECIFICATION).single() as ProductSpecificationMirrorRow
        val childHtml = reportHtml(old + (MirrorSyncTable.PRODUCT_SPECIFICATION to listOf(spec.copy(dosage = "другая", updatedAt = newTime))), old)
        childHtml shouldContain "</span> Товар 1</h3>"
        childHtml shouldNotContain "<caption>Товар\n"
        childHtml.tables().size shouldBe 1
        childHtml.countText("data-record=") shouldBe 1
    }

    test("all tables support conflicts and restoration without selecting a winner") {
        val local = reportFixture(n = 1)
        val remote = reportFixture(n = 2)
        val conflicts = local.flatMap { (table, rows) -> rows.mapIndexed { i, row -> MirrorVersionConflict(table, row, remote.getValue(table)[i]) } }
        val html = SyncAnalysisHtmlReportFormatter().format(MirrorReconciliationPreview(
            MirrorLocalSnapshot(oldTime, local), MirrorRemoteSnapshot(oldTime, remote),
            MirrorReconciliationPlan(emptyList(), emptyList(), conflicts),
        ))
        html.countText("Конфликт\n") shouldBe 22
        html shouldContain "На компьютере"
        html shouldContain "В облаке"
        html shouldContain "<td class=\"changed\">"
        html shouldNotContain "Победитель"
        val deleted = local.mapValues { (_, rows) -> rows.map { it.markDeleted(oldTime) } }
        val restored = reportHtml(reportFixture(at = newTime), deleted)
        restored.countText("Восстановление\n") shouldBe 22
        restored shouldNotContain "<td class=\"changed\">"
    }

    test("version-only changes keep plan counts but do not create business rows or context blocks") {
        for (push in listOf(true, false)) {
            val html = if (push) reportHtml(reportFixture(at = newTime), reportFixture())
                else reportHtml(reportFixture(), reportFixture(at = newTime))
            html shouldContain "${if (push) "Будет отправлено" else "Будет получено"} <small>(22)"
            html shouldContain "Изменений данных: 0. Служебных обновлений: 22"
            html shouldNotContain "Данные не изменились"
            html shouldNotContain "<table>"
            html shouldNotContain "<article"
            html shouldNotContain "Товар 1"
        }
    }

    test("missing dependencies are visible outside action counters and without UUID") {
        val remote = reportFixture().filterKeys { it == MirrorSyncTable.BUY }
        val local = MirrorSyncTable.entries.associateWith { emptyList<MirrorSyncRow>() }
        val html = reportHtml(local, remote)
        html shouldContain "Будет получено <small>(0)"
        html shouldContain "Не включено в план <small>(1)"
        html shouldContain "Связанная запись недоступна"
        html shouldNotContain "secret-id-"
        html shouldContain "Зависимость отсутствует"
    }

    test("hiding service versions never hides an actual date or text change") {
        val old = reportFixture()
        val original = old.getValue(MirrorSyncTable.TRANSACTION).single() as TransactionMirrorRow
        val withSeconds = original.copy(createdAt = LocalDateTime(2026, 10, 25, 10, 0, 1), updatedAt = newTime)
        val html = reportHtml(old + (MirrorSyncTable.TRANSACTION to listOf(withSeconds)), old)
        html.countText("data-record=\"transact\"") shouldBe 1
        html shouldContain "25.10.2026 10:00:01"
        html shouldContain "<td class=\"changed\">"
        val empty = original.copy(comment = "")
        val literal = empty.copy(comment = "пусто", updatedAt = newTime)
        val textHtml = reportHtml(old + (MirrorSyncTable.TRANSACTION to listOf(literal)), old + (MirrorSyncTable.TRANSACTION to listOf(empty)))
        textHtml.countText("data-record=\"transact\"") shouldBe 1
        textHtml shouldContain "<td class=\"changed\">пусто</td>"
    }

    test("moving a child uses the new parent while retaining the old parent in Before") {
        val old = reportFixture()
        val parent = (old.getValue(MirrorSyncTable.PRODUCT).single() as ProductMirrorRow).copy(syncId = "new-parent", displayName = "Новый родитель")
        val spec = old.getValue(MirrorSyncTable.PRODUCT_SPECIFICATION).single() as ProductSpecificationMirrorRow
        val after = old + mapOf(MirrorSyncTable.PRODUCT to (old.getValue(MirrorSyncTable.PRODUCT) + parent),
            MirrorSyncTable.PRODUCT_SPECIFICATION to listOf(spec.copy(productSyncId = parent.syncId, updatedAt = newTime)))
        val html = reportHtml(after, old)
        html shouldContain "</span> Новый родитель</h3>"
        val record = html.tables().single { "<caption>Спецификация" in it }
        record shouldContain "Товар 1"
        record shouldContain "Новый родитель"
        html.countText("data-record=\"product_specification\"") shouldBe 1
    }

    test("defaults distinguish zero false empty null and inaccessible and preserve Long precision") {
        val fixture = reportFixture()
        val movement = (fixture.getValue(MirrorSyncTable.BATCH_MOVEMENT).single() as BatchMovementMirrorRow).copy(count = Long.MAX_VALUE)
        movement.storageLocation shouldBe StorageLocation.MAIN
        val fields = SyncAnalysisReportData(fixture).fields(movement)
        fields.single { it.title == "Склад" }.text shouldBe "Основной"
        fields.single { it.title == "Количество" }.text shouldBe DecimalFormat("#,##0.###").format(BigDecimal.valueOf(Long.MAX_VALUE, 3)) + " кг"
        val transaction = (fixture.getValue(MirrorSyncTable.TRANSACTION).single() as TransactionMirrorRow).copy(comment = "", isCompleted = false)
        val values = SyncAnalysisReportData(fixture).fields(transaction).map { it.text }
        values.contains("пусто") shouldBe true
        values.contains("нет") shouldBe true
        val expense = (fixture.getValue(MirrorSyncTable.EXPENSE).single() as ExpenseMirrorRow).copy(amount = 0)
        SyncAnalysisReportData(fixture).fields(expense).single { it.title == "Сумма" }.text shouldBe "0 ₽"
    }

    test("sales have identical columns whether their batch changes or is only context") {
        val old = reportFixture().toMutableMap().apply {
            put(MirrorSyncTable.BATCH_MOVEMENT, emptyList())
            put(MirrorSyncTable.BUY, emptyList())
            put(MirrorSyncTable.SALE, emptyList())
        }
        val fixture = reportFixture()
        val batch = fixture.getValue(MirrorSyncTable.BATCH).single() as BatchMirrorRow
        val otherBatch = batch.copy(syncId = "new-batch", dateBorn = LocalDate(2026, 10, 26), updatedAt = newTime)
        val movement = fixture.getValue(MirrorSyncTable.BATCH_MOVEMENT).single() as BatchMovementMirrorRow
        val otherMovement = movement.copy(syncId = "other-movement", batchSyncId = otherBatch.syncId)
        val sale = fixture.getValue(MirrorSyncTable.SALE).single() as SaleMirrorRow
        val otherSale = sale.copy(syncId = "other-sale", movementSyncId = otherMovement.syncId)
        val after = old + mapOf(
            MirrorSyncTable.BATCH to listOf(batch, otherBatch),
            MirrorSyncTable.BATCH_MOVEMENT to listOf(movement, otherMovement),
            MirrorSyncTable.SALE to listOf(sale, otherSale),
        )
        val html = reportHtml(after, old)
        val rows = html.tables().filter { "<caption>Движение партии / Продажа" in it }
        rows.size shouldBe 2
        fun header(table: String) = Regex("<thead>.*?</thead>", RegexOption.DOT_MATCHES_ALL).find(table)!!.value
        header(rows[0]) shouldBe header(rows[1])
        header(rows[0]) shouldNotContain "Изменение партии"
        header(rows[0]) shouldNotContain "Родитель"
        html shouldContain "Будет добавлена партия:"
        html shouldNotContain "Будет изменена партия:"
        rows[0] shouldNotContain "Будет добавлена партия:"
        rows[1] shouldNotContain "Будет добавлена партия:"
    }

    test("escapes user text, hides technical metadata, and supplies working internal targets") {
        val fixture = reportFixture()
        val product = (fixture.getValue(MirrorSyncTable.PRODUCT).single() as ProductMirrorRow).copy(displayName = "<script>alert('x') & \"text\"</script>")
        val html = reportHtml(fixture + (MirrorSyncTable.PRODUCT to listOf(product)), emptyMap())
        html shouldContain "&lt;script&gt;alert(&#39;x&#39;) &amp; &quot;text&quot;&lt;/script&gt;"
        html shouldNotContain "<script>"
        html shouldNotContain "secret-id-"
        html shouldNotContain "C:/secret/path"
        html shouldNotContain "s3-secret-key"
        html shouldContain "overflow-x:auto"
        html shouldContain "border-top:5px"
        val ids = Regex("id=\"([^\"]+)\"").findAll(html).map { it.groupValues[1] }.toList()
        ids.distinct().size shouldBe ids.size
        Regex("href=\"#([^\"]+)\"").findAll(html).forEach { ids.contains(it.groupValues[1]) shouldBe true }
        // Общая декларация — полная строка один раз; ссылки остаются в партиях и товарах.
        html.countText("data-record=\"declaration\"") shouldBe 1
        html shouldContain "Будет добавлена декларация"
    }

    test("OPZS output precedes ingredients and transfer pairs actual outgoing and incoming") {
        val base = reportFixture()
        val movement = base.getValue(MirrorSyncTable.BATCH_MOVEMENT).single() as BatchMovementMirrorRow
        val outgoing = movement.copy(syncId = "outgoing", movementType = MovementType.OUTGOING)
        val incoming = movement.copy(syncId = "incoming", movementType = MovementType.INCOMING)
        val transaction = base.getValue(MirrorSyncTable.TRANSACTION).single() as TransactionMirrorRow
        fun html(type: TransactionType) = reportHtml(base + mapOf(
            MirrorSyncTable.TRANSACTION to listOf(transaction.copy(transactionType = type)),
            MirrorSyncTable.BATCH_MOVEMENT to listOf(outgoing, incoming),
            MirrorSyncTable.BUY to emptyList(), MirrorSyncTable.SALE to emptyList()), emptyMap())
        val opzs = html(TransactionType.OPZS)
        (opzs.indexOf(">Приход</td>") < opzs.indexOf(">Расход</td>")) shouldBe true
        val transfer = html(TransactionType.STORAGE_TRANSFER)
        (transfer.indexOf(">Расход</td>") < transfer.indexOf(">Приход</td>")) shouldBe true
    }

    test("deletion uses saved content and labels fallback when saved name is unavailable") {
        val remote = VendorMirrorRow("secret-id-vendor", "Имя перед удалением", "текст", oldTime)
        val saved = remote.copy(displayName = "Последнее имя", updatedAt = newTime, deletedAt = newTime)
        reportHtml(mapOf(MirrorSyncTable.VENDOR to listOf(saved)), mapOf(MirrorSyncTable.VENDOR to listOf(remote))) shouldContain "Последнее имя"
        val fallback = reportHtml(mapOf(MirrorSyncTable.VENDOR to listOf(saved.copy(displayName = "", comment = ""))), mapOf(MirrorSyncTable.VENDOR to listOf(remote)))
        fallback shouldContain "Последнее содержимое: из облака"
        fallback shouldContain "Имя перед удалением"
        val unavailable = reportHtml(mapOf(MirrorSyncTable.VENDOR to listOf(saved.copy(displayName = "", comment = ""))), emptyMap())
        unavailable shouldContain "Последнее содержимое недоступно"
    }

    test("new stock transfers show one neutral product row and only warehouse change in yellow") {
        val fixture = reportFixture()
        val transaction = fixture.getValue(MirrorSyncTable.TRANSACTION).single() as TransactionMirrorRow
        val movement = fixture.getValue(MirrorSyncTable.BATCH_MOVEMENT).single() as BatchMovementMirrorRow
        val outgoing = movement.copy(syncId = "outgoing", movementType = MovementType.OUTGOING, count = 500)
        val incoming = movement.copy(syncId = "incoming", storageLocation = StorageLocation.EXPERIMENTAL, count = 500)
        val after = fixture + mapOf(
            MirrorSyncTable.TRANSACTION to listOf(transaction.copy(transactionType = TransactionType.STORAGE_TRANSFER)),
            MirrorSyncTable.BATCH_MOVEMENT to listOf(outgoing, incoming),
            MirrorSyncTable.BUY to emptyList(), MirrorSyncTable.SALE to emptyList(),
        )
        for (push in listOf(true, false)) {
            val html = if (push) reportHtml(after, emptyMap()) else reportHtml(emptyMap(), after)
            val transfer = html.tables().single { "<caption>Перенос между складами" in it }
            transfer.countText("data-record=\"batch_movement\"") shouldBe 2
            transfer.countText("<td class=\"changed\">") shouldBe 2
            transfer shouldNotContain "<td class=\"added\">"
            transfer shouldNotContain "<td class=\"removed\">"
            transfer shouldContain "<td class=\"changed\">Основной</td>"
            transfer shouldContain "<td class=\"changed\">Для экспериментов</td>"
            transfer shouldContain "<td class=\"action\">Перенос</td>"
            transfer.countText("Товар 1") shouldBe 1
            transfer.countText("кг</td>") shouldBe 1
            html shouldContain "${if (push) "Будет отправлено" else "Будет получено"} <small>(${after.values.sumOf { it.size }})"
        }
    }

    test("incomplete mismatched and ambiguous transfer movements remain separate") {
        val fixture = reportFixture()
        val transaction = fixture.getValue(MirrorSyncTable.TRANSACTION).single() as TransactionMirrorRow
        val movement = fixture.getValue(MirrorSyncTable.BATCH_MOVEMENT).single() as BatchMovementMirrorRow
        val outgoing = movement.copy(syncId = "outgoing", movementType = MovementType.OUTGOING)
        val incoming = movement.copy(syncId = "incoming", storageLocation = StorageLocation.EXPERIMENTAL)
        val cases = listOf(listOf(outgoing), listOf(outgoing, incoming.copy(count = 100)),
            listOf(outgoing, incoming, outgoing.copy(syncId = "another-outgoing")))
        cases.forEach { movements ->
            val after = fixture + mapOf(
                MirrorSyncTable.TRANSACTION to listOf(transaction.copy(transactionType = TransactionType.STORAGE_TRANSFER)),
                MirrorSyncTable.BATCH_MOVEMENT to movements,
                MirrorSyncTable.BUY to emptyList(), MirrorSyncTable.SALE to emptyList(),
            )
            val html = reportHtml(after, emptyMap())
            html shouldNotContain "<caption>Перенос между складами"
            html.countText("data-record=\"batch_movement\"") shouldBe movements.size
        }
    }

    test("blocks follow document dates regardless of sync identifiers and version dates") {
        val early = TransactionMirrorRow("z-early", TransactionType.BUY, LocalDateTime(2026, 10, 10, 12, 0), "", true, updatedAt = newTime)
        val late = TransactionMirrorRow("a-late", TransactionType.SALE, LocalDateTime(2026, 10, 20, 12, 0), "", true, updatedAt = oldTime)
        val expense = ExpenseMirrorRow("expense", expenseType = ExpenseType.OTHER, amount = 100,
            expenseDateTime = LocalDateTime(2026, 10, 15, 12, 0), comment = "", updatedAt = oldTime)
        val dictionary = reportFixture().getValue(MirrorSyncTable.PRODUCT)
        val rows = mapOf(MirrorSyncTable.PRODUCT to dictionary, MirrorSyncTable.TRANSACTION to listOf(late, early),
            MirrorSyncTable.EXPENSE to listOf(expense))
        for (push in listOf(true, false)) {
            val html = if (push) reportHtml(rows, emptyMap()) else reportHtml(emptyMap(), rows)
            val headings = Regex("<h3>.*?</h3>", RegexOption.DOT_MATCHES_ALL).findAll(html).map { it.value }.toList()
            headings.size shouldBe 4
            headings[0] shouldContain "Покупка, 10.10.2026 12:00"
            headings[1] shouldContain "Расход"
            headings[2] shouldContain "Продажа, 20.10.2026 12:00"
            headings[3] shouldContain "Товар 1"
            html.countText("data-record=\"transact\"") shouldBe 2
        }
    }
})

private val oldTime = LocalDateTime(2026, 10, 25, 10, 0)
private val newTime = LocalDateTime(2026, 10, 25, 11, 0)
private val reportDate = LocalDate(2026, 10, 25)
private fun String.countText(text: String) = Regex(Regex.escape(text)).findAll(this).count()
private fun String.tables() = Regex("<table>.*?</table>", RegexOption.DOT_MATCHES_ALL).findAll(this).map { it.value }.toList()
private fun reportHtml(local: Map<MirrorSyncTable, List<MirrorSyncRow>>, remote: Map<MirrorSyncTable, List<MirrorSyncRow>>): String {
    val l = MirrorLocalSnapshot(newTime, local)
    val r = MirrorRemoteSnapshot(newTime, remote)
    return SyncAnalysisHtmlReportFormatter().format(MirrorReconciliationPreview(l, r, MirrorReconciliationPlanner().plan(l, r)))
}

private fun reportFixture(n: Int = 1, at: LocalDateTime = oldTime): Map<MirrorSyncTable, List<MirrorSyncRow>> {
    fun id(name: String) = "secret-id-$name"
    return mapOf(
        MirrorSyncTable.VENDOR to listOf(VendorMirrorRow(id("vendor"), "Поставщик $n", "комментарий $n", at)),
        MirrorSyncTable.DOCUMENT to listOf(DocumentMirrorRow(id("document"), "Документ $n", DocumentType.TU, reportDate, "комментарий $n", at)),
        MirrorSyncTable.DECLARATION to listOf(DeclarationMirrorRow(id("declaration"), "Декларация $n", reportDate, id("vendor"), "Поставщик $n", reportDate, reportDate, true, at)),
        MirrorSyncTable.PRODUCT to listOf(ProductMirrorRow(id("product"), ProductType.FOOD_PF, "Товар $n", "второе $n", reportDate, "комментарий $n", n * 100L, n, n, at)),
        MirrorSyncTable.TRANSACTION to listOf(TransactionMirrorRow(id("transaction"), TransactionType.BUY, oldTime, "комментарий $n", true, updatedAt = at)),
        MirrorSyncTable.EXPERIMENT to listOf(ExperimentMirrorRow(id("experiment"), "Опыт $n", "идея $n", false, at)),
        MirrorSyncTable.PRODUCT_SPECIFICATION to listOf(ProductSpecificationMirrorRow(id("spec"), id("product"), "дозировка $n", "состав $n", "срок $n", "хранение $n", "вид $n", "цвет $n", "запах $n", "вкус $n", "физ $n", "микро $n", "токс $n", "аллерг $n", "гмо $n", at)),
        MirrorSyncTable.SAFETY_STOCK to listOf(SafetyStockMirrorRow(id("stock"), id("product"), n * 1000L, n * 2000L, at)),
        MirrorSyncTable.EXPERIMENT_ENTRY to listOf(ExperimentEntryMirrorRow(id("entry"), id("experiment"), reportDate, oldTime, "запись $n", at)),
        MirrorSyncTable.EXPERIMENT_REMINDER to listOf(ExperimentReminderMirrorRow(id("exp-reminder"), id("experiment"), "напоминание $n", oldTime, at)),
        MirrorSyncTable.PRODUCT_DECLARATION to listOf(ProductDeclarationMirrorRow(id("product-declaration"), id("product"), id("declaration"), n == 1, at)),
        MirrorSyncTable.COMPOSITION to listOf(CompositionMirrorRow(id("composition"), id("product"), id("product"), n * 100L, at)),
        MirrorSyncTable.BATCH to listOf(BatchMirrorRow(id("batch"), id("product"), reportDate, id("declaration"), at)),
        MirrorSyncTable.BATCH_COST_PRICE to listOf(BatchCostPriceMirrorRow(id("cost"), id("batch"), n * 100L, at)),
        MirrorSyncTable.BATCH_MOVEMENT to listOf(BatchMovementMirrorRow(id("movement"), id("batch"), MovementType.INCOMING, n * 1000L, id("transaction"), updatedAt = at)),
        MirrorSyncTable.REMINDER to listOf(ReminderMirrorRow(id("reminder"), id("transaction"), "напоминание $n", oldTime, at)),
        MirrorSyncTable.EXPENSE to listOf(ExpenseMirrorRow(id("expense"), id("transaction"), ExpenseType.OTHER, n * 100L, oldTime, "комментарий $n", at)),
        MirrorSyncTable.BUY to listOf(BuyMirrorRow(id("buy"), id("transaction"), id("movement"), n * 100L, "покупка $n", n, at)),
        MirrorSyncTable.SALE to listOf(SaleMirrorRow(id("sale"), id("transaction"), id("movement"), n * 100L, "продажа $n", id("vendor"), n, at)),
        MirrorSyncTable.MONEY_ACCOUNT to listOf(MoneyAccountMirrorRow(id("account"), "Счёт $n", MoneyAccountType.BANK, oldTime, false, at)),
        MirrorSyncTable.MONEY_MOVEMENT to listOf(MoneyMovementMirrorRow(id("money"), MoneyMovementKind.TRANSFER, null, n * 100L, oldTime, id("account"), id("account"), "получатель $n", "перевод $n", at)),
        MirrorSyncTable.FILE to listOf(FileMirrorRow(id("file"), OwnerType.PRODUCT, id("product"), "Файл $n", "C:/secret/path", "s3-secret-key", updatedAt = at)),
    )
}
