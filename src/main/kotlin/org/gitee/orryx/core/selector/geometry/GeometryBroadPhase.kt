package org.gitee.orryx.core.selector.geometry

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.max

internal object GeometryBroadPhase {

    private const val EPSILON = 1e-9
    private val worldAxes = arrayOf(
        Vector(1.0, 0.0, 0.0),
        Vector(0.0, 1.0, 0.0),
        Vector(0.0, 0.0, 1.0)
    )

    data class Bounds(val center: Vector, val halfExtents: Vector)

    data class EntityBounds(val center: Vector, val halfExtents: Vector)

    class OrientedBox internal constructor(
        val center: Vector,
        val axes: Array<Vector>,
        val halfExtents: Vector
    ) {

        private data class ProjectionAxis(val axis: Vector, val boxRadius: Double)

        private val projectionAxes: List<ProjectionAxis> = buildList(15) {
            axes.forEach { addAxis(it) }
            worldAxes.forEach { addAxis(it) }
            axes.forEach { boxAxis ->
                worldAxes.forEach { worldAxis ->
                    val cross = boxAxis.clone().crossProduct(worldAxis)
                    if (cross.lengthSquared() > EPSILON) {
                        addAxis(cross.normalize())
                    }
                }
            }
        }

        val broadHalfExtents: Vector = Vector(
            axes.sumOf { abs(it.x) * halfExtentFor(it) },
            axes.sumOf { abs(it.y) * halfExtentFor(it) },
            axes.sumOf { abs(it.z) * halfExtentFor(it) }
        )

        private fun MutableList<ProjectionAxis>.addAxis(axis: Vector) {
            val normalized = if (abs(axis.lengthSquared() - 1.0) <= EPSILON) axis else axis.clone().normalize()
            add(
                ProjectionAxis(
                    normalized,
                    halfExtents.x * abs(normalized.dot(axes[0])) +
                        halfExtents.y * abs(normalized.dot(axes[1])) +
                        halfExtents.z * abs(normalized.dot(axes[2]))
                )
            )
        }

        private fun halfExtentFor(axis: Vector): Double {
            return when (axis) {
                axes[0] -> halfExtents.x
                axes[1] -> halfExtents.y
                else -> halfExtents.z
            }
        }

        fun intersects(bounds: EntityBounds): Boolean {
            val offset = bounds.center.clone().subtract(center)
            for ((axis, boxRadius) in projectionAxes) {
                val entityRadius = bounds.halfExtents.x * abs(axis.x) +
                    bounds.halfExtents.y * abs(axis.y) +
                    bounds.halfExtents.z * abs(axis.z)
                if (abs(offset.dot(axis)) > boxRadius + entityRadius + 1e-6) {
                    return false
                }
            }
            return true
        }
    }

    fun basis(direction: Vector): Array<Vector>? {
        if (!direction.isFinite() || direction.lengthSquared() <= EPSILON) {
            return null
        }
        val forward = direction.clone().normalize()
        val referenceUp = if (abs(forward.y) > 0.999) {
            Vector(0.0, 0.0, 1.0)
        } else {
            Vector(0.0, 1.0, 0.0)
        }
        val side = forward.clone().crossProduct(referenceUp).normalize()
        val up = side.clone().crossProduct(forward).normalize()
        return arrayOf(forward, up, side)
    }

    fun orientedBox(center: Vector, axes: Array<Vector>, halfExtents: Vector): OrientedBox {
        return OrientedBox(
            center,
            axes,
            Vector(abs(halfExtents.x), abs(halfExtents.y), abs(halfExtents.z))
        )
    }

    fun entityBounds(entity: LivingEntity): EntityBounds {
        val location = entity.location
        return EntityBounds(
            Vector(location.x, location.y + entity.height / 2.0, location.z),
            Vector(entity.width / 2.0, entity.height / 2.0, entity.width / 2.0)
        )
    }

    fun segmentBounds(start: Vector, end: Vector): Bounds {
        return Bounds(
            start.clone().add(end).multiply(0.5),
            Vector(
                abs(end.x - start.x) / 2.0,
                abs(end.y - start.y) / 2.0,
                abs(end.z - start.z) / 2.0
            )
        )
    }

    fun withinHorizontalRadius(origin: Vector, point: Vector, radius: Double): Boolean {
        if (!origin.isFinite() || !point.isFinite() || !radius.isFinite() || radius < 0.0) return false
        val deltaX = point.x - origin.x
        val deltaZ = point.z - origin.z
        return deltaX * deltaX + deltaZ * deltaZ <= radius * radius + EPSILON
    }

    fun nearbyLivingEntities(world: World, bounds: Bounds): Sequence<LivingEntity> {
        return nearbyLivingEntities(world, bounds.center, bounds.halfExtents)
    }

    fun nearbyLivingEntities(world: World, center: Vector, halfExtents: Vector): Sequence<LivingEntity> {
        if (!center.isFinite() || !halfExtents.isFinite()) {
            return emptySequence()
        }
        val searchCenter = Location(world, center.x, center.y, center.z)
        return world.getNearbyEntities(
            searchCenter,
            max(0.0, halfExtents.x),
            max(0.0, halfExtents.y),
            max(0.0, halfExtents.z)
        ).asSequence().filterIsInstance<LivingEntity>()
    }

    private fun Vector.isFinite(): Boolean {
        return x.isFinite() && y.isFinite() && z.isFinite()
    }
}
