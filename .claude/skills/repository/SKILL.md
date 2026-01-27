---
name: repository
description: Generate repository pattern implementations following Clean Architecture with interface + impl separation
disable-model-invocation: true
---

# Repository Generator

Generate repository pattern implementations following Clean Architecture principles.

## Output Structure

### 1. Repository Interface (Domain Layer)
```kotlin
// core/domain/repository/EntityRepository.kt

interface EntityRepository {
    
    // Single entity operations
    suspend fun getById(id: String): Entity?
    suspend fun save(entity: Entity)
    suspend fun delete(entity: Entity)
    suspend fun deleteById(id: String)
    
    // Bulk operations
    suspend fun insertAll(entities: List<Entity>)
    suspend fun updateAll(entities: List<Entity>)
    
    // Observation (reactive)
    fun observeById(id: String): Flow<Entity?>
    fun observeAll(): Flow<List<Entity>>
    
    // Queries
    suspend fun getAll(): List<Entity>
    suspend fun getByFilter(filter: EntityFilter): List<Entity>
    suspend fun exists(id: String): Boolean
    suspend fun count(): Int
}
```

### 2. Repository Implementation (Data Layer)
```kotlin
// core/data/repository/EntityRepositoryImpl.kt

class EntityRepositoryImpl @Inject constructor(
    private val entityDao: EntityDao,
    private val mapper: EntityMapper
) : EntityRepository {

    override suspend fun getById(id: String): Entity? {
        return entityDao.getById(id)?.let { mapper.toDomain(it) }
    }

    override suspend fun save(entity: Entity) {
        val entityData = mapper.toEntity(entity)
        entityDao.insert(entityData)
    }

    override suspend fun delete(entity: Entity) {
        entityDao.delete(mapper.toEntity(entity))
    }

    override suspend fun deleteById(id: String) {
        entityDao.deleteById(id)
    }

    override suspend fun insertAll(entities: List<Entity>) {
        val entityDataList = entities.map { mapper.toEntity(it) }
        entityDao.insertAll(entityDataList)
    }

    override suspend fun updateAll(entities: List<Entity>) {
        val entityDataList = entities.map { mapper.toEntity(it) }
        entityDao.updateAll(entityDataList)
    }

    override fun observeById(id: String): Flow<Entity?> {
        return entityDao.observeById(id)
            .map { it?.let { mapper.toDomain(it) } }
            .flowOn(Dispatchers.IO)
    }

    override fun observeAll(): Flow<List<Entity>> {
        return entityDao.observeAll()
            .map { entities -> entities.map { mapper.toDomain(it) } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getAll(): List<Entity> {
        return entityDao.getAll().map { mapper.toDomain(it) }
    }

    override suspend fun getByFilter(filter: EntityFilter): List<Entity> {
        return entityDao.getAll()
            .map { mapper.toDomain(it) }
            .filter { filter.matches(it) }
    }

    override suspend fun exists(id: String): Boolean {
        return entityDao.getById(id) != null
    }

    override suspend fun count(): Int {
        return entityDao.count()
    }
}
```

### 3. Repository Module (Hilt DI)
```kotlin
// core/data/di/RepositoryModule.kt

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindEntityRepository(
        impl: EntityRepositoryImpl
    ): EntityRepository
}
```

### 4. Repository Implementation Test
```kotlin
// core/data/repository/EntityRepositoryImplTest.kt

@ExperimentalCoroutinesApi
class EntityRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dao: EntityDao
    private lateinit var mapper: EntityMapper
    private lateinit var repository: EntityRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk()
        mapper = mockk()
        repository = EntityRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getById returns mapped entity`() = runTest {
        // Given
        val entityData = EntityData(id = "1", name = "Test")
        val domainEntity = Entity(id = "1", name = "Test")
        
        coEvery { dao.getById("1") } returns entityData
        every { mapper.toDomain(entityData) } returns domainEntity

        // When
        val result = repository.getById("1")

        // Then
        assertEquals(domainEntity, result)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        // Given
        coEvery { dao.getById("1") } returns null

        // When
        val result = repository.getById("1")

        // Then
        assertNull(result)
    }

    @Test
    fun `save inserts entity via dao`() = runTest {
        // Given
        val domainEntity = Entity(id = "1", name = "Test")
        val entityData = EntityData(id = "1", name = "Test")
        
        every { mapper.toEntity(domainEntity) } returns entityData
        coEvery { dao.insert(entityData) } just Runs

        // When
        repository.save(domainEntity)

        // Then
        coVerify { dao.insert(entityData) }
    }

    @Test
    fun `observeAll emits mapped entities`() = runTest {
        // Given
        val entityDataList = listOf(EntityData(id = "1", name = "Test"))
        val domainList = listOf(Entity(id = "1", name = "Test"))
        
        every { dao.observeAll() } returns flowOf(entityDataList)
        every { mapper.toDomain(any()) } returns domainList[0]

        // When & Then
        repository.observeAll().test {
            assertEquals(domainList, awaitItem())
            awaitComplete()
        }
    }
}
```

## Money-Safe Repository Pattern

For repositories dealing with money:

```kotlin
// core/domain/repository/TransactionRepository.kt

