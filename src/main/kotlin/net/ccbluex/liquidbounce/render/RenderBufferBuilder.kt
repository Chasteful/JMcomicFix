/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("LongParameterList")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.RenderBufferBuilder.Companion.TESSELATOR_A
import net.ccbluex.liquidbounce.render.RenderBufferBuilder.Companion.TESSELATOR_B
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.UV2f
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.minecraft.client.gl.ShaderProgramKey
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.*
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

const val FACE_DOWN = (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3)
const val FACE_UP = (1 shl 4) or (1 shl 5) or (1 shl 6) or (1 shl 7)
const val FACE_NORTH = (1 shl 8) or (1 shl 9) or (1 shl 10) or (1 shl 11)
const val FACE_EAST = (1 shl 12) or (1 shl 13) or (1 shl 14) or (1 shl 15)
const val FACE_SOUTH = (1 shl 16) or (1 shl 17) or (1 shl 18) or (1 shl 19)
const val FACE_WEST = (1 shl 20) or (1 shl 21) or (1 shl 22) or (1 shl 23)

const val EDGE_NORTH_DOWN = ((1 shl 0) or (1 shl (1)))
const val EDGE_EAST_DOWN = ((1 shl 2) or (1 shl (3)))
const val EDGE_SOUTH_DOWN = ((1 shl 4) or (1 shl (5)))
const val EDGE_WEST_DOWN = ((1 shl 6) or (1 shl (7)))

const val EDGE_NORTH_WEST = ((1 shl 8) or (1 shl (9)))
const val EDGE_NORTH_EAST = ((1 shl 10) or (1 shl (11)))
const val EDGE_SOUTH_EAST = ((1 shl 12) or (1 shl (13)))
const val EDGE_SOUTH_WEST = ((1 shl 14) or (1 shl (15)))

const val EDGE_NORTH_UP = ((1 shl 16) or (1 shl (17)))
const val EDGE_EAST_UP = ((1 shl 18) or (1 shl (19)))
const val EDGE_SOUTH_UP = ((1 shl 20) or (1 shl (21)))
const val EDGE_WEST_UP = ((1 shl 22) or (1 shl (23)))


/**
 * A utility class for drawing shapes in batches.
 *
 * Not sync, not send. Not thread-safe at all.
 */
class RenderBufferBuilder<I : VertexInputType>(
    drawMode: DrawMode,
    private val vertexFormat: I,
    private val tesselator: Tessellator
) {
    // Begin drawing lines with position format
    val buffer: BufferBuilder = tesselator.begin(drawMode, vertexFormat.vertexFormat)

    /**
     * Function to draw a solid box using the specified [box].
     *
     * @param box The bounding box of the box.
     */
    fun drawBox(
        env: RenderEnvironment,
        box: Box,
        useOutlineVertices: Boolean = false,
        color: Color4b? = null,
        verticesToUse: Int = -1
    ) {
        val matrix = env.currentMvpMatrix

        val vertexPositions = if (useOutlineVertices) {
            box.outlineVertexPositions()
        } else {
            box.vertexPositions()
        }

        val check = verticesToUse != -1

        // Draw the vertices of the box
        for (i in vertexPositions.indices) {
            if (check && (verticesToUse and (1 shl i)) != 0) {
                continue
            }

            val (x, y, z) = vertexPositions[i]
            val bb = buffer.vertex(matrix, x, y, z)

            if (color != null) {
                bb.color(color.toARGB())
            }
        }
    }

    fun drawGradientBox(
        env: WorldRenderEnvironment,
        box: Box,
        startColor: Color4b,
        endColor: Color4b,
        outlineColor: Color4b? = null
    ) {
        // Call the actual implementation with the environment
        val matrix = env.currentMvpMatrix
        val verts = box.vertexPositions()

        val faceCenters = arrayOf(
            Vec3((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0), // Down
            Vec3((box.minX + box.maxX) / 2.0, box.maxY, (box.minZ + box.maxZ) / 2.0), // Up
            Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, box.minZ), // North
            Vec3(box.maxX, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0), // East
            Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, box.maxZ), // South
            Vec3(box.minX, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0)  // West
        )

        val faces = arrayOf(
            arrayOf(0, 1, 2, 3),   // Down
            arrayOf(4, 5, 6, 7),   // Up
            arrayOf(8, 9, 10, 11),   // North
            arrayOf(12, 13, 14, 15),  // East
            arrayOf(16, 17, 18, 19),  // South
            arrayOf(20, 21, 22, 23)   // West
        )

        for (i in faces.indices) {
            val indices = faces[i]
            val v0 = verts[indices[0]]
            val v1 = verts[indices[1]]
            val v2 = verts[indices[2]]
            val v3 = verts[indices[3]]
            val center = faceCenters[i]

            with(buffer) {
                vertex(matrix, v0.x, v0.y, v0.z).color(startColor.toARGB())
                vertex(matrix, v1.x, v1.y, v1.z).color(startColor.toARGB())
                vertex(matrix, center.x, center.y, center.z).color(endColor.toARGB())

                vertex(matrix, v1.x, v1.y, v1.z).color(startColor.toARGB())
                vertex(matrix, v2.x, v2.y, v2.z).color(startColor.toARGB())
                vertex(matrix, center.x, center.y, center.z).color(endColor.toARGB())

                vertex(matrix, v2.x, v2.y, v2.z).color(startColor.toARGB())
                vertex(matrix, v3.x, v3.y, v3.z).color(startColor.toARGB())
                vertex(matrix, center.x, center.y, center.z).color(endColor.toARGB())

                vertex(matrix, v3.x, v3.y, v3.z).color(startColor.toARGB())
                vertex(matrix, v0.x, v0.y, v0.z).color(startColor.toARGB())
                vertex(matrix, center.x, center.y, center.z).color(endColor.toARGB())
            }
        }

        draw()

        if (outlineColor != null) {
            val outlineR = RenderBufferBuilder(
                DrawMode.DEBUG_LINES,
                VertexInputType.PosColor,
                TESSELATOR_B
            )
            outlineR.drawBox(env, box, useOutlineVertices = true, color = outlineColor)
            outlineR.draw()
        }
    }

    fun draw() {
        val built = buffer.endNullable() ?: return

        RenderSystem.setShader(vertexFormat.shaderProgram)

        BufferRenderer.drawWithGlobalProgram(built)
        tesselator.clear()
    }

    fun reset() {
        buffer.endNullable()
    }

    companion object {
        val TESSELATOR_A: Tessellator = Tessellator(0x200000)
        val TESSELATOR_B: Tessellator = Tessellator(0x200000)
    }
}

