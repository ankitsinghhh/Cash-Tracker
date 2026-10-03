package com.example.server

import android.content.Context
import androidx.room.withTransaction
import com.example.data.model.Account
import com.example.data.model.AccountWithBalance
import com.example.data.model.Category
import com.example.data.model.TransactionWithDetails
import com.example.data.model.AccountType
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.domain.BackupManager
import com.example.domain.CurrencyFormatter
import com.example.domain.FinancialEngine
import com.example.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.RejectedExecutionException
import java.net.URLDecoder

object PCManagerServer {

    private data class DataPage(val accounts: List<AccountWithBalance>, val categories: List<Category>,
        val transactions: List<TransactionWithDetails>, val total: Int, val currency: String)

    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val threadPool = ThreadPoolExecutor(4, 4, 60, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(16)).apply { allowCoreThreadTimeOut(true) }
    private const val MAX_BODY = 8 * 1024 * 1024
    private const val MAX_HEADER_LINE = 8192
    private const val MAX_RESPONSE = 32 * 1024 * 1024
    private val clients = java.util.concurrent.ConcurrentHashMap.newKeySet<Socket>()

    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var port: Int = 8888
        private set

    @Volatile
    var ipAddress: String = "127.0.0.1"
        private set

    @Volatile
    var isPasscodeEnabled: Boolean = false
        private set

    @Volatile
    var passcode: String = "1234"
        private set

    private var serverSocket: ServerSocket? = null
    private var repository: FinanceRepository? = null
    private var appContext: Context? = null
    private var statusCallback: ((Boolean, String, Int) -> Unit)? = null

    private data class RecentTxSubmission(
        val type: TransactionType,
        val amount: Long,
        val accountId: Long,
        val toAccountId: Long?,
        val categoryId: Long,
        val payee: String,
        val note: String,
        val insertedId: Long,
        val timestamp: Long
    )
    private val recentSubmissions = mutableListOf<RecentTxSubmission>()

    val serverUrl: String
        get() = "http://$ipAddress:$port"

    fun start(
        context: Context,
        repo: FinanceRepository,
        requestedPort: Int = 8888,
        passcodeOn: Boolean = false,
        code: String = "1234",
        onStatusChange: ((Boolean, String, Int) -> Unit)? = null
    ): Boolean {
        if (isRunning) {
            updatePasscodeConfig(passcodeOn, code)
            return true
        }

        appContext = context.applicationContext
        repository = repo
        port = requestedPort
        isPasscodeEnabled = passcodeOn
        passcode = code
        statusCallback = onStatusChange
        ipAddress = NetworkUtils.getLocalIpAddress(context)

        return try {
            val socket = tryPort(requestedPort) ?: tryPort(8889) ?: tryPort(8080) ?: return false
            serverSocket = socket
            port = socket.localPort
            isRunning = true

            threadPool.execute {
                listenLoop(socket)
            }

            statusCallback?.invoke(true, ipAddress, port)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning = false
            statusCallback?.invoke(false, ipAddress, port)
            false
        }
    }

    private fun tryPort(targetPort: Int): ServerSocket? {
        return try {
            val s = ServerSocket(targetPort, 50, InetAddress.getByName("0.0.0.0"))
            s.reuseAddress = true
            s
        } catch (e: Exception) {
            null
        }
    }

    fun updatePasscodeConfig(passcodeOn: Boolean, code: String) {
        isPasscodeEnabled = passcodeOn
        passcode = code
    }

    fun refreshIp(context: Context): String {
        ipAddress = NetworkUtils.getLocalIpAddress(context)
        statusCallback?.invoke(isRunning, ipAddress, port)
        return ipAddress
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverSocket = null
        clients.forEach { try { it.close() } catch (_: Exception) {} }
        clients.clear()
        statusCallback?.invoke(false, ipAddress, port)
    }

