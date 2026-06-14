@file:Suppress("UNCHECKED_CAST")
package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.EnderDragonEntityRendererAccessor
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.LivingEntityRendererAccessor
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.`object`.equipment.ElytraModel
import net.minecraft.client.model.player.PlayerCapeModel
import net.minecraft.client.renderer.entity.EnderDragonRenderer
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.state.*
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB

object WireframeRenderer {

    private fun drawWireframe(event:WorldRenderEvent,part: ModelPart?, localPoseStack: PoseStack, color: Color4b, outlineColor: Color4b) {
        renderEnvironmentForWorld(event.matrixStack) {
            part?.visit(localPoseStack) { _, _, _, cuboid ->
                drawBox(
                    AABB(
                        cuboid.minX / 16.0, cuboid.minY / 16.0, cuboid.minZ / 16.0,
                        cuboid.maxX / 16.0, cuboid.maxY / 16.0, cuboid.maxZ / 16.0
                    ),
                    color, outlineColor
                )
            }
        }
    }

    fun renderSnapshot(
        event: WorldRenderEvent,
        env: WorldRenderEnvironment,
        snapshot: WireframeSnapshot,
        color: Color4b,
        outlineColor: Color4b
    ) {
        val entity = snapshot.entity
        val renderer = mc.entityRenderDispatcher.getRenderer(entity) as EntityRenderer<Entity, EntityRenderState>

        val model: EntityModel<EntityRenderState> = when (renderer) {
            is EnderDragonRenderer -> (renderer as EnderDragonEntityRendererAccessor).model as? EntityModel<EntityRenderState>
            else -> (renderer as? RenderLayerParent<*, *>)?.model as? EntityModel<EntityRenderState>
        } ?: return

        env.withPositionRelativeToCamera(snapshot.pos) {
            poseStack.withPush {

                val renderState = snapshot.cachedRenderState

                if (entity is EnderDragon) {
                    val enderState = renderState as EnderDragonRenderState
                    val f = enderState.getHistoricalPos(7).yRot
                    val g = (enderState.getHistoricalPos(5).y - enderState.getHistoricalPos(10).y).toFloat()
                    poseStack.mulPose(Axis.YP.rotationDegrees(-f))
                    poseStack.mulPose(Axis.XP.rotationDegrees(g * 10.0F))
                    poseStack.translate(0.0F, 0.0F, 1.0F)
                } else {
                    val livingState = renderState as? LivingEntityRenderState
                    livingState?.let {
                        poseStack.scale(it.scale, it.scale, it.scale)
                    }

                    val livingRendererInstance = renderer as? LivingEntityRenderer<LivingEntity, LivingEntityRenderState, *>
                    livingRendererInstance?.let {
                        livingState?.let { _ ->
                            (renderer as LivingEntityRendererAccessor<LivingEntity, LivingEntityRenderState>)
                                .invokeSetupRotations(livingState, poseStack, snapshot.bodyYaw, livingState.scale)
                        }
                    }
                }
                when (renderState) {
                    is SlimeRenderState -> {
                        val size = renderState.size
                        val squish = renderState.squish
                        val inv = 1.0f / (squish + 1.0f)
                        poseStack.scale(size * inv, size / inv, size * inv)
                    }
                    is WitherRenderState -> {
                        var f = 2.0F
                        if (renderState.invulnerableTicks > 0.0F) {
                            f -= renderState.invulnerableTicks / 220.0F * 0.5F
                        }
                        poseStack.scale(f, f, f)
                    }
                    else -> {
                        val defaultScale = if (entity is Player) 0.9375f else 1.0f
                        poseStack.scale(defaultScale, defaultScale, defaultScale)
                    }
                }

                poseStack.scale(-1f, -1f, 1f)
                poseStack.translate(0.0, -1.501, 0.0)

                model.setupAnim(renderState)
                drawWireframe(event, model.root(), poseStack, color, outlineColor)

                if (entity is Player && renderState is HumanoidRenderState) {
                    val entityModelSet = mc.entityModels
                    val entry = network.getPlayerInfo(entity.uuid)
                    val skin = entry?.skin
                    if (skin?.cape != null) {
                        val capePart = entityModelSet.bakeLayer(ModelLayers.PLAYER_CAPE)
                        val capeModel = PlayerCapeModel(capePart)
                        val avatarState = renderState as? AvatarRenderState
                        avatarState?.let {
                            capeModel.setupAnim(it)
                        }
                        poseStack.withPush {
                            drawWireframe(event,capePart, poseStack, color, outlineColor)
                        }
                    }

                    val elytraStack = entity.getItemBySlot(EquipmentSlot.CHEST)
                    val hasElytra = elytraStack.`is`(Items.ELYTRA) && elytraStack.has(DataComponents.EQUIPPABLE)
                    if (hasElytra) {
                        val elytraPart = entityModelSet.bakeLayer(ModelLayers.ELYTRA)
                        val elytraModel = ElytraModel(elytraPart)
                        elytraModel.setupAnim(renderState)
                        poseStack.withPush {
                            drawWireframe(event,elytraPart, poseStack, color, outlineColor)
                        }
                    }
                }
            }
        }
    }
}
