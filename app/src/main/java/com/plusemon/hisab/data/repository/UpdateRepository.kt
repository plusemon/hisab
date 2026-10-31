package com.plusemon.hisab.data.repository

import android.util.Log
import com.plusemon.hisab.data.model.UpdateInfo
import com.plusemon.hisab.domain.util.MarkdownUtils
import com.plusemon.hisab.domain.util.VersionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class UpdateRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestRelease(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/plusemon/hisab/releases")
                .header("User-Agent", "Hisab-App")
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyString = response.body?.string() ?: return@withContext null
                val jsonArray = JSONArray(bodyString)
                if (jsonArray.length() == 0) return@withContext null

                for (rIndex in 0 until jsonArray.length()) {
                    val json = jsonArray.optJSONObject(rIndex) ?: continue
                    if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
                        continue
                    }

                    val tagName = json.optString("tag_name", "")
                    if (tagName.isBlank()) continue
                    val version = VersionUtils.cleanVersion(tagName)
                    if (version.isBlank()) continue

                    val assets = json.optJSONArray("assets") ?: continue
                    var downloadUrl: String? = null

                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            val url = asset.optString("browser_download_url", "")
                            if (url.isNotBlank()) {
                                downloadUrl = url
                                break
                            }
                        }
                    }

                    if (downloadUrl.isNullOrBlank()) continue

                    val rawReleaseNotes = json.optString("body", "")
                    val releaseNotes = MarkdownUtils.deduplicateReleaseNotes(rawReleaseNotes)

                    return@withContext UpdateInfo(
                        version = version,
                        rawTagName = tagName,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes
                    )
                }

                null
            }
        } catch (e: Exception) {
            Log.w("UpdateRepository", "Silent check for update skipped or unavailable: ${e.message}")
            null
        }
    }
}
