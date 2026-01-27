---
name: feature-module
description: Scaffold a new feature module with complete structure including build.gradle.kts, package structure, and navigation setup
disable-model-invocation: true
---

# Feature Module Generator

Scaffold a complete feature module following Banyg architecture and conventions.

## Output Structure

```
feature/[feature-name]/
├── build.gradle.kts
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/banyg/feature/[feature]/
│       │       ├── [Feature]Screen.kt
│       │       ├── [Feature]ViewModel.kt
│       │       ├── [Feature]UiState.kt
│       │       ├── components/
│       │       │   └── (feature-specific components)
│       │       └── navigation/
│       │           └── [Feature]Navigation.kt
│       └── res/
│           ├── values/
│           │   └── strings.xml
│           └── drawable/
└── src/test/
    └── kotlin/
        └── com/banyg/feature/[feature]/
            └── (unit tests)
```

## 1. build.gradle.kts
```kotlin
// feature/[feature]/build.gradle.kts

plugins {
    alias(libs.plugins.banyg.android.feature)
    alias(libs.plugins.banyg.hilt)
}

android {
    namespace = "com.banyg.feature.[feature]"
}

dependencies {
    // Core modules
    implementation(project(":core:ui"))
    implementation(project(":core:domain"))
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
```

## 2. UI State
```kotlin
// feature/[feature]/[Feature]UiState.kt

package com.banyg.feature.[feature]

sealed interface FeatureUiState {
    data object Loading : FeatureUiState
    
    data class Success(
        val data: FeatureData,
        val isRefreshing: Boolean = false
    ) : FeatureUiState
    
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : FeatureUiState
}

data class FeatureData(
    val items: List<FeatureItem>,
    // Add feature-specific data fields
)

data class FeatureItem(
    val id: String,
    val title: String,
    val subtitle: String? = null
)

sealed interface FeatureUiEvent {
    data class OnItemClick(val id: String) : FeatureUiEvent
    data object OnRefresh : FeatureUiEvent
    data object OnRetry : FeatureUiEvent
}
```

## 3. ViewModel
```kotlin
// feature/[feature]/[Feature]ViewModel.kt

package com.banyg.feature.[feature]

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureViewModel @Inject constructor(
    // Inject use cases here
    // private val getDataUseCase: GetDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeatureUiState>(FeatureUiState.Loading)
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: FeatureUiEvent) {
        when (event) {
            is FeatureUiEvent.OnItemClick -> handleItemClick(event.id)
            FeatureUiEvent.OnRefresh -> refreshData()
            FeatureUiEvent.OnRetry -> loadData()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = FeatureUiState.Loading
            
            try {
                // Call use case
                // val data = getDataUseCase()
                // _uiState.value = FeatureUiState.Success(data = data)
            } catch (e: Exception) {
                _uiState.value = FeatureUiState.Error(
                    message = e.message ?: "Unknown error occurred",
                    canRetry = true
                )
            }
        }
    }

    private fun handleItemClick(id: String) {
        // Handle navigation or action
    }

    private fun refreshData() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is FeatureUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
                // Refresh logic
            }
        }
    }
}
```

## 4. Screen
```kotlin
// feature/[feature]/[Feature]Screen.kt

package com.banyg.feature.[feature]

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banyg.ui.theme.BanygTheme

@Composable
fun FeatureRoute(
    onNavigateBack: () -> Unit,
    viewModel: FeatureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    FeatureScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureScreen(
    uiState: FeatureUiState,
    onEvent: (FeatureUiEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feature Title") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is FeatureUiState.Loading -> {
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
            
            is FeatureUiState.Success -> {
                SuccessContent(
                    state = uiState,
                    onEvent = onEvent,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            is FeatureUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    canRetry = uiState.canRetry,
                    onRetry = { onEvent(FeatureUiEvent.OnRetry) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SuccessContent(
    state: FeatureUiState.Success,
    onEvent: (FeatureUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implement feature-specific content
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Feature Content")
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            if (canRetry) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

@Preview
@Composable
private fun FeatureScreenPreview() {
    BanygTheme {
        FeatureScreen(
            uiState = FeatureUiState.Success(
                data = FeatureData(items = emptyList())
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
```

