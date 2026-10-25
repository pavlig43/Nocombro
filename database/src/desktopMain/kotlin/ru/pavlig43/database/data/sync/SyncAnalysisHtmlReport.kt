package ru.pavlig43.database.data.sync

import kotlinx.datetime.format
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.sync.mirror.*
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.datetime.dateTimeFormat

/** Формирует самодостаточный HTML только из уже прочитанного preview. */
internal class SyncAnalysisHtmlReportFormatter {
    fun format(preview: MirrorReconciliationPreview): String {
        val local = SyncAnalysisReportData(preview.localSnapshot.rowsByTable)
        val remote = SyncAnalysisReportData(preview.remoteSnapshot.rowsByTable)
        val sections = listOf(
            ReportSection("Будет отправлено", "push", preview.plan.pushChanges.map {
                ReportChange(ReportKey(it.table, it.row.syncId), remote.rows[ReportKey(it.table, it.row.syncId)], it.row)
            }, remote, local),
            ReportSection("Будет получено", "pull", preview.plan.pullChanges.map {
                ReportChange(ReportKey(it.table, it.row.syncId), local.rows[ReportKey(it.table, it.row.syncId)], it.row)
            }, local, remote),
            ReportSection("Конфликты", "conflict", preview.plan.conflicts.map {
                ReportChange(ReportKey(it.table, it.localRow.syncId), it.localRow, it.remoteRow, conflict = true)
            }, local, remote),
            ReportSection("Не включено в план", "excluded", preview.plan.excludedPullChanges.map {
                ReportChange(ReportKey(it.table, it.row.syncId), local.rows[ReportKey(it.table, it.row.syncId)], it.row, excluded = true)
            }, local, remote),
        ).map { section ->
            val visible = section.changes.filter { it.hasBusinessChanges() }
            section.copy(changes = visible, hiddenVersionCount = section.changes.size - visible.size)
        }
        val anchors = sections.flatMap { section -> section.changes.map { it.key } }.distinct()
            .mapIndexed { index, key -> key to "record-${index + 1}" }.toMap()
        val linkTitles = sections.flatMap { it.changes }.associate { it.key to it.linkTitle() }
        return buildString {
            appendLine("<!doctype html>")
            appendLine("<!-- sync-snapshot-at=${preview.remoteSnapshot.loadedAt} -->")
            appendLine("<html lang=\"ru\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            appendLine("<title>Отчёт синхронизации</title><style>$REPORT_CSS</style></head><body><main>")
            appendLine("<h1>Отчёт синхронизации</h1>")
            appendLine("<p>Снимок компьютера: ${preview.localSnapshot.loadedAt.format(dateTimeFormat)}<br>Снимок облака: ${preview.remoteSnapshot.loadedAt.format(dateTimeFormat)}</p>")
            appendLine("<p class=\"legend\"><span class=\"added\">Добавление / восстановление</span> <span class=\"removed\">Удаление</span> <span class=\"changed\">Изменённое поле / перенос между складами / конфликт</span></p>")
            appendLine("<nav>${sections.joinToString(" · ") { "<a href=\"#${it.id}\">${it.title}</a>" }}</nav>")
            appendLine("<p class=\"service-summary\">Документы — по дате, от ранних к поздним. Товары, партии и справочники — после документов.</p>")
            sections.forEach { section -> appendSection(section, anchors, linkTitles) }
            appendLine("</main></body></html>")
        }
    }

