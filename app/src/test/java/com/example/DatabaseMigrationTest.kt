package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {
    @Test fun upgradePreservesLegacyRecordsAndAddsOccurrenceUniqueness() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { legacy ->
            javaClass.getResourceAsStream("/schema-v1.sql")!!.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.forEach(legacy::execSQL)
            }
            legacy.execSQL("INSERT INTO transactions (id,type,dateMillis,amount,accountId,categoryId,payee,note,tags,paymentMethod,transferFee,isExcludedFromStats,isBookmarked) VALUES (42,'EXPENSE',1705320000000,12345,1,1,'Shop','Legacy record','','CASH',0,0,0)")
            legacy.version = 1
        }
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            val dao = upgraded.transactionDao()
            val record = dao.getTransactionById(42)!!
            assertEquals(12345L, record.amount)
            assertEquals("Legacy record", record.note)
            assertNull(record.occurrenceKey)
            val first = dao.insertOccurrence(record.copy(id = 0, occurrenceKey = "recurring:7:123"))
            val second = dao.insertOccurrence(record.copy(id = 0, occurrenceKey = "recurring:7:123"))
            assertTrue(first > 0)
            assertEquals(-1L, second)
            assertEquals(2, dao.count())
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
}
