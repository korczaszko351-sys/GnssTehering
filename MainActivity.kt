package com.example.gnsstethering

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        requestPermissions()

        findViewById<Button>(R.id.startTethering).setOnClickListener {
            TetheringController.start(this) { _, message -> runOnUiThread { status.text = message } }
        }

        findViewById<Button>(R.id.startBridge).setOnClickListener {
            val intent = Intent(this, BridgeForegroundService::class.java).apply {
                putExtra(BridgeForegroundService.EXTRA_HOST, "twoj-serwer.example.com")
                putExtra(BridgeForegroundService.EXTRA_PORT, 8883)
            }
            startForegroundService(intent)
            status.text = "Mostek działa na porcie TCP 9000."
        }

        findViewById<Button>(R.id.stopBridge).setOnClickListener {
            stopService(Intent(this, BridgeForegroundService::class.java))
            status.text = "Mostek zatrzymany."
        }
    }

    private fun requestPermissions() {
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = needed.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
    }
}
