package com.banyg.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banyg.domain.model.Category
import com.banyg.domain.model.CategoryGroups
import com.banyg.domain.usecase.CreateCategoryUseCase
import com.banyg.domain.usecase.DeleteCategoryUseCase
import com.banyg.domain.usecase.GetCategoriesUseCase
import com.banyg.domain.usecase.SeedDefaultCategoriesUseCase
import com.banyg.domain.usecase.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val seedDefaultCategoriesUseCase: SeedDefaultCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var showHidden = false

    init {
        seedCategoriesAndObserve()
    }

    fun onEvent(event: CategoriesUiEvent) {
        when (event) {
            CategoriesUiEvent.OnRefresh -> refreshCategories()
            CategoriesUiEvent.OnRetry -> seedCategoriesAndObserve()
            CategoriesUiEvent.OnAddCategory -> { /* Navigation handled by UI */ }
            CategoriesUiEvent.OnToggleShowHidden -> toggleShowHidden()
            is CategoriesUiEvent.OnCategoryClick -> { /* Navigation handled by UI */ }
            is CategoriesUiEvent.OnEditCategory -> { /* Navigation handled by UI */ }
            is CategoriesUiEvent.OnDeleteCategory -> softDeleteCategory(event.categoryId)
            is CategoriesUiEvent.OnToggleHidden -> toggleCategoryHidden(event.categoryId)
            is CategoriesUiEvent.OnRestoreCategory -> restoreCategory(event.categoryId)
        }
    }

    private fun seedCategoriesAndObserve() {
        viewModelScope.launch {
            // Seed default categories if needed
            seedDefaultCategoriesUseCase()
            // Then start observing
            observeCategories()
        }
    }

    private fun observeCategories() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = CategoriesUiState.Loading
            getCategoriesUseCase.observeAll(includeHidden = true)
                .catch { throwable ->
                    _uiState.value = CategoriesUiState.Error(
                        message = throwable.message ?: "Failed to load categories"
                    )
                }
                .collect { categories ->
                    updateSuccessState(categories)
                }
        }
    }

    private fun refreshCategories() {
        val currentState = _uiState.value
        if (currentState is CategoriesUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true)
        }
        observeCategories()
    }

    private fun updateSuccessState(categories: List<Category>) {
        val visibleCategories = categories.filter { !it.isHidden }
        val hiddenCategories = categories.filter { it.isHidden }

        // Group visible categories by group name
        val grouped = if (showHidden) {
            categories.groupBy { it.groupName }
        } else {
            visibleCategories.groupBy { it.groupName }
        }

        _uiState.value = CategoriesUiState.Success(
            categories = if (showHidden) categories else visibleCategories,
            groupedCategories = grouped,
            hiddenCategories = hiddenCategories,
            showHidden = showHidden,
            isRefreshing = false
        )
    }

    private fun toggleShowHidden() {
        showHidden = !showHidden
        val currentState = _uiState.value
        if (currentState is CategoriesUiState.Success) {
            updateSuccessState(currentState.categories + currentState.hiddenCategories)
        }
    }

    private fun softDeleteCategory(id: String) {
        viewModelScope.launch {
            val result = deleteCategoryUseCase(id, hardDelete = false)
            if (result is DeleteCategoryUseCase.Result.Error) {
                _uiState.value = CategoriesUiState.Error(
                    message = result.message,
                    canRetry = false
                )
            }
        }
    }

    private fun restoreCategory(id: String) {
        viewModelScope.launch {
            val result = deleteCategoryUseCase.restore(id)
            if (result is DeleteCategoryUseCase.Result.Error) {
                _uiState.value = CategoriesUiState.Error(
                    message = result.message,
                    canRetry = false
                )
            }
        }
    }

    private fun toggleCategoryHidden(id: String) {
        viewModelScope.launch {
            val result = deleteCategoryUseCase.toggleHidden(id)
            if (result is DeleteCategoryUseCase.Result.Error) {
                _uiState.value = CategoriesUiState.Error(
                    message = result.message,
                    canRetry = false
                )
            }
        }
    }
}

/**
 * ViewModel for category form (add/edit)
 */
