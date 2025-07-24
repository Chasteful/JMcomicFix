package net.ccbluex.liquidbounce.features.module.modules.combat.tpbow

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.*

@Suppress("TooManyFunctions")
object ModuleBowAura : ClientModule("TPBowAura", Category.COMBAT, disableOnQuit = true) {

    private val mode by enumChoice("Mode", Mode.VELOCITY_1_8, Mode.entries.toTypedArray())
    private val packetMode by enumChoice("PacketMode", PacketMode.C04, PacketMode.entries.toTypedArray())
    private val delay by intRange("Delay", 200..5000, 10..10000, "ms")
    private val maxTargets by int("MaxTargets", 1, 1..150)
    private val moveDistance by float("MoveDistance", 2f, 0.1f..20f, "m")
    private val antiLag by boolean("AntiLag", true)
    private val antiLagHighVersion by boolean("AntiLag-1.9+", true)
    private val throughWalls by boolean("IgnoreWalls", true)

    internal val targetTracker = tree(object : TargetTracker(
        TargetPriority.DISTANCE,
        floatRange("Range", 3.0f..6f, 1f..50f)
    ) {
        override fun validate(entity: LivingEntity): Boolean {
            return entity.isAlive &&
                entity != player &&
                (throughWalls || player.canSee(entity)) &&
                TargetManager.checkEntity(entity) &&
                super.validate(entity)
        }
    })

    object TargetManager {
        fun checkEntity(entity: Entity): Boolean {
            return entity is LivingEntity && entity.isAlive && entity != mc.player
        }
    }

    init {
        tree(targetTracker)
    }

    private val clickTimer = object {
        private var lastMS = System.currentTimeMillis()

        fun reset() {
            lastMS = System.currentTimeMillis()
        }

        fun hasTimePassed(time: Long): Boolean {
            return System.currentTimeMillis() - lastMS >= time
        }

    }
    private var currentPath: List<Vec3d> = emptyList()

    private enum class Mode(override val choiceName: String) : NamedChoice {
        VELOCITY_1_8("1.8"),
        VELOCITY_1_9("1.9")
    }

    private enum class PacketMode(override val choiceName: String) : NamedChoice {
        C04("C04"),
        C06("C06")
    }


