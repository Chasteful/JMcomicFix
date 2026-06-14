package net.ccbluex.liquidbounce.utils.render

import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

data class WireframeSnapshot(
    val pos: Vec3,
    val creationTime: Long,
    val bodyYaw: Float,
    val yaw: Float,
    val pitch: Float,
    val entity: LivingEntity,
    val cachedRenderState: EntityRenderState
)
