package com.clipbridge

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ClipTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!ClipboardService.isRunning) {
            startForegroundService(Intent(this, ClipboardService::class.java))
        } else {
            ClipboardService.isPaused = !ClipboardService.isPaused
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            when {
                !ClipboardService.isRunning -> {
                    state = Tile.STATE_INACTIVE
                    subtitle = "Stopped"
                }
                ClipboardService.isPaused -> {
                    state = Tile.STATE_INACTIVE
                    subtitle = "Paused"
                }
                else -> {
                    state = Tile.STATE_ACTIVE
                    subtitle = "Syncing"
                }
            }
            updateTile()
        }
    }
}
