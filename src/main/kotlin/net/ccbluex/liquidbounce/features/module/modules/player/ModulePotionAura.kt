package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.entity.EntityPose
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.thrown.PotionEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.SplashPotionItem
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

object ModulePotionAura : ClientModule("PotionAura", Category.PLAYER, aliases = arrayOf("AutoDebuff")) {

    private val range = floatRange("Range", 3.0f..4.5f, 2f..8f)
    private val delay by int("Delay", 20, 0..400, "ticks")
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val onlyPlayer by boolean("OnlyPlayer", true)
    private val targetTracker = tree(TargetTracker(range = range))
    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private var currentPlan: BlockChangeIntent<PotionIntentData>? = null
    private var timeout = false

    private val debuffEffects = arrayOf(
        StatusEffects.SLOWNESS,
        StatusEffects.MINING_FATIGUE,
        StatusEffects.INSTANT_DAMAGE,
        StatusEffects.NAUSEA,
        StatusEffects.BLINDNESS,
        StatusEffects.HUNGER,
        StatusEffects.WEAKNESS,
        StatusEffects.POISON,
        StatusEffects.WITHER,
        StatusEffects.GLOWING,
        StatusEffects.LEVITATION,
        StatusEffects.UNLUCK,
        StatusEffects.BAD_OMEN,
        StatusEffects.DARKNESS
    )

    override fun onEnabled() {
        timeout = false
    }

    override fun onDisabled() {
        timeout = false
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (timeout) {
            return@handler
        }

        val enemies = targetTracker.targets().filter {
            !onlyPlayer || it is PlayerEntity
        }

        currentPlan = plan(enemies)

        currentPlan?.let { intent ->
            when (val info = intent.blockChangeInfo) {
                is BlockChangeInfo.UseItem -> {
                    RotationManager.setRotationTarget(
                        intent.planningInfo.rotation,
                        considerInventory = !ignoreOpenInventory,
                        configurable = rotationsConfigurable,
                        Priority.IMPORTANT_FOR_PLAYER_LIFE,
                        this
                    )
                }
            }
        }
    }

    @Suppress("unused")
    private val placementHandler = tickHandler {
        val plan = currentPlan ?: return@tickHandler

        val raycast = raycast()
        if (!validate(plan)) {
            return@tickHandler
        }

        CombatManager.pauseCombatForAtLeast(1)
        SilentHotbar.selectSlotSilently(this, plan.slot, 1)

        when (plan.blockChangeInfo) {
            is BlockChangeInfo.UseItem -> useHotbarSlotOrOffhand(plan.slot)
        }

        timeout = true
        onIntentFullfilled(plan)
        waitTicks(delay)
        timeout = false
    }

    private fun plan(enemies: List<LivingEntity>): BlockChangeIntent<PotionIntentData>? {
        val slot = findDebuffPotion() ?: return null

        for (target in enemies) {
            if (hasDebuff(target)) continue

            val targetPos = target.pos ?: continue
            val (rotation, _) = findOptimalThrowRotation(targetPos, slot.itemStack) ?: continue

            RotationManager.setRotationTarget(
                rotation,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
                configurable = rotationsConfigurable,
                provider = this
            )

            targetTracker.target = target
            return BlockChangeIntent(
                BlockChangeInfo.UseItem(slot.useHand),
                slot,
                IntentTiming.NEXT_PROPITIOUS_MOMENT,
                PotionIntentData(target,rotation),
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

    private fun findOptimalThrowRotation(targetPos: Vec3d, potionStack: ItemStack): Pair<Rotation, Vec3d>? {
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

    private fun validate(plan: BlockChangeIntent<PotionIntentData>): Boolean {
        return plan.slot.itemStack.item is SplashPotionItem &&
            plan.slot.itemStack.getPotionEffects().any { it.effectType in debuffEffects }
    }

    private fun onIntentFullfilled(intent: BlockChangeIntent<PotionIntentData>) {
        targetTracker.target = intent.planningInfo.target
    }

    private data class BlockChangeIntent<T>(
        val blockChangeInfo: BlockChangeInfo,
        val slot: HotbarItemSlot,
        val intentTiming: IntentTiming,
        val planningInfo: T,
        val planner: ModulePotionAura
    )

    private sealed class BlockChangeInfo {
        data class UseItem(val hand: Hand) : BlockChangeInfo()
    }

    private enum class IntentTiming {
        NEXT_PROPITIOUS_MOMENT
    }

    private data class PotionIntentData(
        val target: LivingEntity,
        val rotation: Rotation
    )
}
