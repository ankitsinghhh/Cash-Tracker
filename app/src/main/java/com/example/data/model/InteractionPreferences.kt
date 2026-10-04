package com.example.data.model

data class HapticPreferences(val enabled: Boolean = true, val strength: Int = 50)

data class CsvBackupFolder(val uri: String, val name: String)

data class CsvBackupResult(val uri: String, val fileName: String, val folderName: String)
