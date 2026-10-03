package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.server.PCManagerServer
import com.example.server.PCWebAssets
import com.example.util.NetworkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PCManagerTest {

    @Test
    fun testPCWebAssetsGeneratesValidHtml() {
        val html = PCWebAssets.getIndexHtml("Cash Tracker", "$")
        assertNotNull(html)
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("PC MANAGER"))
        assertTrue(html.contains("Connected to Mobile Device"))
        assertTrue(html.contains("Cash Tracker"))
    }

    @Test
    fun testNetworkUtilsReturnsValidIp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ip = NetworkUtils.getLocalIpAddress(context)
        assertNotNull(ip)
        assertTrue(ip.isNotEmpty())
    }

    @Test
    fun testServerLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val repo = FinanceRepository(db)

        val started = PCManagerServer.start(
            context = context,
            repo = repo,
            requestedPort = 9988,
            passcodeOn = true,
            code = "9876"
        )
        assertTrue("Server should start", started)
        assertTrue(PCManagerServer.isRunning)
        assertTrue(PCManagerServer.isPasscodeEnabled)
        assertEquals("9876", PCManagerServer.passcode)

        PCManagerServer.stop()
        assertFalse(PCManagerServer.isRunning)
    }
}
