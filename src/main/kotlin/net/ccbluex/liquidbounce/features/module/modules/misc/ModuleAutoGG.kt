package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

object ModuleAutoGG : ClientModule("AutoGG", Category.MISC) {
    private val message by text ("Message","gg")
    private var hasTriggered = false
    private var checkDelay = 0

    val renderHandler = handler<WorldRenderEvent> { event ->

        if (mc.isPaused || mc.world == null) return@handler


        if (checkDelay > 0) {
            checkDelay--
            return@handler
        }


        val alivePlayers = mc.world?.players?.filter { player ->
            !player.isDead
                && player != mc.player
                && !player.isSpectator
        } ?: emptyList()


        if (alivePlayers.size == 1
            && alivePlayers[0] == mc.player
            && !hasTriggered
        ) {
            network.sendChatMessage(message)
            hasTriggered = true
            checkDelay = 20
        }
        else if (alivePlayers.size > 1) {
            hasTriggered = false
        }
    }
}
