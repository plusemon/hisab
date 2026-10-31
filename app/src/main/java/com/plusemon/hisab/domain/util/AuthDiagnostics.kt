package com.plusemon.hisab.domain.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object AuthDiagnostics {

    // Debug keystore SHA-1 generated in repository / CI
    const val DEBUG_KEYSTORE_SHA1 = "41:33:99:F7:93:E1:54:93:B7:F9:15:4E:AC:08:B5:FA:24:19:3D:60"

    // Known SHA-1 hashes registered in Firebase for hisab-app-161ed
    val KNOWN_FIREBASE_SHA1_HASHES = listOf(
        DEBUG_KEYSTORE_SHA1,
        "71:D4:ED:DF:3C:4C:36:AC:A6:59:22:C5:A9:F9:74:B5:D2:EE:E3:91", // Device release / install key
        "A2:7F:46:52:5C:5E:1C:93:89:4F:07:C3:17:F9:83:B3:38:CE:51:0E", // Release key 1
        "96:51:7E:55:07:33:3F:1C:F9:50:FA:78:C1:59:B7:6A:CE:27:A2:5D", // Release key 2
    )

    /**
     * Retrieves the current app's signing certificate SHA-1 fingerprint formatted as
     * colon-separated uppercase hex bytes (e.g. 41:33:99:F7:93:E1:...).
     */
    fun getCurrentAppSha1(context: Context): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return "Unavailable"
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(certBytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "Unavailable: ${e.message}"
        }
    }

    /**
     * Checks whether the current app's signing SHA-1 is already in the list of
     * recognized fingerprints in Firebase.
     */
    fun isSha1RegisteredInFirebase(context: Context): Boolean {
        val currentSha1 = getCurrentAppSha1(context).trim().uppercase()
        val normalizedKnown = KNOWN_FIREBASE_SHA1_HASHES.map { it.trim().uppercase() }
        return normalizedKnown.contains(currentSha1)
    }
}
