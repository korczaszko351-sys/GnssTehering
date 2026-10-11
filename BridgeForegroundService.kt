package com.example.gnsstethering

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class BridgeForegroundService : Service() {
    private var bridge: GnssTcpBridge? = null

    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val host = intent?.getStringExtra(EXTRA_HOST) ?: throw IllegalArgumentException("Brak REMOTE_HOST")
        val port = intent.getIntExtra(EXTRA_PORT, 8883)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GNSS tethering bridge")
            .setContentText("Nasłuchiwanie TCP :9000 i wysyłka przez LTE")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        bridge = GnssTcpBridge(9000, host, port).also { it.start() }
        return START_STICKY
    }

    override fun onDestroy() { bridge?.stop(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "GNSS bridge", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        const val EXTRA_HOST = "REMOTE_HOST"
        const val EXTRA_PORT = "REMOTE_PORT"
        private const val CHANNEL_ID = "gnss_tethering_bridge"
        private const val ID = 1
    }
}
