package com.banyg.domain.usecase

import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.repository.AccountRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class CreateAccountUseCaseTest {

    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val useCase = CreateAccountUseCase(accountRepository)

    @Test
    fun `creates account with opening balance as current balance`() = runTest {
        val opening = Money(12_500L, Currency.PHP)
        val now = Instant.parse("2026-01-27T00:00:00Z")

        val result = useCase(
            name = "Cash Wallet",
            type = AccountType.CASH,
            currency = Currency.PHP,
            openingBalance = opening,
            now = now,
            id = "account-1"
        )

        assertEquals(opening, result.currentBalance)
        assertEquals(opening, result.openingBalance)
        assertEquals("account-1", result.id)

        coVerify {
            accountRepository.saveAccount(match {
                it.id == "account-1" && it.currentBalance == opening && it.openingBalance == opening
            })
        }
    }
}
