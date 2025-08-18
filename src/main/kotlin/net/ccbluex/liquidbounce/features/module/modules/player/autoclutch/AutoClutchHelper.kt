package net.ccbluex.liquidbounce.features.module.modules.player.autoclutch

import net.ccbluex.liquidbounce.features.module.modules.player.autoclutch.ModuleAutoClutch.adjacentSafeBlocks
import net.ccbluex.liquidbounce.features.module.modules.player.autoclutch.ModuleAutoClutch.unsafeBlocks
import net.ccbluex.liquidbounce.features.module.modules.player.autoclutch.ModuleAutoClutch.voidThreshold
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.shape.VoxelShapes


 fun canReachSafeBlockFrom(pos: Vec3d = player.pos): Boolean {
    val cache = PlayerSimulationCache.getSimulationForLocalPlayer()
    val snapshots = (0 until 20).map { cache.getSnapshotAt(it) }

    for (snapshot in snapshots) {
        val currentPos = snapshot.pos
        val blockPos = BlockPos(currentPos.x.toInt(), currentPos.y.toInt(), currentPos.z.toInt())
        val belowPos = blockPos.down()
        val belowState = world.getBlockState(belowPos)
        val isSafeLanding = !belowState.isAir && belowState.block !in unsafeBlocks

        val playerBox = player.boundingBox.offset(currentPos.subtract(pos))
        val safeBlockCount = countAdjacentSafeBlocks(blockPos)
        val isNearSafeBlock = isSafeLanding && safeBlockCount >= adjacentSafeBlocks
        if (isNearSafeBlock && !world.getBlockCollisions(player, playerBox).any()) {
            return true
        }
    }
    return false
}

fun isPlayerSafe(): Boolean {
    if (player.isSneaking && isBlockUnder(1.0)) return true
    if (!isInVoid(player.pos) || canReachSafeBlock()) return true
    if (player.velocity.y > 0.2 && canReachSafeBlock()) return true
    return false
}

 fun isPredictingFall(): Boolean {
    if (player.isOnGround) return false
    val velY = player.velocity.y
    if (velY < -0.2 && !canReachSafeBlock()) return true
    if (player.fallDistance > 2f && velY <= 0.0 && !canReachSafeBlock()) return true

    return simulatePlayerTrajectory { pos, _, _ ->
        pos.y <= voidThreshold.toDouble() && !canReachSafeBlock()
    }
}

fun countAdjacentSafeBlocks(center: BlockPos): Int {
    var count = 0
    for (dx in -1..1) for (dz in -1..1) {
        val nearby = BlockPos(center.x + dx, center.y - 1, center.z + dz)
        val state = world.getBlockState(nearby)
        if (!state.isAir && state.block !in unsafeBlocks) count++
    }
    return count
}

fun canReachSafeBlock(): Boolean {
    return simulatePlayerTrajectory { pos, playerBox, blockPos ->
        val belowPos = blockPos.down()
        val belowState = world.getBlockState(belowPos)
        val isSafeLanding = !belowState.isAir && belowState.block !in unsafeBlocks

        val hasCollision = world.getBlockCollisions(player, playerBox).iterator().hasNext()
        val safeBlockCount = countAdjacentSafeBlocks(blockPos)

        hasCollision && isSafeLanding && safeBlockCount >= adjacentSafeBlocks
    }
}


 fun isInVoid(pos: Vec3d): Boolean {
    val boundingBox = player.boundingBox
        .offset(pos.subtract(player.pos))
        .withMinY(voidThreshold.toDouble())
    val collisions = world.getBlockCollisions(player, boundingBox)
    return collisions.none() || collisions.all { shape -> shape == VoxelShapes.empty() }
}

 fun isBlockUnder(height: Double = 5.0): Boolean {

    var offset = 0.0
    while (offset < height) {
        val motionX = player.velocity.x
        val motionZ = player.velocity.z
        val playerBox = player.boundingBox.offset(motionX * offset, -offset, motionZ * offset)
        if (world.getBlockCollisions(player, playerBox).iterator().hasNext()) {
            return true
        }
        offset += 0.5
    }
    return false
}

fun simulatePlayerTrajectory(checkCondition: (Vec3d, Box, BlockPos) -> Boolean): Boolean {
    val cache = PlayerSimulationCache.getSimulationForLocalPlayer()

    for (tick in 0 until ModuleAutoClutch.PlayerTrajectory.trajectoryLength) {
        val snapshot = cache.getSnapshotAt(tick)
        val pos = snapshot.pos
        val playerBox = player.boundingBox.offset(pos.subtract(player.pos))
        val blockPos = BlockPos(pos.x.toInt(), (pos.y - 0.5).toInt(), pos.z.toInt())

        if (checkCondition(pos, playerBox, blockPos)) {
            return true
        }
    }
    return false
}
