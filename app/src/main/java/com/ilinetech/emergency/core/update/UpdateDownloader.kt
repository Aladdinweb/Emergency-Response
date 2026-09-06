package com.ilinetech.emergency.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.ilinetech.emergency.core.data.Prefs
import java.io.File

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Uses the system DownloadManager rather than hand-rolling an HTTP download
 * — it handles retries, shows its own progress notification, and survives
 * the app being backgrounded mid-download for free.
 *
 * Requires android.permission.REQUEST_INSTALL_PACKAGES (declared in the
 * manifest) — the user must separately grant "install unknown apps" for
 * this app in system Settings the first time, since this isn't a Play
 * Store install. See UPDATE_NOTES.md for the exact flow.
 */
object UpdateDownloader {

    private const val APK_FILE_NAME = "emergency-response-update.apk"

    fun startDownload(context: Context, info: UpdateInfo) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle(context.getString(com.ilinetech.emergency.R.string.app_name))
            .setDescription(info.versionName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            .setAllowedOverMetered(true)

        val downloadId = downloadManager.enqueue(request)
        Prefs(context).pendingUpdateDownloadId = downloadId
    }

    /** Called by UpdateDownloadReceiver once DownloadManager confirms completion. */
    fun promptInstall(context: Context) {
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            APK_FILE_NAME
        )
        if (!file.exists()) return

        val apkUri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }
}
