package com.clipbridge

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import org.json.JSONObject

class ClipboardService : Service() {

    companion object {
        const val CHANNEL_ID = "clipbridge_channel"
        const val NOTIF_ID = 1001
        var isRunning = false
        var isPaused = false
        var lastMacId: String? = null
        val recentClips = mutableListOf<Pair<String, String>>() // text, source
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastMacClipText = ""

    private val syncRunnable = object : Runnable {
        override fun run() {
            if (!isPaused) {
                syncAndroidToMac()
                syncMacToAndroid()
            }
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Syncing clipboard..."))
        handler.post(syncRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(syncRunnable)
        isRunning = false
    }

    override fun onBind(intent: Intent?) = null

    private fun syncAndroidToMac() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this).toString()
        if (text.isNotEmpty() && text != lastMacClipText && text.length < 100000) {
            lastMacClipText = text
            SupabaseClient.pushText(text)
            addClip(text, "android")
            updateNotification("Sent: ${text.take(40)}")
        }
    }

    private fun syncMacToAndroid() {
        SupabaseClient.getLatestFromMac { item ->
            item ?: return@getLatestFromMac
            val id = item.optString("id")
            if (id == lastMacId) return@getLatestFromMac
            lastMacId = id
            val content = item.optString("content")
            if (content.isNotEmpty()) {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ClipBridge", content)
                handler.post { cm.setPrimaryClip(clip) }
                addClip(content, "mac")
                updateNotification("From Mac: ${content.take(40)}")
                showIncomingNotification(content)
            }
        }
    }

    private fun addClip(text: String, source: String) {
        recentClips.add(0, Pair(text, source))
        if (recentClips.size > 20) recentClips.removeAt(recentClips.size - 1)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClipBridge")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun showIncomingNotification(content: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📋 Copied from Mac")
            .setContentText(content.take(80))
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setAutoCancel(true)
            .build()
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "ClipBridge Sync",
            NotificationManager.IMPORTANCE_LOW).apply {
            description = "Clipboard sync status"
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }
}
