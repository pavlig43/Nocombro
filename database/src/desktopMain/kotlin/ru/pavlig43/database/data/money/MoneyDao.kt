package ru.pavlig43.database.data.money

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

@Dao
abstract class MoneyDao {
    @Query(
        """
        SELECT * FROM $MONEY_ACCOUNT_TABLE_NAME
        WHERE deleted_at IS NULL
        ORDER BY is_archived ASC, name COLLATE NOCASE ASC, sync_id ASC
        """,
    )
    abstract fun observeAccounts(): Flow<List<MoneyAccount>>

    @Query(
        """
        SELECT * FROM $MONEY_ACCOUNT_TABLE_NAME
        WHERE deleted_at IS NULL AND is_archived = 0
        ORDER BY name COLLATE NOCASE ASC, sync_id ASC
        """,
    )
    abstract fun observeAvailableAccounts(): Flow<List<MoneyAccount>>

    @Query("SELECT * FROM $MONEY_ACCOUNT_TABLE_NAME WHERE id = :id")
    abstract suspend fun getAccount(id: Int): MoneyAccount?

    @Query("SELECT * FROM $MONEY_ACCOUNT_TABLE_NAME WHERE sync_id = :syncId")
    abstract suspend fun getAccountBySyncId(syncId: String): MoneyAccount?

    @Query("SELECT * FROM $MONEY_ACCOUNT_TABLE_NAME")
    abstract suspend fun getAllAccounts(): List<MoneyAccount>

    @Query(
        """
        SELECT * FROM $MONEY_ACCOUNT_TABLE_NAME
        WHERE deleted_at IS NULL
        ORDER BY is_archived ASC, sync_id ASC
        LIMIT 1
        """,
    )
    abstract suspend fun getFirstActiveAccount(): MoneyAccount?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertAccount(account: MoneyAccount): Long

    @Update
    protected abstract suspend fun updateAccountEntity(account: MoneyAccount)

    @Upsert
    protected abstract suspend fun upsertAccountEntity(account: MoneyAccount)

    @Query("DELETE FROM $MONEY_ACCOUNT_TABLE_NAME WHERE id = :id")
    abstract suspend fun deleteAccountById(id: Int)

    @Query("DELETE FROM $MONEY_ACCOUNT_TABLE_NAME WHERE sync_id = :syncId")
    abstract suspend fun deleteAccountBySyncId(syncId: String)

    @Transaction
    open suspend fun createAccount(account: MoneyAccount): Long {
        validateAccount(account)
        return insertAccount(account)
    }

    @Transaction
    open suspend fun updateAccount(account: MoneyAccount) {
        require((account.id != 0) && (getAccount(account.id) != null)) {
            "Money account does not exist"
        }
        validateAccount(account)
        updateAccountEntity(account)
    }

    @Transaction
    open suspend fun upsertAccount(account: MoneyAccount) {
        if (account.deletedAt == null) validateAccount(account)
        upsertAccountEntity(account)
    }

    @Query(
        """
        SELECT * FROM $MONEY_MOVEMENT_TABLE_NAME
        WHERE deleted_at IS NULL AND occurred_at <= :endInclusive
        ORDER BY occurred_at ASC, sync_id ASC
        """,
    )
    abstract fun observeMovementsUntil(endInclusive: LocalDateTime): Flow<List<MoneyMovement>>

    @Query(
        """
        SELECT * FROM $MONEY_MOVEMENT_TABLE_NAME
        WHERE deleted_at IS NULL AND occurred_at <= :endInclusive
        ORDER BY occurred_at ASC, sync_id ASC
        """,
    )
    abstract suspend fun getActiveMovementsUntil(endInclusive: LocalDateTime): List<MoneyMovement>

    @Query("SELECT * FROM $MONEY_MOVEMENT_TABLE_NAME WHERE id = :id")
    abstract suspend fun getMovement(id: Int): MoneyMovement?

