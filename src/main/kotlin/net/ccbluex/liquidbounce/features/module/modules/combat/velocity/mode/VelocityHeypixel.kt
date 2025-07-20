package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.utils.entity.any
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

internal object VelocityHeypixel : VelocityMode("Heypixel") {

    private val chance by (float("Chance", 100f, 0f..100f, "%"))
    private val onlyMove by (boolean("OnlyMove", false))

    private var checkDelay = 0

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val pkt = event.packet) {
            is EntityVelocityUpdateS2CPacket -> handleVelocity(event, pkt)
            is ExplosionS2CPacket -> handleExplosion(event, pkt)
            else -> return@handler
        }
    }

    private fun handleVelocity(event: PacketEvent, pkt: EntityVelocityUpdateS2CPacket) {

        if (!shouldProcess(pkt.entityId)) return
        if (Random.nextInt(100) > chance) return

        val f1 = randomFactor1()
        val f2 = randomFactor2()
        val f3 = randomFactor3()
        val f4 = randomFactor4()


        if (abs(f1) < Float.MIN_VALUE && abs(f2) < Float.MIN_VALUE) {
            event.cancelEvent()
            NoFallBlink.waitUntilGround = true
            return
        }

        val vel = player.movement
        if (abs(f1) > Float.MIN_VALUE) {
            pkt.velocityX = (pkt.velocityX * f1).toInt()
            pkt.velocityZ = (pkt.velocityZ * f1).toInt()
        } else {
            pkt.velocityX = (vel.x * f3 * 8000).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            pkt.velocityZ = (vel.z * f3 * 8000).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        }
        if (abs(f2) > Float.MIN_VALUE) {
            pkt.velocityY = (pkt.velocityY * f2).toInt()
        } else {
            pkt.velocityY = (vel.y * f4 * 8000).toInt().let { if (it == 0) 1 else it }
        }

        NoFallBlink.waitUntilGround = true
    }

    @Suppress("unused")
    private fun handleExplosion(event: PacketEvent, pkt: ExplosionS2CPacket) {
        if (!shouldProcess(player.id)) return
        if (Random.nextInt(100) > chance) return

        pkt.playerKnockback.ifPresent { kb ->
            val f1 = randomFactor1()
            val f2 = randomFactor2()
            val fx = if (abs(f1) > Float.MIN_VALUE) f1 else randomFactor3()
            val fy = if (abs(f2) > Float.MIN_VALUE) f2 else randomFactor4()
            kb.x *= fx; kb.z *= fx; kb.y *= fy
        }

        NoFallBlink.waitUntilGround = true
    }

    private fun shouldProcess(targetId: Int): Boolean {
        if (targetId != player.id) return false
        if (!player.isOnGround) return false
        if (onlyMove && !player.input.playerInput.any) return false
        if (checkDelay > 0) {
            checkDelay--
            return false
        }
        return true
    }


    private fun randomFactor1(): Float {
        val f = sqrt(0f.pow(2) * if (Random.nextBoolean()) 1f else -1f)
        return if (f.isFinite()) f else 0f
    }

    private fun randomFactor2(): Float {
        val f = (0f * Random.nextFloat()).coerceIn(-1f, 1f)
        return if (f.isFinite()) f else 0f
    }

    private fun randomFactor3(): Float {
        val it = Random.nextFloat() - 0.5f
        return abs(it).takeIf { it.isFinite() } ?: 0f
    }

    private fun randomFactor4(): Float {
        val it = 0f * 1.337f / 0.1337f
        return if (it.isFinite()) it else 0f
    }
}
