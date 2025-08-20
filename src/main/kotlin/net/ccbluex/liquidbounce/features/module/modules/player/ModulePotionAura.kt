package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.getRandomInRange
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.entity.EntityPose
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.entity.projectile.thrown.PotionEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.SplashPotionItem
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.jvm.optionals.getOrElse
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

object ModulePotionAura : ClientModule("PotionAura", Category.PLAYER, aliases = arrayOf("AutoDebuff")) {
    private val delay by int("Delay", 10, 0..40, "ticks")
    private val throwRange = floatRange("ThrowRange", 5f..16f, 0f..20f)
    private val onlyPlayer by boolean("OnlyPlayer", true)
    private val boost by boolean("Boost", true)
    private val required by multiEnumChoice(
        "Required",
        Required.NO_MOVEMENT,
        Required.NO_FALLING

    )
    override val running: Boolean
        get() =
            super.running
                && !ModuleScaffold.running
                && (Required.NO_MOVEMENT !in required
                || (player.input.movementForward == 0.0f && player.input.movementSideways == 0.0f))
                && (Required.NO_FALLING !in required || player.fallDistance <= 1.5f)

    private val targetTracker = tree(TargetTracker(range = throwRange))
    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private var currentPlan: BlockChangeIntent? = null
    private var delayCounter = 0
    private var timeout = false

    private val debuffEffects = arrayOf(
        StatusEffects.SLOWNESS,
        StatusEffects.INSTANT_DAMAGE,
        StatusEffects.WEAKNESS,
        StatusEffects.POISON
    )

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (timeout || delayCounter > 0) return@handler

        val enemies = targetTracker.targets().filter { !onlyPlayer || it is PlayerEntity }
        if (enemies.isEmpty()) {
            currentPlan = null
            return@handler
        }

