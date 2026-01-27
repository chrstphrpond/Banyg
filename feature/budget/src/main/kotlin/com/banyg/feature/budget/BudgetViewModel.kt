package com.banyg.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.model.Category
import com.banyg.domain.model.Currency
import com.banyg.domain.model.Money
import com.banyg.domain.model.Transaction
import com.banyg.domain.repository.TransactionRepository
import com.banyg.domain.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Budget screen.
 * 
 * Manages budget creation, updates, and spending progress tracking.
 * Calculates spending totals per category to show budget progress.
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BudgetUiState>(BudgetUiState.Loading)
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _events = Channel<BudgetEvent>()
    val events = _events.receiveAsFlow()

    private val _formState = MutableStateFlow(BudgetFormState())
    val formState: StateFlow<BudgetFormState> = _formState.asStateFlow()

    private var observeJob: Job? = null
    
    // In-memory storage for budgets until proper repository is implemented
    private val budgetsMap = mutableMapOf<String, Budget>()

    init {
        loadBudgets()
    }

    /**
     * Load all budgets with their spending progress.
     */
    fun loadBudgets() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = BudgetUiState.Loading

            // Combine categories with transactions to calculate spending
            combine(
                getCategoriesUseCase(),
                transactionRepository.observeRecentTransactions(limit = 1000)
            ) { categories, transactions ->
                // Calculate spending per category for current month
                val spendingByCategory = calculateSpendingByCategory(transactions)
                
                // Map budgets with progress
                val budgetsWithProgress = budgetsMap.values.map { budget ->
                    val category = categories.find { it.id == budget.categoryId }
                    val spent = spendingByCategory[budget.categoryId] ?: Money.zero(budget.amount.currency)
                    val remaining = budget.calculateRemaining(spent)
                    val progress = budget.calculateProgress(spent)
                    
                    BudgetWithProgress(
                        budget = budget.copy(category = category),
                        spent = spent,
                        remaining = remaining,
                        progress = progress,
                        isOverBudget = remaining.minorUnits < 0
                    )
                }

                // Calculate totals
                val currency = budgetsWithProgress.firstOrNull()?.budget?.amount?.currency ?: Currency.PHP
                val totalBudgeted = Money(
                    budgetsWithProgress.sumOf { it.budget.amount.minorUnits },
                    currency
                )
                val totalSpent = Money(
                    budgetsWithProgress.sumOf { kotlin.math.abs(it.spent.minorUnits) },
                    currency
                )

                BudgetUiState.Success(
                    budgets = budgetsWithProgress,
                    categories = categories.filter { cat -> 
                        // Only show categories not already budgeted
                        budgetsWithProgress.none { it.budget.categoryId == cat.id }
                    },
                    totalBudgeted = totalBudgeted,
                    totalSpent = totalSpent
                )
            }
                .catch { throwable ->
                    _uiState.value = BudgetUiState.Error(
                        message = throwable.message ?: "Failed to load budgets"
                    )
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    /**
     * Create a new budget from the current form state.
     */
    fun createBudget() {
        val form = _formState.value
        if (!form.isValid) {
            _formState.update { it.copy(error = "Please fill in all fields correctly") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, error = null) }

            try {
                val amountValue = form.amountInput.toDoubleOrNull() ?: 0.0
                val currency = Currency.PHP // Default currency, could be from account
                val amount = Money.fromMajor(amountValue, currency)

                val budget = Budget(
                    id = UUID.randomUUID().toString(),
                    categoryId = form.categoryId,
                    amount = amount,
                    period = form.period
                )

                budgetsMap[budget.id] = budget
                
                _formState.update { BudgetFormState() }
                _events.send(BudgetEvent.ShowSnackbar("Budget created successfully"))
                _events.send(BudgetEvent.NavigateBack)
                
                // Refresh the budgets list
                loadBudgets()
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
                _events.send(BudgetEvent.ShowSnackbar("Failed to create budget: ${e.message}"))
            }
        }
    }

    /**
     * Update an existing budget.
     */
    fun updateBudget(budgetId: String) {
        val form = _formState.value
        if (!form.isValid) {
            _formState.update { it.copy(error = "Please fill in all fields correctly") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, error = null) }

            try {
                val existingBudget = budgetsMap[budgetId]
                if (existingBudget == null) {
                    _formState.update { it.copy(isSaving = false, error = "Budget not found") }
                    return@launch
                }

                val amountValue = form.amountInput.toDoubleOrNull() ?: 0.0
                val amount = Money.fromMajor(amountValue, existingBudget.amount.currency)

                val updatedBudget = existingBudget.copy(
                    categoryId = form.categoryId,
                    amount = amount,
                    period = form.period
                )

                budgetsMap[budgetId] = updatedBudget
                
                _formState.update { BudgetFormState() }
                _events.send(BudgetEvent.ShowSnackbar("Budget updated successfully"))
                _events.send(BudgetEvent.NavigateBack)
                
                // Refresh the budgets list
                loadBudgets()
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, error = e.message) }
                _events.send(BudgetEvent.ShowSnackbar("Failed to update budget: ${e.message}"))
            }
        }
    }

    /**
     * Delete a budget.
     */
    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            try {
                budgetsMap.remove(budgetId)
                _events.send(BudgetEvent.ShowSnackbar("Budget deleted"))
                loadBudgets()
            } catch (e: Exception) {
                _events.send(BudgetEvent.ShowSnackbar("Failed to delete budget: ${e.message}"))
            }
        }
    }

    /**
     * Load budget data into form for editing.
     */
    fun loadBudgetForEdit(budgetId: String) {
        viewModelScope.launch {
            val budget = budgetsMap[budgetId]
            if (budget != null) {
                val majorAmount = kotlin.math.abs(budget.amount.minorUnits) / budget.amount.currency.minorUnitsPerMajor
                _formState.update {
                    it.copy(
                        categoryId = budget.categoryId,
                        amountInput = majorAmount.toString(),
                        period = budget.period
                    )
                }
            }
        }
    }

    /**
     * Update form state for category selection.
     */
    fun selectCategory(categoryId: String) {
        _formState.update { it.copy(categoryId = categoryId, error = null) }
    }

    /**
     * Update form state for amount input.
     */
    fun updateAmount(value: String) {
        _formState.update { it.copy(amountInput = value, error = null) }
    }

    /**
     * Update form state for period selection.
     */
    fun selectPeriod(period: BudgetPeriod) {
        _formState.update { it.copy(period = period, error = null) }
    }

    /**
     * Reset form state.
     */
    fun resetForm() {
        _formState.update { BudgetFormState() }
    }

    /**
     * Handle UI events from the screen.
     */
    fun onEvent(event: BudgetUiEvent) {
        when (event) {
            BudgetUiEvent.OnRefresh -> loadBudgets()
            BudgetUiEvent.OnRetry -> loadBudgets()
            BudgetUiEvent.OnAddBudget -> viewModelScope.launch {
                resetForm()
                _events.send(BudgetEvent.NavigateToAddBudget)
            }
            is BudgetUiEvent.OnEditBudget -> viewModelScope.launch {
                loadBudgetForEdit(event.budgetId)
                _events.send(BudgetEvent.NavigateToEditBudget(event.budgetId))
            }
            is BudgetUiEvent.OnDeleteBudget -> deleteBudget(event.budgetId)
            is BudgetUiEvent.OnCategorySelected -> selectCategory(event.categoryId)
            is BudgetUiEvent.OnAmountChanged -> updateAmount(event.value)
            is BudgetUiEvent.OnPeriodChanged -> selectPeriod(event.period)
            BudgetUiEvent.OnSaveBudget -> createBudget()
            BudgetUiEvent.OnCancel -> viewModelScope.launch {
                resetForm()
                _events.send(BudgetEvent.NavigateBack)
            }
        }
    }

    /**
     * Calculate spending by category from transactions.
     * Only considers expenses (negative amounts) in the current period.
     */
    private fun calculateSpendingByCategory(transactions: List<Transaction>): Map<String, Money> {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        
        return transactions
            .filter { 
                it.amount.isExpense && 
                it.date >= startOfMonth &&
                it.categoryId != null
            }
            .groupBy { it.categoryId!! }
            .mapValues { (_, txs) ->
                val currency = txs.first().amount.currency
                val totalMinorUnits = txs.sumOf { kotlin.math.abs(it.amount.minorUnits) }
                Money(totalMinorUnits, currency)
            }
    }
}
