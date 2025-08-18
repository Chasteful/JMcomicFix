package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos

@Suppress("TooManyFunctions")
object ModuleAutoStuck : ClientModule("AutoStuck", Category.WORLD) {

    private val resetTicks by int("ResetTicks", 300, 200..500, "ticks")
    private val fallDistance by int("FallDistance", 5, 0..25, "blocks")
    private val onlyPearl by boolean("OnlyPearl", true)
    private val onlyDuringCombat by boolean("OnlyDuringCombat", false)

    private const val LOWEST_Y = -64

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
            it.directionalInput = DirectionalInput(
                forwards = false,
                backwards = false,
                left = false,
                right = false
            )
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
            shouldActivate = false
        }
    }

    private fun shouldDisableStuck(): Boolean =
        player.isInsideWaterOrBubbleColumn || hasSolidBlockBelow()

    private fun hasSolidBlockBelow(): Boolean {
        val checkDepth = 3
        return (0..checkDepth).any {
            BlockPos(player.x.toInt(), (player.y - it).toInt(), player.z.toInt())
                .getState()
                ?.isAir == false
        }
    }

    @Suppress("unused")
    private val worldChangeEventHandler = handler<WorldChangeEvent> {
        lastGroundY = LOWEST_Y
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



        shouldActivate = (!onlyDuringCombat || CombatManager.isInCombat) &&
                (!onlyPearl || hasPearlInHotbar()) &&
                !player.isOnGround &&
                player.y <= lastGroundY + 1 - fallDistance

        if (shouldActivate && !shouldEnableStuck && stuckCooldown <= 0 && !shouldDisableStuck()) {
            shouldEnableStuck = true
            isInAir = false
        } else if (!shouldActivate && shouldEnableStuck) {
            shouldEnableStuck = false
        }
    }

    override fun onEnabled() {
        stuckTicks = 0
        isInAir = false
        lastGroundY = LOWEST_Y
    }

    override fun onDisabled() {
        shouldEnableStuck = false
    }
}
