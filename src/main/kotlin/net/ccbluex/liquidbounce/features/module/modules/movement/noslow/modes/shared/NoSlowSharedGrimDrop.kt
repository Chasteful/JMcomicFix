package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplier
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.util.Hand
import net.minecraft.item.consume.UseAction

internal class NoSlowSharedGrimDrop (override val parent: ChoiceConfigurable<*>) : Choice("GrimDrop") {

    private var dropped = false

    @Suppress("unused")
    private val playerUseMultiplierHandler = handler<PlayerUseMultiplier> { event ->
        if (player.activeItem.useAction != UseAction.EAT || player.itemUseTimeLeft <= 0) {
            dropped = false
            return@handler
        }

        if (!dropped && player.moving) {
            if ((if (player.activeHand == Hand.MAIN_HAND) {player.mainHandStack}
                else {player.offHandStack}).count > 1) {

                player.dropSelectedItem(false)
                dropped = true
            }
        } else {
            player.isSprinting = true
            event.forward = 1f
            event.sideways = 1f
        }
    }

    override fun OnEnabled() {
        dropped = false
    }
}
