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
package net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.NameProtectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.GenericColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.font.processor.LegacyTextSanitizer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.bypassesNameProtection
import net.ccbluex.liquidbounce.utils.client.toText
import net.minecraft.client.MinecraftClient
import net.minecraft.text.CharacterVisitor
import net.ccbluex.liquidbounce.utils.collection.LfuCache
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.random.Random

private const val DEFAULT_CACHE_SIZE = 256
private const val DEFAULT_BUFFER_SIZE = 64

/**
 * NameProtect module
 *
 * Changes players names clientside.
 */

object ModuleNameProtect : ClientModule("NameProtect", Category.MISC) {

    private val replacement by text("Replacement", "You")

    private val colorMode = choices<GenericColorMode<Unit>>(
        "ColorMode",
        0
    ) {
        arrayOf(
            GenericStaticColorMode(it, Color4b(255, 179, 72, 50)),
            GenericRainbowColorMode(it),
            GenericSyncColorMode(it),
        )

    }

    val applyGarbled by boolean("Garbled", false).onChanged {
        EventManager.callEvent(NameProtectEvent(ModuleNameProtect))
    }
    private val disengageFix by boolean("脱盒修复", false)

    private object ReplaceFriendNames : ToggleableConfigurable(this, "ObfuscateFriends", true) {
        val friendsApplyGarbled by boolean("FriendsApplyGarbled", false)
        val colorMode = choices<GenericColorMode<Unit>>(
            ReplaceFriendNames,
            "ColorMode",
            0
        ) {
            arrayOf(
                GenericStaticColorMode(it, Color4b(0, 241, 255)),
                GenericRainbowColorMode(it),
                GenericSyncColorMode(it),
            )
        }
    }

    private object ReplaceOthers : ToggleableConfigurable(this, "ObfuscateOthers", false) {
        val othersApplyGarbled by boolean("OthersApplyGarbled", true)
        val colorMode = choices<GenericColorMode<Unit>>(
            ReplaceOthers,
            "ColorMode",
            0
        ) {
            arrayOf(
                GenericStaticColorMode(it, Color4b(71, 71, 71)),
                GenericRainbowColorMode(it),
                GenericSyncColorMode(it),
            )
        }
    }

    init {
        tree(ReplaceFriendNames)
        tree(ReplaceOthers)

        // Entirely keep out from public config
        doNotIncludeAlways()
    }

    private val replacementMappings = NameProtectMappings()

    private val coloringInfo = NameProtectMappings.ColoringInfo(
        username = { this.colorMode.activeChoice.getColor(Unit) },
        friends = { ReplaceFriendNames.colorMode.activeChoice.getColor(Unit) },
        otherPlayers = { ReplaceOthers.colorMode.activeChoice.getColor(Unit) },
    )
    private const val ALPHABET = "АБВГҐДЕЄЖЗИІЇЙКЛМНОПРСТУФХЦЧШЩЬЮЯ"
    private var lastPlayerNameCheck = 0L
    private const val NAME_CHECK_INTERVAL = 3000L

    fun getGarbledName(baseName: String): String {
        val nowTick = (System.currentTimeMillis() / 10).toInt()
        val seed = baseName.hashCode() xor nowTick
        val rng = Random(seed)
        val sb = StringBuilder(baseName.length)
        for (i in baseName.indices) {
            sb.append(ALPHABET[rng.nextInt(ALPHABET.length)])
        }
        return sb.toString()
    }
    @Suppress("unused")
    private val renderHandler = handler<GameTickEvent> {

        val friendMappings = if (ReplaceFriendNames.enabled) {
            FriendManager.friends.filter { it.name.isNotBlank() }.mapIndexed { id, friend ->
                val targetName = if (ReplaceFriendNames.friendsApplyGarbled) {
                   getGarbledName(friend.name)
                } else {
                    friend.alias ?: friend.getDefaultName(id)
                }
                friend.name to targetName
            }
        } else {
            emptyList()
        }
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return@handler
        val currentTime = System.currentTimeMillis()
        var cachedPlayerName = player.name.string
        if (currentTime - lastPlayerNameCheck >= NAME_CHECK_INTERVAL) {
            cachedPlayerName = player.name.string
            lastPlayerNameCheck = currentTime
        }

        val playerName = if (disengageFix) {
            cachedPlayerName
        } else {
            player.gameProfile?.name ?: mc.session.username
        }


        val selfPair = playerName to if (applyGarbled) {
           getGarbledName(replacement)
        } else {
            replacement
        }


        val otherPlayers = if (ReplaceOthers.enabled) {
            network.playerList?.mapNotNull { playerListEntry ->
                val otherName = playerListEntry?.profile?.name

                if (otherName != playerName) otherName else null
            }
        } else {
            null
        } ?: emptyList()

        replacementMappings.update(
            selfPair,
            friendMappings,
            otherPlayers,
            coloringInfo
        )
    }

