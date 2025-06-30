package net.ccbluex.liquidbounce.features.module.modules.client



import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b

object ModulePacketQueue : ClientModule("PacketQueueColor", Category.CLIENT, disableActivation = true) {

    val colorMode = choices("ColorMode", 2) {
        arrayOf(
            GenericStaticColorMode(it, Color4b(0, 128, 255, 255)),
            GenericSyncColorMode(it),
            GenericSyncColorMode(it),
        )
    }

    val lineAlpha by int("LineAlpha", 255, 0..255)
    val modelAlpha by int("ModelAlpha", 255, 0..255)
    val modelOutlineAlpha by int("ModelOutlineAlpha", 255, 0..255)
}
