package com.poi.core.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GitHubUpdateClient(
    private val latestReleaseUrl: String =
        "https://api.github.com/repos/DhanushShriyan/Poi/releases/latest",
) {
    suspend fun latestRelease(): Result<AppUpdate?> = runCatching {
        withContext(Dispatchers.IO) {
            val connection = URL(latestReleaseUrl).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "Poi-Android")

                when (connection.responseCode) {
                    HttpURLConnection.HTTP_NOT_FOUND -> null
                    HttpURLConnection.HTTP_OK -> parseRelease(
                        connection.inputStream.bufferedReader().use { it.readText() },
                    )
                    else -> error("GitHub update check failed with HTTP ${connection.responseCode}")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    suspend fun findUpdate(currentVersion: String): Result<AppUpdate?> =
        latestRelease().map { release ->
            release?.takeIf { VersionComparator.isNewer(it.versionName, currentVersion) }
        }

    private fun parseRelease(payload: String): AppUpdate? {
        val release = JSONObject(payload)
        val versionName = release.getString("tag_name").removePrefix("v")

        val assets = release.getJSONArray("assets")
        val apkAsset = (0 until assets.length())
            .asSequence()
            .map(assets::getJSONObject)
            .firstOrNull { asset -> asset.optString("name").endsWith(".apk") }
            ?: return null

        return AppUpdate(
            versionName = versionName,
            downloadUrl = apkAsset.getString("browser_download_url"),
            notes = release.optString("body"),
        )
    }
}