    @Query("SELECT * FROM $MONEY_MOVEMENT_TABLE_NAME WHERE sync_id = :syncId")
    abstract suspend fun getMovementBySyncId(syncId: String): MoneyMovement?

    @Query("SELECT * FROM $MONEY_MOVEMENT_TABLE_NAME")
    abstract suspend fun getAllMovements(): List<MoneyMovement>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertMovement(movement: MoneyMovement): Long

    @Update
    protected abstract suspend fun updateMovementEntity(movement: MoneyMovement)

    @Upsert
    protected abstract suspend fun upsertMovementEntity(movement: MoneyMovement)

    @Query("DELETE FROM $MONEY_MOVEMENT_TABLE_NAME WHERE id = :id")
    abstract suspend fun deleteMovementById(id: Int)

    @Query("DELETE FROM $MONEY_MOVEMENT_TABLE_NAME WHERE sync_id = :syncId")
    abstract suspend fun deleteMovementBySyncId(syncId: String)

    @Transaction
    open suspend fun createMovement(movement: MoneyMovement): Long {
        validateMovement(movement, allowedArchivedAccountIds = emptySet())
        return insertMovement(movement)
    }

    @Transaction
    open suspend fun updateMovement(movement: MoneyMovement) {
        val existing = getMovement(movement.id)
        require((movement.id != 0) && (existing != null)) {
            "Money movement does not exist"
        }
        validateMovement(
            movement = movement,
            allowedArchivedAccountIds = setOfNotNull(
                existing.fromAccountId,
                existing.toAccountId,
            ),
        )
        updateMovementEntity(movement)
    }

    @Transaction
    open suspend fun upsertMovement(movement: MoneyMovement) {
        if (movement.deletedAt == null) {
            validateMovement(
                movement = movement,
                allowedArchivedAccountIds = setOfNotNull(
                    movement.fromAccountId,
                    movement.toAccountId,
                ),
            )
        }
        upsertMovementEntity(movement)
    }

    private fun validateAccount(account: MoneyAccount) {
        require(account.name.isNotBlank()) { "Money account name must not be blank" }
    }

    private suspend fun validateMovement(
        movement: MoneyMovement,
        allowedArchivedAccountIds: Set<Int>,
    ) {
        require(movement.amount > 0) { "Money movement amount must be greater than zero" }

        when (movement.kind) {
            MoneyMovementKind.OPENING_BALANCE -> {
                require((movement.fromAccountId == null) xor (movement.toAccountId == null)) {
                    "Opening balance must reference exactly one account"
                }
                require(movement.category == null) {
                    "Opening balance must not have a category"
                }
            }

            MoneyMovementKind.INCOME -> {
                require(
                    (movement.fromAccountId == null) &&
                        (movement.toAccountId != null),
                ) {
                    "Income must reference only a target account"
                }
                require(movement.category != null) { "Income must have a category" }
            }

            MoneyMovementKind.EXPENSE -> {
                require(
                    (movement.fromAccountId != null) &&
                        (movement.toAccountId == null),
                ) {
                    "Expense must reference only a source account"
                }
                require(movement.category != null) { "Expense must have a category" }
            }

            MoneyMovementKind.TRANSFER -> {
                require(
                    (movement.fromAccountId != null) &&
                        (movement.toAccountId != null) &&
                        (movement.fromAccountId != movement.toAccountId),
                ) {
                    "Transfer must reference two different accounts"
                }
                require(movement.category == null) { "Transfer must not have a category" }
            }
        }

        setOfNotNull(movement.fromAccountId, movement.toAccountId).forEach { accountId ->
            val account = requireNotNull(getAccount(accountId)) {
                "Money account does not exist: $accountId"
            }
            require(account.deletedAt == null) { "Money account is deleted: $accountId" }
            require(!account.isArchived || accountId in allowedArchivedAccountIds) {
                "Money account is archived: $accountId"
            }
            require(movement.occurredAt >= account.openedAt) {
                "Money movement date is before account opening date: $accountId"
            }
        }
    }
}