class BoxRenderer private constructor(private val env: WorldRenderEnvironment) {
    private val faceRenderer = RenderBufferBuilder(
        DrawMode.QUADS,
        VertexInputType.PosColor,
        TESSELATOR_A
    )
    private val outlinesRenderer = RenderBufferBuilder(
        DrawMode.DEBUG_LINES,
        VertexInputType.PosColor,
        TESSELATOR_B
    )

    companion object {
        /**
         * Draws colored boxes. Renders automatically
         */
        fun drawWith(env: WorldRenderEnvironment, fn: BoxRenderer.() -> Unit) {
            val renderer = BoxRenderer(env)

            try {
                fn(renderer)
            } finally {
                renderer.draw()
            }
        }
    }

    fun drawGradientBox(
        box: Box,
        startColor: Color4b,
        endColor: Color4b,
        outlineColor: Color4b? = null
    ) {
        val renderer = RenderBufferBuilder(
            DrawMode.TRIANGLES,
            VertexInputType.PosColor,
            TESSELATOR_A
        )

        renderer.drawGradientBox(env, box, startColor, endColor)
        renderer.draw()

        if (outlineColor != null) {
            val outlineR = RenderBufferBuilder(
                DrawMode.DEBUG_LINES,
                VertexInputType.PosColor,
                TESSELATOR_B
            )
            outlineR.drawBox(env, box, useOutlineVertices = true, color = outlineColor)
            outlineR.draw()
        }
    }

    fun drawBox(
        box: Box,
        faceColor: Color4b,
        outlineColor: Color4b? = null,
        vertices: Int = -1,
        outlineVertices: Int = -1
    ) {
        faceRenderer.drawBox(env, box, color = faceColor, verticesToUse = vertices)

        if (outlineColor != null) {
            outlinesRenderer.drawBox(env, box, true, outlineColor, outlineVertices)
        }
    }

    private fun draw() {
        faceRenderer.draw()
        outlinesRenderer.draw()
    }

}

