package com.example.gnsstethering

import android.content.Context
import android.content.Intent
import android.net.TetheringManager
import android.os.Build
import android.provider.Settings
import java.util.concurrent.Executors

object TetheringController {
    private const val SAMSUNG_HOTSPOT_SETTINGS = "com.samsung.android.settings.WIFI_TETHER_SETTINGS"

    fun start(context: Context, callback: (Boolean, String) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            openHotspotSettings(context)
            callback(false, "Galaxy A10: włącz hotspot ręcznie, następnie uruchom mostek.")
            return
        }
        val manager = context.getSystemService(TetheringManager::class.java)
        val request = TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI).build()
        try {
            manager.startTethering(request, Executors.newSingleThreadExecutor(), object : TetheringManager.StartTetheringCallback() {
                override fun onTetheringStarted() { callback(true, "Hotspot uruchomiony.") }
                override fun onTetheringFailed(error: Int) {
                    openHotspotSettings(context)
                    callback(false, "System odmówił tetheringu ($error). Otwarto ustawienia.")
                }
            })
        } catch (e: SecurityException) {
            openHotspotSettings(context)
            callback(false, "Brak TETHER_PRIVILEGED. Włącz hotspot ręcznie.")
        }
    }

    fun stop(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        try { context.getSystemService(TetheringManager::class.java).stopTethering(TetheringManager.TETHERING_WIFI) } catch (_: SecurityException) {}
    }

    private fun openHotspotSettings(context: Context) {
        val intent = Intent(SAMSUNG_HOTSPOT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