## 5. Navigation
```kotlin
// feature/[feature]/navigation/[Feature]Navigation.kt

package com.banyg.feature.[feature].navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.banyg.feature.[feature].FeatureRoute

const val FEATURE_ROUTE = "[feature]"

fun NavController.navigateToFeature() {
    navigate(FEATURE_ROUTE)
}

fun NavGraphBuilder.featureScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = FEATURE_ROUTE) {
        FeatureRoute(
            onNavigateBack = onNavigateBack
        )
    }
}
```

## 6. strings.xml
```xml
<!-- feature/[feature]/src/main/res/values/strings.xml -->

<resources>
    <string name="feature_title">Feature Title</string>
    <string name="feature_loading">Loading…</string>
    <string name="feature_error_generic">Something went wrong</string>
    <string name="feature_retry">Retry</string>
</resources>
```

## 7. settings.gradle.kts Update
```kotlin
// settings.gradle.kts (in project root)

include(
    ":app",
    ":core:domain",
    ":core:data",
    ":core:ui",
    ":feature:inbox",
    ":feature:accounts",
    ":feature:budget",
    ":feature:reports",
    ":feature:[feature]"  // Add new feature here
)
```

## 8. app/build.gradle.kts Update
```kotlin
// app/build.gradle.kts

dependencies {
    // Existing feature modules
    implementation(project(":feature:inbox"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:budget"))
    implementation(project(":feature:reports"))
    
    // New feature module
    implementation(project(":feature:[feature]"))
}
```

## 9. App Navigation Integration
```kotlin
// app/.../navigation/BanygNavHost.kt

@Composable
fun BanygNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = INBOX_ROUTE,
        modifier = modifier
    ) {
        // Existing feature screens
        inboxScreen(...)
        accountsScreen(...)
        budgetScreen(...)
        reportsScreen(...)
        
        // New feature screen
        featureScreen(
            onNavigateBack = navController::popBackStack
        )
    }
}
```

## Requirements

**Always include:**
- Proper build.gradle.kts with feature plugin
- UI State sealed interface (Loading, Success, Error)
- UI Event sealed interface for user actions
- ViewModel with StateFlow
- Route + Screen composables
- Navigation setup
- Preview composables
- String resources
- Settings and app build.gradle.kts updates

**Follow Banyg conventions:**
- Use BanygTheme for previews
- Material 3 components
- Proper padding with BanygSpacing
- Error states with retry option
- Loading states with progress indicator

## Workflow

1. Create directory structure
2. Create build.gradle.kts with dependencies
3. Create UI State and UI Event
4. Create ViewModel
5. Create Screen composable
6. Create Navigation setup
7. Add string resources
8. Update settings.gradle.kts
9. Update app/build.gradle.kts
10. Integrate into App navigation
11. Sync and build

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Module | kebab-case | `:feature:transaction-detail` |
| Package | lowercase | `com.banyg.feature.transactiondetail` |
| Screen | PascalCase + Screen | `TransactionDetailScreen` |
| ViewModel | PascalCase + ViewModel | `TransactionDetailViewModel` |
| UiState | PascalCase + UiState | `TransactionDetailUiState` |
| Route constant | UPPER_SNAKE | `TRANSACTION_DETAIL_ROUTE` |

## Verification Checklist

- [ ] Module builds successfully (`./gradlew :feature:[name]:build`)
- [ ] No unresolved dependencies
- [ ] Navigation works from app module
- [ ] Previews render correctly
- [ ] String resources externalized
- [ ] Hilt injection compiles
- [ ] Unit test structure in place
