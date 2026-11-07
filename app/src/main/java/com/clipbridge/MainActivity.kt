package com.clipbridge

import android.app.NotificationManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var clipsList: ListView
    private lateinit var toggleBtn: Button
    private lateinit var adapter: ArrayAdapter<String>
    private val displayItems = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        clipsList = findViewById(R.id.clipsList)
        toggleBtn = findViewById(R.id.toggleBtn)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayItems)
        clipsList.adapter = adapter

        toggleBtn.setOnClickListener {
            if (!ClipboardService.isRunning) {
                startForegroundService(Intent(this, ClipboardService::class.java))
            } else {
                ClipboardService.isPaused = !ClipboardService.isPaused
                updateUI()
            }
        }

        findViewById<Button>(R.id.clearBtn).setOnClickListener {
            ClipboardService.recentClips.clear()
            refreshClips()
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // Start service on first launch
        if (!ClipboardService.isRunning) {
            startForegroundService(Intent(this, ClipboardService::class.java))
        }

        // Refresh UI every second
        val handler = android.os.Handler(mainLooper)
        val refresher = object : Runnable {
            override fun run() {
                refreshClips()
                updateUI()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(refresher)
    }

    private fun updateUI() {
        when {
            !ClipboardService.isRunning -> {
                statusText.text = "● Stopped"
                statusText.setTextColor(getColor(android.R.color.holo_red_light))
                toggleBtn.text = "Start Sync"
            }
            ClipboardService.isPaused -> {
                statusText.text = "⏸ Paused"
                statusText.setTextColor(getColor(android.R.color.holo_orange_light))
                toggleBtn.text = "Resume"
            }
            else -> {
                statusText.text = "● Syncing"
                statusText.setTextColor(getColor(android.R.color.holo_green_light))
                toggleBtn.text = "Pause"
            }
        }
    }

    private fun refreshClips() {
        displayItems.clear()
        ClipboardService.recentClips.forEach { (text, source) ->
            val icon = if (source == "mac") "💻" else "📱"
            displayItems.add("$icon ${text.take(60)}${if (text.length > 60) "…" else ""}")
        }
        if (displayItems.isEmpty()) displayItems.add("No clips yet — copy something!")
        adapter.notifyDataSetChanged()
    }
}
