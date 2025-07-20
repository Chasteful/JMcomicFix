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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isHidingNow
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.integration.theme.component.customComponents
import net.ccbluex.liquidbounce.integration.theme.component.types.minimap.ChunkRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.minecraft.client.gui.screen.DisconnectedScreen
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen

/**
 * Module HUD
 *
 * The client in-game dashboard.
 */
object ModuleHud : ClientModule("HUD", Category.RENDER, state = true, hide = true) {

    override val running
        get() = this.enabled && !isDestructed

    private val visible: Boolean
        get() = !isHidingNow && inGame

    override val baseKey: String
        get() = "liquidbounce.module.hud"

    private var browserBrowser: Browser? = null

    private val blur by boolean("Blur", true)

    @Suppress("unused")
    private val vignette by boolean("Vignette", false).onChanged {
        EventManager.callEvent(HudValueChangeEvent(this))
    }
    @Suppress("unused")
    private val refresh by boolean("Refresh", true).onChanged {
        EventManager.callEvent(HudValueChangeEvent(this))
    }
    val centeredCrosshair by boolean("CenteredCrosshair", false)

    @Suppress("unused")
    private val spaceSeperatedNames by boolean("SpaceSeperatedNames", true).onChange { state ->
        EventManager.callEvent(SpaceSeperatedNamesChangeEvent(state))
        state
    }

    val PrimaryColor by color("Primary", Color4b(40,100,180, 255))
        .onChanged {
            EventManager.callEvent(HudValueChangeEvent(this))
        }

    val SecondaryColor by color("Secondary", Color4b(200,140,255, 255))
        .onChanged {
            EventManager.callEvent(HudValueChangeEvent(this))
        }
    @Suppress("unused")
    private val hudZoom by float("ScaleFactor", 1f, 0.5f..2f).onChanged {
        EventManager.callEvent(HudValueChangeEvent(this))
    }
    val clientName by text("ClientName", "")
        .apply(::tagBy)
        .onChanged {
            EventManager.callEvent(HudValueChangeEvent(this))
        }
    @Suppress("unused")
    private val IP by text("ScoreboardIP","")   .onChanged {
        EventManager.callEvent(HudValueChangeEvent(this))
    }
    val isBlurEffectActive
        get() = blur && !(mc.options.hudHidden && mc.currentScreen == null)
    var browserSettings: BrowserSettings? = null


    @Suppress("unused")
    private val render = multiEnumChoice(
        "ArraylistPrefixRender",
        DoPrefix.CHOICE,
        DoPrefix.CHOOSE,
    ).onChanged {
        EventManager.callEvent(HudValueChangeEvent(this))
    }

    @Suppress("Unused")
    enum class DoPrefix(override val choiceName: String) : NamedChoice {
        CHOICE("CHOICE"),
        CHOOSE("CHOOSE"),
        MULTI_CHOOSE("MULTI_CHOOSE"),
        INT_RANGE("INT_RANGE"),
        FLOAT_RANGE("FLOAT_RANGE"),
        INT("INT"),
        FLOAT("FLOAT"),
        TEXT("TEXT"),
    }

    init {
        tree(Configurable("In-built", value = components as MutableList<Value<*>>))
        tree(Configurable("Custom", value = customComponents as MutableList<Value<*>>))
    }

    override fun enable() {


        if (isHidingNow) {
            chat(markAsError(message("hidingAppearance")))
        }


        RenderedEntities.subscribe(this)
        ChunkScanner.subscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        if (visible) {
            open()
        }
    }

    override fun disable() {
        // Closes tab entirely
        browserBrowser?.close()
        browserBrowser = null
        // Minimap
        RenderedEntities.unsubscribe(this)
        ChunkScanner.unsubscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        ChunkRenderer.unloadEverything()
    }

    @Suppress("unused")
    private val browserReadyHandler = handler<BrowserReadyEvent> { event ->
        tree(GlobalBrowserSettings)
        browserSettings = tree(BrowserSettings(60, ::reopen))
    }
    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        // Close the tab when the HUD is not running, is hiding now, or the player is not in-game
        if (!enabled || !visible) {
            close()
            return@handler
        }
        // Otherwise, open the tab and set its visibility
        val browserTab = open()
        browserTab.visible = event.screen !is DisconnectedScreen && event.screen !is ConnectScreen
    }

    @Suppress("unused")
    private val virtualScreenHandler = handler<VirtualTypeEvent> { event ->
        if (event.virtualScreenType == VirtualScreenType.LAYOUT_EDITOR) {
            close()
        }
    }


    private fun open(): Browser {
        if (browserBrowser != null) {
            return browserBrowser!!
        }

        return ThemeManager.openImmediate(
            VirtualScreenType.HUD,
            true,
            browserSettings!!
        ).also { browser ->
            browserBrowser = browser
        }
    }


    private fun close() {

        browserBrowser?.close()
        browserBrowser = null
    }

    fun reopen() {
        close()
        if (enabled && visible) {
            open()
        }
    }

}
