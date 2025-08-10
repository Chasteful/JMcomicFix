package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * TatakoExemptV0.97.3-a2 Velocity by _0x16z
 */
internal object VelocityTatakoExempt : VelocityMode("TatakoExemptV0.97.3-a2") {

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        if (player.moving) return@handler

        player.swingHand(Hand.MAIN_HAND)

        val pos = BlockPos(player.x.toInt(), (player.y - 1.0).toInt(), player.z.toInt())
        val hitResult = BlockHitResult(player.pos, Direction.UP, pos, false)

        network.sendPacket(
            PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 1)
        )

        player.setPosition(player.x, player.y - player.y / 64.0, player.z)
        network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(true,player.horizontalCollision))
    }
}
