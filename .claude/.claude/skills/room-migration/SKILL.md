---
name: room-migration
description: Create Room entities, DAOs, migrations with verification steps and tests
disable-model-invocation: true
---

# Room Migration Generator

Generate Room database components with proper migrations, verification, and tests.

## Output Components

### 1. Entity Class
```kotlin
// core/data/local/entity/[Entity]Entity.kt

@Entity(
    tableName = "table_name",
    indices = [
        Index(value = ["column_name"]),
        Index(value = ["foreign_key_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ParentEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EntityNameEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,  // Always Long for money

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
```

### 2. DAO Interface
```kotlin
// core/data/local/dao/[Entity]Dao.kt

@Dao
interface EntityNameDao {

    @Query("SELECT * FROM table_name WHERE id = :id")
    suspend fun getById(id: String): EntityNameEntity?

    @Query("SELECT * FROM table_name ORDER BY created_at DESC")
    fun observeAll(): Flow<List<EntityNameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EntityNameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EntityNameEntity>)

    @Update
    suspend fun update(entity: EntityNameEntity)

    @Delete
    suspend fun delete(entity: EntityNameEntity)

    @Query("DELETE FROM table_name WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    @Query("SELECT * FROM table_name WHERE parent_id = :parentId")
    suspend fun getWithRelations(parentId: String): List<EntityWithRelations>
}
```

### 3. Migration
```kotlin
// core/data/local/migration/Migration_X_to_Y.kt

val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `table_name` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `amount_minor` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
        """.trimIndent())

        // Create indices
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_table_name_column`
            ON `table_name` (`column_name`)
        """.trimIndent())

        // Add column to existing table (if altering)
        // db.execSQL("ALTER TABLE `existing_table` ADD COLUMN `new_column` TEXT")

        // Data migration (if needed)
        // db.execSQL("UPDATE `table_name` SET `new_column` = 'default_value'")
    }
}
```

### 4. Database Class Update
```kotlin
// core/data/local/BanygDatabase.kt

@Database(
    entities = [
        // ... existing entities,
        EntityNameEntity::class
    ],
    version = Y,  // Increment version
    exportSchema = true
)
abstract class BanygDatabase : RoomDatabase() {

    abstract fun entityNameDao(): EntityNameDao

    companion object {
        fun build(context: Context): BanygDatabase {
            return Room.databaseBuilder(
                context,
                BanygDatabase::class.java,
                "banyg_database"
            )
            .addMigrations(
                // ... existing migrations,
                MIGRATION_X_Y
            )
            .build()
        }
    }
}
```

### 5. Migration Test
```kotlin
// core/data/local/migration/MigrationTest.kt

@RunWith(AndroidJUnit4::class)
class MigrationXtoYTest {

    private val TEST_DB_NAME = "migration_test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BanygDatabase::class.java
    )

    @Test
    fun migrate_X_to_Y() {
        // Create database at version X
        val db = helper.createDatabase(TEST_DB_NAME, X).apply {
            // Insert test data in old schema
            execSQL("""
                INSERT INTO old_table (id, name, amount)
                VALUES ('test-id', 'Test', 100)
            """)
            close()
        }

        // Run migration
        helper.runMigrationsAndValidate(TEST_DB_NAME, Y, true, MIGRATION_X_Y)

        // Verify data was migrated correctly
        helper.runMigrationsAndValidate(TEST_DB_NAME, Y, true, MIGRATION_X_Y).apply {
            query("SELECT * FROM table_name WHERE id = 'test-id'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Test", cursor.getString(cursor.getColumnIndex("name")))
                assertEquals(100L, cursor.getLong(cursor.getColumnIndex("amount_minor")))
            }
            close()
        }
    }

    @Test
    fun migrate_X_to_Y_preserves_indices() {
        helper.createDatabase(TEST_DB_NAME, X).close()
        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, Y, true, MIGRATION_X_Y)

        // Verify indices exist
        val cursor = db.query("SELECT * FROM sqlite_master WHERE type='index' AND tbl_name='table_name'")
        assertTrue(cursor.count > 0)
        cursor.close()
        db.close()
    }
}
```

### 6. Repository Update
```kotlin
// core/data/repository/EntityNameRepositoryImpl.kt

class EntityNameRepositoryImpl(
    private val dao: EntityNameDao,
    private val mapper: EntityMapper
) : EntityNameRepository {

    override fun observeAll(): Flow<List<EntityName>> {
        return dao.observeAll()
            .map { entities -> entities.map { mapper.toDomain(it) } }
    }

    override suspend fun save(entity: EntityName) {
        dao.insert(mapper.toEntity(entity))
    }
}
```

## Requirements

**Always include:**
- Proper indices on foreign keys and frequently queried columns
- Money amounts as `Long` (amount_minor, not amount)
- Timestamps as `Long` (Unix epoch millis)
- Foreign key constraints with appropriate cascade rules
- Migration test that verifies:
  - Schema is created correctly
  - Data is preserved/migrated
  - Indices are created
  - Foreign keys work
- Export schema to `schemas/` directory

**Index these columns:**
- Foreign keys (accountId, categoryId, etc.)
- Date fields used in queries
- Status/state fields used in WHERE clauses
- Any column used in ORDER BY

## Common Patterns

### Money columns
```kotlin
// ✅ CORRECT
@ColumnInfo(name = "amount_minor")
val amountMinor: Long

// ❌ WRONG
@ColumnInfo(name = "amount")
val amount: Double
```

### Timestamps
```kotlin
@ColumnInfo(name = "created_at")
val createdAt: Long  // Unix epoch millis

@ColumnInfo(name = "date")
val date: Long  // YYYYMMDD as Long, or epoch millis
```

### Soft Delete
```kotlin
@ColumnInfo(name = "deleted_at")
val deletedAt: Long?  // Null if not deleted, timestamp if deleted

@Query("SELECT * FROM table_name WHERE deleted_at IS NULL")
fun observeActive(): Flow<List<EntityNameEntity>>
```

## Workflow

1. Define entity with proper column types (Long for money)
2. Add indices on foreign keys and query columns
3. Create DAO with common operations
4. Write migration SQL (CREATE TABLE, indices, ALTER if needed)
5. Update database version and entity list
6. Generate migration test (verify schema and data)
7. Update or create repository implementation
8. Create mapper if domain model differs from entity
9. Run migration test to verify
10. Export schema for version control

## Schema Export

Enable schema export in build.gradle:
```kotlin
android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
}
```

Commit schema JSON files to track database evolution.

## Before Schema Changes

1. Confirm the change is necessary
2. Plan the migration path
3. Consider backward compatibility
4. Update DATA_MODEL.md (if it exists)
5. Get approval for breaking changes
