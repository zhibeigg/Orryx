package org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic

import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.abs

private const val SEGMENT_EPSILON = 1e-12

internal fun closestPointsOnSegments(
    firstStart: Vector3dc,
    firstEnd: Vector3dc,
    secondStart: Vector3dc,
    secondEnd: Vector3dc,
    firstResult: Vector3d,
    secondResult: Vector3d
) {
    val firstDirection = Vector3d(firstEnd).sub(firstStart)
    val secondDirection = Vector3d(secondEnd).sub(secondStart)
    val offset = Vector3d(firstStart).sub(secondStart)
    val firstLengthSquared = firstDirection.lengthSquared()
    val secondLengthSquared = secondDirection.lengthSquared()
    val secondProjection = secondDirection.dot(offset)

    var firstParameter: Double
    var secondParameter: Double
    if (firstLengthSquared <= SEGMENT_EPSILON && secondLengthSquared <= SEGMENT_EPSILON) {
        firstParameter = 0.0
        secondParameter = 0.0
    } else if (firstLengthSquared <= SEGMENT_EPSILON) {
        firstParameter = 0.0
        secondParameter = (secondProjection / secondLengthSquared).coerceIn(0.0, 1.0)
    } else {
        val firstProjection = firstDirection.dot(offset)
        if (secondLengthSquared <= SEGMENT_EPSILON) {
            secondParameter = 0.0
            firstParameter = (-firstProjection / firstLengthSquared).coerceIn(0.0, 1.0)
        } else {
            val directionDot = firstDirection.dot(secondDirection)
            val denominator = firstLengthSquared * secondLengthSquared - directionDot * directionDot
            firstParameter = if (abs(denominator) > SEGMENT_EPSILON) {
                ((directionDot * secondProjection - firstProjection * secondLengthSquared) / denominator)
                    .coerceIn(0.0, 1.0)
            } else {
                0.0
            }
            secondParameter = (directionDot * firstParameter + secondProjection) / secondLengthSquared
            when {
                secondParameter < 0.0 -> {
                    secondParameter = 0.0
                    firstParameter = (-firstProjection / firstLengthSquared).coerceIn(0.0, 1.0)
                }
                secondParameter > 1.0 -> {
                    secondParameter = 1.0
                    firstParameter = ((directionDot - firstProjection) / firstLengthSquared).coerceIn(0.0, 1.0)
                }
            }
        }
    }

    firstResult.set(firstDirection).mul(firstParameter).add(firstStart)
    secondResult.set(secondDirection).mul(secondParameter).add(secondStart)
}

internal fun closestPointsSegmentAabb(
    segmentStart: Vector3dc,
    segmentEnd: Vector3dc,
    min: Vector3dc,
    max: Vector3dc,
    segmentResult: Vector3d,
    boxResult: Vector3d
): Double {
    val direction = Vector3d(segmentEnd).sub(segmentStart)
    val breakpoints = ArrayList<Double>(8)
    breakpoints += 0.0
    breakpoints += 1.0

    fun addBreakpoint(start: Double, delta: Double, boundary: Double) {
        if (abs(delta) <= SEGMENT_EPSILON) return
        val parameter = (boundary - start) / delta
        if (parameter > 0.0 && parameter < 1.0) breakpoints += parameter
    }

    addBreakpoint(segmentStart.x(), direction.x, min.x())
    addBreakpoint(segmentStart.x(), direction.x, max.x())
    addBreakpoint(segmentStart.y(), direction.y, min.y())
    addBreakpoint(segmentStart.y(), direction.y, max.y())
    addBreakpoint(segmentStart.z(), direction.z, min.z())
    addBreakpoint(segmentStart.z(), direction.z, max.z())
    breakpoints.sort()

    var bestDistanceSquared = Double.POSITIVE_INFINITY
    val point = Vector3d()
    val closest = Vector3d()

    fun evaluate(parameter: Double) {
        point.set(direction).mul(parameter).add(segmentStart)
        closest.set(
            point.x.coerceIn(min.x(), max.x()),
            point.y.coerceIn(min.y(), max.y()),
            point.z.coerceIn(min.z(), max.z())
        )
        val distanceSquared = point.distanceSquared(closest)
        if (distanceSquared < bestDistanceSquared) {
            bestDistanceSquared = distanceSquared
            segmentResult.set(point)
            boxResult.set(closest)
        }
    }

    breakpoints.forEach(::evaluate)
    for (index in 0 until breakpoints.lastIndex) {
        val lower = breakpoints[index]
        val upper = breakpoints[index + 1]
        if (upper - lower <= SEGMENT_EPSILON) continue
        val midpoint = (lower + upper) / 2.0
        point.set(direction).mul(midpoint).add(segmentStart)

        var numerator = 0.0
        var denominator = 0.0
        fun includeAxis(value: Double, start: Double, delta: Double, minimum: Double, maximum: Double) {
            val boundary = when {
                value < minimum -> minimum
                value > maximum -> maximum
                else -> return
            }
            numerator += delta * (start - boundary)
            denominator += delta * delta
        }

        includeAxis(point.x, segmentStart.x(), direction.x, min.x(), max.x())
        includeAxis(point.y, segmentStart.y(), direction.y, min.y(), max.y())
        includeAxis(point.z, segmentStart.z(), direction.z, min.z(), max.z())
        if (denominator > SEGMENT_EPSILON) {
            evaluate((-numerator / denominator).coerceIn(lower, upper))
        }
    }

    return bestDistanceSquared
}
