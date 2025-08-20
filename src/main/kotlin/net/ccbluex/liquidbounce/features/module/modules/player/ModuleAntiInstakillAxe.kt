package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.SpawnEggItem
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

object ModuleAntiInstakillAxe : ClientModule("AntiInstakillAxe", Category.PLAYER) {
    private val delay by int("Delay", 10, 0..40, "ticks")
    private val detectionRange by floatRange("DetectionRange", 0f..3f, 0.0f..6.0f, "blocks")

    private val rotationsConfigurable = tree(RotationsConfigurable(this))
    private val cooldownTimer = Chronometer()

    private var currentPlan: SpawnEggPlan? = null
    private var delayCounter = 0

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (delayCounter > 0 || !cooldownTimer.hasElapsed(1000L)) return@handler

        val player = mc.player ?: return@handler
        val world = mc.world ?: return@handler

        val enemies = world.players.filter {
            it != player && it.isAlive
                && isAimingAtPlayer(it)
                && player.pos.distanceTo(it.pos) <= detectionRange.endInclusive
                && hasInstakillAxe(it)
        }
        if (enemies.isEmpty()) {
            currentPlan = null
            return@handler
        }

        val target = enemies.minByOrNull { player.pos.distanceTo(it.pos) } ?: return@handler
        currentPlan = plan(target)
        currentPlan?.let { plan ->
            RotationManager.setRotationTarget(
                plan.rotation,
                false,
                rotationsConfigurable,
                Priority.NORMAL,
                this
            )
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (delayCounter > 0) {
            delayCounter--
            return@handler
        }

        val plan = currentPlan ?: return@handler
        if (!isValidSpawnEgg(plan.slot.itemStack)) {
            currentPlan = null
            return@handler
        }

        val target = plan.target
        if (!target.isAlive) {
            currentPlan = null
            return@handler
        }

        CombatManager.pauseCombatForAtLeast(1)
        useHotbarSlotOrOffhand(plan.slot, 0, plan.rotation.yaw, plan.rotation.pitch)
        delayCounter = delay
        currentPlan = null
        cooldownTimer.reset()
    }


    private fun findSpawnEggSlot(): HotbarItemSlot? {
        return Slots.OffhandWithHotbar.findClosestSlot { stack ->
            isValidSpawnEgg(stack)
        }
    }

    private fun isValidSpawnEgg(stack: ItemStack): Boolean {
        return stack.item is SpawnEggItem
    }

    private fun isAimingAtPlayer(enemy: PlayerEntity): Boolean {
        val player = mc.player ?: return false
        val eyePos = enemy.eyePos
        val direction = enemy.rotationVector.normalize()
        val raycastEnd = eyePos.add(direction.multiply(3.0))
        val box = player.box.expand(0.5)
        return box.raycast(eyePos, raycastEnd).isPresent
    }

    private fun hasInstakillAxe(player: PlayerEntity): Boolean {
        val mainHandStack = player.mainHandStack

        if (mainHandStack.item != Items.GOLDEN_AXE || mainHandStack.isEmpty) {
            return false
        }
        val sharpnessLevel = mainHandStack.getEnchantment(Enchantments.SHARPNESS)
        return sharpnessLevel > 10
    }

    private fun plan(target: PlayerEntity): SpawnEggPlan? {
        val slot = findSpawnEggSlot() ?: return null
        val targetPos = target.pos ?: return null
        val playerPos = mc.player?.pos ?: return null

        val midPoint = playerPos.add(targetPos.subtract(playerPos).multiply(0.5))
        val targetBlockPos = BlockPos.ofFloored(midPoint)

        val worldState = targetBlockPos.getState() ?: return null
        if (!worldState.isAir) return null

        val placementTarget = findBestBlockPlacementTarget(
            targetBlockPos,
            BlockPlacementTargetFindingOptions(
                BlockOffsetOptions(
                    listOf(Vec3i.ZERO),
                    BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE),
                FaceHandlingOptions(CenterTargetPositionFactory),
                stackToPlaceWith = slot.itemStack,
                PlayerLocationOnPlacement(position = playerPos)
            )
        ) ?: return null

        return SpawnEggPlan(
            slot = slot,
            target = target,
            rotation = placementTarget.rotation,
            targetPos = targetBlockPos
        )
    }

    override fun onEnabled() {
        delayCounter = 0
        currentPlan = null
        cooldownTimer.reset()
    }

    override fun onDisabled() {
        currentPlan = null
    }

    private data class SpawnEggPlan(
        val slot: HotbarItemSlot,
        val target: PlayerEntity,
        val rotation: Rotation,
        val targetPos: BlockPos
    )
}

