package com.example.domain

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.example.data.model.CsvBackupFolder
import com.example.data.model.CsvBackupResult
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Uses only the folder permission explicitly granted through Android's system picker. */
object CsvFolderBackup {
    fun selectFolder(resolver: ContentResolver, treeUri: Uri): CsvBackupFolder {
        val name = folderName(resolver, treeUri)
        resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return CsvBackupFolder(treeUri.toString(), name)
    }

    private fun folderName(resolver: ContentResolver, treeUri: Uri): String {
        require(DocumentsContract.isTreeUri(treeUri)) { "Choose a backup folder using the system picker." }
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_FLAGS)
        val result = if (Build.VERSION.SDK_INT >= 26) resolver.query(documentUri, projection, null, null)
            else resolver.query(documentUri, projection, null, null, null)
        result?.use { cursor ->
            if (cursor.moveToFirst()) {
                require(cursor.getString(1) == DocumentsContract.Document.MIME_TYPE_DIR &&
                    cursor.getInt(2) and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE != 0) {
                    "This folder does not allow new files. Choose a writable folder."
                }
                return cursor.getString(0) ?: "Backup folder"
            }
        }
        throw IOException("The backup folder is unavailable. Choose the folder again.")
    }

    fun save(resolver: ContentResolver, folder: CsvBackupFolder, csv: String): CsvBackupResult {
        val tree = Uri.parse(folder.uri)
        val name = folderName(resolver, tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "Cash_Tracker_Backup_${stamp}_${UUID.randomUUID().toString().take(8)}.csv"
        val document = DocumentsContract.createDocument(resolver, parent, "text/csv", fileName)
            ?: throw IOException("Could not create a CSV in this folder. Choose a writable folder.")
        try {
            val output = resolver.openOutputStream(document, "wt") ?: throw IOException("Could not open the backup file for writing.")
            output.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
            return CsvBackupResult(document.toString(), fileName, name)
        } catch (error: Exception) {
            // A failed save must not leave an empty/truncated CSV looking like a successful backup.
            try { DocumentsContract.deleteDocument(resolver, document) } catch (_: Exception) { }
            throw error
        }
    }
}