    private fun StringBuilder.appendSection(
        section: ReportSection,
        anchors: Map<ReportKey, String>,
        linkTitles: Map<ReportKey, String>,
    ) {
        appendLine("<section id=\"${section.id}\"><h2>${section.title} <small>(${section.changes.size + section.hiddenVersionCount})</small></h2>")
        if (section.hiddenVersionCount > 0) {
            appendLine("<p class=\"service-summary\">Изменений данных: ${section.changes.size}. Служебных обновлений: ${section.hiddenVersionCount} — без списка.</p>")
        }
        if (section.changes.isEmpty()) {
            appendLine("<p>${if (section.id == "conflict") "Конфликтов нет." else if (section.id == "excluded") "Исключённых строк нет." else if (section.hiddenVersionCount > 0) "Изменений данных нет." else "Расхождений нет."}</p></section>")
            return
        }
        val changed = section.changes.associateBy { it.key }
        val nodes = linkedMapOf<ReportKey, ReportChange>()
        val parents = mutableMapOf<ReportKey, ReportKey>()
        // Только явные предки. Неизменённые потомки не становятся изменениями родителя.
        section.changes.forEach { change ->
            nodes[change.key] = change
            var current = change
            val visited = mutableSetOf(change.key)
            while (true) {
                val parent = section.after.parent(current.row) ?: break
                if (!visited.add(parent)) break
                val parentRow = section.after.rows[parent] ?: break
                parents[current.key] = parent
                val parentChange = changed[parent] ?: ReportChange(parent, section.before.rows[parent], parentRow, context = true)
                nodes.putIfAbsent(parent, parentChange)
                current = parentChange
            }
        }
        // Контекст не может заменить полную изменённую строку.
        nodes.putAll(changed)
        // Изменённое движение показывается одной товарной строкой с его покупкой/продажей.
        section.after.rows.forEach { (key, row) ->
            if (row is BuyMirrorRow || row is SaleMirrorRow) {
                val parent = section.after.parent(row)
                if (parent in nodes && key !in nodes && nodes[parent]?.row?.deletedAt == null) {
                    nodes[key] = ReportChange(key, section.before.rows[key], row, context = true)
                    parents[key] = requireNotNull(parent)
                }
            }
        }
        val children = nodes.values.groupBy { parents[it.key] }
        val rendered = mutableSetOf<ReportKey>()
        fun render(change: ReportChange, depth: Int) {
            if (!rendered.add(change.key)) return
            val childRows = children[change.key].orEmpty()
            val merged = if (change.row is BatchMovementMirrorRow) childRows.filter {
                it.row is BuyMirrorRow || it.row is SaleMirrorRow
            } else emptyList()
            rendered.addAll(merged.map { it.key })
            if (!change.context || depth > 0 || merged.isNotEmpty()) {
                appendRecord(section, listOf(change) + merged, depth, anchors, linkTitles)
            }
            val transferPairs = storageTransferPairs(change.row, childRows.filter { children[it.key].isNullOrEmpty() })
            (childRows - merged.toSet()).sortedWith(recordOrder(change.row)).forEach { child ->
                val pair = transferPairs[child.key]
                if (pair == null) {
                    render(child, depth + 1)
                } else if (rendered.add(pair.first.key)) {
                    rendered.add(pair.second.key)
                    appendStorageTransfer(section, pair.first, pair.second, depth + 1, anchors)
                }
            }
            // Файлы покупки/продажи (при расширении владельцев) остаются под своей строкой.
            merged.forEach { item -> children[item.key].orEmpty().forEach { render(it, depth + 1) } }
        }
        children[null].orEmpty().sortedWith(
            compareBy<ReportChange> { documentDateTime(it.row) == null }
                .thenBy { documentDateTime(it.row) }
                .thenBy { section.after.label(it.key) }
                .thenBy { it.key.table.applyOrder }
                .thenBy { it.key.id }
        ).forEach { root ->
            appendLine("<article class=\"block\"><h3><span class=\"direction\">${section.title}</span> ${escapeHtml(section.after.label(root.key))}</h3>")
            render(root, 0)
            appendLine("</article>")
        }
        // Повреждённые циклические связи не должны скрыть строку.
        nodes.values.filter { it.key !in rendered }.forEach {
            appendLine("<article class=\"block\">"); render(it, 0); appendLine("</article>")
        }
        appendLine("</section>")
    }

