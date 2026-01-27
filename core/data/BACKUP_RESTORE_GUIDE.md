# Database Backup & Restore Guide

This guide explains how the automatic backup system works in Banyg and how to restore from backups if needed.

## Overview

Banyg automatically creates database backups before migrations to protect your financial data. This is a critical safety feature for a finance app.

## Automatic Backup System

### When Backups Are Created

Backups are automatically created:
- Before any database migration runs
- On every database open (as a safety measure)
- Stored with timestamps for easy identification

### Backup Location

Backups are stored in app-private storage:

**Primary location (preferred):**
```
/storage/emulated/0/Android/data/com.banyg/files/database_backups/
```

**Fallback location (if external storage unavailable):**
```
/data/data/com.banyg/files/database_backups/
```

### Backup Naming Convention

```
banyg_backup_YYYYMMDD_HHMMSS.db
```

Example: `banyg_backup_20240128_143022.db`

### Backup Retention Policy

- **Maximum backups kept:** 5 (most recent)
- **Cleanup:** Old backups are automatically deleted when exceeding the limit
- **Storage management:** Prevents unlimited storage growth

### What Gets Backed Up

The entire SQLite database file is copied, including:
- All accounts
- All transactions
- All categories
- All budgets
- All splits
- Database schema and indices

## Restoring from Backup

### Option 1: Programmatic Restore (Recommended)

Use the `MigrationBackupCallback.restoreBackup()` method:

```kotlin
import com.banyg.data.local.migration.MigrationBackupCallback
import java.io.File

// List available backups
val backups = MigrationBackupCallback.listBackups(context)

// Select a backup (e.g., most recent)
val backupToRestore = backups.firstOrNull()

if (backupToRestore != null) {
    try {
        // IMPORTANT: Close database first!
        // This should be done when app is not running or database is closed

        MigrationBackupCallback.restoreBackup(
            context = context,
            backupFile = backupToRestore,
            databaseName = "banyg_database"
        )

        // Restart the app
        // The app will now use the restored database

    } catch (e: IOException) {
        // Handle restore failure
        Log.e("Restore", "Failed to restore backup", e)
    }
}
```

### Option 2: Manual Restore via ADB (Developer/Support)

For manual restore (requires USB debugging):

1. **List available backups:**
```bash
adb shell "ls -lh /storage/emulated/0/Android/data/com.banyg/files/database_backups/"
```

2. **Copy backup to computer:**
```bash
adb pull /storage/emulated/0/Android/data/com.banyg/files/database_backups/banyg_backup_YYYYMMDD_HHMMSS.db ./backup.db
```

3. **Stop the app:**
```bash
adb shell am force-stop com.banyg
```

4. **Backup current database (safety):**
```bash
adb pull /data/data/com.banyg/databases/banyg_database ./current_db_backup.db
```

5. **Replace with backup:**
```bash
adb push ./backup.db /data/data/com.banyg/databases/banyg_database
```

6. **Clear database cache files:**
```bash
adb shell "rm -f /data/data/com.banyg/databases/banyg_database-wal"
adb shell "rm -f /data/data/com.banyg/databases/banyg_database-shm"
```

7. **Restart the app:**
```bash
adb shell am start -n com.banyg/.MainActivity
```

### Option 3: Manual Restore via File Manager (Root Required)

⚠️ **Requires rooted device**

1. Use a root file manager (e.g., Root Explorer, MiXplorer)
2. Navigate to `/storage/emulated/0/Android/data/com.banyg/files/database_backups/`
3. Copy desired backup file
4. Force stop Banyg app
5. Navigate to `/data/data/com.banyg/databases/`
6. Backup current `banyg_database` file (optional but recommended)
7. Paste and rename backup file to `banyg_database`
8. Delete `banyg_database-wal` and `banyg_database-shm` if they exist
9. Restart Banyg app

## Implementing User-Facing Restore Feature

To add a restore feature in the app UI:

