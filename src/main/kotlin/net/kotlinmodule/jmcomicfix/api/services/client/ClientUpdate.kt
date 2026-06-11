package net.kotlinmodule.jmcomicfix.api.services.client

import com.vdurmont.semver4j.Semver
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.AsyncLazy
import net.ccbluex.liquidbounce.utils.client.logger

object ClientUpdate {

    val release by AsyncLazy {
        runCatching {
            val latest = ClientApi.getLatestRelease() ?: return@AsyncLazy null

            val currentVersionStr = LiquidBounce.clientVersion
            val currentVersion = Semver(currentVersionStr, Semver.SemverType.LOOSE)

            val tagName = latest.tagName
            val latestVersionStr = if (tagName.startsWith("v")) {
                tagName.substring(1)
            } else {
                tagName
            }

            val latestVersion = try {
                Semver(latestVersionStr, Semver.SemverType.LOOSE)
            } catch (_: Exception) {
                null
            }

            if (latestVersion == null || !latestVersion.isGreaterThan(currentVersion)) {
                null
            } else {
                latest
            }
        }.onFailure { e ->
            logger.error("Failed to check for update", e)
        }.getOrNull()
    }
}
