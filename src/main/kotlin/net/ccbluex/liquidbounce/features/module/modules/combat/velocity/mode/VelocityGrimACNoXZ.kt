package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode


import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.util.Hand

    internal object VelocityGrimACNoXZ : VelocityMode("Heypixel") {
    private val attackTimes by int("AttackTimes", 4, 0..20, "times")
    private val chance by int("Chance", 50, 0..100, "%")

    private var velocityInput = false
    private var attacked = false
    private var target: Entity? = null

    @Suppress("unused","ComplexCondition")
    private val tickHandler = tickHandler {
        if (player.hurtTime == 0) {
            velocityInput = false
            attacked = false
        }

        if (velocityInput && attacked) {
            if (target != null
                && player.isAlive
                && !player.isSpectator
                && !player.abilities.flying
                && !player.isInFluid
                && !player.isClimbing
                && !player.isOnFire
                && !player.usingItem
            ) {
                player.setVelocity(
                    player.velocity.x * 0.07776,
                    player.velocity.y,
                    player.velocity.z * 0.07776
                )
            }
            attacked = false
        }
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        target = ModuleKillAura.targetTracker.target ?: return@handler

        if (event.packet is EntityVelocityUpdateS2CPacket
            && packet.entityId == player.id && (1..100).random() <= chance) {
            velocityInput = true

            val sprinting = player.isSprinting

            if (!sprinting) {
                network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(player.isOnGround, player.horizontalCollision))
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING))
            }

            repeat(attackTimes) {
                network.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking))
                network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
            }

            attacked = true

            if (!sprinting) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
            }
        }
    }

    override fun enable() {
        velocityInput = false
        attacked = false
        target = null
    }
}