```kotlin
// ViewModel
class BackupRestoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun listBackups(): List<BackupFile> {
        return MigrationBackupCallback.listBackups(context).map { file ->
            BackupFile(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                date = Date(file.lastModified())
            )
        }
    }

    fun restoreBackup(backupFile: File, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Show warning dialog first!
                // "This will replace your current data. Continue?"

                MigrationBackupCallback.restoreBackup(context, backupFile)

                withContext(Dispatchers.Main) {
                    onComplete(Result.success(Unit))
                    // Restart app or show "restart required" dialog
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(e))
                }
            }
        }
    }
}

// UI
@Composable
fun BackupRestoreScreen(viewModel: BackupRestoreViewModel = hiltViewModel()) {
    val backups = remember { viewModel.listBackups() }

    LazyColumn {
        items(backups) { backup ->
            BackupItem(
                backup = backup,
                onRestore = {
                    // Show confirmation dialog
                    viewModel.restoreBackup(File(backup.path)) { result ->
                        // Handle result
                    }
                }
            )
        }
    }
}
```

## Backup Verification

To verify a backup file is valid:

```kotlin
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

fun verifyBackup(backupFile: File): Boolean {
    return try {
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(backupFile.absolutePath)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) {}
            })
            .build()

        val helper = factory.create(config)
        val db = helper.readableDatabase

        // Try to query accounts table
        db.query("SELECT COUNT(*) FROM accounts")
        db.close()

        true
    } catch (e: Exception) {
        false
    }
}
```

## Troubleshooting

### "Cannot restore: database file is locked"

**Cause:** Database is currently open.

**Solution:** Ensure the database is closed before restoring. This typically requires:
- Stopping the app completely
- Or implementing a "close database and restore" flow

### "Backup file is corrupt"

**Cause:** Backup was interrupted or storage corruption.

**Solution:**
- Try an older backup
- Use the verification method above to check backup integrity

### "No backups available"

**Cause:** App is newly installed or backups were cleared.

**Solution:**
- Check both external and internal storage locations
- Backups may be in `/data/data/com.banyg/files/database_backups/` if external storage is unavailable

### "Restore succeeded but data is old"

**Cause:** Restored an older backup.

**Solution:**
- Check backup timestamps carefully before restoring
- Consider implementing backup notes/descriptions

## Best Practices

1. **Always show confirmation before restore:** Clearly warn users that current data will be replaced

2. **Create emergency backup before restore:** The restore method automatically creates a "pre-restore" backup

3. **Restart app after restore:** Database connection must be reopened to use restored data

4. **Test backups periodically:** Implement a "verify backup" feature to ensure backups are valid

5. **Export backups to external storage:** Allow users to manually export backups for safekeeping

6. **Document restore process for users:** Provide clear in-app instructions

## Security Considerations

- Backups are stored in app-private directories (not accessible to other apps)
- Backups are cleared when app is uninstalled
- Consider encrypting backups if storing sensitive financial data
- Do not store backups in publicly accessible locations

## Future Enhancements

Consider implementing:

1. **Manual backup trigger:** Allow users to create backups on demand
2. **Backup descriptions:** Let users add notes to backups
3. **Cloud backup sync:** Upload encrypted backups to user's cloud storage
4. **Scheduled backups:** Create backups on a schedule (daily, weekly)
5. **Backup compression:** Reduce storage space usage
6. **Backup encryption:** Protect sensitive financial data
7. **Export to CSV:** Allow exporting data before migration as additional safety

## Technical Details

### Implementation

- **Callback:** `MigrationBackupCallback` extends `RoomDatabase.Callback`
- **Hook:** `onOpen()` method triggers before migrations
- **Thread safety:** Backup operations run on I/O thread
- **Error handling:** Failures are logged but don't block database operations

### Storage Requirements

- **Typical database size:** 1-10 MB (varies by usage)
- **5 backups:** 5-50 MB total
- **Location:** App-specific external storage (doesn't count against user quota)

### Performance

- **Backup duration:** ~10-50ms for small databases (< 10 MB)
- **Impact:** Minimal - runs asynchronously on first database access
- **Frequency:** Once per app launch (when database opens)

## Related Files

- Implementation: `core/data/src/main/kotlin/com/banyg/data/local/migration/MigrationBackupCallback.kt`
- Tests: `core/data/src/test/kotlin/com/banyg/data/local/migration/MigrationBackupCallbackTest.kt`
- Database: `core/data/src/main/kotlin/com/banyg/data/local/BanygDatabase.kt`

## Support

For issues or questions about backup/restore:
1. Check logs for backup creation confirmation
2. Verify backup files exist in expected locations
3. Test restore on non-production devices first
4. Contact support with backup file metadata (size, timestamp, location)
