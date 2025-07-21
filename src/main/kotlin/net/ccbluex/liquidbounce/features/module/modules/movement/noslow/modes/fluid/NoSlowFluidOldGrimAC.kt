package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.fluid

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

internal class NoSlowFluidOldGrimAC(override val parent: ChoiceConfigurable<*>) : Choice("OldGrimAC") {
    @Suppress("unused")
    private val motionHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        if (event.state != EventState.POST) return@handler
        val player = mc.player ?: return@handler
        val world = mc.world ?: return@handler

        for (x in -2..2) {
            for (y in -2..2) {
                for (z in -2..2) {
                    val pos = BlockPos(player.x.toInt() + x, player.y.toInt() + y, player.z.toInt() + z)
                    val block = world.getBlockState(pos).block
                    if (block == Blocks.WATER || block == Blocks.WATER_CAULDRON
                        || block == Blocks.LAVA || block == Blocks.LAVA_CAULDRON
                    ) {
                        val connection = mc.networkHandler ?: continue
                        connection.sendPacket(
                            PlayerActionC2SPacket
                                (PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN)
                        )
                        connection.sendPacket(
                            PlayerActionC2SPacket
                                (PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN)
                        )
                    }
                }
            }
        }
    }
}
