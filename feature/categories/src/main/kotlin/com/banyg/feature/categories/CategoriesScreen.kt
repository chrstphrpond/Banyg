package com.banyg.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.domain.model.Category
import com.banyg.domain.model.CategoryGroups
import com.banyg.ui.components.GradientCard
import com.banyg.ui.components.OutlinedPillButton
import com.banyg.ui.theme.BanygGradients
import com.banyg.ui.theme.BanygTheme

@Composable
fun CategoriesRoute(
    onNavigateBack: () -> Unit,
    onNavigateToAddCategory: () -> Unit,
    onNavigateToEditCategory: (String) -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CategoriesScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToAddCategory = onNavigateToAddCategory,
        onNavigateToEditCategory = onNavigateToEditCategory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoriesScreen(
    uiState: CategoriesUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (CategoriesUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAddCategory: () -> Unit,
    onNavigateToEditCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BanygTheme.colors.backgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Categories", color = BanygTheme.colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BanygTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BanygTheme.colors.backgroundDark
                )
            )
        },
        floatingActionButton = {
            if (uiState is CategoriesUiState.Success) {
                FloatingActionButton(
                    onClick = onNavigateToAddCategory,
                    containerColor = BanygTheme.colors.limeGreen,
                    contentColor = BanygTheme.colors.textOnLime
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        }
    ) { paddingValues ->
        when (uiState) {
            CategoriesUiState.Loading -> CategoriesLoading(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is CategoriesUiState.Error -> CategoriesError(
                message = uiState.message,
                canRetry = uiState.canRetry,
                onRetry = { onEvent(CategoriesUiEvent.OnRetry) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is CategoriesUiState.Success -> CategoriesContent(
                state = uiState,
                onEvent = onEvent,
                onNavigateToEditCategory = onNavigateToEditCategory,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun CategoriesContent(
    state: CategoriesUiState.Success,
    onEvent: (CategoriesUiEvent) -> Unit,
    onNavigateToEditCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = BanygTheme.spacing.screenPadding,
            vertical = BanygTheme.spacing.regular
        ),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        // Header with stats
        item {
            CategoriesHeader(
                visibleCount = state.categories.count { !it.isHidden },
                hiddenCount = state.hiddenCategories.size,
                showHidden = state.showHidden,
                onToggleShowHidden = { onEvent(CategoriesUiEvent.OnToggleShowHidden) }
            )
        }

        if (state.categories.isEmpty() && !state.showHidden) {
            item {
                CategoriesEmptyState(
                    onAddCategory = { onEvent(CategoriesUiEvent.OnAddCategory) }
                )
            }
        } else {
            // Grouped categories
            state.groupedCategories.forEach { (groupName, categories) ->
                if (categories.isNotEmpty()) {
                    item {
                        GroupHeader(name = groupName ?: "Uncategorized")
                    }

                    items(categories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            onEdit = { onNavigateToEditCategory(category.id) },
                            onToggleHidden = {
                                onEvent(CategoriesUiEvent.OnToggleHidden(category.id))
                            },
                            onRestore = {
                                onEvent(CategoriesUiEvent.OnRestoreCategory(category.id))
                            },
                            onDelete = {
                                onEvent(CategoriesUiEvent.OnDeleteCategory(category.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesHeader(
    visibleCount: Int,
    hiddenCount: Int,
    showHidden: Boolean,
    onToggleShowHidden: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        GradientCard(
            gradient = BanygGradients.DarkDepth,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
            ) {
                Text(
                    text = "Organize your transactions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BanygTheme.colors.textSecondary
                )

                Text(
                    text = "$visibleCount categories",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BanygTheme.colors.textPrimary
                )

                if (hiddenCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$hiddenCount hidden",
                            style = MaterialTheme.typography.bodySmall,
                            color = BanygTheme.colors.textTertiary
                        )
                        OutlinedPillButton(
                            text = if (showHidden) "Hide Hidden" else "Show Hidden",
                            onClick = onToggleShowHidden
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleSmall,
        color = BanygTheme.colors.limeGreen,
        modifier = modifier.padding(
            top = BanygTheme.spacing.regular,
            bottom = BanygTheme.spacing.small
        )
    )
}

@Composable
private fun CategoryCard(
    category: Category,
    onEdit: () -> Unit,
    onToggleHidden: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    GradientCard(
        gradient = if (category.isHidden) BanygGradients.SubtleDark else BanygGradients.DarkDepth,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(BanygTheme.spacing.regular)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
            ) {
                // Category color indicator
                category.color?.let { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = parseColor(colorHex),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }

                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (category.isHidden) BanygTheme.colors.textTertiary else BanygTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (category.isHidden) {
                        Text(
                            text = "Hidden",
                            style = MaterialTheme.typography.bodySmall,
                            color = BanygTheme.colors.textTertiary
                        )
                    }
                }
            }

            // Actions menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = BanygTheme.colors.textSecondary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = BanygTheme.colors.surfaceDarkElevated
                ) {
                    if (category.isHidden) {
                        DropdownMenuItem(
                            text = { Text("Restore", color = BanygTheme.colors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = BanygTheme.colors.textSecondary
                                )
                            },
                            onClick = {
                                onRestore()
                                showMenu = false
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Edit", color = BanygTheme.colors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = BanygTheme.colors.textSecondary
                                )
                            },
                            onClick = {
                                onEdit()
                                showMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Hide", color = BanygTheme.colors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = BanygTheme.colors.textSecondary
                                )
                            },
                            onClick = {
                                onToggleHidden()
                                showMenu = false
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Delete", color = BanygTheme.colors.errorRed) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = BanygTheme.colors.errorRed
                            )
                        },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriesEmptyState(
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    GradientCard(
        gradient = BanygGradients.SubtleDark,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small),
            modifier = Modifier.padding(BanygTheme.spacing.regular)
        ) {
            Text(
                text = "No categories yet",
                style = MaterialTheme.typography.titleMedium,
                color = BanygTheme.colors.textPrimary
            )
            Text(
                text = "Create categories to organize your transactions.",
                style = MaterialTheme.typography.bodySmall,
                color = BanygTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(BanygTheme.spacing.small))
            OutlinedPillButton(
                text = "Create category",
                onClick = onAddCategory
            )
        }
    }
}

@Composable
private fun CategoriesLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(BanygTheme.spacing.screenPadding)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.headlineLarge,
            color = BanygTheme.colors.textPrimary
        )

        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        BanygTheme.colors.surfaceDarkElevated,
                        shape = BanygTheme.shapes.card
                    )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = BanygTheme.colors.limeGreen,
                strokeWidth = 2.dp
            )
            Text(
                text = "Loading categories",
                style = MaterialTheme.typography.bodySmall,
                color = BanygTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun CategoriesError(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular),
            modifier = Modifier.padding(BanygTheme.spacing.screenPadding)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = BanygTheme.colors.textSecondary
            )
            if (canRetry) {
                OutlinedPillButton(
                    text = "Retry",
                    onClick = onRetry
                )
            }
        }
    }
}

@Composable
private fun parseColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        BanygTheme.colors.limeGreen
    }
}

// Previews

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CategoriesLoadingPreview() {
    BanygTheme {
        CategoriesScreen(
            uiState = CategoriesUiState.Loading,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddCategory = {},
            onNavigateToEditCategory = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CategoriesEmptyPreview() {
    BanygTheme {
        CategoriesScreen(
            uiState = CategoriesUiState.Success(
                categories = emptyList(),
                groupedCategories = emptyMap(),
                hiddenCategories = emptyList()
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddCategory = {},
            onNavigateToEditCategory = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CategoriesSuccessPreview() {
    BanygTheme {
        CategoriesScreen(
            uiState = CategoriesUiState.Success(
                categories = listOf(
                    Category(
                        id = "1",
                        name = "Groceries",
                        groupId = CategoryGroups.FOOD,
                        groupName = "Food",
                        color = "#FF9800",
                        icon = null
                    ),
                    Category(
                        id = "2",
                        name = "Dining Out",
                        groupId = CategoryGroups.FOOD,
                        groupName = "Food",
                        color = "#FF9800",
                        icon = null
                    ),
                    Category(
                        id = "3",
                        name = "Fuel",
                        groupId = CategoryGroups.TRANSPORTATION,
                        groupName = "Transportation",
                        color = "#2196F3",
                        icon = null
                    ),
                    Category(
                        id = "4",
                        name = "Salary",
                        groupId = CategoryGroups.INCOME,
                        groupName = "Income",
                        color = "#4CAF50",
                        icon = null
                    )
                ),
                groupedCategories = mapOf(
                    "Food" to listOf(
                        Category(
                            id = "1",
                            name = "Groceries",
                            groupId = CategoryGroups.FOOD,
                            groupName = "Food",
                            color = "#FF9800",
                            icon = null
                        ),
                        Category(
                            id = "2",
                            name = "Dining Out",
                            groupId = CategoryGroups.FOOD,
                            groupName = "Food",
                            color = "#FF9800",
                            icon = null
                        )
                    ),
                    "Transportation" to listOf(
                        Category(
                            id = "3",
                            name = "Fuel",
                            groupId = CategoryGroups.TRANSPORTATION,
                            groupName = "Transportation",
                            color = "#2196F3",
                            icon = null
                        )
                    ),
                    "Income" to listOf(
                        Category(
                            id = "4",
                            name = "Salary",
                            groupId = CategoryGroups.INCOME,
                            groupName = "Income",
                            color = "#4CAF50",
                            icon = null
                        )
                    )
                ),
                hiddenCategories = emptyList()
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddCategory = {},
            onNavigateToEditCategory = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CategoriesErrorPreview() {
    BanygTheme {
        CategoriesScreen(
            uiState = CategoriesUiState.Error(message = "Unable to load categories"),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
            onNavigateToAddCategory = {},
            onNavigateToEditCategory = {}
        )
    }
}
