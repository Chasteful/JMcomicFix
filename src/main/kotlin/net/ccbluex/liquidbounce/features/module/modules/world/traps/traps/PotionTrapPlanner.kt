package net.ccbluex.liquidbounce.features.module.modules.world.traps.traps

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeInfo
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeIntent
import net.ccbluex.liquidbounce.features.module.modules.world.traps.IntentTiming
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap.targetTracker
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityPose
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.projectile.thrown.PotionEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.SplashPotionItem
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class PotionTrapPlanner(parent: EventListener) : TrapPlanner<PotionTrapPlanner.PotionIntentData>(
    parent,
    "Potion",
    true
) {

    private val debuffEffects = arrayOf(
        StatusEffects.POISON,
        StatusEffects.SLOWNESS,
        StatusEffects.WEAKNESS,
        StatusEffects.INSTANT_DAMAGE
    )

    override fun plan(enemies: List<LivingEntity>): BlockChangeIntent<PotionIntentData>? {
        val slot = findDebuffPotion() ?: return null

        for (target in enemies) {

            if (hasDebuff(target)) continue

            val targetPos = TrapPlayerSimulation.findPosForTrap(
                target, isTargetLocked = targetTracker.target == target
            ) ?: continue

            val (rotation, _) = findOptimalThrowRotation(targetPos, slot.itemStack) ?: continue

            RotationManager.setRotationTarget(
                rotation,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
                configurable = ModuleAutoTrap.rotationsConfigurable,
                provider = ModuleAutoTrap
            )

            targetTracker.target = target
            return BlockChangeIntent(
                BlockChangeInfo.UseItem(slot.useHand),
                slot,
                IntentTiming.NEXT_PROPITIOUS_MOMENT,
                PotionIntentData(target),
                this
            )
        }

        return null
    }

    private fun findDebuffPotion(): HotbarItemSlot? {
        return Slots.OffhandWithHotbar.findClosestSlot { stack ->
            stack.item is SplashPotionItem && stack.getPotionEffects().any { effect ->
                effect.effectType in debuffEffects
            }
        }
    }

    private fun hasDebuff(entity: LivingEntity): Boolean {
        return entity.statusEffects.any { it.effectType in debuffEffects }
    }

    private fun findOptimalThrowRotation(targetPos: Vec3d, potionStack: ItemStack): Pair<Rotation, Vec3d>?{
        var bestRotation: Rotation? = null
        var bestScore = Double.MAX_VALUE
        var currentRotation = Rotation(player.yaw, 0f)
        var currentScore = Double.MAX_VALUE
        var temperature = 50.0f
        var iterations = 0
        var iterationsWithoutImprovement = 0

        val targetYaw = Math.toDegrees(atan2(targetPos.z - player.pos.z, targetPos.x - player.pos.x)).toFloat() - 90f

        while (iterations < 2000 && temperature >= 0.01f) {
            val newRotation = Rotation(
                yaw = (targetYaw + getRandomInRange(-temperature * 10f, temperature * 10f)).coerceIn(-180f, 180f),
                pitch = (currentRotation.pitch + getRandomInRange(-temperature * 5f, temperature * 5f)).coerceIn(-90f, 90f)
            )

            val landingPos = simulatePotionTrajectory(newRotation, potionStack) ?: continue
            val score = evaluateLandingPosition(landingPos, targetPos, potionStack)

            val delta = score - currentScore
            if (delta < 0 || Math.random() < exp(-delta / temperature)) {
                currentRotation = newRotation
                currentScore = score
                if (score < bestScore) {
                    bestRotation = newRotation
                    bestScore = score
                    iterationsWithoutImprovement = 0
                } else {
                    iterationsWithoutImprovement++
                    if (iterationsWithoutImprovement > 1500) break
                }
            }

            temperature *= 0.99f
            iterations++
        }

        return bestRotation?.let { it to (simulatePotionTrajectory(it, potionStack) ?: return null) }
    }

    private fun simulatePotionTrajectory(rotation: Rotation, potionStack: ItemStack): Vec3d? {
        val yawRad = Math.toRadians(rotation.yaw.toDouble())
        val pitchRad = Math.toRadians(rotation.pitch.toDouble())
        val trajectoryInfo = TrajectoryInfo.POTION
        val velocity = trajectoryInfo.initialVelocity
        var motion = Vec3d(
            -sin(yawRad) * cos(pitchRad) * velocity,
            -sin(pitchRad) * velocity,
            cos(yawRad) * cos(pitchRad) * velocity
        )

        if (trajectoryInfo.copiesPlayerVelocity) {
            motion = motion.add(player.velocity)
        }

        val potionEntity = PotionEntity(mc.world!!, player, potionStack)
        var pos = player.pos.add(0.0, player.getEyeHeight(EntityPose.STANDING) - 0.2, 0.0)

        repeat(100) {
            val newPos = pos.add(motion)
            val drag = if (!world.getBlockState(newPos.toBlockPos()).fluidState.isEmpty) {
                trajectoryInfo.dragInWater
            } else {
                trajectoryInfo.drag
            }
            motion = motion.multiply(drag, drag, drag)
            motion = motion.add(0.0, -trajectoryInfo.gravity, 0.0)

            val blockHitResult = mc.world!!.raycast(
                RaycastContext(
                    pos, newPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    potionEntity
                )
            )

            if (blockHitResult != null && blockHitResult.type != HitResult.Type.MISS) {
                return blockHitResult.pos
            }

            pos = newPos
            if (motion.lengthSquared() < 0.01) return null
        }
        return pos
    }

    private fun evaluateLandingPosition(landingPos: Vec3d, targetPos: Vec3d, potionStack: ItemStack): Double {
        val playerDistance = landingPos.distanceTo(player.pos.add(player.velocity.multiply(0.5)))
        if (playerDistance < 6) return Double.MAX_VALUE

        val targetDistance = landingPos.distanceTo(targetPos)
        if (targetDistance > 4) return Double.MAX_VALUE

        val blockPos = landingPos.toBlockPos()
        val blockState = world.getBlockState(blockPos)
        if (blockState.isFullCube(world, blockPos) || !blockState.getCollisionShape(world, blockPos).isEmpty) {
            return Double.MAX_VALUE
        }

        val box = Box(landingPos.x, landingPos.y, landingPos.z, landingPos.x, landingPos.y, landingPos.z).expand(4.0)
        val entities = world.getEntitiesByClass(LivingEntity::class.java, box) { it.isAlive && it.isAffectedBySplashPotions }
        if (entities.any { entity ->
                potionStack.getPotionEffects().any { effect ->
                    entity.statusEffects.any { it.effectType == effect.effectType && it.amplifier >= effect.amplifier }
                }
            }) {
            return Double.MAX_VALUE
        }

        val effectStrength = potionStack.getPotionEffects().sumOf { (it.amplifier + 1.0) * it.duration / 20.0 }
        return targetDistance * targetDistance / (effectStrength + 1.0)
    }
    private fun getRandomInRange(min: Float, max: Float): Float {
        return (Math.random() * (max - min) + min).toFloat()
    }

    override fun validate(plan: BlockChangeIntent<PotionIntentData>, raycast: BlockHitResult): Boolean {
        return plan.slot.itemStack.item is SplashPotionItem &&
            plan.slot.itemStack.getPotionEffects().any { it.effectType in debuffEffects }
    }

    override fun onIntentFullfilled(intent: BlockChangeIntent<PotionIntentData>) {
        targetTracker.target = intent.planningInfo.target
    }

    class PotionIntentData(
        val target: LivingEntity
    )
}
