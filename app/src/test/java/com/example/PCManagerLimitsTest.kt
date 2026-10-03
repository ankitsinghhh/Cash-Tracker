package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.server.PCManagerServer
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.Socket

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PCManagerLimitsTest {
    private fun request(path: String, headers: String = ""): String = Socket("127.0.0.1", PCManagerServer.port).use { socket ->
        socket.soTimeout = 10000
        socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: localhost\r\n$headers\r\n".toByteArray())
        socket.getInputStream().bufferedReader().readText()
    }

    @Test fun paginationIsBoundedAndMalformedRequestsAreRejected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            database.accountDao().insert(Account(1, "Cash", AccountType.CASH))
            database.categoryDao().insert(Category(1, "Food", TransactionType.EXPENSE))
            database.transactionDao().insertAll(List(650) { index -> TransactionEntity(
                type = TransactionType.EXPENSE, dateMillis = index.toLong(), amount = 100, accountId = 1, categoryId = 1) })
            assertTrue(PCManagerServer.start(context, FinanceRepository(database, scope), requestedPort = 0))
            val firstResponse = request("/api/data?limit=999999")
            assertTrue(firstResponse.startsWith("HTTP/1.1 200"))
            val first = JSONObject(firstResponse.substringAfter("\r\n\r\n"))
            assertEquals(650, first.getInt("totalCount"))
            assertEquals(500, first.getJSONArray("transactions").length())
            assertEquals(500, first.getInt("nextOffset"))
            val second = JSONObject(request("/api/data?offset=500&limit=500").substringAfter("\r\n\r\n"))
            assertEquals(150, second.getJSONArray("transactions").length())
            assertTrue(second.isNull("nextOffset"))
            database.transactionDao().insert(TransactionEntity(type = TransactionType.INCOME,
                dateMillis = 9999, amount = 200, accountId = 1, categoryId = 1, note = "Just saved"))
            val refreshed = JSONObject(request("/api/data?limit=1").substringAfter("\r\n\r\n"))
            assertEquals(651, refreshed.getInt("totalCount"))
            assertEquals("Just saved", refreshed.getJSONArray("transactions").getJSONObject(0).getString("note"))
            assertEquals(-64800L, refreshed.getJSONArray("accounts").getJSONObject(0).getLong("balance"))
            assertTrue(request("/api/status", "Content-Length: 9000000\r\n").startsWith("HTTP/1.1 400"))
            assertTrue(request("/api/status", "Content-Length: -1\r\n").startsWith("HTTP/1.1 400"))
            assertTrue(request("/api/status", "X-Large: ${"x".repeat(9000)}\r\n").startsWith("HTTP/1.1 400"))
        } finally { PCManagerServer.stop(); scope.cancel(); database.close() }
    }
}