    /** Две новые записи одного переноса показываются как смена склада, а не два новых товара. */
    private fun StringBuilder.appendStorageTransfer(
        section: ReportSection,
        outgoing: ReportChange,
        incoming: ReportChange,
        depth: Int,
        anchors: Map<ReportKey, String>,
    ) {
        val from = outgoing.row as BatchMovementMirrorRow
        val to = incoming.row as BatchMovementMirrorRow
        val fields = section.after.fields(from).filterNot { it.title == "Склад" || it.title == "Приход/расход" }
        appendLine("<div class=\"record depth-${depth.coerceAtMost(3)}\"><div class=\"scroll\" tabindex=\"0\" role=\"region\" aria-label=\"Перенос между складами\">")
        appendLine("<table><caption>Перенос между складами")
        listOf(outgoing, incoming).forEach {
            append("<span id=\"${anchors.getValue(it.key)}\" class=\"anchor\" data-record=\"batch_movement\"></span>")
        }
        appendLine("</caption><thead><tr><th rowspan=\"2\" scope=\"col\">Действие</th>")
        fields.forEach { appendLine("<th rowspan=\"2\" scope=\"col\">${escapeHtml(it.title)}</th>") }
        appendLine("<th colspan=\"2\" scope=\"colgroup\">Склад</th></tr><tr><th scope=\"col\">Было</th><th scope=\"col\">Стало</th></tr></thead>")
        appendLine("<tbody><tr><td class=\"action\">Перенос</td>")
        fields.forEach { appendCell(it, "", anchors) }
        appendCell(ReportField("Склад", from.storageLocation.displayName), "changed", anchors)
        appendCell(ReportField("Склад", to.storageLocation.displayName), "changed", anchors)
        appendLine("</tr></tbody></table></div></div>")
    }