    private val stringMappingCache = LfuCache<String, String>(DEFAULT_CACHE_SIZE)
    private val orderedTextMappingCache = LfuCache<OrderedText, OrderedText>(DEFAULT_CACHE_SIZE)

    fun replace(original: String): String {
        if (!running) {
            return original
        }

        return stringMappingCache.getOrPut(original) { replace0(original) }
    }

    private fun replace0(original: String): String {
        val replacements = replacementMappings.findReplacements(original)

        if (replacements.isEmpty()) {
            return original
        }

        val output = StringBuilder(DEFAULT_BUFFER_SIZE)

        var currReplacementIndex = 0
        var currentIndex = 0

        while (currentIndex < original.length) {
            val replacement = replacements.getOrNull(currReplacementIndex)

            val replacementStartIdx = replacement?.first?.start

            if (replacementStartIdx == currentIndex) {
                output.append(replacement.second.newName)

                currentIndex = replacement.first.end + 1
                currReplacementIndex += 1
            } else {
                val maxCopyIdx = replacementStartIdx ?: original.length

                output.append(original, currentIndex, maxCopyIdx)

                currentIndex = maxCopyIdx
            }
        }

        return output.toString()
    }

    fun wrap(original: OrderedText): OrderedText {
        if (!running) {
            return original
        }

        return orderedTextMappingCache.getOrPut(original) { wrap0(original) }
    }

    /**
     * Wraps an [OrderedText] to apply name protection.
     */
    private fun wrap0(original: OrderedText): OrderedText {
        val mappedCharacters = ObjectArrayList<MappedCharacter>(DEFAULT_BUFFER_SIZE)

        val originalCharacters = ObjectArrayList<MappedCharacter>(DEFAULT_BUFFER_SIZE)

        original.accept { _, style, codePoint ->
            originalCharacters += MappedCharacter(
                style,
                style.color?.bypassesNameProtection ?: false,
                codePoint
            )

            true
        }

        val text = buildString(originalCharacters.size) {
            originalCharacters.forEach { appendCodePoint(it.codePoint) }
        }
        val replacements = replacementMappings.findReplacements(text)

        var currReplacementIndex = 0
        var currentIndex = 0

        while (currentIndex < originalCharacters.size) {
            val replacement = replacements.getOrNull(currReplacementIndex)

            val replacementStartIdx = replacement?.first?.start

            if (replacementStartIdx == currentIndex) {
                if (originalCharacters[replacementStartIdx].bypassesNameProtection) {
                    currReplacementIndex++

                    continue
                }

                val color = replacement.second.colorGetter()

                mappedCharacters.ensureCapacity(mappedCharacters.size + replacement.second.newName.length)
                replacement.second.newName.mapTo(mappedCharacters) { ch ->
                    MappedCharacter(
                        originalCharacters[currentIndex].style.withColor(color.toARGB()),
                        false,
                        ch.code
                    )
                }

                currentIndex = replacement.first.end + 1
                currReplacementIndex += 1
            } else {
                val maxCopyIdx = replacementStartIdx ?: originalCharacters.size

                mappedCharacters.addAll(originalCharacters.subList(currentIndex, maxCopyIdx))

                currentIndex = maxCopyIdx
            }
        }

        // Access the inner array
        val innerMappedCharacters: Array<out Any?> = mappedCharacters.elements()
        val size = mappedCharacters.size

        return OrderedText { visitor ->
            for (index in 0 until size) {
                val char = innerMappedCharacters[index] as MappedCharacter
                if (!visitor.accept(index, char.style, char.codePoint)) {
                    return@OrderedText false
                }
            }

            true
        }
    }

    @JvmRecord
    private data class MappedCharacter(val style: Style, val bypassesNameProtection: Boolean, val codePoint: Int)
}

/**
 * Sanitizes texts which are sent to the client.
 * 1. Degenerates legacy formatting into new formatting [LegacyTextSanitizer]
 * 2. Applies [ModuleNameProtect] - if needed
 */
fun Text.sanitizeForeignInput(): Text {
    val degeneratedText = LegacyTextSanitizer.SanitizedLegacyText(this)

    if (!ModuleNameProtect.running) {
        return degeneratedText.toText()
    }

    return ModuleNameProtect.wrap(degeneratedText).toText()
}