@HiltViewModel
class CategoryFormViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryFormUiState>(CategoryFormUiState.Loading)
    val uiState: StateFlow<CategoryFormUiState> = _uiState.asStateFlow()

    private var categoryId: String? = null

    fun loadCategory(id: String?) {
        categoryId = id
        if (id == null) {
            // Create mode
            _uiState.value = CategoryFormUiState.Create()
        } else {
            // Edit mode
            viewModelScope.launch {
                _uiState.value = CategoryFormUiState.Loading
                val category = getCategoriesUseCase.getById(id)
                if (category != null) {
                    _uiState.value = CategoryFormUiState.Edit(
                        category = category,
                        name = category.name,
                        groupId = category.groupId,
                        groupName = category.groupName,
                        icon = category.icon,
                        color = category.color,
                        isHidden = category.isHidden
                    )
                } else {
                    _uiState.value = CategoryFormUiState.Error("Category not found")
                }
            }
        }
    }

    fun onEvent(event: CategoryFormUiEvent, onSuccess: () -> Unit = {}) {
        when (event) {
            is CategoryFormUiEvent.OnNameChange -> updateName(event.name)
            is CategoryFormUiEvent.OnGroupChange -> updateGroup(event.groupId, event.groupName)
            is CategoryFormUiEvent.OnIconChange -> updateIcon(event.icon)
            is CategoryFormUiEvent.OnColorChange -> updateColor(event.color)
            CategoryFormUiEvent.OnSave -> saveCategory(onSuccess)
            CategoryFormUiEvent.OnCancel -> { /* Navigation handled by UI */ }
        }
    }

    private fun updateName(name: String) {
        when (val state = _uiState.value) {
            is CategoryFormUiState.Create -> {
                _uiState.value = state.copy(name = name, error = null)
            }
            is CategoryFormUiState.Edit -> {
                _uiState.value = state.copy(name = name, error = null)
            }
            else -> { /* no-op */ }
        }
    }

    private fun updateGroup(groupId: String?, groupName: String?) {
        when (val state = _uiState.value) {
            is CategoryFormUiState.Create -> {
                _uiState.value = state.copy(
                    groupId = groupId,
                    groupName = groupName ?: groupId?.let { getDefaultGroupName(it) }
                )
            }
            is CategoryFormUiState.Edit -> {
                _uiState.value = state.copy(
                    groupId = groupId,
                    groupName = groupName ?: groupId?.let { getDefaultGroupName(it) }
                )
            }
            else -> { /* no-op */ }
        }
    }

    private fun updateIcon(icon: String?) {
        when (val state = _uiState.value) {
            is CategoryFormUiState.Create -> _uiState.value = state.copy(icon = icon)
            is CategoryFormUiState.Edit -> _uiState.value = state.copy(icon = icon)
            else -> { /* no-op */ }
        }
    }

    private fun updateColor(color: String?) {
        when (val state = _uiState.value) {
            is CategoryFormUiState.Create -> _uiState.value = state.copy(color = color)
            is CategoryFormUiState.Edit -> _uiState.value = state.copy(color = color)
            else -> { /* no-op */ }
        }
    }

    private fun saveCategory(onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val state = _uiState.value) {
                is CategoryFormUiState.Create -> {
                    _uiState.value = state.copy(isSaving = true, error = null)
                    val result = createCategoryUseCase(
                        name = state.name,
                        groupId = state.groupId,
                        groupName = state.groupName,
                        icon = state.icon,
                        color = state.color
                    )
                    when (result) {
                        is CreateCategoryUseCase.Result.Success -> onSuccess()
                        is CreateCategoryUseCase.Result.Error -> {
                            _uiState.value = state.copy(isSaving = false, error = result.message)
                        }
                    }
                }
                is CategoryFormUiState.Edit -> {
                    _uiState.value = state.copy(isSaving = true, error = null)
                    val result = updateCategoryUseCase(
                        id = state.category.id,
                        name = state.name,
                        groupId = state.groupId,
                        groupName = state.groupName,
                        icon = state.icon,
                        color = state.color
                    )
                    when (result) {
                        is UpdateCategoryUseCase.Result.Success -> onSuccess()
                        is UpdateCategoryUseCase.Result.Error -> {
                            _uiState.value = state.copy(isSaving = false, error = result.message)
                        }
                    }
                }
                else -> { /* no-op */ }
            }
        }
    }

    private fun getDefaultGroupName(groupId: String): String {
        return when (groupId) {
            CategoryGroups.INCOME -> "Income"
            CategoryGroups.FOOD -> "Food"
            CategoryGroups.TRANSPORTATION -> "Transportation"
            CategoryGroups.SHOPPING -> "Shopping"
            CategoryGroups.BILLS -> "Bills & Utilities"
            CategoryGroups.ENTERTAINMENT -> "Entertainment"
            CategoryGroups.HEALTH -> "Health"
            CategoryGroups.EDUCATION -> "Education"
            CategoryGroups.TRANSFER -> "Transfer"
            CategoryGroups.OTHER -> "Other"
            else -> groupId.replaceFirstChar { it.uppercase() }
        }
    }
}