fun Box.vertexPositions(): Array<Vec3> {
    return arrayOf(
        // down
        Vec3(minX, minY, minZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, minY, maxZ),
        Vec3(minX, minY, maxZ),

        // up
        Vec3(minX, maxY, minZ),
        Vec3(minX, maxY, maxZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(maxX, maxY, minZ),

        // north
        Vec3(minX, minY, minZ),
        Vec3(minX, maxY, minZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, minY, minZ),

        // east
        Vec3(maxX, minY, minZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(maxX, minY, maxZ),

        // south
        Vec3(minX, minY, maxZ),
        Vec3(maxX, minY, maxZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(minX, maxY, maxZ),

        // west
        Vec3(minX, minY, minZ),
        Vec3(minX, minY, maxZ),
        Vec3(minX, maxY, maxZ),
        Vec3(minX, maxY, minZ)
    )
}

fun Box.outlineVertexPositions(): Array<Vec3> {
    return arrayOf(
        // down north
        Vec3(minX, minY, minZ),
        Vec3(maxX, minY, minZ),

        // down east
        Vec3(maxX, minY, minZ),
        Vec3(maxX, minY, maxZ),

        // down south
        Vec3(maxX, minY, maxZ),
        Vec3(minX, minY, maxZ),

        // down west
        Vec3(minX, minY, maxZ),
        Vec3(minX, minY, minZ),

        // north west
        Vec3(minX, minY, minZ),
        Vec3(minX, maxY, minZ),

        // north east
        Vec3(maxX, minY, minZ),
        Vec3(maxX, maxY, minZ),

        // south east
        Vec3(maxX, minY, maxZ),
        Vec3(maxX, maxY, maxZ),

        // south west
        Vec3(minX, minY, maxZ),
        Vec3(minX, maxY, maxZ),

        // up north
        Vec3(minX, maxY, minZ),
        Vec3(maxX, maxY, minZ),

        // up east
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, maxY, maxZ),

        // up south
        Vec3(maxX, maxY, maxZ),
        Vec3(minX, maxY, maxZ),

        // up west
        Vec3(minX, maxY, maxZ),
        Vec3(minX, maxY, minZ)
    )
}

fun RenderBufferBuilder<VertexInputType.PosTexColor>.drawQuad(
    env: RenderEnvironment,
    pos1: Vec3d,
    uv1: UV2f,
    pos2: Vec3d,
    uv2: UV2f,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x.toFloat(), pos2.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv2.v)
            .color(color.toARGB())
        vertex(matrix, pos2.x.toFloat(), pos2.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv2.v)
            .color(color.toARGB())
        vertex(matrix, pos2.x.toFloat(), pos1.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv1.v)
            .color(color.toARGB())
        vertex(matrix, pos1.x.toFloat(), pos1.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv1.v)
            .color(color.toARGB())
    }
}

fun RenderBufferBuilder<VertexInputType.Pos>.drawQuad(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x, pos2.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos2.z)
        vertex(matrix, pos2.x, pos1.y, pos2.z)
        vertex(matrix, pos1.x, pos1.y, pos1.z)
    }
}
fun RenderBufferBuilder<VertexInputType.PosTexColor>.drawGradientQuad(
    env: RenderEnvironment,
    pos1: Vec3d,
    uv1: UV2f,
    pos2: Vec3d,
    uv2: UV2f,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the quad
    with(buffer) {
        vertex(matrix, pos1.x.toFloat(), pos1.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv1.v)
            .color(color.toARGB())
        vertex(matrix, pos2.x.toFloat(), pos1.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv1.v)
            .color(color.toARGB())
        vertex(matrix, pos2.x.toFloat(), pos2.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv2.v)
            .color(color.toARGB())
        vertex(matrix, pos1.x.toFloat(), pos2.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv2.v)
            .color(color.toARGB())
    }
}

fun RenderBufferBuilder<VertexInputType.PosTexColor>.drawGradientQuad(
    env: RenderEnvironment,
    pos1: Vec3d,
    uv1: UV2f,
    pos2: Vec3d,
    uv2: UV2f,
    pos3: Vec3d,
    uv3: UV2f,
    pos4: Vec3d,
    uv4: UV2f,
    color1: Color4b,
    color2: Color4b,
    color3: Color4b,
    color4: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the quad with per-vertex colors
    with(buffer) {
        vertex(matrix, pos1.x.toFloat(), pos1.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv1.v)
            .color(color1.toARGB())
        vertex(matrix, pos2.x.toFloat(), pos2.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv2.v)
            .color(color2.toARGB())
        vertex(matrix, pos3.x.toFloat(), pos3.y.toFloat(), pos3.z.toFloat())
            .texture(uv3.u, uv3.v)
            .color(color3.toARGB())
        vertex(matrix, pos4.x.toFloat(), pos4.y.toFloat(), pos4.z.toFloat())
            .texture(uv4.u, uv4.v)
            .color(color4.toARGB())
    }
}
fun RenderBufferBuilder<VertexInputType.Pos>.drawQuadOutlines(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x, pos1.y, pos1.z)
        vertex(matrix, pos1.x, pos2.y, pos1.z)

        vertex(matrix, pos1.x, pos2.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos1.z)

        vertex(matrix, pos2.x, pos1.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos1.z)

        vertex(matrix, pos1.x, pos1.y, pos1.z)
        vertex(matrix, pos2.x, pos1.y, pos1.z)
    }
}

fun RenderBufferBuilder<VertexInputType.PosColor>.drawLine(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(color.toARGB())
        vertex(matrix, pos2.x, pos2.y, pos2.z).color(color.toARGB())
    }
}

sealed class VertexInputType {
    abstract val vertexFormat: VertexFormat
    abstract val shaderProgram: ShaderProgramKey

    object Pos : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION
        override val shaderProgram: ShaderProgramKey
            get() = ShaderProgramKeys.POSITION
    }

    object PosColor : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_COLOR
        override val shaderProgram: ShaderProgramKey
            get() = ShaderProgramKeys.POSITION_COLOR
    }

    object PosTexColor : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_TEXTURE_COLOR
        override val shaderProgram: ShaderProgramKey
            get() = ShaderProgramKeys.POSITION_TEX_COLOR
    }

}
