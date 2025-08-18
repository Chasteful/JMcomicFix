package net.ccbluex.liquidbounce.utils.render.trajectory

import com.viaversion.viaversion.api.minecraft.Vector3f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.util.math.Vec3d

object TrajectorySegments {
    data class TrajectorySegment(
        val start: Vector3f,
        val end: Vector3f,
        val color: Color4b
    )

    private const val SEGMENTS_PER_TICK = 5

    fun generateTrajectorySegments(
        points: List<Pair<Vec3d, Boolean>>,
        safeColor: Color4b,
        unsafeColor: Color4b
    ): List<TrajectorySegment> {
        if (points.size < 2) return emptyList()

        val result = mutableListOf<TrajectorySegment>()
        val pointList = points.map { it.first }
        val isSafeList = points.map { it.second }

        for (i in 0 until pointList.size - 1) {
            val segmentPoints = getSegmentPoints(pointList, i)
            val isSafePair = isSafeList[i] to isSafeList[i + 1]
            addInterpolatedSegments(segmentPoints, isSafePair, safeColor, unsafeColor, result)
        }

        return result
    }

    private fun getSegmentPoints(points: List<Vec3d>, index: Int): List<Vec3d> {
        val p1 = points[index]
        val p2 = points[index + 1]
        val p0 = if (index > 0) points[index - 1] else p1
        val p3 = if (index < points.size - 2) points[index + 2] else p2
        return listOf(p0, p1, p2, p3)
    }

    private fun addInterpolatedSegments(
        points: List<Vec3d>,
        isSafePair: Pair<Boolean, Boolean>,
        safeColor: Color4b,
        unsafeColor: Color4b,
        result: MutableList<TrajectorySegment>
    ) {
        var last: Vec3d? = null
        for (j in 0..SEGMENTS_PER_TICK) {
            val t = j / SEGMENTS_PER_TICK.toFloat()
            val interpolated = catmullRom(t, points[0], points[1], points[2], points[3])

            if (last != null) {
                val color = getSegmentColor(isSafePair.first, isSafePair.second, t, safeColor, unsafeColor)
                result.add(
                    TrajectorySegment(
                        Vector3f(last.x.toFloat(), last.y.toFloat(), last.z.toFloat()),
                        Vector3f(interpolated.x.toFloat(), interpolated.y.toFloat(), interpolated.z.toFloat()),
                        color
                    )
                )
            }
            last = interpolated
        }
    }

    private fun catmullRom(t: Float, p0: Vec3d, p1: Vec3d, p2: Vec3d, p3: Vec3d): Vec3d {
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

    private fun getSegmentColor(
        isSafe1: Boolean,
        isSafe2: Boolean,
        alpha: Float,
        safeColor: Color4b,
        unsafeColor: Color4b): Color4b {
        if (isSafe1 == isSafe2) {
            return if (isSafe1) safeColor else unsafeColor
        }
        val startColor = if (isSafe1) safeColor else unsafeColor
        val endColor = if (isSafe2) safeColor else unsafeColor
        return Color4b.lerp(startColor, endColor, alpha)
    }
}
