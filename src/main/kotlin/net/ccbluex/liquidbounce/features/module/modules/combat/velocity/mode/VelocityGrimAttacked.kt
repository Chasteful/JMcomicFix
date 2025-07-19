package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode


import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.script.bindings.api.ScriptInteractionUtil.attackEntity
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.util.Hand

internal object VelocityGrimAttacked : VelocityMode("GrimAttacked") {

    private object Settings : ToggleableConfigurable(ModuleVelocity, "Settings", true) {
        val onlySprint by boolean("OnlySprint", true)
        val onlyOnGround by boolean("OnlyOnGround", true)
        val attackCounts by int("AttackCounts", 5, 1..16)
        val keepSprint by boolean("KeepSprint", false)
    }

    private var velocityInput = false
    private var targetEntity: Entity? = null
    private var velocityX = 0.0
    private var velocityY = 0.0
    private var velocityZ = 0.0

    init {
        tree(Settings)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {

            velocityX = packet.velocityX / 8000.0
            velocityY = packet.velocityY / 8000.0
            velocityZ = packet.velocityZ / 8000.0


             if (velocityX == 0.0 && velocityZ == 0.0 && velocityY == -0.078375) {
                 return@handler
             }
            val rotation = RotationManager.currentRotation ?:player.rotation

            targetEntity = raytraceEntity(
                range = (ModuleKillAura.range).toDouble(),
                rotation = rotation,
                filter = { it.isAlive }
            )?.entity ?: ModuleKillAura.targetTracker.target

            if (!checkConditions()) return@handler

            velocityInput = true
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (velocityInput) {
            velocityInput = false

            val wasSprinting = player.isSprinting
            val wasOnGround = player.isOnGround

            sendSprintPackets(true)

            repeat(Settings.attackCounts) {
                targetEntity?.let { entity ->
                    attackEntity(
                        entity = entity,
                        true,
                         Settings.keepSprint
                    )
                }

                player.velocity = player.velocity.multiply(0.6, 1.0, 0.6)
            }

            sendSprintPackets(false)
            player.isSprinting = wasSprinting
            player.isOnGround = wasOnGround

            targetEntity = null
        }
    }

    private fun checkConditions(): Boolean {

        if (player.isRemoved) return false


        if (Settings.onlySprint && !player.isSprinting) return false
        if (Settings.onlyOnGround && !player.isOnGround) return false


        val entity = targetEntity ?: return false
        return !(!entity.isAlive || entity.isRemoved)
    }

    private fun sendSprintPackets(start: Boolean) {
        val packetType = if (start) {
            PlayerMoveC2SPacket.OnGroundOnly(true, false)
        }
        else {
            PlayerMoveC2SPacket.OnGroundOnly(false, false)
        }
        network.sendPacket(packetType)

        if (start) {
            network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
        }
    }
}