    @Suppress("unused")
    private val repeatable = tickHandler {
        // Check for bow in hand
        val bowItem = player.inventory.main.firstOrNull { it?.item == Items.BOW } ?: return@tickHandler

        // Check delay
        if (!clickTimer.hasTimePassed(delay.random().toLong())) return@tickHandler

        // Get valid targets
        val targets = targetTracker.targets().take(maxTargets)
        if (targets.isEmpty()) {
            currentPath = emptyList()
            return@tickHandler
        }

        // Process each target
        for (target in targets) {
            // Calculate projectile trajectory
            val rotation = calculateBowAngles(target, target.x, target.z)


            when (mode) {
                Mode.VELOCITY_1_8 -> handleVelocity18(target)
                Mode.VELOCITY_1_9 -> handleVelocity19(target)
            }
        }

        clickTimer.reset()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (antiLag && packet is PlayerPositionLookS2CPacket) {
            event.cancelEvent()


            val playerPosition = packet.change

            val diffX = player.x - playerPosition.position.x
            val diffY = player.y - playerPosition.position.y
            val diffZ = player.z - playerPosition.position.z
            val distance = sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ)

            network.sendPacket(
                PlayerMoveC2SPacket.Full(
                    player.x, player.y, player.z,
                    player.yaw, player.pitch,
                    player.isOnGround,
                    false
                )
            )

            if (antiLagHighVersion) {

                repeat((distance / 10 + 1).roundToInt()) {
                    network.sendPacket(
                        PlayerMoveC2SPacket.Full(
                            player.x, player.y, player.z,
                            player.yaw, player.pitch,
                            player.isOnGround,
                            false
                        )
                    )
                }
            }
        }
    }
    private fun findPath(start: Vec3d, end: Vec3d, maxDistance: Double): List<Vec3d> {
        val distance = start.distanceTo(end)
        val steps = (distance / maxDistance).toInt().coerceAtLeast(1)

        return (1..steps).map { step ->
            val ratio = step.toDouble() / steps.toDouble()
            Vec3d(
                start.x + (end.x - start.x) * ratio,
                start.y + (end.y - start.y) * ratio,
                start.z + (end.z - start.z) * ratio
            )
        }.plus(end) // Ensure the final point is exactly at the target's collision box center
    }

    private suspend fun travelPath(path: List<Vec3d>, sendPacket: (Vec3d) -> Unit) {
        for (point in path) {
            sendPacket(point)
            delay(10)
        }
    }

    private fun findPathWithObstacles(start: Vec3d, target: LivingEntity, maxDistance: Double): List<Vec3d> {
        // Convert start Vec3d to BlockPos by truncating to integers
        val startPos = BlockPos(start.x.toInt(), start.y.toInt(), start.z.toInt())
        // Convert target's bounding box center (Vec3d) to BlockPos
        val targetPos = BlockPos(
            target.boundingBox.center.x.toInt(),
            target.boundingBox.center.y.toInt(), target.boundingBox.center.z.toInt())
        val path = mutableListOf<Vec3d>()
        var currentPos = startPos
        val maxSteps = 1000 // Prevent infinite loops

        while (currentPos.getSquaredDistance(targetPos) > maxDistance * maxDistance && path.size < maxSteps) {
            val direction = Vec3d(
                (targetPos.x - currentPos.x).toDouble(),
                (targetPos.y - currentPos.y).toDouble(),
                (targetPos.z - currentPos.z).toDouble()
            ).normalize().multiply(maxDistance)

            val nextPos = currentPos.add(direction.x.toInt(), direction.y.toInt(), direction.z.toInt())

            if (isPassable(nextPos)) {
                path.add(Vec3d(nextPos.x + 0.5, nextPos.y.toDouble(), nextPos.z + 0.5))
                currentPos = nextPos
            } else {
                val alternatePos = findAlternatePath(currentPos, targetPos)
                if (alternatePos == currentPos) break // No valid path found
                path.add(Vec3d(alternatePos.x + 0.5, alternatePos.y.toDouble(), alternatePos.z + 0.5))
                currentPos = alternatePos
            }
        }

        // Add the final target position (center of the collision box)
        path.add(target.boundingBox.center)
        return path
    }

    private fun findAlternatePath(current: BlockPos, target: BlockPos): BlockPos {
        val directions = listOf(
            Vec3i(1, 0, 0), Vec3i(-1, 0, 0), Vec3i(0, 0, 1), Vec3i(0, 0, -1),
            Vec3i(0, 1, 0), Vec3i(0, -1, 0) // Include vertical movement
        )

        // Sort directions by distance to target to prioritize closer paths
        val sortedDirections = directions.sortedBy { dir ->
            val newPos = current.add(dir)
            newPos.getSquaredDistance(target)
        }

        for (dir in sortedDirections) {
            val newPos = current.add(dir)
            if (isPassable(newPos)) return newPos
        }

        return current // Fallback to current position if no valid path is found
    }
    private fun handleVelocity18(target: LivingEntity) {
        currentPath = if (throughWalls) {
            findPath(player.pos, target.pos, moveDistance.toDouble())
        } else {
            findPathWithObstacles(player.pos, target, moveDistance.toDouble())
        }

        if (currentPath.isEmpty()) return

        // Send movement packets
        currentPath.forEach { vec ->
            when (packetMode) {
                PacketMode.C04 -> network.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(vec.x, vec.y, vec.z, true, false)
                )

                PacketMode.C06 -> network.sendPacket(
                    PlayerMoveC2SPacket.Full(vec.x, vec.y, vec.z, player.yaw, player.pitch, true, false)
                )
            }
        }

        // Attack at destination
        doAttack(target, currentPath.last().x, currentPath.last().z)

        // Return path
        currentPath.reversed().forEach { vec ->
            when (packetMode) {
                PacketMode.C04 -> network.sendPacket(
                    PlayerMoveC2SPacket.PositionAndOnGround(vec.x, vec.y, vec.z, true, false)
                )

                PacketMode.C06 -> network.sendPacket(
                    PlayerMoveC2SPacket.Full(vec.x, vec.y, vec.z, player.yaw, player.pitch, true, false)
                )
            }
        }
    }


    private fun isPassable(pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        val block = state.block

        return state.isAir ||
            block.defaultState.isReplaceable ||
            state.fluidState.isStill
    }


    private fun handleVelocity19(target: LivingEntity) {
        val diff = player.getAttackDistanceScalingFactor(target)
        val times = (diff / moveDistance).toInt()

        // Travel to target
        repeat(times) {
            sendPositionPacket(target.x, target.y, target.z)
        }

        currentPath = listOf(Vec3d(target.z, target.y, target.z))
        doAttack(target, target.x, target.z)

        // Travel back
        repeat(times) {
            sendPositionPacket(player.x, player.y, player.z)
        }
    }

    private fun sendPositionPacket(x: Double, y: Double, z: Double) {
        when (packetMode) {
            PacketMode.C04 -> network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, false)
            )

            PacketMode.C06 -> network.sendPacket(
                PlayerMoveC2SPacket.Full(
                    x, y, z,
                    player.yaw,
                    player.pitch,
                    true,
                    false
                )
            )
        }
    }

    private fun doAttack(target: Entity, x: Double, z: Double) {
        // 1. Switch to bow slot if not already holding it
        val bowSlot = (0..8).firstOrNull { player.inventory.getStack(it).item == Items.BOW } ?: return
        if (player.inventory.selectedSlot != bowSlot) {
            SilentHotbar.selectSlotSilently(this, HotbarItemSlot(bowSlot), 0)
        }

        // 2. Start using the bow (equivalent to C08PacketPlayerBlockPlacement)
        network.sendPacket(
            PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                0,
                player.yaw,
                player.pitch
            )
        )

        // 3. Aim at target
        repeat(20) {
            val (yaw, pitch) = calculateBowAngles(target, x, z)
            network.sendPacket(
                PlayerMoveC2SPacket.LookAndOnGround(
                    yaw,
                    pitch,
                    player.isOnGround,
                    false
                )
            )
        }

        // 4. Release the bow (equivalent to C07PacketPlayerDigging.Action.RELEASE_USE_ITEM)
        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN,
                Direction.DOWN,
                0
            )
        )

        // 5. Reset position packets
        repeat(2) {
            network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(
                    player.x, player.y, player.z,
                    player.isOnGround,
                    false
                )
            )
        }

        // 6. Reset rotation
        network.sendPacket(
            PlayerMoveC2SPacket.LookAndOnGround(
                player.yaw,
                player.pitch,
                player.isOnGround,
                false
            )
        )
    }

    private fun calculateBowAngles(entity: Entity, x: Double, z: Double): Pair<Float, Float> {
        val posX = entity.x - x
        val posY = entity.boundingBox.minY + entity.eyeY - 0.15 -
            player.boundingBox.minY - player.eyeY
        val posZ = entity.z - z
        val posSqrt = sqrt(posX * posX + posZ * posZ)

        val yaw = (atan2(posZ, posX) * 180.0 / PI - 90.0).toFloat()
        val pitch = (-Math.toDegrees(
            atan(
                (1 - sqrt(abs(1 - 0.006f * (0.006f * (posSqrt * posSqrt) + 2 * posY * 1))) /
                    (0.006f * posSqrt)
                    )
            )
        ).toFloat())

        return Pair(yaw, pitch.coerceIn(-90f, 90f))
    }

    @Suppress("unused")
    private suspend fun handleMovement(target: LivingEntity) {
        when (mode) {
            Mode.VELOCITY_1_8 -> handleVelocity18Movement(target)
            Mode.VELOCITY_1_9 -> handleVelocity19Movement(target)
        }
        clickTimer.reset()
    }

    private suspend fun handleVelocity18Movement(target: LivingEntity) {
        currentPath = if (throughWalls) {
            findPath(player.pos, target.pos, moveDistance.toDouble())
        } else {
            findPathWithObstacles(player.pos, target, moveDistance.toDouble())
        }

        if (currentPath.isEmpty()) return

        travelToTarget(currentPath)
        doAttack(target, currentPath.last().x, currentPath.last().z)
        travelBack(currentPath.reversed())
    }

    private suspend fun handleVelocity19Movement(target: LivingEntity) {
        val diff = player.distanceTo(target)
        val times = (diff / moveDistance).toInt()

        repeatMoveToTarget(times, target)
        currentPath = listOf(Vec3d(target.x, target.y, target.z))
        doAttack(target, target.x, target.z)
        repeatMoveBack(times)
    }

    private suspend fun travelToTarget(path: List<Vec3d>) {
        travelPath(path) { vec -> sendMovementPacket(vec) }
    }

    private suspend fun travelBack(path: List<Vec3d>) {
        travelPath(path) { vec -> sendMovementPacket(vec) }
    }

    private fun repeatMoveToTarget(times: Int, target: LivingEntity) {
        repeat(times) { sendMovementPacket(Vec3d(target.x, target.y, target.z)) }
    }

    private fun repeatMoveBack(times: Int) {
        repeat(times) { sendMovementPacket(Vec3d(player.x, player.y, player.z)) }
    }

    private fun sendMovementPacket(vec: Vec3d) {
        when (packetMode) {
            PacketMode.C04 -> network.sendPacket(
                PlayerMoveC2SPacket.PositionAndOnGround(
                    vec.x, vec.y, vec.z, true, false
                )
            )

            PacketMode.C06 -> network.sendPacket(
                PlayerMoveC2SPacket.Full(
                    vec.x, vec.y, vec.z,
                    player.yaw, player.pitch,
                    true, false
                )
            )
        }
    }
}
