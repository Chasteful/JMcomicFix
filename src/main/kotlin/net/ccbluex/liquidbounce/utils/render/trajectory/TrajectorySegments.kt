package net.ccbluex.liquidbounce.utils.render.trajectory

import com.viaversion.viaversion.api.minecraft.Vector3f
import net.ccbluex.liquidbounce.features.module.modules.player.autoclutch.ModuleAutoClutch.TrajectorySegment
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.render.Camera
import net.minecraft.util.math.Vec3d

object TrajectorySegments {
    fun generateTrajectorySegments(
        points: List<Pair<Vec3d, Boolean>>,
        camera: Camera
    ): List<TrajectorySegment> {
        val result = mutableListOf<TrajectorySegment>()
        val segmentsPerTick = 5

        if (points.size < 2) return emptyList()

        fun catmullRom(t: Float, p0: Vec3d, p1: Vec3d, p2: Vec3d, p3: Vec3d): Vec3d {
            val t2 = t * t
            val t3 = t2 * t
            val a = -0.5 * t3 + t2 - 0.5 * t
            val b = 1.5 * t3 - 2.5 * t2 + 1.0
            val c = -1.5 * t3 + 2.0 * t2 + 0.5 * t
            val d = 0.5 * t3 - 0.5 * t2
            return Vec3d(
                a * p0.x + b * p1.x + c * p2.x + d * p3.x,
                a * p0.y + b * p1.y + c * p2.y + d * p3.y,
                a * p0.z + b * p1.z + c * p2.z + d * p3.z
            )
        }

        val pointList = points.map { it.first }
        val isSafeList = points.map { it.second }

        for (i in 0 until pointList.size - 1) {
            val p1 = pointList[i]
            val p2 = pointList[i + 1]
            val p0 = if (i > 0) pointList[i - 1] else p1
            val p3 = if (i < pointList.size - 2) pointList[i + 2] else p2

            val isSafe1 = isSafeList[i]
            val isSafe2 = isSafeList[i + 1]

            var last: Vector3f? = null
            for (j in 0..segmentsPerTick) {
                val t = j / segmentsPerTick.toFloat()
                val interpolated = catmullRom(t, p0, p1, p2, p3)
                val rel = Vector3f(
                    (interpolated.x - camera.pos.x).toFloat(),
                    (interpolated.y - camera.pos.y).toFloat() + 0.1f,
                    (interpolated.z - camera.pos.z).toFloat()
                )

                val color = if (isSafe1 == isSafe2) {
                    if (isSafe1){
                        Color4b(0x20, 0xC2, 0x06, 200)
                    } else {
                        Color4b(0xD7, 0x09, 0x09, 200)
                    }
                } else {
                    val colorT = t
                    val r = ((if (isSafe1) 0x20 else 0xD7) + ((if (isSafe2) 0x20 else 0xD7) - (if (isSafe1) 0x20 else 0xD7)) * colorT).toInt()
                    val g = ((if (isSafe1) 0xC2 else 0x09) + ((if (isSafe2) 0xC2 else 0x09) - (if (isSafe1) 0xC2 else 0x09)) * colorT).toInt()
                    val b = ((if (isSafe1) 0x06 else 0x09) + ((if (isSafe2) 0x06 else 0x09) - (if (isSafe1) 0x06 else 0x09)) * colorT).toInt()
                    Color4b(r, g, b, 200)
                }

                if (last != null) {
                    result.add(TrajectorySegment(last, rel, color))
                }
                last = rel
            }
        }

        return result
    }

}
