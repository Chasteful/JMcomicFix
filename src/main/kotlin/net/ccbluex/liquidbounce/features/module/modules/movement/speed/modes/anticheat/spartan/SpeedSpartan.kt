package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.anticheat.spartan

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.stopXZVelocity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

class SpeedSpartan(override val parent: ChoiceConfigurable<*>) : Choice("Spartan") {

    private val mode by enumChoice("Mode", SpartanMode.SPARTAN_4043)

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        when (mode) {
            SpartanMode.SPARTAN_4043 -> handleSpartan4043Move(event)
            SpartanMode.SPARTAN_4043_FASTFALL -> handleSpartan4043FastFallMove(event)
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (mode == SpartanMode.SPARTAN_4043_FASTFALL) {
            handleSpartan4043FastFallPacket(event)
        }
    }

    override fun disable() {
        if (mode == SpartanMode.SPARTAN_4043_FASTFALL) {
            player.stopXZVelocity()
        }
    }

    private fun handleSpartan4043Move(event: PlayerMoveEvent) {
        if (!player.input.playerInput.forward) {
            return
        }

        val wearingLeatherBoots = player.inventory.getArmorStack(0).item == Items.LEATHER_BOOTS
        val horizontalMove = if (wearingLeatherBoots) 1.8 else 1.3

        if (player.isOnGround) {
            event.movement.x = player.velocity.x * horizontalMove
            event.movement.z = player.velocity.z * horizontalMove

            repeat(4) {
                player.jump()
            }
            event.movement.y = player.jumpVelocity.toDouble()
        }
    }

    private fun handleSpartan4043FastFallMove(event: PlayerMoveEvent) {
        if (!player.input.playerInput.forward) {
            return
        }

        val wearingLeatherBoots = player.inventory.getArmorStack(0).item == Items.LEATHER_BOOTS
        val horizontalMove = if (wearingLeatherBoots) 1.2 else 1.05
        val jumps = if (wearingLeatherBoots) 7 else 3

        if (player.isOnGround) {
            event.movement.x = player.velocity.x * horizontalMove
            event.movement.z = player.velocity.z * horizontalMove

            repeat(jumps) {
                player.jump()
            }

            event.movement.y = 0.42
        } else if (player.airTicks == 1) {
            Timer.requestTimerSpeed(0.5f, Priority.NORMAL, ModuleSpeed, 0)
            event.movement.y = -0.0784
        }
    }

    private fun handleSpartan4043FastFallPacket(event: PacketEvent) {
        if (event.packet is PlayerMoveC2SPacket && player.airTicks == 1) {
            event.packet.onGround = true
        }
    }

    enum class SpartanMode(override val choiceName: String) : NamedChoice {
        SPARTAN_4043("Spartan4043"),
        SPARTAN_4043_FASTFALL("Spartan4043FastFall")
    }
}
