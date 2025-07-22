package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.client.toDegrees
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.entity.LivingEntity
import net.minecraft.util.Identifier
import org.joml.Quaternionf
import java.awt.Color
import kotlin.math.atan2

object ModulePointerESP : ClientModule("PointerESP", Category.RENDER) {

    private val modes = choices("ColorMode", 0) {
        arrayOf(
            DistanceColor,
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b.WHITE)
        )
    }

    private object ImageOffset : ToggleableConfigurable(this, "Offset", true) {
        val offsetX by float("OffsetX", 0f, -1f..1f)
        val offsetY by float("OffsetY", 0.3f, -1f..1f)
        val offsetZ by float("OffsetZ", 0f, -1f..1f)
    }

    private object DistanceColor : GenericColorMode<LivingEntity>("Distance") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        private val gradientRange by floatRange("GradientRange", 8f..48f, 0f..256f)
        private val saturation by float("Saturation", 1f, 0f..1f)
        private val brightness by float("Brightness", 1f, 0f..1f)

        override fun getColors(param: LivingEntity): Pair<Color4b, Color4b> {
            val length = gradientRange.endInclusive - gradientRange.start
            val hue = (param.distanceTo(player).coerceIn(gradientRange) - gradientRange.start) / length / 3f
            val color = Color4b(Color.getHSBColor(hue, saturation, brightness))
            return color to color
        }

        override fun getColor(param: LivingEntity): Color4b {
            return getColors(param).first
        }
    }

    init {
        tree(ImageOffset)
    }
    private val renderRadius by int("RenderRadius", 100, 50..500)
    private val pointerSize by float("PointerSize", 15f, 1f..20f)
    private val pointerAlpha by float("PointerAlpha", 1f, 0f..1f)
    private val pitchLimit by floatRange("PitchLimit", 30f..90f, 0f..90f).onChanged {
        negativePitchLimit = -it.endInclusive..-it.start
    }

    private var negativePitchLimit: ClosedFloatingPointRange<Float> = -pitchLimit.endInclusive..-pitchLimit.start
    private var prevRotateX = 0f

    private val pointerTexture: Identifier = "image/hud/triangle.png".registerAsDynamicImageFromClientResources()
    val pointerShader: ShaderProgram by lazy {

        mc.shaderLoader.getOrCreateProgram(ShaderProgramKeys.POSITION_TEX_COLOR)!!
    }

    private data class Pointer(
        val radius: Int,
        val color: Color4b,
        val rotateX: Float,
        val rotateZ: Float
    )

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        val matrices = event.context.matrices
        val pointers = findRenderedEntities().mapArray {
            val diff = it.interpolateCurrentPosition(event.tickDelta) - player.pos
            val rawAngle = atan2(diff.z, diff.x).toFloat().toDegrees()
            val angle = (player.yaw - 90f - rawAngle).let { theta ->
                if (mc.options.perspective.isFrontView) -theta else theta
            }
            val rotateX = player.pitch.let { p ->
                when {
                    p in -pitchLimit.start..pitchLimit.start -> prevRotateX
                    p < 0 -> 90f + p.coerceIn(negativePitchLimit)
                    else -> 90f + p.coerceIn(pitchLimit)
                }
            }

            Pointer(
                renderRadius,
                modes.activeChoice.getColor(it),
                rotateX,
                angle
            )
        }

        renderEnvironmentForGUI(matrices) {
            val window = mc.window
            val cx = window.scaledWidth / 2f
            val cy = window.scaledHeight / 2f

            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableCull()
            RenderSystem.depthMask(false)
            RenderSystem.setShader(pointerShader)
            RenderSystem.setShaderTexture(0, pointerTexture)

            pointers.forEach { p ->
                matrices.push()

                val offsetX = if (ImageOffset.enabled) ImageOffset.offsetX * window.scaledWidth else 0f
                val offsetY = if (ImageOffset.enabled) ImageOffset.offsetY * window.scaledHeight else 0f
                val offsetZ = if (ImageOffset.enabled) ImageOffset.offsetZ * 100f else 0f

                matrices.translate(
                    (cx + offsetX).toDouble(),
                    (cy + offsetY).toDouble(),
                    offsetZ.toDouble()
                )


                matrices.multiply(Quaternionf().rotateX(Math.toRadians(p.rotateX.toDouble()).toFloat()))
                matrices.multiply(Quaternionf().rotateZ(Math.toRadians(p.rotateZ.toDouble()).toFloat()))

                matrices.translate(0.0, -p.radius.toDouble(), 0.0)

                matrices.scale(pointerSize, pointerSize, 1f)
                matrices.translate(-0.5, -0.5, 0.0)

                val color = p.color.with(a = (pointerAlpha * 255f).toInt().coerceIn(0, 255))
                drawCustomMesh(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    ShaderProgramKeys.POSITION_TEX_COLOR
                ) { mat ->
                    vertex(mat, 0f, 0f, 0f).texture(0f, 0f).color(color.toARGB())
                    vertex(mat, 1f, 0f, 0f).texture(1f, 0f).color(color.toARGB())
                    vertex(mat, 1f, 1f, 0f).texture(1f, 1f).color(color.toARGB())
                    vertex(mat, 0f, 1f, 0f).texture(0f, 1f).color(color.toARGB())
                }

                matrices.pop()
            }

            RenderSystem.disableBlend()
            RenderSystem.enableCull()
            RenderSystem.depthMask(true)
        }

        prevRotateX = pointers.firstOrNull()?.rotateX ?: prevRotateX
    }

    private fun findRenderedEntities() = world.entities
        .filterIsInstance<LivingEntity>()
        .filter { it.shouldBeShown() }

    override fun disable() {
        prevRotateX = 0f
        super.disable()
    }
}