    private fun listenLoop(socket: ServerSocket) {
        while (isRunning && !socket.isClosed) {
            try {
                val clientSocket = socket.accept()
                clients.add(clientSocket)
                try { threadPool.execute { handleClient(clientSocket) } }
                catch (_: RejectedExecutionException) { clients.remove(clientSocket); clientSocket.close() }
            } catch (e: SocketException) {
                // Expected when socket is closed
                break
            } catch (e: Exception) {
                if (!isRunning) break
                e.printStackTrace()
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 15000
            val rawIn = BufferedInputStream(client.getInputStream())
            val rawOut = BufferedOutputStream(client.getOutputStream())

            // Read header lines byte by byte to prevent buffering request body into character reader
            val lineBaos = ByteArrayOutputStream()
            fun readHeaderLine(): String {
                lineBaos.reset()
                while (true) {
                    val b = rawIn.read()
                    if (b == -1) break
                    if (b == '\n'.code) break
                    if (b != '\r'.code) {
                        if (lineBaos.size() >= MAX_HEADER_LINE) throw IllegalArgumentException("Header too large")
                        lineBaos.write(b)
                    }
                }
                return lineBaos.toString(StandardCharsets.UTF_8.name())
            }

            val requestLine = readHeaderLine()
            if (requestLine.isBlank()) return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val fullPath = parts[1]
            val path = if (fullPath.contains("?")) fullPath.substringBefore("?") else fullPath

            // Read HTTP headers
            val headers = mutableMapOf<String, String>()
            var headerCount = 0
            var headerBytes = 0
            while (true) {
                val line = readHeaderLine()
                if (line.isBlank()) break
                headerBytes += line.length
                require(++headerCount <= 64 && headerBytes <= 32768) { "Headers too large" }
                val colonIdx = line.indexOf(":")
                if (colonIdx > 0) {
                    val k = line.substring(0, colonIdx).trim().lowercase()
                    val v = line.substring(colonIdx + 1).trim()
                    headers[k] = v
                }
            }

            // Read Request Body (exact byte count from Content-Length)
            val contentLength = headers["content-length"]?.let { it.toIntOrNull() ?: throw IllegalArgumentException("Invalid content length") } ?: 0
            require(contentLength in 0..MAX_BODY) { "Request body too large" }
            require(!headers.containsKey("transfer-encoding")) { "Unsupported transfer encoding" }
            val body = if (contentLength > 0) {
                val buf = ByteArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val r = rawIn.read(buf, readTotal, contentLength - readTotal)
                    if (r == -1) break
                    readTotal += r
                }
                require(readTotal == contentLength) { "Incomplete request body" }
                String(buf, 0, readTotal, StandardCharsets.UTF_8)
            } else {
                ""
            }

            // Handle CORS Preflight
            if (method == "OPTIONS") {
                sendResponse(rawOut, 200, "OK", "text/plain", "", extraHeaders = getCorsHeaders())
                return
            }

            // Passcode Verification Check
            val isAuthed = checkAuth(headers)

            if (isPasscodeEnabled && !isAuthed) {
                // Allow login and status check without auth
                if (path == "/api/login" && method == "POST") {
                    handleLogin(body, rawOut)
                    return
                } else if (path == "/api/status") {
                    handleStatus(rawOut, authenticated = false)
                    return
                } else if (path.startsWith("/api/")) {
                    sendResponse(
                        rawOut,
                        401,
                        "Unauthorized",
                        "application/json",
                        """{"error":"Authentication required","passcodeRequired":true}""",
                        extraHeaders = getCorsHeaders()
                    )
                    return
                }
            }

            // Route Requests
            when {
                path == "/" || path == "/index.html" -> handleIndexHtml(rawOut)
                path == "/api/status" -> handleStatus(rawOut, authenticated = true)
                path == "/api/login" && method == "POST" -> handleLogin(body, rawOut)
                path == "/api/data" -> handleGetData(fullPath, rawOut)
                path == "/api/transactions" && method == "POST" -> handleCreateTransaction(body, rawOut)
                path == "/api/transactions/update" && method == "POST" -> handleUpdateTransaction(body, rawOut)
                path == "/api/transactions/delete" && method == "POST" -> handleDeleteTransaction(body, rawOut)
                path == "/api/accounts" && method == "POST" -> handleCreateAccount(body, rawOut)
                path == "/api/export/csv" -> handleExportCsv(rawOut)
                path == "/api/export/json" -> handleExportJson(rawOut)
                path == "/api/import/json" && method == "POST" -> handleImportJson(body, rawOut)
                else -> sendResponse(rawOut, 404, "Not Found", "text/plain", "Not Found", extraHeaders = getCorsHeaders())
            }
        } catch (error: IllegalArgumentException) {
            try { sendResponse(BufferedOutputStream(client.getOutputStream()), 400, "Bad Request", "application/json",
                JSONObject().put("error", error.message ?: "Invalid request").toString()) } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clients.remove(client)
            try {
                client.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun checkAuth(headers: Map<String, String>): Boolean {
        if (!isPasscodeEnabled) return true

        val clientPasscodeHeader = headers["x-passcode"]
        val tokenExpected = "${passcode.hashCode()}"
        if (!clientPasscodeHeader.isNullOrBlank()) {
            if (clientPasscodeHeader == passcode || clientPasscodeHeader == tokenExpected) {
                return true
            }
        }

        val cookieHeader = headers["cookie"] ?: ""
        if (cookieHeader.contains("mm_token=$tokenExpected") || cookieHeader.contains("mm_token=$passcode")) {
            return true
        }

        return false
    }

    private fun handleLogin(body: String, out: BufferedOutputStream) {
        try {
            val json = JSONObject(body)
            val submittedCode = json.optString("passcode", "").trim()
            if (submittedCode == passcode || !isPasscodeEnabled) {
                val token = passcode
                val hashToken = "${passcode.hashCode()}"
                val cookie = "mm_token=$token; Path=/; SameSite=Lax"
                val extraHeaders = getCorsHeaders().toMutableMap()
                extraHeaders["Set-Cookie"] = cookie
                sendResponse(
                    out,
                    200,
                    "OK",
                    "application/json",
                    """{"success":true,"token":"$token","hashToken":"$hashToken"}""",
                    extraHeaders = extraHeaders
                )
            } else {
                sendResponse(
                    out,
                    403,
                    "Forbidden",
                    "application/json",
                    """{"success":false,"error":"Invalid passcode"}""",
                    extraHeaders = getCorsHeaders()
                )
            }
        } catch (e: Exception) {
            sendResponse(
                out,
                400,
                "Bad Request",
                "application/json",
                """{"success":false,"error":"Malformed JSON"}""",
                extraHeaders = getCorsHeaders()
            )
        }
    }

    private fun handleStatus(out: BufferedOutputStream, authenticated: Boolean) {
        val resp = JSONObject()
        resp.put("running", true)
        resp.put("appName", "Cash Tracker")
        resp.put("passcodeRequired", isPasscodeEnabled)
        resp.put("authenticated", authenticated || !isPasscodeEnabled)
        sendResponse(out, 200, "OK", "application/json", resp.toString(), extraHeaders = getCorsHeaders())
    }

    private fun handleIndexHtml(out: BufferedOutputStream) {
        val repo = repository
        val currencyCode = runBlocking(Dispatchers.IO) {
            repo?.getSettingFlow("primary_currency")?.first()
                ?: repo?.getSettingFlow("currency_code")?.first()
                ?: "INR"
        }
        val symbol = CurrencyFormatter.getCurrencySymbol(currencyCode)
        val html = PCWebAssets.getIndexHtml("Cash Tracker", symbol)
        sendResponse(out, 200, "OK", "text/html; charset=utf-8", html, extraHeaders = getCorsHeaders())
    }

    private fun handleGetData(fullPath: String, out: BufferedOutputStream) {
        val repo = repository ?: return
        runBlocking(Dispatchers.IO) {
            try {
                val params = fullPath.substringAfter('?', "").split('&').filter { it.contains('=') }.associate {
                    URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('='), "UTF-8")
                }
                val offset = params["offset"]?.toIntOrNull() ?: 0
                val limit = (params["limit"]?.toIntOrNull() ?: 100).coerceIn(1, 500)
                require(offset >= 0) { "Invalid page offset" }
                val snapshot = repo.database.withTransaction {
                    val accounts = repo.database.accountDao().getAllAccounts().first()
                    val categories = repo.database.categoryDao().getAllCategoriesSync()
                    val accountMap = accounts.associateBy { it.id }
                    val categoryMap = categories.associateBy { it.id }
                    val page = repo.database.transactionDao().getPage(limit, offset).map { tx ->
                        TransactionWithDetails(tx, accountMap[tx.accountId], accountMap[tx.toAccountId],
                            categoryMap[tx.categoryId], categoryMap[tx.subcategoryId])
                    }
                    DataPage(FinancialEngine.calculateAccountBalances(accounts, repo.database.transactionDao().getAllTransactionsSync()),
                        categories, page, repo.database.transactionDao().count(),
                        repo.getSetting("primary_currency") ?: repo.getSetting("currency_code") ?: "INR")
                }
                val accountsWithBalances = snapshot.accounts
                val categories = snapshot.categories
                val txWithDetails = snapshot.transactions
                val total = snapshot.total
                val currencyCode = snapshot.currency
                val symbol = CurrencyFormatter.getCurrencySymbol(currencyCode)

                val root = JSONObject()
                root.put("totalCount", total)
                root.put("offset", offset)
                root.put("nextOffset", if (offset.toLong() + txWithDetails.size < total) offset + txWithDetails.size else JSONObject.NULL)
                root.put("currencyCode", currencyCode)
                root.put("currencySymbol", symbol)

                // Accounts
                val accArray = JSONArray()
                for (ab in accountsWithBalances) {
                    val aObj = JSONObject()
                    aObj.put("id", ab.account.id)
                    aObj.put("name", ab.account.name)
                    aObj.put("type", ab.account.type.name)
                    aObj.put("balance", ab.calculatedBalance)
                    accArray.put(aObj)
                }
                root.put("accounts", accArray)

                // Categories
                val catArray = JSONArray()
                for (cat in categories) {
                    val cObj = JSONObject()
                    cObj.put("id", cat.id)
                    cObj.put("name", cat.name)
                    cObj.put("type", cat.type.name)
                    cObj.put("icon", cat.iconName)
                    cObj.put("color", cat.colorHex)
                    catArray.put(cObj)
                }
                root.put("categories", catArray)

                // Transactions
                val txArray = JSONArray()
                for (t in txWithDetails) {
                    val tx = t.transaction
                    val tObj = JSONObject()
                    tObj.put("id", tx.id)
                    tObj.put("type", tx.type.name)
                    tObj.put("amount", tx.amount)
                    tObj.put("dateMillis", tx.dateMillis)
                    tObj.put("accountId", tx.accountId)
                    tObj.put("accountName", t.account?.name ?: "Unknown")
                    tObj.put("toAccountId", tx.toAccountId ?: JSONObject.NULL)
                    tObj.put("toAccountName", t.toAccount?.name ?: JSONObject.NULL)
                    tObj.put("categoryId", tx.categoryId)
                    tObj.put("categoryName", t.category?.name ?: "Uncategorized")
                    tObj.put("subcategoryId", tx.subcategoryId ?: JSONObject.NULL)
                    tObj.put("subcategoryName", t.subcategory?.name ?: JSONObject.NULL)
                    tObj.put("payee", tx.payee)
                    tObj.put("note", tx.note)
                    txArray.put(tObj)
                }
                root.put("transactions", txArray)

                sendResponse(out, 200, "OK", "application/json", root.toString(), extraHeaders = getCorsHeaders())
            } catch (e: Exception) {
                e.printStackTrace()
                sendResponse(out, 500, "Internal Error", "application/json", """{"error":"${e.message}"}""")
            }
        }
    }

    private fun handleCreateTransaction(body: String, out: BufferedOutputStream) {
        val repo = repository
        if (repo == null) {
            sendResponse(out, 500, "Internal Server Error", "application/json", """{"success":false,"error":"Repository not initialized"}""", extraHeaders = getCorsHeaders())
            return
        }
        try {
            if (body.isBlank()) {
                sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"Request body is empty"}""", extraHeaders = getCorsHeaders())
                return
            }
            val json = JSONObject(body)
            val typeStr = json.optString("type", "EXPENSE").uppercase()
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (e: Exception) {
                TransactionType.EXPENSE
            }

            val amount = when (val rawAmount = json.opt("amount")) {
                is Number -> rawAmount.toLong()
                is String -> rawAmount.toLongOrNull() ?: (rawAmount.toDoubleOrNull()?.times(100)?.toLong() ?: 0L)
                else -> 0L
            }

            if (amount <= 0L) {
                sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"Amount must be greater than 0"}""", extraHeaders = getCorsHeaders())
                return
            }

            val dateMillis = if (json.has("dateMillis") && !json.isNull("dateMillis")) {
                val d = json.optLong("dateMillis", 0L)
                if (d > 0L) d else System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }

            var accountId = if (json.has("accountId") && !json.isNull("accountId")) {
                json.optLong("accountId", 0L)
            } else 0L

            if (accountId <= 0L) {
                val firstAcc = runBlocking(Dispatchers.IO) { repo.allAccounts.first().firstOrNull() }
                accountId = firstAcc?.id ?: 1L
            }

            val toAccountId = if (type == TransactionType.TRANSFER && json.has("toAccountId") && !json.isNull("toAccountId")) {
                val toId = json.optLong("toAccountId", 0L)
                if (toId > 0L) toId else null
            } else {
                null
            }

            var categoryId = if (json.has("categoryId") && !json.isNull("categoryId")) {
                json.optLong("categoryId", 0L)
            } else 0L

            if (categoryId <= 0L && type != TransactionType.TRANSFER) {
                val firstCat = runBlocking(Dispatchers.IO) {
                    repo.allCategories.first().firstOrNull { it.type == type }
                }
                categoryId = firstCat?.id ?: 0L
            }

            val payee = json.optString("payee", "").trim()
            val note = json.optString("note", "").trim()

            // Server-side deduplication guard: prevent duplicate transactions created within 1.5 seconds
            val now = System.currentTimeMillis()
            synchronized(recentSubmissions) {
                recentSubmissions.removeAll { now - it.timestamp > 10_000 }

                val duplicate = recentSubmissions.firstOrNull {
                    it.type == type &&
                    it.amount == amount &&
                    it.accountId == accountId &&
                    it.toAccountId == toAccountId &&
                    it.categoryId == categoryId &&
                    it.payee == payee &&
                    it.note == note &&
                    (now - it.timestamp < 1_500)
                }

                if (duplicate != null) {
                    sendResponse(
                        out,
                        200,
                        "OK",
                        "application/json",
                        """{"success":true,"id":${duplicate.insertedId},"deduplicated":true}""",
                        extraHeaders = getCorsHeaders()
                    )
                    return
                }
            }

            val tx = TransactionEntity(
                type = type,
                amount = amount,
                dateMillis = dateMillis,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                payee = payee,
                note = note,
                paymentMethod = PaymentMethod.CASH
            )

            val newId = runBlocking(Dispatchers.IO) {
                repo.insertTransaction(tx)
            }

            synchronized(recentSubmissions) {
                recentSubmissions.add(
                    RecentTxSubmission(
                        type = type,
                        amount = amount,
                        accountId = accountId,
                        toAccountId = toAccountId,
                        categoryId = categoryId,
                        payee = payee,
                        note = note,
                        insertedId = newId,
                        timestamp = now
                    )
                )
            }

            sendResponse(out, 200, "OK", "application/json", """{"success":true,"id":$newId}""", extraHeaders = getCorsHeaders())
        } catch (e: Exception) {
            e.printStackTrace()
            val safeMsg = (e.message ?: "Unknown error").replace("\"", "'")
            sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"$safeMsg"}""", extraHeaders = getCorsHeaders())
        }
    }

    private fun handleUpdateTransaction(body: String, out: BufferedOutputStream) {
        val repo = repository ?: return
        try {
            val json = JSONObject(body)
            val id = json.optLong("id", 0L)
            if (id <= 0L) {
                sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"Invalid transaction ID"}""", extraHeaders = getCorsHeaders())
                return
            }
            val typeStr = json.optString("type", "EXPENSE").uppercase()
            val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
            val amount = when (val rawAmount = json.opt("amount")) {
                is Number -> rawAmount.toLong()
                is String -> rawAmount.toLongOrNull() ?: (rawAmount.toDoubleOrNull()?.times(100)?.toLong() ?: 0L)
                else -> 0L
            }
            val dateMillis = if (json.has("dateMillis") && !json.isNull("dateMillis")) {
                val d = json.optLong("dateMillis", 0L)
                if (d > 0L) d else System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }
            var accountId = if (json.has("accountId") && !json.isNull("accountId")) json.optLong("accountId", 0L) else 0L
            if (accountId <= 0L) {
                val firstAcc = runBlocking(Dispatchers.IO) { repo.allAccounts.first().firstOrNull() }
                accountId = firstAcc?.id ?: 1L
            }
            val toAccountId = if (type == TransactionType.TRANSFER && json.has("toAccountId") && !json.isNull("toAccountId")) {
                val toId = json.optLong("toAccountId", 0L)
                if (toId > 0L) toId else null
            } else {
                null
            }
            val categoryId = if (json.has("categoryId") && !json.isNull("categoryId")) json.optLong("categoryId", 0L) else 0L
            val payee = json.optString("payee", "").trim()
            val note = json.optString("note", "").trim()

            val tx = TransactionEntity(
                id = id,
                type = type,
                amount = amount,
                dateMillis = dateMillis,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                payee = payee,
                note = note
            )

            runBlocking(Dispatchers.IO) {
                repo.updateTransaction(tx)
            }

            sendResponse(out, 200, "OK", "application/json", """{"success":true}""", extraHeaders = getCorsHeaders())
        } catch (e: Exception) {
            e.printStackTrace()
            val safeMsg = (e.message ?: "Unknown error").replace("\"", "'")
            sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"$safeMsg"}""", extraHeaders = getCorsHeaders())
        }
    }

    private fun handleDeleteTransaction(body: String, out: BufferedOutputStream) {
        val repo = repository ?: return
        try {
            val json = JSONObject(body)
            val id = json.optLong("id", 0L)
            if (id <= 0L) {
                sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"Invalid transaction ID"}""", extraHeaders = getCorsHeaders())
                return
            }
            runBlocking(Dispatchers.IO) {
                repo.deleteTransactionById(id)
            }
            sendResponse(out, 200, "OK", "application/json", """{"success":true}""", extraHeaders = getCorsHeaders())
        } catch (e: Exception) {
            val safeMsg = (e.message ?: "Unknown error").replace("\"", "'")
            sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"$safeMsg"}""", extraHeaders = getCorsHeaders())
        }
    }

    private fun handleCreateAccount(body: String, out: BufferedOutputStream) {
        val repo = repository ?: return
        try {
            val json = JSONObject(body)
            val name = json.optString("name", "").trim()
            if (name.isBlank()) {
                sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"Account name cannot be empty"}""", extraHeaders = getCorsHeaders())
                return
            }
            val typeStr = json.optString("type", "BANK")
            val type = try { AccountType.valueOf(typeStr) } catch (e: Exception) { AccountType.BANK }
            val initBal = json.optLong("initialBalance", 0L)
            val currency = runBlocking(Dispatchers.IO) {
                repo.getSettingFlow("primary_currency").first()
                    ?: repo.getSettingFlow("currency_code").first()
                    ?: "INR"
            }

            val acc = Account(
                name = name,
                type = type,
                initialBalance = initBal,
                currency = currency
            )

            val id = runBlocking(Dispatchers.IO) {
                repo.insertAccount(acc)
            }

            sendResponse(out, 200, "OK", "application/json", """{"success":true,"id":$id}""", extraHeaders = getCorsHeaders())
        } catch (e: Exception) {
            val safeMsg = (e.message ?: "Unknown error").replace("\"", "'")
            sendResponse(out, 400, "Bad Request", "application/json", """{"success":false,"error":"$safeMsg"}""", extraHeaders = getCorsHeaders())
        }
    }

    private fun handleExportCsv(out: BufferedOutputStream) {
        val repo = repository ?: return
        runBlocking(Dispatchers.IO) {
            try {
                val txs = repo.currentTransactionsWithDetails()
                val currency = repo.getSettingFlow("primary_currency").first()
                    ?: repo.getSettingFlow("currency_code").first()
                    ?: "INR"
                val csvContent = BackupManager.exportRealbyteCsv(repo.database, txs, currency)

                val extra = getCorsHeaders().toMutableMap()
                val fileName = "Cash_Tracker_Export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
                extra["Content-Disposition"] = "attachment; filename=\"$fileName\""
                sendResponse(out, 200, "OK", "text/csv; charset=utf-8", csvContent, extraHeaders = extra)
            } catch (e: Exception) {
                e.printStackTrace()
                sendResponse(out, 500, "Export Error", "text/plain", "Failed to export CSV: ${e.message}", extraHeaders = getCorsHeaders())
            }
        }
    }

    private fun handleExportJson(out: BufferedOutputStream) {
        val repo = repository ?: return
        runBlocking(Dispatchers.IO) {
            try {
                val json = BackupManager.createJsonBackup(repo.database)
                val extra = getCorsHeaders().toMutableMap()
                val fileName = "Cash_Tracker_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
                extra["Content-Disposition"] = "attachment; filename=\"$fileName\""
                sendResponse(out, 200, "OK", "application/json; charset=utf-8", json, extraHeaders = extra)
            } catch (e: Exception) {
                sendResponse(out, 500, "Export Error", "text/plain", "Failed to export JSON", extraHeaders = getCorsHeaders())
            }
        }
    }

    private fun handleImportJson(body: String, out: BufferedOutputStream) {
        val repo = repository ?: return
        runBlocking(Dispatchers.IO) {
            try {
                val success = BackupManager.restoreJsonBackup(repo.database, body)
                sendResponse(
                    out,
                    200,
                    "OK",
                    "application/json",
                    """{"success":$success,"message":"${if (success) "Restored successfully" else "Failed to restore database"}"}""",
                    extraHeaders = getCorsHeaders()
                )
            } catch (e: Exception) {
                sendResponse(out, 400, "Import Error", "application/json", """{"success":false,"error":"${e.message}"}""")
            }
        }
    }

    private fun getCorsHeaders(): Map<String, String> {
        return mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, PUT, DELETE, OPTIONS",
            "Access-Control-Allow-Headers" to "Content-Type, Authorization, X-Passcode, Cookie"
        )
    }

    private fun sendResponse(
        out: BufferedOutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: String,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        if (body.length > MAX_RESPONSE) {
            sendResponse(out, 413, "Response Too Large", "application/json", """{"error":"Export is too large for PC Manager. Export from the app instead."}""", getCorsHeaders())
            return
        }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_RESPONSE) {
            sendResponse(out, 413, "Response Too Large", "application/json", """{"error":"Export is too large for PC Manager. Export from the app instead."}""", getCorsHeaders())
            return
        }
        val sb = StringBuilder()
        sb.append("HTTP/1.1 $statusCode $statusText\r\n")
        sb.append("Content-Type: $contentType\r\n")
        sb.append("Content-Length: ${bytes.size}\r\n")
        sb.append("Connection: close\r\n")
        for ((k, v) in extraHeaders) {
            sb.append("$k: $v\r\n")
        }
        sb.append("\r\n")

        out.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
        out.flush()
    }
}
