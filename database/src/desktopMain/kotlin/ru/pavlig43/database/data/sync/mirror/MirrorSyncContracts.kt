package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDateTime

/**
 * Транспортный контракт typed mirror между локальной Room-базой и удаленным хранилищем.
 *
 * Gateway работает с актуальными снимками строк, а не с журналом событий. Поэтому
 * реализации не должны хранить cursor, подтверждать отдельные события или
 * интерпретировать бизнес-содержимое строк: выбор победившей версии выполняется
 * отдельно в [MirrorReconciliationPlanner].
 *
 * Методы загрузки и записи возвращают [Result], чтобы сетевые, JDBC- и schema-ошибки
 * оставались частью явного контракта синхронизации. [getStatus] не бросает ожидаемые
 * ошибки доступности и сообщает их через [MirrorRemoteStatus.error].
 */
interface MirrorSyncRemoteGateway {
    /**
     * Проверяет конфигурацию и доступность remote mirror.
     *
     * Реализация может попутно создавать отсутствующие typed tables. Время в
     * [MirrorRemoteStatus.checkedAt] обозначает момент завершения проверки.
     */
    suspend fun getStatus(): MirrorRemoteStatus

    /**
     * Загружает полный удаленный snapshot только для запрошенных [tables].
     *
     * Каждая строка должна находиться в секции, соответствующей ее [MirrorSyncTable].
     * Отсутствующая секция трактуется вызывающим кодом как пустая таблица.
     */
    suspend fun loadRemoteSnapshot(
        tables: List<MirrorSyncTable> = MirrorSyncTable.mirroredBusinessTables,
    ): Result<MirrorRemoteSnapshot>

    /**
     * Записывает локальные победившие версии в remote mirror через idempotent upsert.
     *
     * Список может содержать как активные строки, так и tombstone. Метод не выполняет
     * reconciliation повторно и предполагает, что [changes] уже рассчитаны planner-ом.
     */
    suspend fun pushMirrorState(
        changes: List<MirrorPushEntityChange>,
    ): Result<MirrorPushResult>

    /**
     * Возвращает строки запрошенных таблиц в унифицированном формате изменений.
     *
     * Этот метод не выбирает remote winners относительно Room. Такое сравнение
     * выполняется после получения snapshot.
     */
    suspend fun pullMirrorState(
        request: MirrorPullRequest = MirrorPullRequest(),
    ): Result<MirrorPullResult>
}

/** Результат проверки конфигурации, доступности и состава remote mirror. */
data class MirrorRemoteStatus(
    /** Настроен ли remote transport на текущей установке. */
    val configured: Boolean,
    /** Физически доступные имена typed mirror tables. */
    val availableTables: Set<String>,
    /** Локальное время завершения проверки. */
    val checkedAt: LocalDateTime,
    /** Диагностическое сообщение; `null`, если gateway готов к работе. */
    val error: String? = null,
)

/** Полный удаленный снимок запрошенных mirror tables на момент [loadedAt]. */
data class MirrorRemoteSnapshot(
    val loadedAt: LocalDateTime,
    val rowsByTable: Map<MirrorSyncTable, List<MirrorSyncRow>>,
)

/**
 * Локальный снимок Room с добавленными tombstone из deletion journal.
 *
 * Для одного `sync_id` snapshot содержит только строку с максимальной логической
 * версией, поэтому потребители могут сравнивать его с remote без знания о журнале.
 */
data class MirrorLocalSnapshot(
    val loadedAt: LocalDateTime,
    val rowsByTable: Map<MirrorSyncTable, List<MirrorSyncRow>>,
)

/** Одна typed-строка вместе с таблицей, к которой она относится. */
data class MirrorPushEntityChange(
    val table: MirrorSyncTable,
    val row: MirrorSyncRow,
)

/** Подтверждение успешной записи набора строк в remote mirror. */
data class MirrorPushResult(
    val pushedAt: LocalDateTime,
    val affectedTables: Set<MirrorSyncTable>,
)

/** Ограничивает pull перечисленными typed mirror tables. */
data class MirrorPullRequest(
    val tables: List<MirrorSyncTable> = MirrorSyncTable.mirroredBusinessTables,
)

/**
 * Удаленный snapshot, развернутый в плоский список typed-изменений.
 *
 * Наличие строки здесь еще не означает, что ее следует применять локально.
 */
data class MirrorPullResult(
    val pulledAt: LocalDateTime,
    val changes: List<MirrorPushEntityChange>,
)
