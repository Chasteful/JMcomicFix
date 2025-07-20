package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.ccbluex.liquidbounce.utils.entity.doesNotCollideBelow
import net.ccbluex.liquidbounce.utils.entity.withStrafe

internal object NoFallGrim2365 : Choice("Grim2365") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    @Suppress("unused")
    val motionHandler = handler<PlayerTickEvent> {
        if (player.fallDistance >= 3.0F) {
            // Reduce horizontal motion
            player.velocity = player.velocity.multiply(0.2, 1.0, 0.2)

            if (player.doesNotCollideBelow(until = -2.0)) {
                return@handler
            }

            if (player.fallDistance > 2.0f) {

                player.velocity.withStrafe(strength = 0.19)
            }

            if (player.fallDistance > 3.0f && player.speed < 0.2) {
                network.sendPacket(MovePacketType.ON_GROUND_ONLY.generatePacket().apply {
                    onGround = true
                })
                player.fallDistance = 0f
            }
        }
    }
}
