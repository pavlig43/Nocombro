package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.money.MoneyAccountType
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import java.sql.PreparedStatement
import java.sql.ResultSet

internal object MoneyAccountYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.MONEY_ACCOUNT
    override val columnNames = listOf(
        "sync_id",
        "name",
        "account_type",
        "opened_at",
        "is_archived",
        "updated_at",
        "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is MoneyAccountMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.name)
        statement.setString(3, row.accountType.name)
        statement.setString(4, row.openedAt.toString())
        statement.setBoolean(5, row.isArchived)
        statement.setString(6, row.updatedAt.toString())
        statement.setString(7, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = MoneyAccountMirrorRow(
        syncId = resultSet.getString("sync_id"),
        name = resultSet.getString("name"),
        accountType = enumValueOf<MoneyAccountType>(resultSet.getString("account_type")),
        openedAt = LocalDateTime.parse(resultSet.getString("opened_at")),
        isArchived = resultSet.getBoolean("is_archived"),
        updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
        deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
    )
}

internal object MoneyMovementYdbMirrorCodec : YdbMirrorRowCodec {
    override val table = MirrorSyncTable.MONEY_MOVEMENT
    override val columnNames = listOf(
        "sync_id",
        "kind",
        "category",
        "amount",
        "occurred_at",
        "from_account_sync_id",
        "to_account_sync_id",
        "counterparty",
        "comment",
        "updated_at",
        "deleted_at",
    )

    @Suppress("MagicNumber")
    override fun bind(statement: PreparedStatement, row: MirrorSyncRow) {
        require(row is MoneyMovementMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.kind.name)
        statement.setString(3, row.category?.name)
        statement.setLong(4, row.amount)
        statement.setString(5, row.occurredAt.toString())
        statement.setString(6, row.fromAccountSyncId)
        statement.setString(7, row.toAccountSyncId)
        statement.setString(8, row.counterparty)
        statement.setString(9, row.comment)
        statement.setString(10, row.updatedAt.toString())
        statement.setString(11, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = MoneyMovementMirrorRow(
        syncId = resultSet.getString("sync_id"),
        kind = enumValueOf<MoneyMovementKind>(resultSet.getString("kind")),
        category = resultSet.getString("category")?.let { enumValueOf<MoneyMovementCategory>(it) },
        amount = resultSet.getLong("amount"),
        occurredAt = LocalDateTime.parse(resultSet.getString("occurred_at")),
        fromAccountSyncId = resultSet.getString("from_account_sync_id"),
        toAccountSyncId = resultSet.getString("to_account_sync_id"),
        counterparty = resultSet.getString("counterparty"),
        comment = resultSet.getString("comment"),
        updatedAt = LocalDateTime.parse(resultSet.getString("updated_at")),
        deletedAt = resultSet.getString("deleted_at")?.let(LocalDateTime::parse),
    )
}