        currentPlan = plan(enemies)
        currentPlan?.let { plan ->
            RotationManager.setRotationTarget(
                plan.rotation,
                false,
                rotationsConfigurable,
                Priority.NORMAL,
                this
            )
            targetTracker.target = plan.target
        }
    }
    @Suppress("unused")
    private val moveInputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (!boost){
            return@handler
        }
        val plan = currentPlan ?: return@handler

        if (player.isOnGround) {
            player.jump()
           event.directionalInput = event.directionalInput.copy(forwards = true)
        }

        val jumpStartY = plan.target.pos?.y?.let { it - 0.01 } ?: player.pos.y
        val maxY = jumpStartY + 1.25
        if (player.pos.y >= maxY) {

            event.directionalInput = event.directionalInput.copy(forwards = true)
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (delayCounter > 0) {
            delayCounter--
            return@tickHandler
        }

        val plan = currentPlan ?: return@tickHandler
        if (!(plan.slot.itemStack.item is SplashPotionItem &&
                    plan.slot.itemStack.getPotionEffects().any { it.effectType in debuffEffects })) {
            currentPlan = null
            return@tickHandler
        }

        val target = plan.target
        if (!target.isAlive || hasDebuff(target) || target.pos == null) {
            currentPlan = null
            return@tickHandler
        }

        CombatManager.pauseCombatForAtLeast(1)
        SilentHotbar.selectSlotSilently(this, plan.slot, 1)
        useHotbarSlotOrOffhand(plan.slot)

        delayCounter = delay
        targetTracker.target = plan.target
        currentPlan = null
    }


    private fun plan(enemies: List<LivingEntity>): BlockChangeIntent? {
        val slot = findDebuffPotion() ?: return null
        val sortedTargets = enemies
            .filter { !hasDebuff(it) && it.pos != null }
            .sortedBy { it.pos!!.distanceTo(player.pos) }
            .take(2)

        for (target in sortedTargets) {
            if (hasDebuff(target) || target.pos == null) continue

            val targetPos = target.pos ?: continue
            val (rotation, _) = findOptimalThrowRotation(targetPos, slot.itemStack) ?: continue

            return BlockChangeIntent(
                BlockChangeInfo.UseItem(slot.useHand),
                slot,
                target,
                rotation
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

    @Suppress("NestedBlockDepth")
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
                (targetYaw + getRandomInRange(-temperature * 10f, temperature * 10f)).coerceIn(-180f, 180f),
               (currentRotation.pitch + getRandomInRange(-temperature * 5f, temperature * 5f)).coerceIn(-90f, 90f)
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
        val yawRadians = rotation.yaw.toRadians()
        val pitchRadians = (rotation.pitch - 20.0F).toRadians()
        val initialVelocity = 0.5
        val gravity = 0.05
        val drag = 0.99
        val dragInWater = 0.6
        val hitboxRadius = 0.25

        var motion = Vec3d(
            -sin(yawRadians) * cos(pitchRadians) * initialVelocity,
            -sin(pitchRadians) * initialVelocity,
            cos(yawRadians) * cos(pitchRadians) * initialVelocity
        ).add(player.velocity)

        val potionEntity = PotionEntity(mc.world!!, player, potionStack)
        var pos = player.pos.add(0.0, player.getEyeHeight(EntityPose.STANDING) - 0.1, 0.0)

        val hitbox = Box.of(
            Vec3d.ZERO,
            hitboxRadius * 2.0,
            hitboxRadius * 2.0,
            hitboxRadius * 2.0
        )

        repeat(233) {
            val prevPos = pos
            pos = pos.add(motion)

            val blockHitResult = mc.world!!.raycast(
                RaycastContext(
                    prevPos,
                    pos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    potionEntity
                )
            )
            if (blockHitResult != null && blockHitResult.type != HitResult.Type.MISS) {
                return blockHitResult.pos
            }

            val entityHitResult = ProjectileUtil.getEntityCollision(
                mc.world!!,
                player,
                prevPos,
                pos,
                hitbox.offset(pos).stretch(motion).expand(1.0)
            ) { entity ->
                entity.isAlive && !entity.isSpectator && entity.canHit() && !player.isConnectedThroughVehicle(entity)
            }
            if (entityHitResult != null && entityHitResult.type != HitResult.Type.MISS) {
                val hitPos = entityHitResult.entity.box.expand(hitboxRadius).raycast(prevPos, pos)
                return hitPos.getOrElse { pos }
            }

            val blockState = world.getBlockState(pos.toBlockPos())
            val currentDrag = if (!blockState.fluidState.isEmpty) dragInWater else drag
            motion = motion.multiply(currentDrag, currentDrag, currentDrag).add(0.0, -gravity, 0.0)

            if (motion.lengthSquared() < 0.01 || pos.y < world.bottomY) {
                return null
            }
        }
        return pos
    }


    private fun evaluateLandingPosition(landingPos: Vec3d, targetPos: Vec3d, potionStack: ItemStack): Double {
        if (landingPos.squaredDistanceTo(player.pos.add(player.velocity.multiply(0.5))) <= 4.1 * 4.1){
            return Double.MAX_VALUE
        }

        val targetDistance = landingPos.distanceTo(targetPos)
        if (targetDistance > 4.0) return Double.MAX_VALUE

        val box = Box.of(landingPos, 0.5, 0.5, 0.5).expand(4.0, 2.0, 4.0)
        val entities = world.getNonSpectatingEntities(LivingEntity::class.java, box)
            .filter { it.isAlive && it.isAffectedBySplashPotions && it.squaredDistanceTo(landingPos) <= 16.0 }

        if (entities.any { entity ->
                potionStack.getPotionEffects().any { effect ->
                    entity.statusEffects.any { it.effectType == effect.effectType && it.amplifier >= effect.amplifier }
                }
            }){
            return Double.MAX_VALUE
        }

        val effectStrength = potionStack.getPotionEffects().sumOf { (it.amplifier + 1.0) * it.duration / 20.0 }
        return targetDistance * targetDistance / (effectStrength + 1.0)
    }

    private data class BlockChangeIntent(
        val blockChangeInfo: BlockChangeInfo,
        val slot: HotbarItemSlot,
        val target: LivingEntity,
        val rotation: Rotation
    )

    private sealed class BlockChangeInfo {
        data class UseItem(val hand: Hand) : BlockChangeInfo()
    }

    override fun onEnabled() {
        timeout = false
        currentPlan = null
    }

    override fun onDisabled() {
        timeout = false
        currentPlan = null
    }
    private enum class Required(
        override val choiceName: String
    ) : NamedChoice {
        NO_MOVEMENT("NoMovement"),
        NO_FALLING("NoFalling"),
    }
}

