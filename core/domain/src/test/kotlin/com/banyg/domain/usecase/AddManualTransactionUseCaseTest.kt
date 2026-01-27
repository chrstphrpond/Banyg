package com.banyg.domain.usecase

import com.banyg.domain.model.Account
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.AccountRepository
import com.banyg.domain.repository.TransactionRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AddManualTransactionUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val useCase = AddManualTransactionUseCase(transactionRepository, accountRepository)

    @Test
    fun `adds pending transaction and updates account balance`() = runTest {
        val now = Instant.parse("2026-01-27T00:00:00Z")
        val account = Account(
            id = "acc-1",
            name = "Checking",
            type = AccountType.CHECKING,
            currency = Currency.PHP,
            openingBalance = Money(10_000L, Currency.PHP),
            currentBalance = Money(10_000L, Currency.PHP),
            createdAt = now,
            updatedAt = now
        )

        val amount = Money(-2_500L, Currency.PHP)
        val date = LocalDate.parse("2026-01-27")

        val transaction = useCase(
            account = account,
            amount = amount,
            merchant = "Coffee",
            memo = "Latte",
            date = date,
            now = now,
            id = "txn-1"
        )

        assertEquals("txn-1", transaction.id)
        assertEquals(TransactionStatus.PENDING, transaction.status)
        assertEquals(account.id, transaction.accountId)

        coVerify {
            transactionRepository.saveTransaction(match {
                it.id == "txn-1" && it.status == TransactionStatus.PENDING && it.amount == amount
            })
        }

        coVerify {
            accountRepository.updateBalance(
                account.id,
                Money(7_500L, Currency.PHP)
            )
        }
    }

    @Test
    fun `throws when currency mismatches account`() = runTest {
        val now = Instant.parse("2026-01-27T00:00:00Z")
        val account = Account(
            id = "acc-1",
            name = "Checking",
            type = AccountType.CHECKING,
            currency = Currency.PHP,
            openingBalance = Money(10_000L, Currency.PHP),
            currentBalance = Money(10_000L, Currency.PHP),
            createdAt = now,
            updatedAt = now
        )

        val amount = Money(5_000L, Currency.USD)

        try {
            useCase(
                account = account,
                amount = amount,
                merchant = "Salary"
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `throws when amount is zero`() = runTest {
        val now = Instant.parse("2026-01-27T00:00:00Z")
        val account = Account(
            id = "acc-1",
            name = "Checking",
            type = AccountType.CHECKING,
            currency = Currency.PHP,
            openingBalance = Money(10_000L, Currency.PHP),
            currentBalance = Money(10_000L, Currency.PHP),
            createdAt = now,
            updatedAt = now
        )

        val amount = Money(0L, Currency.PHP)

        try {
            useCase(
                account = account,
                amount = amount,
                merchant = "Zero"
            )
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
