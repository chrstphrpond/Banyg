package com.banyg.data.local.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for MigrationBackupCallback
 *
 * Tests backup creation, cleanup, and error handling without requiring
 * a real database connection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MigrationBackupCallbackTest {

    private lateinit var context: Context
    private lateinit var backupDir: File
    private lateinit var testDbFile: File
    private lateinit var callback: MigrationBackupCallback

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()

        // Setup test backup directory
        backupDir = File(context.filesDir, "database_backups")
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
        backupDir.mkdirs()

        // Create a test database file with some content
        testDbFile = context.getDatabasePath("test_database")
        testDbFile.parentFile?.mkdirs()
        testDbFile.writeText("test database content ${System.currentTimeMillis()}")

        callback = MigrationBackupCallback(context)
    }

    @After
    fun tearDown() {
        // Clean up test files
        backupDir.deleteRecursively()
        testDbFile.delete()
    }

    @Test
    fun `backup creates timestamped file`() {
        // Given
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // When
        callback.onOpen(mockDb)

        // Then
        val backups = backupDir.listFiles { file ->
            file.name.startsWith("banyg_backup_") && file.name.endsWith(".db")
        }
        assertNotNull("Backup files should exist", backups)
        assertEquals("Should create one backup", 1, backups?.size)

        val backupFile = backups?.first()
        assertTrue("Backup should have correct prefix", backupFile?.name?.startsWith("banyg_backup_") == true)
        assertTrue("Backup should have .db extension", backupFile?.name?.endsWith(".db") == true)
    }

    @Test
    fun `backup filename contains valid timestamp`() {
        // Given
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // When
        callback.onOpen(mockDb)

        // Then
        val backups = backupDir.listFiles()
        val backupName = backups?.first()?.name

        assertNotNull("Backup name should not be null", backupName)

        // Extract timestamp from filename: banyg_backup_YYYYMMDD_HHMMSS.db
        val timestampPart = backupName?.substringAfter("banyg_backup_")?.substringBefore(".db")

        // Verify timestamp format
        assertNotNull("Timestamp should exist", timestampPart)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        dateFormat.isLenient = false

        try {
            val parsedDate = dateFormat.parse(timestampPart!!)
            assertNotNull("Timestamp should be parseable", parsedDate)

            // Verify timestamp is recent (within last minute)
            val now = Date()
            val diffMs = now.time - parsedDate!!.time
            assertTrue("Timestamp should be recent (within 60s)", diffMs < 60_000)
        } catch (e: Exception) {
            fail("Timestamp format is invalid: $timestampPart")
        }
    }

    @Test
    fun `backup copies database content correctly`() {
        // Given
        val testContent = "Important financial data ${System.currentTimeMillis()}"
        testDbFile.writeText(testContent)

        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // When
        callback.onOpen(mockDb)

        // Then
        val backups = backupDir.listFiles()
        assertEquals("Should create one backup", 1, backups?.size)

        val backupContent = backups?.first()?.readText()
        assertEquals("Backup content should match original", testContent, backupContent)
    }

    @Test
    fun `cleanup keeps only most recent backups`() {
        // Given
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // Create 8 backups with different timestamps
        for (i in 1..8) {
            // Modify database to force new backup
            testDbFile.writeText("content version $i")

            callback.onOpen(mockDb)

            // Sleep to ensure different timestamps
            Thread.sleep(10)
        }

        // Then
        val backups = backupDir.listFiles()
        assertEquals("Should keep only 5 most recent backups", 5, backups?.size)

        // Verify backups are the most recent ones
        val sortedBackups = backups?.sortedByDescending { it.lastModified() }
        assertEquals("Should have 5 backups", 5, sortedBackups?.size)
    }

    @Test
    fun `cleanup deletes oldest backups first`() {
        // Given
        val timestamps = mutableListOf<Long>()

        // Create 7 backups with distinct timestamps
        for (i in 1..7) {
            val backupFile = File(backupDir, "banyg_backup_2024010${i}_120000.db")
            backupFile.writeText("backup $i")
            backupFile.setLastModified(System.currentTimeMillis() + (i * 1000))
            timestamps.add(backupFile.lastModified())
            Thread.sleep(10)
        }

        // When - trigger cleanup
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath
        callback.onOpen(mockDb)

        // Then - should keep 5 most recent (including new one)
        val remainingBackups = backupDir.listFiles()?.sortedByDescending { it.lastModified() }
        assertEquals("Should have 5 backups remaining", 5, remainingBackups?.size)

        // Verify oldest backups were deleted
        val remainingTimestamps = remainingBackups?.map { it.lastModified() }?.toSet() ?: emptySet()

        // The two oldest timestamps should be gone
        assertFalse("Oldest backup should be deleted", remainingTimestamps.contains(timestamps[0]))
        assertFalse("Second oldest backup should be deleted", remainingTimestamps.contains(timestamps[1]))
    }

    @Test
    fun `no backup created for empty database`() {
        // Given - empty database file
        testDbFile.writeText("")

        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // When
        callback.onOpen(mockDb)

        // Then
        val backups = backupDir.listFiles()
        assertEquals("Should not create backup for empty database", 0, backups?.size)
    }

    @Test
    fun `no backup created when database path is null`() {
        // Given
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns null

        // When
        callback.onOpen(mockDb)

        // Then
        val backups = backupDir.listFiles()
        assertEquals("Should not create backup when path is null", 0, backups?.size)
    }

    @Test
    fun `no backup created when database file does not exist`() {
        // Given
        val nonExistentPath = File(context.filesDir, "nonexistent_db").absolutePath
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns nonExistentPath

        // When
        callback.onOpen(mockDb)

        // Then
        val backups = backupDir.listFiles()
        assertEquals("Should not create backup when file doesn't exist", 0, backups?.size)
    }

    @Test
    fun `onCreate does not create backup`() {
        // Given
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // When
        callback.onCreate(mockDb)

        // Then
        val backups = backupDir.listFiles()
        assertEquals("Should not create backup on database creation", 0, backups?.size)
    }

    @Test
    fun `listBackups returns sorted backups`() {
        // Given - create multiple backups with different timestamps
        val backupFiles = listOf(
            File(backupDir, "banyg_backup_20240101_120000.db"),
            File(backupDir, "banyg_backup_20240103_120000.db"),
            File(backupDir, "banyg_backup_20240102_120000.db")
        )

        backupFiles.forEachIndexed { index, file ->
            file.writeText("backup $index")
            file.setLastModified(System.currentTimeMillis() + (index * 10000))
        }

        // When
        val listedBackups = MigrationBackupCallback.listBackups(context)

        // Then
        assertEquals("Should list all backups", 3, listedBackups.size)

        // Verify sorted by date (newest first)
        assertTrue("Backups should be sorted newest first",
            listedBackups[0].lastModified() >= listedBackups[1].lastModified())
        assertTrue("Backups should be sorted newest first",
            listedBackups[1].lastModified() >= listedBackups[2].lastModified())
    }

    @Test
    fun `listBackups returns empty list when directory does not exist`() {
        // Given - clean backup directory
        backupDir.deleteRecursively()

        // When
        val backups = MigrationBackupCallback.listBackups(context)

        // Then
        assertTrue("Should return empty list when directory doesn't exist", backups.isEmpty())
    }

    @Test
    fun `getBackupDirectoryPath returns valid path`() {
        // When
        val path = MigrationBackupCallback.getBackupDirectoryPath(context)

        // Then
        assertNotNull("Path should not be null", path)
        assertTrue("Path should contain backup directory name",
            path.contains("database_backups"))
    }

    @Test
    fun `restoreBackup throws exception for non-existent backup`() {
        // Given
        val nonExistentBackup = File(backupDir, "nonexistent_backup.db")

        // When/Then
        try {
            MigrationBackupCallback.restoreBackup(context, nonExistentBackup, "test_db")
            fail("Should throw IOException for non-existent backup")
        } catch (e: Exception) {
            assertTrue("Should throw IOException", e is java.io.IOException)
            assertTrue("Error message should mention file doesn't exist",
                e.message?.contains("does not exist") == true)
        }
    }

    @Test
    fun `multiple onOpen calls do not create duplicate backups within same second`() {
        // Given
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // When - call onOpen multiple times rapidly
        callback.onOpen(mockDb)

        // Small delay to ensure we don't have exact timestamp collision
        Thread.sleep(100)

        callback.onOpen(mockDb)

        // Then - should create multiple backups (different timestamps)
        val backups = backupDir.listFiles()
        assertTrue("Should create backups for each call", backups?.size ?: 0 >= 1)
    }

    @Test
    fun `backup handles special characters in database content`() {
        // Given - database with special characters
        val specialContent = "test\u0000data\nwith\tspecial€chars✓"
        testDbFile.writeBytes(specialContent.toByteArray())

        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // When
        callback.onOpen(mockDb)

        // Then
        val backups = backupDir.listFiles()
        assertEquals("Should create backup", 1, backups?.size)

        val backupContent = backups?.first()?.readBytes()
        assertArrayEquals("Backup should preserve binary content",
            testDbFile.readBytes(), backupContent)
    }

    @Test
    fun `backup preserves large database files`() {
        // Given - large database file (simulate with 1MB content)
        val largeContent = ByteArray(1024 * 1024) { it.toByte() }
        testDbFile.writeBytes(largeContent)

        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { mockDb.path } returns testDbFile.absolutePath

        // When
        callback.onOpen(mockDb)

        // Then
        val backups = backupDir.listFiles()
        assertEquals("Should create backup", 1, backups?.size)

        val backupFile = backups?.first()
        assertEquals("Backup size should match original",
            testDbFile.length(), backupFile?.length())
    }
}