interface TransactionRepository {
    suspend fun getById(id: String): Transaction?
    suspend fun save(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    
    // Money-specific queries
    suspend fun getBalanceForAccount(accountId: String): Long
    suspend fun getTotalSpentForCategory(
        categoryId: String, 
        monthKey: String
    ): Long
    
    fun observeByAccount(accountId: String): Flow<List<Transaction>>
    fun observeByCategory(categoryId: String): Flow<List<Transaction>>
}

// core/data/repository/TransactionRepositoryImpl.kt

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val mapper: TransactionMapper
) : TransactionRepository {

    override suspend fun getBalanceForAccount(accountId: String): Long {
        return transactionDao.getSumByAccount(accountId) ?: 0L
    }

    override suspend fun getTotalSpentForCategory(
        categoryId: String,
        monthKey: String
    ): Long {
        val (startDate, endDate) = monthKey.toDateRange()
        
        return transactionDao.getTransactionsForCategoryInRange(
            categoryId = categoryId,
            startDate = startDate,
            endDate = endDate
        )
            .filter { it.amountMinor < 0 }  // Only expenses
            .sumOf { kotlin.math.abs(it.amountMinor) }
    }

    private fun String.toDateRange(): Pair<Long, Long> {
        val parts = split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        
        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
        
        return start.toEpochDay() to end.toEpochDay()
    }
}
```

## Repository Patterns

### Soft Delete Pattern
```kotlin
interface SoftDeleteRepository {
    suspend fun delete(entity: Entity) {
        // Soft delete: mark as deleted instead of removing
        val deleted = entity.copy(deletedAt = System.currentTimeMillis())
        save(deleted)
    }
    
    suspend fun permanentlyDelete(entity: Entity)
    
    suspend fun restore(id: String): Entity?
}
```

### Cached Repository Pattern
```kotlin
class CachedEntityRepository @Inject constructor(
    private val localSource: EntityLocalDataSource,
    private val remoteSource: EntityRemoteDataSource? = null
) : EntityRepository {
    
    private val cache = MutableStateFlow<List<Entity>?>(null)
    
    override fun observeAll(): Flow<List<Entity>> {
        return cache.filterNotNull()
    }
    
    override suspend fun getAll(): List<Entity> {
        cache.value?.let { return it }
        
        val entities = localSource.getAll()
        cache.value = entities
        return entities
    }
    
    override suspend fun save(entity: Entity) {
        localSource.save(entity)
        remoteSource?.save(entity)
        invalidateCache()
    }
    
    private fun invalidateCache() {
        cache.value = null
    }
}
```

### Transaction-Aware Repository
```kotlin
class TransactionalRepository @Inject constructor(
    private val db: BanygDatabase,
    private val entityDao: EntityDao,
    private val relatedDao: RelatedDao
) {
    suspend fun performComplexOperation(entities: List<Entity>) {
        db.withTransaction {
            // All operations in this block are atomic
            entities.forEach { entity ->
                entityDao.insert(mapper.toEntity(entity))
                relatedDao.insert(createRelated(entity))
            }
        }
    }
}
```

## Requirements

**Always include:**
- Interface in `core/domain/repository/` (domain layer)
- Implementation in `core/data/repository/` (data layer)
- Constructor injection with `@Inject`
- Mapper for entity <-> domain conversion
- Both suspend and Flow-based methods where appropriate
- Hilt module binding
- Comprehensive unit tests with mocked DAO

**Repository responsibilities:**
- Coordinate data sources (local, remote)
- Handle data mapping between layers
- Implement business logic for data access patterns
- Manage caching strategy
- Handle transactions for multi-table operations

**NOT repository responsibilities:**
- UI logic
- Business rules validation (use cases do this)
- Direct database queries (DAO does this)
- Network calls (data source does this)

## Workflow

1. Define repository interface in domain layer
2. Identify required DAO methods
3. Create mapper for entity <-> domain conversion
4. Implement repository in data layer
5. Add Hilt binding in RepositoryModule
6. Write unit tests with mocked dependencies
7. Use in use cases

## Testing Guidelines

- Mock the DAO, not the database
- Test mapping logic
- Test Flow emissions
- Test error handling
- Test caching behavior if applicable

## Anti-Patterns to Avoid

```kotlin
// ❌ DON'T: Skip the interface
data class BadRepository @Inject constructor(
    private val dao: EntityDao
)

// ❌ DON'T: Expose implementation details
interface BadRepository {
    fun getDao(): EntityDao  // Leaks implementation
}

// ❌ DON'T: Do business logic in repository
class BadRepository {
    suspend fun createEntity(data: EntityData) {
        if (data.amount > 1000) {  // Business rule!
            throw IllegalArgumentException()
        }
    }
}

// ✅ DO: Clean separation
interface GoodRepository {
    suspend fun save(entity: Entity)
}

class GoodRepositoryImpl @Inject constructor(
    private val dao: EntityDao,
    private val mapper: EntityMapper
) : GoodRepository {
    override suspend fun save(entity: Entity) {
        dao.insert(mapper.toEntity(entity))
    }
}
```
