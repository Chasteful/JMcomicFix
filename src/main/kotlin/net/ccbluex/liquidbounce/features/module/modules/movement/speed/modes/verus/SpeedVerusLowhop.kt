package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.verus

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.withStrafe
/**
 * @anticheat Verus
 * @testedOn mc.loyisa.cn
 * @note Proper VerusLowHop implementation matching original behavior
 */
class SpeedVerusLowHop(override val parent: ChoiceConfigurable<*>) : Choice("VerusLowHop") {
    private val strength by float("Strength", 0.33f, 0.1f..1f)

    private var wasOnGround = false
    override fun enable() {
        wasOnGround = false
        super.enable()
    }
    val moveHandler = handler<MovementInputEvent> { event ->
            if (event.directionalInput.isMoving) {
                if (player.isOnGround) {
                    player.jump()
                    wasOnGround = true

                } else if (!player.horizontalCollision) {
                    player.velocity.y = -0.0784000015258789
                }
                wasOnGround = false

                player.velocity = player.velocity.withStrafe(strength = strength.toDouble())
            } else {

                player.velocity.x = 0.0
                player.velocity.z = 0.0
        }
    }
}
