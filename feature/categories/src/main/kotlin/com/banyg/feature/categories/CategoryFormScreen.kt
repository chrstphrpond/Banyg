package com.banyg.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.domain.model.CategoryGroups
import com.banyg.ui.components.PillButton
import com.banyg.ui.theme.BanygTheme

@Composable
fun CategoryFormRoute(
    categoryId: String?,
    onNavigateBack: () -> Unit,
    viewModel: CategoryFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(categoryId) {
        viewModel.loadCategory(categoryId)
    }

    val title = when (uiState) {
        is CategoryFormUiState.Edit -> "Edit Category"
        is CategoryFormUiState.Create -> "New Category"
        else -> "Category"
    }

    CategoryFormScreen(
        title = title,
        uiState = uiState,
        onEvent = { event ->
            if (event is CategoryFormUiEvent.OnCancel) {
                onNavigateBack()
            } else {
                viewModel.onEvent(event, onSuccess = onNavigateBack)
            }
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryFormScreen(
    title: String,
    uiState: CategoryFormUiState,
    onEvent: (CategoryFormUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BanygTheme.colors.backgroundDark,
        topBar = {
            TopAppBar(
                title = { Text(title, color = BanygTheme.colors.textPrimary) },
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
                ),
                actions = {
                    if (uiState is CategoryFormUiState.Edit) {
                        TextButton(
                            onClick = { onEvent(CategoryFormUiEvent.OnSave) },
                            enabled = !uiState.isSaving && uiState.name.isNotBlank()
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BanygTheme.colors.limeGreen,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Save",
                                    color = BanygTheme.colors.limeGreen
                                )
                            }
                        }
                    } else if (uiState is CategoryFormUiState.Create) {
                        TextButton(
                            onClick = { onEvent(CategoryFormUiEvent.OnSave) },
                            enabled = !uiState.isSaving && uiState.name.isNotBlank()
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BanygTheme.colors.limeGreen,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Create",
                                    color = BanygTheme.colors.limeGreen
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            CategoryFormUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BanygTheme.colors.limeGreen)
                }
            }

            is CategoryFormUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BanygTheme.colors.errorRed
                    )
                }
            }

            is CategoryFormUiState.Create -> {
                CategoryFormContent(
                    name = uiState.name,
                    groupId = uiState.groupId,
                    groupName = uiState.groupName,
                    icon = uiState.icon,
                    color = uiState.color,
                    error = uiState.error,
                    onNameChange = { onEvent(CategoryFormUiEvent.OnNameChange(it)) },
                    onGroupChange = { groupId, groupName ->
                        onEvent(CategoryFormUiEvent.OnGroupChange(groupId, groupName))
                    },
                    onIconChange = { onEvent(CategoryFormUiEvent.OnIconChange(it)) },
                    onColorChange = { onEvent(CategoryFormUiEvent.OnColorChange(it)) },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is CategoryFormUiState.Edit -> {
                CategoryFormContent(
                    name = uiState.name,
                    groupId = uiState.groupId,
                    groupName = uiState.groupName,
                    icon = uiState.icon,
                    color = uiState.color,
                    error = uiState.error,
                    onNameChange = { onEvent(CategoryFormUiEvent.OnNameChange(it)) },
                    onGroupChange = { groupId, groupName ->
                        onEvent(CategoryFormUiEvent.OnGroupChange(groupId, groupName))
                    },
                    onIconChange = { onEvent(CategoryFormUiEvent.OnIconChange(it)) },
                    onColorChange = { onEvent(CategoryFormUiEvent.OnColorChange(it)) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormContent(
    name: String,
    groupId: String?,
    groupName: String?,
    icon: String?,
    color: String?,
    error: String?,
    onNameChange: (String) -> Unit,
    onGroupChange: (String?, String?) -> Unit,
    onIconChange: (String?) -> Unit,
    onColorChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val groups = remember {
        listOf(
            null to "None",
            CategoryGroups.INCOME to "Income",
            CategoryGroups.FOOD to "Food",
            CategoryGroups.TRANSPORTATION to "Transportation",
            CategoryGroups.SHOPPING to "Shopping",
            CategoryGroups.BILLS to "Bills & Utilities",
            CategoryGroups.ENTERTAINMENT to "Entertainment",
            CategoryGroups.HEALTH to "Health & Fitness",
            CategoryGroups.EDUCATION to "Education",
            CategoryGroups.TRANSFER to "Transfer",
            CategoryGroups.OTHER to "Other"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(BanygTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
    ) {
        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Category Name", color = BanygTheme.colors.textSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            isError = error != null,
            supportingText = error?.let { { Text(it, color = BanygTheme.colors.errorRed) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BanygTheme.colors.textPrimary,
                unfocusedTextColor = BanygTheme.colors.textPrimary,
                focusedBorderColor = BanygTheme.colors.limeGreen,
                unfocusedBorderColor = BanygTheme.colors.textTertiary,
                focusedLabelColor = BanygTheme.colors.limeGreen,
                unfocusedLabelColor = BanygTheme.colors.textSecondary,
                errorBorderColor = BanygTheme.colors.errorRed,
                errorLabelColor = BanygTheme.colors.errorRed
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Group dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = groupName ?: "None",
                onValueChange = {},
                readOnly = true,
                label = { Text("Group", color = BanygTheme.colors.textSecondary) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BanygTheme.colors.textPrimary,
                    unfocusedTextColor = BanygTheme.colors.textPrimary,
                    focusedBorderColor = BanygTheme.colors.limeGreen,
                    unfocusedBorderColor = BanygTheme.colors.textTertiary,
                    focusedLabelColor = BanygTheme.colors.limeGreen,
                    unfocusedLabelColor = BanygTheme.colors.textSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = BanygTheme.colors.surfaceDarkElevated
            ) {
                groups.forEach { (id, displayName) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                displayName,
                                color = BanygTheme.colors.textPrimary
                            )
                        },
                        onClick = {
                            onGroupChange(id, displayName.takeIf { it != "None" })
                            expanded = false
                        }
                    )
                }
            }
        }

        // Color selection
        Text(
            text = "Color",
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary,
            modifier = Modifier.padding(top = BanygTheme.spacing.small)
        )

        ColorPicker(
            selectedColor = color,
            onColorSelected = onColorChange
        )

        // Icon selection (simplified - just text for now)
        Text(
            text = "Icon",
            style = MaterialTheme.typography.bodyMedium,
            color = BanygTheme.colors.textSecondary,
            modifier = Modifier.padding(top = BanygTheme.spacing.small)
        )

        IconPicker(
            selectedIcon = icon,
            onIconSelected = onIconChange
        )
    }
}

@Composable
private fun ColorPicker(
    selectedColor: String?,
    onColorSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = remember {
        listOf(
            "#4CAF50" to Color(0xFF4CAF50),  // Green
            "#8BC34A" to Color(0xFF8BC34A),  // Light Green
            "#CDDC39" to Color(0xFFCDDC39),  // Lime
            "#FFEB3B" to Color(0xFFFFEB3B),  // Yellow
            "#FFC107" to Color(0xFFFFC107),  // Amber
            "#FF9800" to Color(0xFFFF9800),  // Orange
            "#FF5722" to Color(0xFFFF5722),  // Deep Orange
            "#F44336" to Color(0xFFF44336),  // Red
            "#E91E63" to Color(0xFFE91E63),  // Pink
            "#9C27B0" to Color(0xFF9C27B0),  // Purple
            "#673AB7" to Color(0xFF673AB7),  // Deep Purple
            "#3F51B5" to Color(0xFF3F51B5),  // Indigo
            "#2196F3" to Color(0xFF2196F3),  // Blue
            "#03A9F4" to Color(0xFF03A9F4),  // Light Blue
            "#00BCD4" to Color(0xFF00BCD4),  // Cyan
            "#009688" to Color(0xFF009688),  // Teal
            "#795548" to Color(0xFF795548),  // Brown
            "#607D8B" to Color(0xFF607D8B),  // Blue Grey
            "#9E9E9E" to Color(0xFF9E9E9E),  // Grey
            "#000000" to Color(0xFF000000)   // Black
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small)
    ) {
        // Selected color preview
        selectedColor?.let { hex ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.regular)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(parseColor(hex))
                )
                Text(
                    text = hex.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = BanygTheme.colors.textSecondary
                )
            }
        }

        // Color grid
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            colors.take(10).forEach { (hex, color) ->
                ColorOption(
                    color = color,
                    isSelected = selectedColor == hex,
                    onClick = { onColorSelected(hex) }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            colors.drop(10).forEach { (hex, color) ->
                ColorOption(
                    color = color,
                    isSelected = selectedColor == hex,
                    onClick = { onColorSelected(hex) }
                )
            }
        }
    }
}

@Composable
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.padding(2.dp)
                        .background(BanygTheme.colors.backgroundDark, CircleShape)
                        .padding(2.dp)
                        .background(color, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Click handling overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier
                    } else {
                        Modifier.background(Color.Transparent)
                    }
                )
        ) {
            // Invisible click target
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
private fun IconPicker(
    selectedIcon: String?,
    onIconSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val icons = remember {
        listOf(
            null to "Default",
            "restaurant" to "Food",
            "transport" to "Transport",
            "shopping" to "Shopping",
            "receipt" to "Bills",
            "entertainment" to "Fun",
            "health" to "Health",
            "income" to "Income",
            "flight" to "Travel",
            "swap" to "Transfer",
            "category" to "Other"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(BanygTheme.spacing.small),
        modifier = modifier.fillMaxWidth()
    ) {
        icons.forEach { (iconId, label) ->
            PillButton(
                text = label,
                onClick = { onIconSelected(iconId) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

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
private fun CategoryFormCreatePreview() {
    BanygTheme {
        CategoryFormScreen(
            title = "New Category",
            uiState = CategoryFormUiState.Create(
                name = "Groceries",
                groupId = CategoryGroups.FOOD,
                groupName = "Food",
                color = "#FF9800"
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CategoryFormEditPreview() {
    BanygTheme {
        CategoryFormScreen(
            title = "Edit Category",
            uiState = CategoryFormUiState.Edit(
                category = com.banyg.domain.model.Category(
                    id = "1",
                    name = "Groceries",
                    groupId = CategoryGroups.FOOD,
                    groupName = "Food",
                    color = "#FF9800"
                ),
                name = "Groceries",
                groupId = CategoryGroups.FOOD,
                groupName = "Food",
                color = "#FF9800",
                isHidden = false
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CategoryFormLoadingPreview() {
    BanygTheme {
        CategoryFormScreen(
            title = "Edit Category",
            uiState = CategoryFormUiState.Loading,
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun CategoryFormErrorPreview() {
    BanygTheme {
        CategoryFormScreen(
            title = "Edit Category",
            uiState = CategoryFormUiState.Error("Category not found"),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
