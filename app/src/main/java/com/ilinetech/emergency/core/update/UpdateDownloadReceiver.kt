package com.ilinetech.emergency.core.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ilinetech.emergency.core.data.Prefs

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Confirms the completed download is the one WE started (matching against
 * Prefs.pendingUpdateDownloadId) before doing anything — DOWNLOAD_COMPLETE
 * is a broadcast any app could theoretically send, so this check is a real
 * safety measure, not just bookkeeping.
 */
class UpdateDownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val prefs = Prefs(context)
        if (completedId == -1L || completedId != prefs.pendingUpdateDownloadId) return

        prefs.pendingUpdateDownloadId = -1L
        UpdateDownloader.promptInstall(context)
    }
}
