package ru.pavlig43.database

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.money.MoneyAccount
import ru.pavlig43.database.data.money.MoneyAccountType
import ru.pavlig43.database.data.money.MoneyMovement
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase

class MoneyDaoTest : DesktopMainDispatcherFunSpec({

    test("money dao saves valid movements and orders equal timestamps by sync id") {
        withEmptyTestDatabase { db ->
            val dao = db.moneyDao
            val openedAt = LocalDateTime(2026, 7, 1, 9, 0)
            val cashId = dao.createAccount(
                MoneyAccount(
                    name = "Cash",
                    accountType = MoneyAccountType.CASH,
                    openedAt = openedAt,
                )
            ).toInt()
            val bankId = dao.createAccount(
                MoneyAccount(
                    name = "Bank",
                    accountType = MoneyAccountType.BANK,
                    openedAt = openedAt,
                )
            ).toInt()
            val occurredAt = LocalDateTime(2026, 7, 2, 10, 0)

            dao.createMovement(
                MoneyMovement(
                    kind = MoneyMovementKind.INCOME,
                    category = MoneyMovementCategory.SALE_PAYMENT,
                    amount = 20_000,
                    occurredAt = occurredAt,
                    fromAccountId = null,
                    toAccountId = cashId,
                    syncId = "movement-b",
                )
            )
            dao.createMovement(
                MoneyMovement(
                    kind = MoneyMovementKind.TRANSFER,
                    category = null,
                    amount = 5_000,
                    occurredAt = occurredAt,
                    fromAccountId = cashId,
                    toAccountId = bankId,
                    syncId = "movement-a",
                )
            )

            dao.observeMovementsUntil(occurredAt).first().map { it.syncId } shouldContainExactly
                listOf("movement-a", "movement-b")
            dao.getAllAccounts().size shouldBe 2
            dao.getAllMovements().size shouldBe 2
        }
    }

    test("money dao rejects invalid shape amount archived account and early date") {
        withEmptyTestDatabase { db ->
            val dao = db.moneyDao
            val openedAt = LocalDateTime(2026, 7, 10, 9, 0)
            val account = MoneyAccount(
                name = "Cash",
                accountType = MoneyAccountType.CASH,
                openedAt = openedAt,
            )
            val accountId = dao.createAccount(account).toInt()

            suspend fun create(movement: MoneyMovement) = dao.createMovement(movement)

            shouldThrow<IllegalArgumentException> {
                create(
                    validIncome(
                        accountId = accountId,
                        occurredAt = openedAt,
                    ).copy(amount = 0)
                )
            }
            shouldThrow<IllegalArgumentException> {
                create(
                    validIncome(
                        accountId = accountId,
                        occurredAt = openedAt,
                    ).copy(fromAccountId = accountId)
                )
            }
            shouldThrow<IllegalArgumentException> {
                create(
                    MoneyMovement(
                        kind = MoneyMovementKind.TRANSFER,
                        category = null,
                        amount = 1_000,
                        occurredAt = openedAt,
                        fromAccountId = accountId,
                        toAccountId = accountId,
                    )
                )
            }
            shouldThrow<IllegalArgumentException> {
                dao.upsertMovement(
                    MoneyMovement(
                        kind = MoneyMovementKind.EXPENSE,
                        category = null,
                        amount = 1_000,
                        occurredAt = openedAt,
                        fromAccountId = accountId,
                        toAccountId = null,
                    )
                )
            }
            shouldThrow<IllegalArgumentException> {
                create(
                    validIncome(
                        accountId = accountId,
                        occurredAt = LocalDateTime(2026, 7, 9, 23, 59),
                    )
                )
            }

            dao.updateAccount(account.copy(id = accountId, isArchived = true))
            shouldThrow<IllegalArgumentException> {
                create(validIncome(accountId = accountId, occurredAt = openedAt))
            }
        }
    }

    test("money dao supports update and hard delete") {
        withEmptyTestDatabase { db ->
            val dao = db.moneyDao
            val openedAt = LocalDateTime(2026, 7, 1, 0, 0)
            val accountId = dao.createAccount(
                MoneyAccount(
                    name = "Cash",
                    accountType = MoneyAccountType.CASH,
                    openedAt = openedAt,
                )
            ).toInt()
            val movementId = dao.createMovement(
                validIncome(accountId = accountId, occurredAt = openedAt)
            ).toInt()
            val movement = requireNotNull(dao.getMovement(movementId))

            dao.updateMovement(movement.copy(amount = 42_000))
            dao.getMovement(movementId)?.amount shouldBe 42_000

            dao.deleteMovementById(movementId)
            dao.deleteAccountById(accountId)
            dao.getAllMovements() shouldContainExactly emptyList()
            dao.getAllAccounts() shouldContainExactly emptyList()
        }
    }
})

private fun validIncome(
    accountId: Int,
    occurredAt: LocalDateTime,
): MoneyMovement = MoneyMovement(
    kind = MoneyMovementKind.INCOME,
    category = MoneyMovementCategory.SALE_PAYMENT,
    amount = 10_000,
    occurredAt = occurredAt,
    fromAccountId = null,
    toAccountId = accountId,
)
