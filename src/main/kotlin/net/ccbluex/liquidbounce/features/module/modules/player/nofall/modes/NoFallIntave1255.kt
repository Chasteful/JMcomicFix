package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.doesNotCollideBelow
import net.ccbluex.liquidbounce.utils.kotlin.Priority

internal object NoFallIntave1255 : Choice("Intave12") {
    private val minFallDist by float("MinFallDistance", 1.5f, 0f..1.5f)

    private var shouldSlowTimer = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    val tickHandler = tickHandler {
        if (shouldSlowTimer) {
            Timer.requestTimerSpeed(1f, Priority.NORMAL, ModuleNoFall)
            shouldSlowTimer = false
        }
    }

    @Suppress("unused")
    val motionHandler = handler<PlayerTickEvent> {

        if (player.fallDistance > minFallDist && hasCollisionBelow()) {

            Timer.requestTimerSpeed(0.4f, Priority.IMPORTANT_FOR_PLAYER_LIFE, ModuleNoFall)
            shouldSlowTimer = true

            network.sendPacket(MovePacketType.ON_GROUND_ONLY.generatePacket().apply {
                onGround = true
            })

            player.fallDistance = 0f
        }
    }

    private fun hasCollisionBelow(): Boolean {
        if (player.y < 0) return false
        val checkDepth = (player.y + 2).toInt()
        return (0..checkDepth step 2).any { offset ->
            !player.doesNotCollideBelow(until = -offset.toDouble())
        }
    }
}
