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

package net.ccbluex.liquidbounce.features.module.modules.misc.bettertitle

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.TitleEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBetterTitle
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes

object TitleFilter : ToggleableConfigurable(ModuleBetterTitle, "TitleFilter", true) {

    private var regexFilters = emptySet<Regex>()
    private val filters by textList("Filters", mutableListOf()).onChanged {
        compileFilters()
    }

    private fun compileFilters() {
        regexFilters = filters.mapTo(HashSet(filters.size, 1.0f)) { Regex(".*${Regex.escape(it)}.*") }
    }

    private inline fun <reified E : TitleEvent.TextContent> filterHandler(
    ) = handler<E> { event ->
        val string = event.text
            ?.string
            ?.stripMinecraftColorCodes()
            ?.takeUnless(String::isBlank)
            ?: return@handler

        if (regexFilters.any { it.matches(string) }) {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    val titleFilterHandler = filterHandler<TitleEvent.Title>()

    @Suppress("unused")
    val subtitleFilterHandler = filterHandler<TitleEvent.Subtitle>()
}
