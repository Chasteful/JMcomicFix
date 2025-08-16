/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityPose
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.jvm.optionals.getOrNull
import kotlin.math.sqrt

@Suppress("LongParameterList")
class FallingPlayer(
    private val player: ClientPlayerEntity,
    var x: Double,
    var y: Double,
    var z: Double,
    private var motionX: Double,
    private var motionY: Double,
    private var motionZ: Double,
    private val yaw: Float
) {
    companion object {
        fun fromPlayer(player: ClientPlayerEntity): FallingPlayer {
            return FallingPlayer(
                player,
                player.x,
                player.y,
                player.z,
                player.velocity.x,
                player.velocity.y,
                player.velocity.z,
                player.yaw
            )
        }
    }

    private var simulatedTicks: Int = 0

    private fun calculateForTick(rotationVec: Vec3d) {
        var d = 0.08
        val bl: Boolean = motionY <= 0.0

        if (bl && hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            d = 0.01
        }

        val j: Float = this.player.pitch * 0.017453292f

        val k = sqrt(rotationVec.x * rotationVec.x + rotationVec.z * rotationVec.z)
        val l = sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ)

        val m = rotationVec.length()
        var n = MathHelper.cos(j)

        n = (n.toDouble() * n.toDouble() * 1.0.coerceAtMost(m / 0.4)).toFloat()

        var vec3d5 = Vec3d(this.motionX, this.motionY, this.motionZ).add(0.0, d * (-1.0 + n.toDouble() * 0.75), 0.0)

        vec3d5 = applyElytraPhysics(vec3d5, rotationVec, j, l, k, d)

        vec3d5 = vec3d5.add(
            Entity.movementInputToVelocity(
                Vec3d(
                    this.player.input.movementSideways.toDouble() * 0.98,
                    0.0,
                    this.player.input.movementForward.toDouble() * 0.98
                ),
                0.02F,
                yaw
            )
        )

        val velocityCoFactor: Float = this.player.velocityMultiplier

        this.motionX = vec3d5.x * 0.9900000095367432 * velocityCoFactor
        this.motionY = vec3d5.y * 0.9800000190734863
        this.motionZ = vec3d5.z * 0.9900000095367432 * velocityCoFactor

        this.x += this.motionX
        this.y += this.motionY
        this.z += this.motionZ

        this.simulatedTicks++
    }
    private fun applyElytraPhysics(
        velocity: Vec3d,
        rotationVec: Vec3d,
        pitch: Float,
        horizontalSpeed: Double,
        rotationHorizontal: Double,
        gravity: Double): Vec3d {
        var result = velocity.add(0.0, gravity * (-1.0 + MathHelper.cos(pitch) * 0.75), 0.0)

        if (result.y < 0.0 && rotationHorizontal > 0.0) {
            val q = result.y * -0.1 * MathHelper.cos(pitch)
            result = result.add(rotationVec.x * q / rotationHorizontal, q, rotationVec.z * q / rotationHorizontal)
        }

        if (pitch < 0.0f && rotationHorizontal > 0.0) {
            val q = horizontalSpeed * (-MathHelper.sin(pitch)) * 0.04
            result = result.add(
                -rotationVec.x * q / rotationHorizontal,
                q * 3.2,
                -rotationVec.z * q / rotationHorizontal)
        }

        if (rotationHorizontal > 0.0) {
            result = result.add(
                (rotationVec.x / rotationHorizontal * horizontalSpeed - result.x) * 0.1,
                0.0,
                (rotationVec.z / rotationHorizontal * horizontalSpeed - result.z) * 0.1
            )
        }

        return result
    }

    private fun hasStatusEffect(effect: RegistryEntry<StatusEffect>): Boolean {
        val instance = player.getStatusEffect(effect) ?: return false

        return instance.duration >= this.simulatedTicks
    }

    fun findCollision(ticks: Int): CollisionResult? {
        val rotationVec = player.rotationVector

        for (i in 0 until ticks) {
            val start = Vec3d(x, y, z)

            calculateForTick(rotationVec)

            val end = Vec3d(x, y, z)

            val box = player.getDimensions(EntityPose.STANDING).getBoxAt(start).stretch(end.subtract(start))

            world.findSupportingBlockPos(player, box).getOrNull()?.let {
                return CollisionResult(it, i)
            }
        }
        return null
    }

    class CollisionResult(val pos: BlockPos?, val tick: Int)
}
