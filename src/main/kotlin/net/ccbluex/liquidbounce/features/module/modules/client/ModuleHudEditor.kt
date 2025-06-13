package net.ccbluex.liquidbounce.features.module.modules.client


import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.integration.VirtualDisplayScreen
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.browser.supports.tab.ITab
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.glfw.GLFW

object ModuleHudEditor :
    ClientModule("HudLayoutEditor", Category.CLIENT, bind = GLFW.GLFW_KEY_RIGHT_ALT, disableActivation = true) {

    override val running = true

    @Suppress("UnusedPrivateProperty")
    private val StartEditor by boolean("StartEditor", false).onChanged { cache ->
        RenderSystem.recordRenderCall {
            if (cache) {
                createView()
            } else {
                closeView()
            }

            if (mc.currentScreen is VirtualDisplayScreen || mc.currentScreen is ClickScreen) {
               enable()
            }
        }
    }



    object Snapping : ToggleableConfigurable(this, "Snapping", true) {

        @Suppress("UnusedPrivateProperty")
        private val gridSize by int("GridSize", 10, 1..100, "px").onChanged {
            EventManager.callEvent(ClickGuiValueChangeEvent(ModuleHudEditor))
        }

        init {
            inner.find { it.name == "Enabled" }?.onChanged {
                EventManager.callEvent(ClickGuiValueChangeEvent(ModuleHudEditor))
            }
        }
    }

    private var clickGuiTab: ITab? = null
    private const val WORLD_CHANGE_SECONDS_UNTIL_RELOAD = 5

    init {
        tree(Snapping)
    }

    override fun enable() {
        // Pretty sure we are not in a game, so we can't open the clickgui
        if (!inGame) {
            return
        }

        mc.setScreen(
            if (clickGuiTab == null) {
                VirtualDisplayScreen(VirtualScreenType.LAYOUT_EDITOR)
            } else {
                ClickScreen()
            }
        )
        super.enable()
    }

    /**
     * Creates the ClickGUI view
     */
    private fun createView() {
        if (clickGuiTab != null) {
            return
        }

        clickGuiTab = ThemeManager.openInputAwareImmediate(VirtualScreenType.LAYOUT_EDITOR, true) {
            mc.currentScreen is ClickScreen
        }.preferOnTop()
    }
    private fun closeView() {
        clickGuiTab?.closeTab()
        clickGuiTab = null
    }
    fun reloadView() {
        clickGuiTab?.reload()
    }

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING,
    ) {
        // A hack to prevent the clickgui from being drawn
        if (mc.currentScreen !is ClickScreen) {
            clickGuiTab?.drawn = true
        }
    }

    @Suppress("unused")
    private val browserReadyHandler = handler<BrowserReadyEvent> {
        createView()
    }

    @Suppress("unused")
    private val worldChangeHandler = sequenceHandler<WorldChangeEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING,
    ) { event ->
        if (event.world == null) {
            return@sequenceHandler
        }

        waitSeconds(WORLD_CHANGE_SECONDS_UNTIL_RELOAD)
        if (mc.currentScreen !is ClickScreen) {
            reloadView()
        }
    }

    @Suppress("unused")
    private val clientLanguageChangedHandler = handler<ClientLanguageChangedEvent> {
        if (mc.currentScreen !is ClickScreen) {
            reloadView()
        }
    }

    /**
     * An empty screen that acts as hint when to draw the clickgui
     */
    class ClickScreen : Screen("HUDEditor".asText()) {

        override fun close() {
            mc.mouse.lockCursor()
            super.close()
        }

        override fun shouldPause(): Boolean {
            // preventing game pause
            return false
        }
    }

}
