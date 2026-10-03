package com.example.domain

import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

data class FullBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appName: String = "Cash Tracker",
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<TransactionEntity>,
    val budgets: List<Budget>,
    val recurring: List<RecurringTransaction>,
    val installments: List<InstallmentPlan>,
    val memos: List<DailyMemo>,
    val bookmarks: List<Bookmark>
)

data class ImportResult(
    val success: Boolean,
    val totalImported: Int,
    val accountsCreated: List<String> = emptyList(),
    val categoriesCreated: List<String> = emptyList(),
    val totalExpense: Long = 0L,
    val totalIncome: Long = 0L,
    val message: String = ""
)

object BackupManager {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(FullBackupData::class.java).indent("  ")
    private val standardFormats = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
    private val standardDateFormat get() = standardFormats.get()!!
    private val exportFormats = ThreadLocal.withInitial { SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US) }
    private val realbyteExportDateFormat get() = exportFormats.get()!!

    suspend fun createJsonBackup(database: AppDatabase): String = database.withTransaction {
        val accounts = database.accountDao().getAllAccounts().first()
        val categories = database.categoryDao().getAllCategories().first()
        val transactions = database.transactionDao().getAllTransactions().first()
        val budgets = database.budgetDao().getAllBudgets().first()
        val recurring = database.recurringDao().getAllRecurring().first()
        val installments = database.installmentDao().getAllInstallments().first()
        val memos = database.memoDao().getAllMemos().first()
        val bookmarks = database.bookmarkDao().getAllBookmarks().first()

        val backupObj = FullBackupData(
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            recurring = recurring,
            installments = installments,
            memos = memos,
            bookmarks = bookmarks
        )
        jsonAdapter.toJson(backupObj)
    }

    suspend fun restoreJsonBackup(database: AppDatabase, jsonString: String): Boolean {
        return try {
            val backup = jsonAdapter.fromJson(jsonString) ?: return false
            require(backup.version == 1) { "Unsupported backup version" }
            val accountIds = backup.accounts.map { it.id }.toSet()
            val categoryIds = backup.categories.map { it.id }.toSet()
            require(accountIds.size == backup.accounts.size && categoryIds.size == backup.categories.size)
            require(accountIds.all { it > 0 } && categoryIds.all { it > 0 }) { "Invalid backup IDs" }
            require(backup.categories.all { it.parentId == null || it.parentId in categoryIds }) { "Invalid category hierarchy" }
            require(backup.transactions.map { it.id }.distinct().size == backup.transactions.size)
            val occurrenceKeys = backup.transactions.mapNotNull { it.occurrenceKey }
            require(occurrenceKeys.distinct().size == occurrenceKeys.size) { "Duplicate scheduled occurrences in backup" }
            require(backup.transactions.all { tx -> tx.id > 0 && tx.amount > 0 && tx.transferFee >= 0 &&
                tx.accountId in accountIds && (tx.toAccountId == null || tx.toAccountId in accountIds) &&
                (tx.type == TransactionType.TRANSFER || tx.categoryId in categoryIds) }) { "Invalid backup references" }
            require(backup.budgets.all { it.amount >= 0 && (it.categoryId == 0L || it.categoryId in categoryIds) }) { "Invalid budget" }
            require(backup.recurring.all { it.amount > 0 && it.accountId in accountIds &&
                (it.toAccountId == null || it.toAccountId in accountIds) &&
                (it.type == TransactionType.TRANSFER || it.categoryId in categoryIds) }) { "Invalid recurring rule" }
            require(backup.installments.all { it.monthlyAmount > 0 && it.totalInstallments > 0 &&
                it.paidInstallments in 0..it.totalInstallments && it.accountId in accountIds && it.categoryId in categoryIds }) { "Invalid installment plan" }
            require(backup.bookmarks.all { it.amount >= 0 && it.accountId in accountIds &&
                (it.toAccountId == null || it.toAccountId in accountIds) &&
                (it.type == TransactionType.TRANSFER || it.categoryId in categoryIds) }) { "Invalid bookmark" }
            database.withTransaction {
                DemoDataGenerator.clearAllData(database)
                database.accountDao().insertAll(backup.accounts)
                database.categoryDao().insertAll(backup.categories)
                backup.transactions.chunked(500).forEach { database.transactionDao().insertAll(it) }
                database.budgetDao().insertAll(backup.budgets)
                database.recurringDao().insertAll(backup.recurring)
                database.installmentDao().insertAll(backup.installments)
                database.memoDao().insertAll(backup.memos)
                database.bookmarkDao().insertAll(backup.bookmarks)
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Realbyte Money Manager Standard Excel / CSV Export
     * Header: Date,Account,Category,Subcategory,Note,INR,Income/Expense,Description,Amount,Currency,Account
     */
    suspend fun exportRealbyteCsv(
        database: AppDatabase,
        transactions: List<TransactionWithDetails>,
        currencyCode: String = "INR"
    ): String {
        val resolvedCurrency = if (currencyCode.isNotBlank() && currencyCode != "USD") {
            currencyCode
        } else {
            database.settingDao().getValue("primary_currency")
                ?: database.settingDao().getValue("currency_code")
                ?: if (currencyCode.isNotBlank()) currencyCode else "INR"
        }

        val sb = StringBuilder()
        // Prefix with UTF-8 BOM (\uFEFF) so Excel renders emojis, rupee symbols (₹) and regional text properly
        sb.append("\uFEFF")
        sb.append("Date,Account,Category,Subcategory,Note,$resolvedCurrency,Income/Expense,Description,Amount,Currency,Account\n")

        for (item in transactions) {
            val tx = item.transaction
            val dateStr = realbyteExportDateFormat.format(Date(tx.dateMillis))
            val amountDecimal = CurrencyFormatter.toDecimalString(tx.amount)
            val amountInt = (tx.amount / 100).toString()
            val accName = item.account?.name?.replace("\"", "\"\"") ?: "Cash"
            val toAccName = item.toAccount?.name?.replace("\"", "\"\"") ?: accName
            val catName = item.category?.name?.replace("\"", "\"\"") ?: "General"
            val subName = item.subcategory?.name?.replace("\"", "\"\"") ?: ""
            val note = tx.note.replace("\"", "\"\"")
            val payeeDesc = tx.payee.replace("\"", "\"\"")

            val typeStr = when (tx.type) {
                TransactionType.EXPENSE -> "Expense"
                TransactionType.INCOME -> "Income"
                TransactionType.TRANSFER -> "Transfer"
            }

            val itemCurrency = item.account?.currency?.takeIf { it.isNotBlank() } ?: resolvedCurrency

            sb.append("\"$dateStr\",\"$accName\",\"$catName\",\"$subName\",\"$note\",$amountInt,$typeStr,\"$payeeDesc\",$amountDecimal,$itemCurrency,\"$toAccName\"\n")
        }
        return sb.toString()
    }

    /**
     * Standard Spreadsheet CSV Export (Excel & Google Sheets)
     */
    suspend fun exportTransactionsToCsv(
        database: AppDatabase,
        transactions: List<TransactionWithDetails>,
        currencyCode: String = "INR"
    ): String {
        val resolvedCurrency = if (currencyCode.isNotBlank() && currencyCode != "USD") {
            currencyCode
        } else {
            database.settingDao().getValue("primary_currency")
                ?: database.settingDao().getValue("currency_code")
                ?: if (currencyCode.isNotBlank()) currencyCode else "INR"
        }

        val sb = StringBuilder()
        // Prefix with UTF-8 BOM (\uFEFF)
        sb.append("\uFEFF")
        sb.append("ID,Date,Type,Amount,Currency,Account,To Account,Category,Subcategory,Payee,Payment Method,Note,Tags\n")

        for (item in transactions) {
            val tx = item.transaction
            val dateStr = standardDateFormat.format(Date(tx.dateMillis))
            val amountFormatted = CurrencyFormatter.toDecimalString(tx.amount)
            val accName = item.account?.name?.replace("\"", "\"\"") ?: ""
            val toAccName = item.toAccount?.name?.replace("\"", "\"\"") ?: ""
            val catName = item.category?.name?.replace("\"", "\"\"") ?: ""
            val subName = item.subcategory?.name?.replace("\"", "\"\"") ?: ""
            val payee = tx.payee.replace("\"", "\"\"")
            val note = tx.note.replace("\"", "\"\"")
            val tags = tx.tags.replace("\"", "\"\"")
            val itemCurrency = item.account?.currency?.takeIf { it.isNotBlank() } ?: resolvedCurrency

            sb.append("${tx.id},\"$dateStr\",${tx.type},$amountFormatted,$itemCurrency,\"$accName\",\"$toAccName\",\"$catName\",\"$subName\",\"$payee\",${tx.paymentMethod.displayName},\"$note\",\"$tags\"\n")
        }
        return sb.toString()
    }

    /**
     * Universal Smart Importer supporting Realbyte Money Manager, Excel, and Custom CSV formats
     */
    suspend fun importTransactionsUniversal(database: AppDatabase, csvContent: String, defaultCurrency: String = "INR"): ImportResult = try {
        require(csvContent.length <= 32 * 1024 * 1024) { "Import is too large; split the file" }
        database.withTransaction { importValidated(database, csvContent, defaultCurrency) }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        ImportResult(false, 0, message = error.message ?: "Import failed. No data was changed.")
    }

    private suspend fun importValidated(
        database: AppDatabase,
        csvContent: String,
        defaultCurrency: String = "INR"
    ): ImportResult {
        val rawLines = parseCsvRows(csvContent).iterator()
        if (!rawLines.hasNext()) {
            return ImportResult(success = false, totalImported = 0, message = "CSV file is empty.")
        }

        val headerTokens = rawLines.next().map { it.trim().removePrefix("\uFEFF").lowercase().replace("\"", "").replace(".", "") }

        // Find column indices
        var dateCol = -1
        var accCol = -1
        var toAccCol = -1
        var catCol = -1
        var subCatCol = -1
        var amtCol = -1
        var currencyAmtCol = -1
        var typeCol = -1
        var noteCol = -1
        var descCol = -1
        var feeCol = -1

        for ((idx, col) in headerTokens.withIndex()) {
            when {
                dateCol == -1 && (col.contains("date") || col.contains("time") || col == "period") -> dateCol = idx
                accCol == -1 && (col == "account" || col == "accounts" || col == "asset" || col == "wallet" || col == "bank" || col == "from account") -> accCol = idx
                toAccCol == -1 && idx > 1 && (col.contains("to account") || col.contains("transfer account") || col.contains("dest") || (col == "account" && idx >= 9)) -> toAccCol = idx
                catCol == -1 && (col == "category" || col == "cat" || col == "group") -> catCol = idx
                subCatCol == -1 && (col.contains("sub") || col == "subcategory" || col == "sub category") -> subCatCol = idx
                amtCol == -1 && (col == "amount" || col == "amt" || col == "value" || col == "money" || col.contains("amount")) -> amtCol = idx
                currencyAmtCol == -1 && (col == "inr" || col == "usd" || col == "eur" || col == "gbp" || col == "jpy" || col == "cad" || col == "aud") -> currencyAmtCol = idx
                typeCol == -1 && (col.contains("income/expense") || col.contains("type") || col == "exp/inc" || col == "cr/dr" || col == "kind") -> typeCol = idx
                noteCol == -1 && (col == "note" || col == "memo" || col == "remarks" || col == "comment" || col == "title") -> noteCol = idx
                descCol == -1 && (col.contains("desc") || col == "payee" || col == "merchant") -> descCol = idx
                feeCol == -1 && (col.contains("fee") || col.contains("charge")) -> feeCol = idx
            }
        }

        // Missing optional named columns stay absent; positional guesses can turn
        // an Amount column into a subcategory or a Type column into a note.
        if (amtCol == -1 && currencyAmtCol != -1) amtCol = currencyAmtCol
        require(dateCol != -1 && amtCol != -1) { "CSV needs named Date and Amount columns" }

        val existingAccounts = database.accountDao().getAllAccounts().first().toMutableList()
        val existingCategories = database.categoryDao().getAllCategories().first().toMutableList()

        val newAccountsCreated = mutableListOf<String>()
        val newCategoriesCreated = mutableListOf<String>()
        val txList = mutableListOf<TransactionEntity>()

        var totalExpense = 0L
        var totalIncome = 0L

        val knownDateFormats = listOf(
            SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US),
            SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US),
            SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.US),
            SimpleDateFormat("MM-dd-yyyy", Locale.US)
        ).onEach { it.isLenient = false }

        var importedCount = 0
        var i = 0
        while (rawLines.hasNext()) {
            i++
            val tokens = rawLines.next()
            if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

            try {
                val dateRaw = tokens.getOrNull(dateCol)?.trim() ?: ""
                val accName = tokens.getOrNull(accCol)?.trim()?.ifBlank { "Cash" } ?: "Cash"
                val toAccNameRaw = if (toAccCol != -1) tokens.getOrNull(toAccCol)?.trim() ?: "" else ""
                val catName = tokens.getOrNull(catCol)?.trim()?.ifBlank { "General" } ?: "General"
                val subCatName = if (subCatCol != -1) tokens.getOrNull(subCatCol)?.trim() ?: "" else ""
                
                // Priority: Amount column, else Currency amount column (e.g. INR), else fallback
                var amtRaw = if (amtCol != -1) tokens.getOrNull(amtCol)?.trim() ?: "" else ""
                if (amtRaw.isBlank() && currencyAmtCol != -1) {
                    amtRaw = tokens.getOrNull(currencyAmtCol)?.trim() ?: ""
                }
                if (amtRaw.isBlank()) {
                    // search first numeric token
                    amtRaw = tokens.firstOrNull { it.matches(Regex("^-?\\d+(\\.\\d+)?$")) } ?: "0"
                }

                val typeRaw = if (typeCol != -1) tokens.getOrNull(typeCol)?.trim() ?: "" else ""
                var noteText = if (noteCol != -1) tokens.getOrNull(noteCol)?.trim() ?: "" else ""
                var payeeText = if (descCol != -1) tokens.getOrNull(descCol)?.trim() ?: "" else ""
                val feeRaw = if (feeCol != -1) tokens.getOrNull(feeCol)?.trim() ?: "0" else "0"

                if (noteText.isBlank() && payeeText.isNotBlank()) {
                    noteText = payeeText
                    payeeText = ""
                }

                // If subcategory exists, append to note or note tag
                if (subCatName.isNotBlank() && !noteText.contains(subCatName)) {
                    noteText = if (noteText.isNotBlank()) "[$subCatName] $noteText" else subCatName
                }

                // Parse Date
                var parsedMillis = 0L
                for (df in knownDateFormats) {
                    try {
                        val position = java.text.ParsePosition(0)
                        val d = df.parse(dateRaw, position)
                        if (d != null && position.index == dateRaw.length) {
                            parsedMillis = d.time
                            break
                        }
                    } catch (_: Exception) {}
                }
                if (parsedMillis == 0L) {
                    parsedMillis = dateRaw.toLongOrNull() ?: error("Invalid date on CSV row ${i + 1}")
                }

                // Determine Transaction Type
                val typeLower = typeRaw.lowercase()
                val isTransfer = typeLower.startsWith("trans") || typeLower == "transfer" || 
                        (toAccNameRaw.isNotBlank() && !toAccNameRaw.matches(Regex("^\\d+(\\.\\d+)?$")) && !toAccNameRaw.equals(accName, ignoreCase = true))

                val type = when {
                    isTransfer -> TransactionType.TRANSFER
                    typeLower.startsWith("inc") || typeLower == "income" || typeLower == "cr" || typeLower == "credit" -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }

                // Parse Amount
                val amountMinor = CurrencyFormatter.parseToMinorUnits(amtRaw)
                if (amountMinor <= 0L) continue

                val feeMinor = if (feeRaw.isNotBlank()) CurrencyFormatter.parseToMinorUnits(feeRaw) else 0L

                // Auto-resolve or create Account
                var account = existingAccounts.firstOrNull { it.name.equals(accName, ignoreCase = true) }
                if (account == null) {
                    val newAccType = when {
                        accName.contains("card", ignoreCase = true) -> AccountType.CREDIT_CARD
                        accName.contains("cash", ignoreCase = true) -> AccountType.CASH
                        accName.contains("invest", ignoreCase = true) || accName.contains("fund", ignoreCase = true) -> AccountType.INVESTMENT
                        else -> AccountType.BANK
                    }
                    val newAcc = Account(
                        name = accName,
                        type = newAccType,
                        groupName = if (newAccType == AccountType.CREDIT_CARD) "Cards" else "Accounts",
                        institution = accName,
                        currency = defaultCurrency
                    )
                    val generatedId = database.accountDao().insert(newAcc)
                    account = newAcc.copy(id = generatedId)
                    existingAccounts.add(account)
                    newAccountsCreated.add(accName)
                }

                // Auto-resolve or create To-Account if Transfer
                var toAccountId: Long? = null
                if (type == TransactionType.TRANSFER && toAccNameRaw.isNotBlank() && !toAccNameRaw.matches(Regex("^\\d+(\\.\\d+)?$"))) {
                    var toAccount = existingAccounts.firstOrNull { it.name.equals(toAccNameRaw, ignoreCase = true) }
                    if (toAccount == null) {
                        val newToAcc = Account(
                            name = toAccNameRaw,
                            type = AccountType.BANK,
                            groupName = "Accounts",
                            institution = toAccNameRaw,
                            currency = defaultCurrency
                        )
                        val generatedId = database.accountDao().insert(newToAcc)
                        toAccount = newToAcc.copy(id = generatedId)
                        existingAccounts.add(toAccount)
                        newAccountsCreated.add(toAccNameRaw)
                    }
                    toAccountId = toAccount.id
                }

                // Auto-resolve or create Category
                var category = existingCategories.firstOrNull {
                    it.type == type && it.parentId == null && (it.name.equals(catName, ignoreCase = true) ||
                    it.name.contains(catName, ignoreCase = true) ||
                    catName.contains(it.name, ignoreCase = true))
                }
                if (category == null && type != TransactionType.TRANSFER) {
                    // Extract icon & color from category name
                    val catLower = catName.lowercase()
                    val (iconName, colorHex) = when {
                        catLower.contains("food") || catLower.contains("🍜") || catLower.contains("restaurant") || catLower.contains("dinner") -> Pair("Restaurant", "#F59E0B")
                        catLower.contains("transport") || catLower.contains("🚖") || catLower.contains("cab") || catLower.contains("petrol") || catLower.contains("metro") -> Pair("DirectionsCar", "#3B82F6")
                        catLower.contains("rent") || catLower.contains("🏪") || catLower.contains("house") || catLower.contains("🪑") -> Pair("Home", "#8B5CF6")
                        catLower.contains("social") || catLower.contains("👬") || catLower.contains("entertainment") || catLower.contains("movie") -> Pair("Movie", "#EC4899")
                        catLower.contains("health") || catLower.contains("🧘") || catLower.contains("medic") || catLower.contains("gym") -> Pair("FitnessCenter", "#10B981")
                        catLower.contains("apparel") || catLower.contains("🧥") || catLower.contains("cloth") || catLower.contains("shirt") -> Pair("Checkroom", "#6366F1")
                        catLower.contains("education") || catLower.contains("📙") || catLower.contains("fee") || catLower.contains("course") -> Pair("School", "#F97316")
                        catLower.contains("gift") || catLower.contains("🎁") || catLower.contains("reward") -> Pair("CardGiftcard", "#06B6D4")
                        catLower.contains("cashback") || catLower.contains("💸") || catLower.contains("salary") || catLower.contains("income") -> Pair("Payments", "#10B981")
                        catLower.contains("culture") || catLower.contains("🖼") || catLower.contains("temple") || catLower.contains("travel") -> Pair("Flight", "#14B8A6")
                        catLower.contains("recharge") || catLower.contains("⚡") || catLower.contains("bill") || catLower.contains("electric") -> Pair("Bolt", "#EAB308")
                        type == TransactionType.INCOME -> Pair("AccountBalanceWallet", "#10B981")
                        else -> Pair("Receipt", "#64748B")
                    }

                    val newCat = Category(
                        name = catName,
                        type = type,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                    val generatedId = database.categoryDao().insert(newCat)
                    category = newCat.copy(id = generatedId)
                    existingCategories.add(category)
                    newCategoriesCreated.add(catName)
                }

                val finalCatId = category?.id ?: existingCategories.firstOrNull()?.id ?: 1L

                txList.add(
                    TransactionEntity(
                        type = type,
                        dateMillis = parsedMillis,
                        amount = amountMinor,
                        accountId = account.id,
                        toAccountId = toAccountId,
                        transferFee = feeMinor,
                        categoryId = finalCatId,
                        payee = payeeText,
                        note = noteText
                    )
                )

                if (type == TransactionType.EXPENSE) totalExpense += amountMinor
                if (type == TransactionType.INCOME) totalIncome += amountMinor
                importedCount++
                if (txList.size >= 500) {
                    database.transactionDao().insertAll(txList)
                    txList.clear()
                }

            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                throw IllegalArgumentException("Could not import CSV row ${i + 1}: ${e.message ?: "invalid data"}", e)
            }
        }

        if (txList.isNotEmpty()) {
            txList.chunked(500).forEach { database.transactionDao().insertAll(it) }
        }

        return ImportResult(
            success = importedCount > 0,
            totalImported = importedCount,
            accountsCreated = newAccountsCreated.distinct(),
            categoriesCreated = newCategoriesCreated.distinct(),
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            message = if (importedCount > 0) "Successfully imported $importedCount transactions!" else "No valid transaction rows found to import."
        )
    }

    /**
     * Robust CSV Parser that handles multiline quoted strings and escaped quotes
     */
    private fun parseCsvRows(csvContent: String): Sequence<List<String>> = sequence {
        val currentTokens = mutableListOf<String>()
        val currentCell = StringBuilder()
        var insideQuote = false
        var i = 0
        val length = csvContent.length

        while (i < length) {
            val c = csvContent[i]
            when {
                c == '\"' -> {
                    if (insideQuote && i + 1 < length && csvContent[i + 1] == '\"') {
                        currentCell.append('\"')
                        i++ // Skip escaped quote
                    } else {
                        insideQuote = !insideQuote
                    }
                }
                (c == ',' || c == '\t' || c == ';') && !insideQuote -> {
                    currentTokens.add(currentCell.toString().trim())
                    currentCell.clear()
                }
                (c == '\r' || c == '\n') && !insideQuote -> {
                    if (c == '\r' && i + 1 < length && csvContent[i + 1] == '\n') {
                        i++
                    }
                    currentTokens.add(currentCell.toString().trim())
                    currentCell.clear()
                    if (currentTokens.isNotEmpty() && currentTokens.any { it.isNotBlank() }) {
                        yield(currentTokens.toList())
                    }
                    currentTokens.clear()
                }
                else -> {
                    currentCell.append(c)
                }
            }
            i++
        }

        require(!insideQuote) { "CSV contains an unterminated quoted field" }
        if (currentCell.isNotEmpty() || currentTokens.isNotEmpty()) {
            currentTokens.add(currentCell.toString().trim())
            if (currentTokens.any { it.isNotBlank() }) {
                yield(currentTokens.toList())
            }
        }

    }
}
