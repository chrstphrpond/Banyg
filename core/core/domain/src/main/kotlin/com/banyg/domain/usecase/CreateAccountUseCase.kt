package com.banyg.domain.usecase

import com.banyg.domain.model.Account
import com.banyg.domain.model.AccountType
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.repository.AccountRepository
import java.time.Instant
import java.util.UUID

class CreateAccountUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        name: String,
        type: AccountType,
        currency: Currency,
        openingBalance: Money,
        now: Instant = Instant.now(),
        id: String = UUID.randomUUID().toString()
    ): Account {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Account name cannot be blank" }
        require(openingBalance.currency == currency) {
            "Opening balance currency must match account currency"
        }

        val account = Account(
            id = id,
            name = trimmedName,
            type = type,
            currency = currency,
            openingBalance = openingBalance,
            currentBalance = openingBalance,
            isArchived = false,
            createdAt = now,
            updatedAt = now
        )

        accountRepository.saveAccount(account)
        return account
    }
}