    private fun StringBuilder.appendRecord(
        section: ReportSection,
        pieces: List<ReportChange>,
        depth: Int,
        anchors: Map<ReportKey, String>,
        linkTitles: Map<ReportKey, String>,
    ) {
        if (pieces.size == 1 && pieces.single().context) {
            val context = pieces.single()
            val text = escapeHtml("${reportTableTitle(context.key.table)}: ${section.after.label(context.key)}")
            val anchor = anchors[context.key]
            appendLine("<p class=\"context depth-${depth.coerceAtMost(3)}\">${if (anchor == null) text else "<a href=\"#$anchor\">$text</a>"}</p>")
            return
        }
        val cells = pieces.flatMap { piece ->
            val beforeRow = piece.before
            val afterRow = deletionContent(piece)
            val afterData = if (afterRow === piece.row) section.after else section.before
            val oldFields = beforeRow?.let(section.before::fields).orEmpty()
            val newFields = afterData.fields(afterRow)
            val titles = (newFields.map { it.title } + oldFields.map { it.title }).distinct()
            val result = titles.map { title -> ReportCell(piece, oldFields.find { it.title == title }, newFields.find { it.title == title }) }.toMutableList()
            val oldParent = beforeRow?.let(section.before::parent)
            val newParent = section.after.parent(piece.row)
            if (beforeRow != null && oldParent != newParent) {
                result += ReportCell(piece,
                    oldParent?.let { ReportField("Родитель", section.before.label(it), it) },
                    newParent?.let { ReportField("Родитель", section.after.label(it), it) })
            }
            if (piece.conflict) result += ReportCell(piece,
                ReportField("Удалена", if (beforeRow?.deletedAt != null) "да" else "нет"),
                ReportField("Удалена", if (piece.row.deletedAt != null) "да" else "нет"))
            val oldTransaction = when (beforeRow) {
                is BuyMirrorRow -> beforeRow.transactionSyncId
                is SaleMirrorRow -> beforeRow.transactionSyncId
                else -> null
            }
            val newTransaction = when (val row = piece.row) {
                is BuyMirrorRow -> row.transactionSyncId
                is SaleMirrorRow -> row.transactionSyncId
                else -> null
            }
            if (oldTransaction != newTransaction && beforeRow != null) result += ReportCell(piece,
                oldTransaction?.let { ReportKey(MirrorSyncTable.TRANSACTION, it) }?.let { ReportField("Операция", section.before.label(it), it) },
                newTransaction?.let { ReportKey(MirrorSyncTable.TRANSACTION, it) }?.let { ReportField("Операция", section.after.label(it), it) })
            result
        }
        val paired = pieces.any { it.conflict || (!it.context && it.before != null && it.row.deletedAt == null && it.before.deletedAt == null) }
        val conflict = pieces.any { it.conflict }
        appendLine("<div class=\"record depth-${depth.coerceAtMost(3)}\"><div class=\"scroll\" tabindex=\"0\" role=\"region\" aria-label=\"${escapeHtml(pieces.joinToString { reportTableTitle(it.key.table) })}\">")
        appendLine("<table><caption>${pieces.joinToString(" / ") { reportTableTitle(it.key.table) }}")
        pieces.filterNot { it.context }.forEach { piece ->
            append("<span id=\"${anchors.getValue(piece.key)}\" class=\"anchor\" data-record=\"${piece.key.table.tableName}\"></span>")
        }
        appendLine("</caption><thead><tr><th rowspan=\"${if (paired) 2 else 1}\" scope=\"col\">Действие</th>")
        cells.forEach { appendLine("<th scope=\"col${if (paired) "group" else ""}\"${if (paired) " colspan=\"2\"" else ""}>${escapeHtml((it.after ?: it.before)!!.title)}</th>") }
        appendLine("</tr>")
        if (paired) {
            appendLine("<tr>")
            cells.forEach { appendLine(if (conflict) "<th scope=\"col\">На компьютере</th><th scope=\"col\">В облаке</th>" else "<th scope=\"col\">Было</th><th scope=\"col\">Стало</th>") }
            appendLine("</tr>")
        }
        appendLine("</thead><tbody><tr><td class=\"action\">")
        pieces.filterNot { it.context }.forEach { piece ->
            appendLine("<div>${if (pieces.size > 1) "${reportTableTitle(piece.key.table)}: " else ""}${piece.action()}")
            if (piece.excluded) append("<br><small>Зависимость отсутствует, удалена или не включена в план</small>")
            if (piece.row.deletedAt != null && deletionContent(piece) !== piece.row) {
                append("<br><small>Последнее содержимое: ${if (section.id == "push") "из облака" else "с компьютера"}</small>")
            }
            if (piece.row.deletedAt != null && !hasContent(deletionContent(piece))) append("<br><small>Последнее содержимое недоступно</small>")
            appendLine("</div>")
        }
        appendLine("</td>")
        cells.forEach { cell ->
            if (paired) {
                appendCell(cell.before, cell.color(), anchors)
                appendCell(cell.after, cell.color(), anchors)
            } else {
                appendCell(cell.after ?: cell.before, cell.color(), anchors)
            }
        }
        appendLine("</tr></tbody></table></div></div>")
        pieces.forEach { piece ->
            val parent = section.after.parent(piece.row)
            if (parent != null && section.after.rows[parent] == null) {
                appendLine("<p class=\"related\">Связанная запись недоступна</p>")
            }
            val movement = piece.row as? BatchMovementMirrorRow
            if (movement != null) {
                val batch = ReportKey(MirrorSyncTable.BATCH, movement.batchSyncId)
                anchors[batch]?.let { appendLine("<p class=\"related\"><a href=\"#$it\">${linkTitles.getValue(batch)}: ${escapeHtml(section.after.label(batch))}</a></p>") }
            }
        }
        // Общая связь показывается полно только в своём блоке, здесь остаётся ссылка.
        pieces.mapNotNull { it.row as? ProductDeclarationMirrorRow }.forEach { row ->
            val key = ReportKey(MirrorSyncTable.DECLARATION, row.declarationSyncId)
            anchors[key]?.let { appendLine("<p class=\"related\"><a href=\"#$it\">${linkTitles.getValue(key)}</a></p>") }
        }
    }

    private fun StringBuilder.appendCell(field: ReportField?, color: String, anchors: Map<ReportKey, String>) {
        append("<td${if (color.isEmpty()) "" else " class=\"$color\""}>")
        val link = field?.reference?.let(anchors::get)
        if (link != null) append("<a href=\"#$link\">")
        append(escapeHtml(field?.text ?: "Недоступно"))
        if (link != null) append("</a>")
        appendLine("</td>")
    }
}

private data class ReportSection(
    val title: String, val id: String, val changes: List<ReportChange>,
    val before: SyncAnalysisReportData, val after: SyncAnalysisReportData,
    val hiddenVersionCount: Int = 0,
)
private data class ReportChange(
    val key: ReportKey, val before: MirrorSyncRow?, val row: MirrorSyncRow,
    val context: Boolean = false, val conflict: Boolean = false, val excluded: Boolean = false,
) {
    fun hasBusinessChanges(): Boolean {
        if (before == null || conflict || excluded || before.deletedAt != null || row.deletedAt != null) return true
        val aligned = before.withSyncVersion(row.updatedAt)
        return if (aligned is FileMirrorRow && row is FileMirrorRow) {
            aligned.copy(path = row.path, remoteObjectKey = row.remoteObjectKey, remoteStorageProvider = row.remoteStorageProvider) != row
        } else aligned != row
    }

    fun action(): String = when {
        context -> ""; conflict -> "Конфликт"; excluded -> "Не включено"
        row.deletedAt != null -> "Удаление"; before == null -> "Добавление"
        before.deletedAt != null -> "Восстановление"; else -> "Изменение"
    }

    fun linkTitle(): String {
        val noun = if (key.table == MirrorSyncTable.DECLARATION) "декларация" else "партия"
        return when (action()) {
            "Добавление" -> "Будет добавлена $noun"
            "Удаление" -> "Будет удалена $noun"
            "Восстановление" -> "Будет восстановлена $noun"
            "Конфликт" -> "Конфликт: $noun"
            "Не включено" -> "Не включено в план: $noun"
            else -> "Будет изменена $noun"
        }
    }
}
private data class ReportCell(val owner: ReportChange, val before: ReportField?, val after: ReportField?) {
    fun color(): String = when {
        owner.context || owner.excluded -> ""
        owner.conflict -> if (before != after) "changed" else ""
        owner.row.deletedAt != null -> "removed"
        owner.before == null || owner.before.deletedAt != null -> "added"
        before != after -> "changed"
        else -> ""
    }
}

private fun deletionContent(change: ReportChange): MirrorSyncRow =
    if (change.row.deletedAt != null && !hasContent(change.row) && change.before != null && hasContent(change.before)) change.before else change.row

private fun storageTransferPairs(
    parent: MirrorSyncRow,
    children: List<ReportChange>,
): Map<ReportKey, Pair<ReportChange, ReportChange>> {
    if (parent !is TransactionMirrorRow || parent.transactionType != TransactionType.STORAGE_TRANSFER) return emptyMap()
    val candidates = children.filter {
        it.row is BatchMovementMirrorRow && it.before == null && it.row.deletedAt == null &&
            !it.context && !it.conflict && !it.excluded
    }.groupBy {
        val row = it.row as BatchMovementMirrorRow
        row.batchSyncId to row.count
    }
    return buildMap {
        candidates.values.filter { it.size == 2 }.forEach { pair ->
            val outgoing = pair.singleOrNull { (it.row as BatchMovementMirrorRow).movementType == MovementType.OUTGOING }
            val incoming = pair.singleOrNull { (it.row as BatchMovementMirrorRow).movementType == MovementType.INCOMING }
            if (outgoing != null && incoming != null &&
                (outgoing.row as BatchMovementMirrorRow).storageLocation != (incoming.row as BatchMovementMirrorRow).storageLocation) {
                put(outgoing.key, outgoing to incoming)
                put(incoming.key, outgoing to incoming)
            }
        }
    }
}

private fun hasContent(row: MirrorSyncRow): Boolean = when (row) {
    is VendorMirrorRow -> row.displayName.isNotBlank() || row.comment.isNotBlank()
    is ProductMirrorRow -> row.displayName.isNotBlank() || row.secondName.isNotBlank() || row.comment.isNotBlank()
    is DocumentMirrorRow -> row.displayName.isNotBlank() || row.comment.isNotBlank()
    is DeclarationMirrorRow -> row.displayName.isNotBlank()
    is ExperimentMirrorRow -> row.title.isNotBlank() || row.ideaDescription.isNotBlank()
    is FileMirrorRow -> row.displayName.isNotBlank()
    is MoneyAccountMirrorRow -> row.name.isNotBlank()
    else -> true
}

private fun recordOrder(parent: MirrorSyncRow): Comparator<ReportChange> = compareBy<ReportChange> {
    if (it.row is BatchCostPriceMirrorRow) -1 else 0
}.thenBy {
    val row = it.row
    if (row is BatchMovementMirrorRow && parent is TransactionMirrorRow && parent.transactionType == TransactionType.STORAGE_TRANSFER) row.batchSyncId else ""
}.thenBy {
    val row = it.row
    if (row is BatchMovementMirrorRow) {
        val first = if (parent is TransactionMirrorRow && parent.transactionType == TransactionType.STORAGE_TRANSFER) MovementType.OUTGOING else MovementType.INCOMING
        if (row.movementType == first) 0 else 1
    } else 2
}.thenBy { (it.row as? BatchMirrorRow)?.dateBorn }
    .thenBy { documentDateTime(it.row) }
    .thenBy { it.key.table.applyOrder }

/** Дата делового документа, а не создания справочника или служебной версии. */
private fun documentDateTime(row: MirrorSyncRow): LocalDateTime? = when (row) {
    is TransactionMirrorRow -> row.createdAt
    is DocumentMirrorRow -> LocalDateTime(row.createdAt.year, row.createdAt.monthNumber, row.createdAt.dayOfMonth, 0, 0)
    is ExpenseMirrorRow -> row.expenseDateTime
    is MoneyMovementMirrorRow -> row.occurredAt
    else -> null
}

internal fun escapeHtml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;")
    .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")

private const val REPORT_CSS = """
body{margin:0;background:#f6f7f9;color:#18212f;font:15px/1.5 system-ui,'Segoe UI',sans-serif}
main{padding:28px 32px;max-width:1800px;margin:auto}h1{font-size:28px;margin:0 0 12px}h2{margin-top:36px;font-size:23px}h3{font-size:18px;margin:0 0 14px}small{font-size:13px;font-weight:400}
.legend span{display:inline-block;padding:4px 9px;margin:0 8px 6px 0;border-radius:4px}.block{border-top:5px solid #475569;padding:20px 0 26px;margin:22px 0}.record{margin:12px 0}.depth-1{margin-left:18px}.depth-2,.depth-3{margin-left:36px}.direction{display:block;font-size:13px;color:#475569;font-weight:400;margin-bottom:5px}.context{color:#475569;margin-top:8px;margin-bottom:8px}.service-summary{color:#475569}
.scroll{overflow-x:auto;background:white;border:1px solid #cbd5e1;border-radius:5px;padding-bottom:3px}.scroll:focus{outline:2px solid #2563eb}table{border-collapse:collapse;min-width:100%;text-align:left}caption{text-align:left;font-weight:650;padding:9px 12px;background:#e9eef4}
th,td{border:1px solid #ccd3dd;padding:9px 12px;vertical-align:top;min-width:115px;white-space:pre-wrap;overflow-wrap:anywhere;max-width:420px}th{background:#edf1f6;font-weight:600}td.action{min-width:150px;font-weight:600}td.action div+div{margin-top:10px}a{color:#154f9a;text-underline-offset:3px}.added{background:#d8f2df}.removed{background:#fadddd}.changed{background:#fff0a8}.anchor{scroll-margin-top:18px}.related{margin:6px 18px}nav{margin:18px 0}section{scroll-margin-top:20px}
"""
