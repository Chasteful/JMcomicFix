package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.isInventoryOpen
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.isInContainerScreen
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.getRandomInRange
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
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
import kotlin.math.*

object ModulePotionAura : ClientModule("PotionAura", Category.PLAYER, aliases = arrayOf("AutoDebuff")) {
    private val delay by int("Delay", 20, 0..400, "ticks")

    private val range = floatRange("Range", 5f..16f, 2f..20f)
    private val throwRange by floatRange("ThrowRange", 5f..12f, 5f..20f)
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    object AllowMoveBoost : ToggleableConfigurable(this, "AllowMoveBoost", false) {
        val jump by boolean("Jump", true)
        val move by boolean("Move", true)
    }

    init {
        tree(AllowMoveBoost)
    }

    private val required by multiEnumChoice("Required", Required.NO_MOVEMENT)
    private val targetTracker = tree(TargetTracker(range = range))
    private val rotations = tree(RotationsConfigurable(this))
    private val targetRenderer = tree(WorldTargetRenderer(this))
    private var currentPlan: BlockChangeIntent? = null
    private var timeout = false

    private val debuffEffects = arrayOf(
        StatusEffects.SLOWNESS,
        StatusEffects.INSTANT_DAMAGE,
        StatusEffects.WEAKNESS,
        StatusEffects.POISON
    )

    override val running: Boolean
        get() = super.running &&
                !ModuleScaffold.running &&
                (Required.NO_MOVEMENT !in required || (player.input.movementForward == 0.0f && player.input.movementSideways == 0.0f)) &&
                (Required.NO_FALLING !in required || player.fallDistance <= 1.5f)

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (timeout || (isInventoryOpen || isInContainerScreen) && !ignoreOpenInventory) {
            targetTracker.reset()
            currentPlan = null
            return@handler
        }

        val enemies = targetTracker.targets().filter { it is PlayerEntity }
        if (enemies.isEmpty()) {
            currentPlan = null
            targetTracker.reset()
            return@handler
        }

        currentPlan = plan(enemies)
        currentPlan?.let { plan ->
            RotationManager.setRotationTarget(
                plan.rotation,
                false,
                rotations,
                Priority.NORMAL,
                this
            )
            targetTracker.target = plan.target
        }
    }
    @Suppress("unused")
    private val moveInputHandler = handler<MovementInputEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (!AllowMoveBoost.enabled) return@handler
        val plan = currentPlan ?: return@handler

        if (AllowMoveBoost.jump && player.isOnGround) {
            player.jump()
            if (AllowMoveBoost.move) event.directionalInput = event.directionalInput.copy(forwards = true)
        }

        val jumpStartY = plan.target.pos?.y?.let { it - 0.01 } ?: player.pos.y
        val maxY = jumpStartY + 1.25
        if (player.pos.y >= maxY) {
            event.directionalInput = event.directionalInput.copy(forwards = true)
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.isDead || player.isSpectator || CombatManager.shouldPauseCombat) {
            currentPlan = null
            return@tickHandler
        }

        val plan = currentPlan ?: return@tickHandler
        if (!(plan.slot.itemStack.item is SplashPotionItem &&
                    plan.slot.itemStack.getPotionEffects().any { it.effectType in debuffEffects })) {
            currentPlan = null
            return@tickHandler
        }

        val target = plan.target
        if (!validateThrow(target, null)) {
            currentPlan = null
            return@tickHandler
        }

        CombatManager.pauseCombatForAtLeast(1)
        SilentHotbar.selectSlotSilently(this, plan.slot, 1)
        useHotbarSlotOrOffhand(plan.slot)

        timeout = true
        targetTracker.target = plan.target
        waitTicks(delay)
        timeout = false
        currentPlan = null
    }
    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val target = targetTracker.target?.takeIf { targetRenderer.enabled } ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, event.partialTicks)
        }
    }
    private fun plan(enemies: List<LivingEntity>): BlockChangeIntent? {
        val slot = findDebuffPotion() ?: return null
        val maximumRange = throwRange.endInclusive
        val squaredMaxRange = maximumRange * maximumRange
        val squaredNormalRange = throwRange.endInclusive * throwRange.endInclusive

        val sortedTargets = enemies
            .filter {
                !hasDebuff(it) && it.pos != null && it.shouldBeAttacked() &&
                        it.squaredBoxedDistanceTo(player) <= squaredMaxRange
            }
            .sortedBy {
                if (it.squaredBoxedDistanceTo(player) <= squaredNormalRange) 0 else 1
            }

        for (target in sortedTargets) {
            val (rotation, landingPos) = findOptimalThrowRotation(target.pos!!, slot.itemStack) ?: continue
            if (!validateThrow(target, landingPos)) continue

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

    private fun validateThrow(target: LivingEntity, landingPos: Vec3d?): Boolean {
        if (landingPos == null) return false
        val isInInventoryScreen = isInventoryOpen || isInContainerScreen
        return !isInInventoryScreen && target.isAlive && !hasDebuff(target)
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
        val velocity = 0.5
        var motion = Vec3d(
            -sin(yawRad) * cos(pitchRad) * velocity,
            -sin(pitchRad) * velocity,
            cos(yawRad) * cos(pitchRad) * velocity
        ).add(player.velocity)

        val potionEntity = PotionEntity(mc.world!!, player, potionStack)
        var pos = player.pos.add(0.0, player.getEyeHeight(EntityPose.STANDING) - 0.2, 0.0)

        repeat(100) {
            val newPos = pos.add(motion)
            val drag = if (!world.getBlockState(newPos.toBlockPos()).fluidState.isEmpty) {
                0.96
            } else {
                0.96
            }
            motion = motion.multiply(drag, drag, drag).add(0.0, -0.05, 0.0)

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
        targetTracker.reset()
        targetRenderer.reset()
    }
    private enum class Required(
        override val choiceName: String
    ) : NamedChoice {
        NO_MOVEMENT("NoMovement"),
        NO_FALLING("NoFalling"),
    }
}
