/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.api.services.client

import com.vdurmont.semver4j.Semver
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.AsyncLazy
import net.ccbluex.liquidbounce.utils.client.logger
import java.text.SimpleDateFormat
import java.util.*

object ClientUpdate {

    val gitInfo = Properties().also { properties ->
        val inputStream = LiquidBounce::class.java.classLoader.getResourceAsStream("git.properties")

        if (inputStream != null) {
            properties.load(inputStream)
        } else {
            properties["git.build.version"] = "1.7"
        }
    }

    val update by AsyncLazy {
        runCatching {
            val newestBuild = runCatching {
                ClientApi.requestNewestBuildEndpoint(
                    branch = LiquidBounce.clientBranch,
                    release = !LiquidBounce.IN_DEVELOPMENT
                )
            }.onFailure { exception ->
                logger.error("Unable to receive update information", exception)
            }.getOrNull() ?: return@AsyncLazy null

            val newestSemVersion = Semver(newestBuild.lbVersion, Semver.SemverType.LOOSE)

            val isNewer = if (LiquidBounce.IN_DEVELOPMENT) { // check if new build is newer than current build
                val newestVersionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(newestBuild.date)
                val currentVersionDate =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(gitInfo["git.commit.time"].toString())

                newestVersionDate.after(currentVersionDate)
            } else {
                // check if version number is higher than current version number (on release builds only!)
                val clientSemVersion = Semver(LiquidBounce.clientVersion, Semver.SemverType.LOOSE)

                newestBuild.release && newestSemVersion.isGreaterThan(clientSemVersion)
            }

            if (isNewer) {
                newestBuild
            } else {
                null
            }
        }.onFailure { exception ->
            logger.error("Failed to check for update", exception)
        }.getOrNull()
    }

}
