package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

object NetworkUtils {

    fun getLocalIpAddress(context: Context): String {
        try {
            // Check Wi-Fi manager first if available
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val ipInt = wifiManager.connectionInfo.ipAddress
                if (ipInt != 0) {
                    val ipStr = String.format(
                        Locale.US,
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                    if (ipStr != "0.0.0.0" && !ipStr.startsWith("127.")) {
                        return ipStr
                    }
                }
            }

            // Scan network interfaces (e.g. wlan0, eth0, tethering, etc.)
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First look specifically for wlan / eth
            for (intf in interfaces) {
                if (intf.isUp && !intf.isLoopback) {
                    val name = intf.name.lowercase(Locale.ROOT)
                    if (name.contains("wlan") || name.contains("eth") || name.contains("ap")) {
                        val addrs = Collections.list(intf.inetAddresses)
                        for (addr in addrs) {
                            if (!addr.isLoopbackAddress && addr is Inet4Address) {
                                val host = addr.hostAddress
                                if (!host.isNullOrBlank() && !host.startsWith("127.")) {
                                    return host
                                }
                            }
                        }
                    }
                }
            }

            // Fallback: any non-loopback IPv4
            for (intf in interfaces) {
                if (intf.isUp && !intf.isLoopback) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && !host.startsWith("127.")) {
                                return host
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    fun isWifiOrLanConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            true
        }
    }
}
