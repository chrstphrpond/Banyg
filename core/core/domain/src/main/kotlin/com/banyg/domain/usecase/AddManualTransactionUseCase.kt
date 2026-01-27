package com.banyg.domain.usecase

import com.banyg.domain.calculator.MoneyCalculator
import com.banyg.domain.model.Account
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.model.TransactionStatus
import com.banyg.domain.repository.AccountRepository
import com.banyg.domain.repository.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AddManualTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        account: Account,
        amount: Money,
        merchant: String,
        memo: String? = null,
        date: LocalDate = LocalDate.now(),
        now: Instant = Instant.now(),
        id: String = UUID.randomUUID().toString()
    ): Transaction {
        require(amount.currency == account.currency) {
            "Transaction currency must match account currency"
        }
        require(!amount.isZero) { "Transaction amount cannot be zero" }

        val cleanedMerchant = merchant.trim().ifBlank { "Manual entry" }
        val cleanedMemo = memo?.trim()?.takeIf { it.isNotBlank() }

        val transaction = Transaction(
            id = id,
            accountId = account.id,
            date = date,
            amount = amount,
            merchant = cleanedMerchant,
            memo = cleanedMemo,
            categoryId = null,
            status = TransactionStatus.PENDING,
            clearedAt = null,
            transferId = null,
            splits = emptyList(),
            createdAt = now,
            updatedAt = now
        )

        transactionRepository.saveTransaction(transaction)

        val newBalance = MoneyCalculator.add(account.currentBalance, amount)
        accountRepository.updateBalance(account.id, newBalance)

        return transaction
    }
}
