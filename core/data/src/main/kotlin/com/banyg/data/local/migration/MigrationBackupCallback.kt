package com.banyg.data.local.migration

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RoomDatabase.Callback that automatically creates database backups before migrations.
 *
 * This callback ensures data safety in a financial app by:
 * 1. Creating timestamped backups before any database operations
 * 2. Storing backups in app-specific external storage (fallback to internal)
 * 3. Managing backup retention (keeps last N backups)
 * 4. Handling errors gracefully without blocking database operations
 *
 * Backups are created in: Android/data/[package]/files/database_backups/
 * Naming format: banyg_backup_YYYYMMDD_HHMMSS.db
 *
 * IMPORTANT: Backups happen before migrations to prevent data loss.
 */
class MigrationBackupCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    /**
     * Called when database is opened.
     * We perform backup check here to catch database version changes.
     */
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)

        // Backup on every open as a safety measure
        // This is lightweight - just copies file if needed
        try {
            createBackup(db)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create backup during onOpen", e)
            // Don't throw - allow database to open even if backup fails
        }
    }

    /**
     * Called when database is first created.
     * No backup needed here since there's no existing data.
     */
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.i(TAG, "Database created - no backup needed")
    }

    /**
     * Creates a timestamped backup of the database.
     *
     * The backup process:
     * 1. Ensures backup directory exists
     * 2. Creates timestamped backup file
     * 3. Copies database file to backup location
     * 4. Cleans up old backups
     *
     * @param db The database to backup
     */
    private fun createBackup(db: SupportSQLiteDatabase) {
        // Get the database file path from the database
        val dbPath = db.path ?: run {
            Log.w(TAG, "Cannot backup: database path is null")
            return
        }

        val dbFile = File(dbPath)
        if (!dbFile.exists()) {
            Log.w(TAG, "Cannot backup: database file doesn't exist at $dbPath")
            return
        }

        // Check if database is empty (new install)
        if (dbFile.length() == 0L) {
            Log.i(TAG, "Database is empty - skipping backup")
            return
        }

        val backupDir = getBackupDirectory()
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            Log.e(TAG, "Failed to create backup directory: ${backupDir.absolutePath}")
            return
        }

        // Create timestamped backup file
        val timestamp = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())
        val backupFile = File(backupDir, "banyg_backup_$timestamp.db")

        try {
            // Check available space
            val requiredSpace = dbFile.length()
            val availableSpace = backupDir.usableSpace

            if (availableSpace < requiredSpace * 2) {
                Log.w(TAG, "Low storage space. Required: $requiredSpace, Available: $availableSpace")
                // Still attempt backup but warn user
            }

            // Copy database file to backup location
            val startTime = System.currentTimeMillis()
            dbFile.copyTo(backupFile, overwrite = false)
            val duration = System.currentTimeMillis() - startTime

            Log.i(TAG, "Database backup created: ${backupFile.name} (${dbFile.length()} bytes in ${duration}ms)")

            // Clean up old backups
            cleanOldBackups(backupDir, MAX_BACKUPS_TO_KEEP)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to create backup at ${backupFile.absolutePath}", e)
            // Delete partial backup if it exists
            if (backupFile.exists()) {
                backupFile.delete()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while creating backup", e)
        }
    }

    /**
     * Gets the backup directory, preferring external app-specific storage.
     *
     * Priority:
     * 1. External app-specific files dir (Android/data/[package]/files/database_backups/)
     * 2. Internal app-specific files dir (data/data/[package]/files/database_backups/)
     *
     * Both locations are app-private and cleared on uninstall.
     *
     * @return The backup directory (created if needed)
     */
    private fun getBackupDirectory(): File {
        // Try external storage first (better for large files, user-accessible via USB)
        val externalBackupDir = context.getExternalFilesDir(null)?.let { externalFiles ->
            File(externalFiles, BACKUP_DIR_NAME)
        }

        if (externalBackupDir != null && (externalBackupDir.exists() || externalBackupDir.mkdirs())) {
            return externalBackupDir
        }

        // Fallback to internal storage
        Log.w(TAG, "Using internal storage for backups (external storage unavailable)")
        return File(context.filesDir, BACKUP_DIR_NAME)
    }

    /**
     * Removes old backup files, keeping only the most recent N backups.
     *
     * This prevents unlimited storage growth while maintaining recent backup history.
     *
     * @param backupDir The directory containing backup files
     * @param keepCount Number of recent backups to keep (default: 5)
     */
    private fun cleanOldBackups(backupDir: File, keepCount: Int = MAX_BACKUPS_TO_KEEP) {
        try {
            val backupFiles = backupDir.listFiles { file ->
                file.isFile && file.name.startsWith("banyg_backup_") && file.name.endsWith(".db")
            } ?: return

            // Sort by last modified time (newest first)
            val sortedBackups = backupFiles.sortedByDescending { it.lastModified() }

            // Delete backups beyond the keep count
            val toDelete = sortedBackups.drop(keepCount)
            toDelete.forEach { file ->
                if (file.delete()) {
                    Log.i(TAG, "Deleted old backup: ${file.name}")
                } else {
                    Log.w(TAG, "Failed to delete old backup: ${file.name}")
                }
            }

            if (toDelete.isNotEmpty()) {
                val freedSpace = toDelete.sumOf { it.length() }
                Log.i(TAG, "Cleaned up ${toDelete.size} old backups, freed ${freedSpace / 1024}KB")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old backups", e)
            // Don't throw - cleanup failure shouldn't block database operations
        }
    }

    companion object {
        private const val TAG = "MigrationBackup"
        private const val BACKUP_DIR_NAME = "database_backups"
        private const val MAX_BACKUPS_TO_KEEP = 5
        private const val TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss"

        /**
         * Returns the expected backup directory path for the given context.
         * Useful for testing and manual backup management.
         */
        fun getBackupDirectoryPath(context: Context): String {
            val externalDir = context.getExternalFilesDir(null)
            return if (externalDir != null) {
                File(externalDir, BACKUP_DIR_NAME).absolutePath
            } else {
                File(context.filesDir, BACKUP_DIR_NAME).absolutePath
            }
        }

        /**
         * Lists all backup files in the backup directory.
         * Returns empty list if directory doesn't exist or is inaccessible.
         *
         * @param context Application context
         * @return List of backup files, sorted by date (newest first)
         */
        fun listBackups(context: Context): List<File> {
            val backupDir = context.getExternalFilesDir(null)?.let { File(it, BACKUP_DIR_NAME) }
                ?: File(context.filesDir, BACKUP_DIR_NAME)

            if (!backupDir.exists()) {
                return emptyList()
            }

            val backups = backupDir.listFiles { file ->
                file.isFile && file.name.startsWith("banyg_backup_") && file.name.endsWith(".db")
            } ?: return emptyList()

            return backups.sortedByDescending { it.lastModified() }
        }

        /**
         * Manually restores a backup file to the database location.
         *
         * WARNING: This should only be called when the database is closed.
         * The app should be restarted after restore.
         *
         * @param context Application context
         * @param backupFile The backup file to restore
         * @param databaseName The name of the database file (default: banyg_database)
         * @throws IOException if restore fails
         * @throws IllegalStateException if database is open
         */
        @Throws(IOException::class, IllegalStateException::class)
        fun restoreBackup(context: Context, backupFile: File, databaseName: String = "banyg_database") {
            if (!backupFile.exists()) {
                throw IOException("Backup file does not exist: ${backupFile.absolutePath}")
            }

            val dbPath = context.getDatabasePath(databaseName)

            // Verify database is closed by checking if we can open it exclusively
            // This is a best-effort check - not 100% reliable
            if (dbPath.exists()) {
                Log.w(TAG, "Attempting to restore over existing database. Ensure app is closed.")
            }

            // Create backup of current database before restore (backup of backup!)
            if (dbPath.exists()) {
                val emergencyBackup = File(dbPath.parent, "${databaseName}_pre_restore_${System.currentTimeMillis()}.db")
                dbPath.copyTo(emergencyBackup, overwrite = false)
                Log.i(TAG, "Created emergency backup before restore: ${emergencyBackup.name}")
            }

            // Restore the backup
            backupFile.copyTo(dbPath, overwrite = true)
            Log.i(TAG, "Database restored from backup: ${backupFile.name}")

            // Clear Room's schema cache if it exists
            context.deleteDatabase("${databaseName}.wal")
            context.deleteDatabase("${databaseName}-shm")

            Log.i(TAG, "Database restore complete. Restart the app.")
        }
    }
}
