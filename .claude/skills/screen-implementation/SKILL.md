---
name: screen-implementation
description: Implement a complete screen end-to-end with UI state, ViewModel, repository, error states, and loading skeletons
disable-model-invocation: true
---

# Screen Implementation Generator

Generate a complete screen implementation following Banyg architecture patterns.

## Full Screen Stack

### 1. UI State
```kotlin
// feature/[feature]/[Screen]UiState.kt

sealed interface ScreenNameUiState {
    data object Loading : ScreenNameUiState

    data class Success(
        val items: List<Item>,
        val summary: Summary,
        val isRefreshing: Boolean = false
    ) : ScreenNameUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : ScreenNameUiState
}

// UI Events from user interactions
sealed interface ScreenNameUiEvent {
    data class OnItemClick(val id: String) : ScreenNameUiEvent
    data object OnRefresh : ScreenNameUiEvent
    data object OnRetry : ScreenNameUiEvent
    data class OnSearchQuery(val query: String) : ScreenNameUiEvent
}
```

### 2. ViewModel
```kotlin
// feature/[feature]/[Screen]ViewModel.kt

@HiltViewModel
class ScreenNameViewModel @Inject constructor(
    private val repository: Repository,
    private val useCase: UseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenNameUiState>(ScreenNameUiState.Loading)
    val uiState: StateFlow<ScreenNameUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: ScreenNameUiEvent) {
        when (event) {
            is ScreenNameUiEvent.OnItemClick -> handleItemClick(event.id)
            ScreenNameUiEvent.OnRefresh -> refreshData()
            ScreenNameUiEvent.OnRetry -> loadData()
            is ScreenNameUiEvent.OnSearchQuery -> searchData(event.query)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = ScreenNameUiState.Loading

            try {
                repository.getData()
                    .catch { e ->
                        _uiState.value = ScreenNameUiState.Error(
                            message = e.message ?: "Failed to load data",
                            canRetry = true
                        )
                    }
                    .collect { data ->
                        _uiState.value = ScreenNameUiState.Success(
                            items = data.items,
                            summary = data.summary
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = ScreenNameUiState.Error(
                    message = e.message ?: "Unknown error",
                    canRetry = true
                )
            }
        }
    }

    private fun handleItemClick(id: String) {
        // Handle item click
    }

    private fun refreshData() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ScreenNameUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
                // Refresh logic
            }
        }
    }
}
```

### 3. Screen Composable
```kotlin
// feature/[feature]/[Screen]Screen.kt

@Composable
fun ScreenNameRoute(
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ScreenNameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenNameScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun ScreenNameScreen(
    uiState: ScreenNameUiState,
    onEvent: (ScreenNameUiEvent) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Title") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is ScreenNameUiState.Loading -> {
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }

            is ScreenNameUiState.Success -> {
                SuccessContent(
                    state = uiState,
                    onEvent = onEvent,
                    onNavigateToDetail = onNavigateToDetail,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is ScreenNameUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    canRetry = uiState.canRetry,
                    onRetry = { onEvent(ScreenNameUiEvent.OnRetry) },
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygSpacing.md)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessContent(
    state: ScreenNameUiState.Success,
    onEvent: (ScreenNameUiEvent) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { onEvent(ScreenNameUiEvent.OnRefresh) }
    )

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        LazyColumn(
            contentPadding = PaddingValues(BanygSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BanygSpacing.sm)
        ) {
            items(
                items = state.items,
                key = { it.id }
            ) { item ->
                ItemCard(
                    item = item,
                    onClick = { onNavigateToDetail(item.id) }
                )
            }
        }

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
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
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BanygSpacing.md),
            modifier = Modifier.padding(BanygSpacing.lg)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (canRetry) {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
```

### 4. Loading Skeleton
```kotlin
@Composable
private fun ItemSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(BanygSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
            )

            Spacer(modifier = Modifier.height(BanygSpacing.sm))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}
```

### 5. Preview Composables
```kotlin
@Preview(name = "Loading State")
@Composable
private fun ScreenNameLoadingPreview() {
    BanygTheme {
        ScreenNameScreen(
            uiState = ScreenNameUiState.Loading,
            onEvent = {},
            onNavigateToDetail = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Success State")
@Composable
private fun ScreenNameSuccessPreview() {
    BanygTheme {
        ScreenNameScreen(
            uiState = ScreenNameUiState.Success(
                items = listOf(/* sample data */),
                summary = Summary(/* sample data */)
            ),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Error State")
@Composable
private fun ScreenNameErrorPreview() {
    BanygTheme {
        ScreenNameScreen(
            uiState = ScreenNameUiState.Error(
                message = "Failed to load data. Please check your connection.",
                canRetry = true
            ),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigateBack = {}
        )
    }
}
```

### 6. Navigation Integration
```kotlin
// feature/[feature]/navigation/[Screen]Navigation.kt

const val SCREEN_NAME_ROUTE = "screen_name"

fun NavController.navigateToScreenName() {
    navigate(SCREEN_NAME_ROUTE)
}

fun NavGraphBuilder.screenNameScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(route = SCREEN_NAME_ROUTE) {
        ScreenNameRoute(
            onNavigateToDetail = onNavigateToDetail,
            onNavigateBack = onNavigateBack
        )
    }
}
```

## Requirements

**Always include:**
- Sealed interface for UI state (Loading, Success, Error)
- Sealed interface for UI events
- ViewModel with StateFlow
- Stateless screen composable
- Route composable that collects state
- Loading state with indicator or skeleton
- Error state with retry option
- Pull-to-refresh (if applicable)
- Proper navigation callbacks
- Multiple preview composables
- Accessibility semantics

**Follow Banyg patterns:**
- Use Material 3 components
- Apply spacing from BanygSpacing
- Handle empty states gracefully
- Show user-friendly error messages
- Loading states should feel calm (no jarring transitions)

## Optional Enhancements

Add if relevant to the screen:
- Search functionality
- Filter/sort options
- Floating action button
- Bottom sheet
- Swipe to delete
- Empty state illustration
- Pagination (if large lists)

## Workflow

1. Understand screen requirements
2. Define UI state variants (Loading, Success, Error)
3. Define UI events (user interactions)
4. Create ViewModel with StateFlow
5. Implement repository/use case calls
6. Create screen composable (route + stateless)
7. Implement Loading, Success, Error content
8. Add loading skeleton or indicator
9. Add pull-to-refresh if applicable
10. Create preview composables for all states
11. Wire up navigation
12. Test all states (loading, success, error, empty)
