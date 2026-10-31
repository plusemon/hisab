package com.plusemon.hisab.data.repository

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.plusemon.hisab.data.model.UpdateInfo
import com.plusemon.hisab.domain.util.VersionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class UpdateManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("hisab_update_prefs", Context.MODE_PRIVATE)

    fun getDismissedVersion(): String? {
        return prefs.getString("dismissed_version", null)
    }

    fun saveDismissedVersion(version: String) {
        prefs.edit().putString("dismissed_version", VersionUtils.cleanVersion(version)).apply()
    }

    fun getTargetApkFile(version: String): File {
        val clean = VersionUtils.cleanVersion(version)
        val fileName = "Hisab-v$clean.apk"
        return File(context.getExternalFilesDir(null), fileName)
    }

    /**
     * Deletes previously downloaded Hisab-v*.apk files whose version is older than or equal to currentVersion.
     */
    fun cleanupOldApks(currentVersion: String) {
        try {
            val dir = context.getExternalFilesDir(null) ?: return
            val apkFiles = dir.listFiles { file ->
                file.isFile && file.name.startsWith("Hisab-v", ignoreCase = true) && file.name.endsWith(".apk", ignoreCase = true)
            } ?: return

            for (file in apkFiles) {
                val fileVersion = file.name
                    .removePrefix("Hisab-v")
                    .removePrefix("hisab-v")
                    .removeSuffix(".apk")
                    .removeSuffix(".APK")
                if (!VersionUtils.isVersionNewer(currentVersion, fileVersion)) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }

    fun downloadApk(updateInfo: UpdateInfo): Flow<Float> = flow {
        val destinationFile = getTargetApkFile(updateInfo.version)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val displayVer = VersionUtils.formatDisplayVersion(updateInfo.version)
        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl)).apply {
            setTitle("Hisab $displayVer")
            setDescription("Downloading app update...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, null, destinationFile.name)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadId = downloadManager.enqueue(request)
        var downloading = true

        try {
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                        val bytesDownloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                        val bytesTotal = if (bytesTotalIndex >= 0) cursor.getLong(bytesTotalIndex) else 0L
                        val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                        val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else 0

                        if (bytesTotal > 0L) {
                            val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                            emit(progress.coerceIn(0f, 1f))
                        } else {
                            emit(0f)
                        }

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                emit(1f)
                                downloading = false
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                throw Exception("Download failed (error $reason)")
                            }
                        }
                    } else {
                        // Row was removed (e.g. user cancelled the download from notification shade)
                        downloading = false
                        throw CancellationException("Download cancelled")
                    }
                }

                if (downloading) {
                    delay(500)
                }
            }
        } catch (e: CancellationException) {
            downloadManager.remove(downloadId)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            throw e
        }
    }

    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun promptInstallApk(version: String): Boolean {
        val apkFile = getTargetApkFile(version)
        if (!apkFile.exists() || apkFile.length() == 0L) return false

        if (!canInstallUnknownApps()) {
            return false
        }

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
