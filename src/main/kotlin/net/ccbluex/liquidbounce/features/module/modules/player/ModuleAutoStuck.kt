package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos
import kotlin.math.ceil
import kotlin.math.floor

@Suppress("TooManyFunctions")
object ModuleAutoStuck : ClientModule("AutoStuck", Category.WORLD) {

    private val resetTicks by int("ResetTicks", 300, 200..500, "ticks")

    private object StuckSettings : ToggleableConfigurable(ModuleAutoStuck, "StuckSettings", true) {
        val stuckOnlyVoid by boolean("OnlyVoid", true)
        val stuckOnlyPearl by boolean("OnlyPearl", true)
        val onlyDuringCombat by boolean("OnlyDuringCombat", false)
        val stuckFallDistance by int("FallDistance", 5, 1..50, "blocks")
    }

    init {
        tree(StuckSettings)
    }

    private const val LOWEST_Y = -64
    private const val BLOCK_EDGE = 0.3

    private var stuckTicks = 0
    private var stuckCooldown = 0
    private var lastGroundY = LOWEST_Y

    var isInAir = false
    var shouldEnableStuck = false
    var shouldActivate = false

    private fun hasPearlInHotbar() =
        player.inventory.main.any { it?.item == Items.ENDER_PEARL }

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> {
        if (shouldEnableStuck) {
            player.movement.x = 0.0
            player.movement.y = 0.0
            player.movement.z = 0.0
        }
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (!shouldEnableStuck) return@handler

        if (!player.isOnGround) {
            isInAir = true

            when (event.packet) {
                is PlayerPositionLookS2CPacket -> shouldEnableStuck = false
                is PlayerMoveC2SPacket -> event.cancelEvent()
                is PlayerInteractItemC2SPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        PlayerMoveC2SPacket.LookAndOnGround(
                            player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                        )
                    )
                    sendPacketSilently(
                        PlayerInteractItemC2SPacket(
                            event.packet.hand, event.packet.sequence, player.yaw, player.pitch
                        )
                    )
                }
            }
        } else if (isInAir) {
            shouldEnableStuck = false
        }
    }

    private fun shouldDisableStuck(): Boolean =
        player.isInsideWaterOrBubbleColumn || isTouchingLadder() || hasSolidBlockBelow()

    private fun hasSolidBlockBelow(): Boolean {
        val checkDepth = 3
        return (0..checkDepth).any {
            BlockPos(player.x.toInt(), (player.y - it).toInt(), player.z.toInt())
                .getState()
                ?.isAir == false
        }
    }

    private fun isTouchingLadder(): Boolean {
        val positions = mutableListOf<BlockPos>().apply {
            add(BlockPos(player.x.toInt(), (player.y - 0.1).toInt(), player.z.toInt()))
            val lookVec = player.rotationVector
            when {
                lookVec.x > 0.7 -> add(BlockPos((player.x + 1).toInt(), player.y.toInt(), player.z.toInt()))
                lookVec.x < -0.7 -> add(BlockPos((player.x - 1).toInt(), player.y.toInt(), player.z.toInt()))
                lookVec.z > 0.7 -> add(BlockPos(player.x.toInt(), player.y.toInt(), (player.z + 1).toInt()))
                lookVec.z < -0.7 -> add(BlockPos(player.x.toInt(), player.y.toInt(), (player.z - 1).toInt()))
            }
        }
        return positions.any { world.getBlockState(it).block.translationKey.contains("ladder") }
    }

    @Suppress("unused")
    private val worldChangeEventHandler = handler<WorldChangeEvent> {
        lastGroundY = LOWEST_Y
    }

    fun aboveVoid(voidDistance: Int = -1): Boolean {
        if (player.isOnGround) return false

        val xMin = if (player.x - floor(player.x) <= BLOCK_EDGE) -1 else 0
        val xMax = if (ceil(player.x) - player.x <= BLOCK_EDGE) 1 else 0
        val zMin = if (player.z - floor(player.z) <= BLOCK_EDGE) -1 else 0
        val zMax = if (ceil(player.z) - player.z <= BLOCK_EDGE) 1 else 0

        return (xMin..xMax).any { dx ->
            (zMin..zMax).any { dz ->
                val yRange = if (voidDistance == -1) {
                    LOWEST_Y..lastGroundY
                }
                else {
                    (lastGroundY - voidDistance).coerceAtLeast(LOWEST_Y)..lastGroundY
                }
                yRange.all { y ->
                    BlockPos(player.x.toInt() + dx, y, player.z.toInt() + dz)
                        .getBlock()
                        ?.translationKey == "block.minecraft.air"
                }
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.isOnGround) lastGroundY = player.y.toInt() - 1

        if (stuckCooldown > 0) {
            stuckCooldown--
            return@tickHandler
        }

        if (shouldEnableStuck) {
            stuckTicks++
            if (stuckTicks >= resetTicks || shouldDisableStuck()) {
                stuckTicks = 0
                shouldEnableStuck = false
                stuckCooldown = 1
            }
        } else {
            stuckTicks = 0
        }

        if (!StuckSettings.enabled) return@tickHandler

        shouldActivate =
            (!StuckSettings.stuckOnlyVoid || aboveVoid()) &&
                (!StuckSettings.onlyDuringCombat || CombatManager.isInCombat) &&
                (!StuckSettings.stuckOnlyPearl || hasPearlInHotbar()) &&
                !player.isOnGround &&
                player.y <= lastGroundY + 1 - StuckSettings.stuckFallDistance

        if (shouldActivate && !shouldEnableStuck && stuckCooldown <= 0 && !shouldDisableStuck()) {
            shouldEnableStuck = true
            isInAir = false
        } else if (!shouldActivate && shouldEnableStuck) {
            shouldEnableStuck = false
        }
    }

    override fun enable() {
        stuckTicks = 0
        isInAir = false
        lastGroundY = LOWEST_Y
    }

    override fun disable() {
        shouldEnableStuck = false
    }
}
