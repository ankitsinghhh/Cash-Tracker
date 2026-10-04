package com.example

import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.test.core.app.ApplicationProvider
import com.example.domain.CsvFolderBackup
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CsvFolderBackupTest {
    private val authority = "com.example.test.backups"
    private lateinit var provider: TestFolderProvider
    private lateinit var context: Context
    private val tree get() = DocumentsContract.buildTreeDocumentUri(authority, "root")

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        provider = TestFolderProvider()
        provider.attachInfo(context, ProviderInfo().apply {
            authority = this@CsvFolderBackupTest.authority
            exported = true; grantUriPermissions = true
            readPermission = "android.permission.MANAGE_DOCUMENTS"
            writePermission = "android.permission.MANAGE_DOCUMENTS"
        })
        ShadowContentResolver.registerProviderInternal(authority, provider)
    }
    @After fun cleanup() { provider.directory.listFiles()?.forEach { it.delete() }; provider.directory.delete() }

    @Test fun selectedFolderKeepsWritePermissionAndRepeatedBackupsCreateDistinctUtf8Files() {
        val folder = CsvFolderBackup.selectFolder(context.contentResolver, tree)
        assertEquals("Ledger backups", folder.name)
        assertTrue(context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission })
        val csv = "\uFEFFDate,Note,Amount\n2026-10-04,\"Coffee ₹, café\",12.50\n"
        val first = CsvFolderBackup.save(context.contentResolver, folder, csv)
        val second = CsvFolderBackup.save(context.contentResolver, folder, csv + "extra\n")
        assertNotEquals(first.uri, second.uri)
        assertNotEquals(first.fileName, second.fileName)
        assertEquals(2, provider.files.size)
        assertEquals(csv, provider.files[DocumentsContract.getDocumentId(Uri.parse(first.uri))]!!.readText(Charsets.UTF_8))
        assertEquals(csv + "extra\n", provider.files[DocumentsContract.getDocumentId(Uri.parse(second.uri))]!!.readText(Charsets.UTF_8))
    }

    @Test fun failedWriteDeletesOnlyTheIncompleteNewFile() {
        val folder = CsvFolderBackup.selectFolder(context.contentResolver, tree)
        val first = CsvFolderBackup.save(context.contentResolver, folder, "complete backup")
        provider.failOpen = true
        assertThrows(Exception::class.java) { CsvFolderBackup.save(context.contentResolver, folder, "will fail") }
        assertEquals(1, provider.files.size)
        assertEquals("complete backup", provider.files[DocumentsContract.getDocumentId(Uri.parse(first.uri))]!!.readText())
    }

    @Test fun readOnlyOrUnavailableFolderNeverCreatesABackup() {
        val folder = CsvFolderBackup.selectFolder(context.contentResolver, tree)
        provider.writable = false
        assertThrows(IllegalArgumentException::class.java) { CsvFolderBackup.save(context.contentResolver, folder, "data") }
        assertTrue(provider.files.isEmpty())
        provider.rejectAccess = true
        assertThrows(SecurityException::class.java) { CsvFolderBackup.save(context.contentResolver, folder, "data") }
        assertTrue(provider.files.isEmpty())
    }

    private class TestFolderProvider : DocumentsProvider() {
        val directory = Files.createTempDirectory("cash-csv-").toFile()
        val files = linkedMapOf<String, File>()
        var writable = true
        var failOpen = false
        var rejectAccess = false
        override fun onCreate() = true
        override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(projection ?: arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID))
        override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
            if (rejectAccess) throw SecurityException("Folder permission revoked")
            val columns = projection ?: arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_FLAGS)
            val root = documentId == "root"
            val file = files[documentId]
            if (!root && file == null) throw FileNotFoundException(documentId)
            return MatrixCursor(columns).apply { addRow(columns.map<String, Any?> { column -> when (column) {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID -> documentId
                DocumentsContract.Document.COLUMN_DISPLAY_NAME -> if (root) "Ledger backups" else file!!.name
                DocumentsContract.Document.COLUMN_MIME_TYPE -> if (root) DocumentsContract.Document.MIME_TYPE_DIR else "text/csv"
                DocumentsContract.Document.COLUMN_FLAGS -> if (root) {
                    if (writable) DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE else 0
                } else DocumentsContract.Document.FLAG_SUPPORTS_WRITE or DocumentsContract.Document.FLAG_SUPPORTS_DELETE
                DocumentsContract.Document.COLUMN_SIZE -> file?.length() ?: 0L
                else -> null
            } }.toTypedArray()) }
        }
        override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor =
            MatrixCursor(projection ?: arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
        override fun isChildDocument(parentDocumentId: String, documentId: String) = parentDocumentId == "root"
        override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
            check(writable)
            val id = "file_${files.size}_${System.nanoTime()}"
            files[id] = File(directory, displayName).apply { createNewFile() }
            return id
        }
        override fun deleteDocument(documentId: String) { files.remove(documentId)?.delete() }
        override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
            if (failOpen) throw FileNotFoundException("Simulated storage failure")
            return ParcelFileDescriptor.open(files[documentId] ?: throw FileNotFoundException(documentId), ParcelFileDescriptor.parseMode(mode))
        }
    }
}
