package net.kotlinmodule.jmcomicfix.api.services.client

import net.ccbluex.liquidbounce.utils.client.logger
import org.graalvm.shadowed.org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime

object ClientApi {

    private const val GITHUB_API_LATEST_RELEASE =
        "https://api.github.com/repos/Chasteful/JMcomicFix/releases/latest"

    data class GithubRelease(
        val tagName: String,
        val downloadUrl: String,
        val publishedAt: OffsetDateTime,
        val prerelease: Boolean
    )

    fun getLatestRelease(): GithubRelease? {
        return try {
            val connection = URL(GITHUB_API_LATEST_RELEASE).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode != 200) return null

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val result = reader.use { it.readText() }
            val json = JSONObject(result)

            val tag = json.optString("tag_name", "New Version")
            val assets = json.optJSONArray("assets")
            val downloadUrl = if (assets != null && assets.length() > 0) {
                assets.getJSONObject(0).optString("browser_download_url", "")
            } else {
                ""
            }

            val publishedAt = OffsetDateTime.parse(json.optString("published_at"))
            val prerelease = json.optBoolean("prerelease", false)

            GithubRelease(tag, downloadUrl, publishedAt, prerelease)
        } catch (e: Exception) {
            logger.error("Failed to fetch GitHub release", e)
            null
        }
    }
}
