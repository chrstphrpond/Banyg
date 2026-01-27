package com.banyg.data.local.migration

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * High-level API for managing database backups.
 *
 * This class provides a simple interface for:
 * - Listing available backups
 * - Restoring from backups
 * - Exporting backups to external storage
 * - Verifying backup integrity
 *
 * Usage:
 * ```
 * val backupManager = BackupManager(context)
 *
 * // List backups
 * val backups = backupManager.listBackups()
 *
 * // Restore most recent backup
 * backups.firstOrNull()?.let { backup ->
 *     backupManager.restoreBackup(backup) { result ->
 *         result.onSuccess {
 *             // Restart app
 *         }.onFailure { error ->
 *             // Handle error
 *         }
 *     }
 * }
 * ```
 */
class BackupManager(private val context: Context) {

    /**
     * Data class representing a backup file with metadata.
     */
    data class BackupInfo(
        val file: File,
        val fileName: String,
        val sizeBytes: Long,
        val timestamp: Long,
        val formattedSize: String = formatFileSize(sizeBytes),
        val formattedDate: String = formatTimestamp(timestamp)
    ) {
        companion object {
            private fun formatFileSize(bytes: Long): String {
                return when {
                    bytes < 1024 -> "$bytes B"
                    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                    else -> "${bytes / (1024 * 1024)} MB"
                }
            }

            private fun formatTimestamp(timestamp: Long): String {
                return java.text.SimpleDateFormat(
                    "MMM dd, yyyy HH:mm:ss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(timestamp))
            }
        }
    }

    /**
     * Lists all available backups with metadata.
     *
     * @return List of backup info, sorted by date (newest first)
     */
    fun listBackups(): List<BackupInfo> {
        return MigrationBackupCallback.listBackups(context).map { file ->
            BackupInfo(
                file = file,
                fileName = file.name,
                sizeBytes = file.length(),
                timestamp = file.lastModified()
            )
        }
    }

    /**
     * Restores a backup file.
     *
     * WARNING: This will replace the current database. The app must be restarted after restore.
     *
     * @param backupInfo The backup to restore
     * @param onComplete Callback with restore result
     */
    fun restoreBackup(backupInfo: BackupInfo, onComplete: (Result<Unit>) -> Unit) {
        try {
            MigrationBackupCallback.restoreBackup(
                context = context,
                backupFile = backupInfo.file,
                databaseName = "banyg_database"
            )
            onComplete(Result.success(Unit))
        } catch (e: IOException) {
            Log.e(TAG, "Failed to restore backup: ${backupInfo.fileName}", e)
            onComplete(Result.failure(e))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Illegal state during restore: ${backupInfo.fileName}", e)
            onComplete(Result.failure(e))
        }
    }

    /**
     * Exports a backup to a user-accessible location (e.g., Downloads folder).
     *
     * This allows users to manually copy backups for safekeeping.
     *
     * @param backupInfo The backup to export
     * @param destinationDir The destination directory (e.g., Environment.getExternalStoragePublicDirectory(DOWNLOADS))
     * @return Result with exported file or error
     */
    fun exportBackup(backupInfo: BackupInfo, destinationDir: File): Result<File> {
        return try {
            if (!destinationDir.exists() && !destinationDir.mkdirs()) {
                return Result.failure(IOException("Failed to create destination directory"))
            }

            val exportFile = File(destinationDir, "banyg_export_${System.currentTimeMillis()}.db")
            backupInfo.file.copyTo(exportFile, overwrite = false)

            Log.i(TAG, "Backup exported to: ${exportFile.absolutePath}")
            Result.success(exportFile)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to export backup", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a backup file.
     *
     * @param backupInfo The backup to delete
     * @return true if deleted successfully, false otherwise
     */
    fun deleteBackup(backupInfo: BackupInfo): Boolean {
        return try {
            val deleted = backupInfo.file.delete()
            if (deleted) {
                Log.i(TAG, "Backup deleted: ${backupInfo.fileName}")
            } else {
                Log.w(TAG, "Failed to delete backup: ${backupInfo.fileName}")
            }
            deleted
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception deleting backup", e)
            false
        }
    }

    /**
     * Gets the total size of all backups.
     *
     * @return Total size in bytes
     */
    fun getTotalBackupSize(): Long {
        return listBackups().sumOf { it.sizeBytes }
    }

    /**
     * Gets the backup directory path.
     *
     * @return Absolute path to backup directory
     */
    fun getBackupDirectoryPath(): String {
        return MigrationBackupCallback.getBackupDirectoryPath(context)
    }

    /**
     * Verifies that a backup file is valid (not corrupted).
     *
     * This performs a basic check to ensure the file is a valid SQLite database.
     *
     * @param backupInfo The backup to verify
     * @return true if backup appears valid, false otherwise
     */
    fun verifyBackup(backupInfo: BackupInfo): Boolean {
        return try {
            // Check file exists and is not empty
            if (!backupInfo.file.exists() || backupInfo.file.length() == 0L) {
                return false
            }

            // Check SQLite magic number (first 16 bytes should be "SQLite format 3\u0000")
            val magicBytes = ByteArray(16)
            backupInfo.file.inputStream().use { stream ->
                val bytesRead = stream.read(magicBytes)
                if (bytesRead != 16) {
                    return false
                }
            }

            val magic = String(magicBytes, 0, 15, Charsets.US_ASCII)
            magic == "SQLite format 3"

        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify backup: ${backupInfo.fileName}", e)
            false
        }
    }

    /**
     * Imports a backup from an external location to the backup directory.
     *
     * Useful for restoring backups that were previously exported.
     *
     * @param sourceFile The external backup file to import
     * @return Result with imported BackupInfo or error
     */
    fun importBackup(sourceFile: File): Result<BackupInfo> {
        return try {
            // Verify it's a valid SQLite database
            val magicBytes = ByteArray(16)
            sourceFile.inputStream().use { stream ->
                val bytesRead = stream.read(magicBytes)
                if (bytesRead != 16) {
                    return Result.failure(IOException("Invalid database file: too small"))
                }
            }

            val magic = String(magicBytes, 0, 15, Charsets.US_ASCII)
            if (magic != "SQLite format 3") {
                return Result.failure(IOException("Invalid database file: not SQLite format"))
            }

            // Copy to backup directory
            val backupDir = File(getBackupDirectoryPath())
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                return Result.failure(IOException("Failed to create backup directory"))
            }

            val timestamp = java.text.SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                java.util.Locale.US
            ).format(java.util.Date())

            val importedFile = File(backupDir, "banyg_backup_imported_$timestamp.db")
            sourceFile.copyTo(importedFile, overwrite = false)

            val backupInfo = BackupInfo(
                file = importedFile,
                fileName = importedFile.name,
                sizeBytes = importedFile.length(),
                timestamp = importedFile.lastModified()
            )

            Log.i(TAG, "Backup imported: ${backupInfo.fileName}")
            Result.success(backupInfo)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to import backup", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "BackupManager"
    }
}
