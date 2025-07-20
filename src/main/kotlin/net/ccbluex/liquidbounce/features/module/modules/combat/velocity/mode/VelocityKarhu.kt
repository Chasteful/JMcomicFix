package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode


import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.minecraft.util.shape.VoxelShapes

internal object VelocityKarhu : VelocityMode("Karhu") {

    @Suppress("unused")
    private val blockShapeHandler = sequenceHandler<BlockShapeEvent> { event ->
        if (player.hurtTime > 0 && event.state.isAir && event.pos.y == player.blockPos.y + 1) {
            event.shape = VoxelShapes.cuboid(
                event.pos.x.toDouble(),
                event.pos.y.toDouble(),
                event.pos.z.toDouble(),
                event.pos.x + 1.0,
                event.pos.y + 1.0,
                event.pos.z + 1.0
            )
        }
    }
}
