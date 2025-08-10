package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler

internal object VelocityPolar : VelocityMode("OldPolar") {

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> {
        player.velocity = player.velocity.multiply(
            0.45,
            1.0,
            0.45
        )
        player.isSprinting = false
    }
}
