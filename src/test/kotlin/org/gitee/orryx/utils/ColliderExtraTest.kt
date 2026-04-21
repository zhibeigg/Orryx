package org.gitee.orryx.utils

import org.gitee.orryx.api.collider.*
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private interface MockTarget2 : ITargetLocation<Unit> {
    override fun getSource() = Unit
    override val world get() = throw UnsupportedOperationException()
    override val location get() = throw UnsupportedOperationException()
    override val eyeLocation get() = throw UnsupportedOperationException()
}

private class MockSphere2(
    override var center: Vector3d, override var radius: Double
) : ISphere<MockTarget2>, MockTarget2 {
    private var disabled = false
    override val fastCollider: IAABB<MockTarget2>? = null
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

private class MockAABB2(
    override var center: Vector3d, override var halfExtents: Vector3d
) : IAABB<MockTarget2>, MockTarget2 {
    private var disabled = false
    override val min get() = Vector3d(center).sub(halfExtents)
    override val max get() = Vector3d(center).add(halfExtents)
    override val fastCollider: IAABB<MockTarget2>? = null
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

private class MockRay2(
    private val _origin: Vector3d, override var direction: Vector3d, override var length: Double
) : IRay<MockTarget2>, MockTarget2 {
    private var disabled = false
    override val origin get() = Vector3d(_origin)
    override val end get() = Vector3d(_origin).add(Vector3d(direction).mul(length))
    override val fastCollider: IAABB<MockTarget2>? = null
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

private class MockOBB2(
    override var center: Vector3d, override var halfExtents: Vector3d,
    override var rotation: Quaterniond = Quaterniond()
) : IOBB<MockTarget2>, MockTarget2 {
    private var disabled = false
    override val fastCollider: IAABB<MockTarget2>? = null
    override val vertices: Array<Vector3d> get() {
        val hx = halfExtents.x; val hy = halfExtents.y; val hz = halfExtents.z
        val localVertices = arrayOf(
            Vector3d(-hx, -hy, -hz), Vector3d(hx, -hy, -hz),
            Vector3d(hx, hy, -hz),   Vector3d(-hx, hy, -hz),
            Vector3d(-hx, -hy, hz),  Vector3d(hx, -hy, hz),
            Vector3d(hx, hy, hz),    Vector3d(-hx, hy, hz)
        )
        return Array(8) { i -> localVertices[i].rotate(rotation).add(center) }
    }
    override val axes: Array<Vector3d> get() = arrayOf(
        rotation.transform(Vector3d(1.0, 0.0, 0.0)),
        rotation.transform(Vector3d(0.0, 1.0, 0.0)),
        rotation.transform(Vector3d(0.0, 0.0, 1.0))
    )
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

private class MockComposite(
    private val colliders: MutableList<ICollider<MockTarget2>> = mutableListOf()
) : IComposite<MockTarget2, ICollider<MockTarget2>>, MockTarget2 {
    private var disabled = false
    override val collidersCount get() = colliders.size
    override fun getCollider(index: Int) = colliders[index]
    override fun setCollider(index: Int, collider: ICollider<MockTarget2>) { colliders[index] = collider }
    override fun addCollider(collider: ICollider<MockTarget2>) { colliders.add(collider) }
    override fun removeCollider(index: Int) { colliders.removeAt(index) }
    override val fastCollider: IAABB<MockTarget2>? = null
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

class ColliderExtraTest {

    @Nested
    inner class RayRay {
        @Test
        fun `crossing rays`() {
            val ray1 = MockRay2(Vector3d(0.0, 0.0, 0.0), Vector3d(1.0, 0.0, 0.0), 10.0)
            val ray2 = MockRay2(Vector3d(5.0, -5.0, 0.0), Vector3d(0.0, 1.0, 0.0), 10.0)
            assertTrue(isColliding(ray1, ray2))
        }

        @Test
        fun `parallel rays no collision`() {
            val ray1 = MockRay2(Vector3d(0.0, 0.0, 0.0), Vector3d(1.0, 0.0, 0.0), 10.0)
            val ray2 = MockRay2(Vector3d(0.0, 5.0, 0.0), Vector3d(1.0, 0.0, 0.0), 10.0)
            assertFalse(isColliding(ray1, ray2))
        }

        @Test
        fun `non-intersecting rays`() {
            val ray1 = MockRay2(Vector3d(0.0, 0.0, 0.0), Vector3d(1.0, 0.0, 0.0), 3.0)
            val ray2 = MockRay2(Vector3d(5.0, -5.0, 0.0), Vector3d(0.0, 1.0, 0.0), 3.0)
            // ray1 ends at (3,0,0), ray2 goes from (5,-5,0) to (5,-2,0) — no crossing
            assertFalse(isColliding(ray1, ray2))
        }
    }

    @Nested
    inner class CompositeCollision {
        @Test
        fun `composite with single sphere hits`() {
            val sphere = MockSphere2(Vector3d(0.0, 0.0, 0.0), 2.0)
            val composite = MockComposite(mutableListOf(sphere))
            val target = MockSphere2(Vector3d(1.0, 0.0, 0.0), 1.0)
            assertTrue(isColliding(composite, target))
        }

        @Test
        fun `composite with no colliders misses`() {
            val composite = MockComposite()
            val target = MockSphere2(Vector3d(0.0, 0.0, 0.0), 1.0)
            assertFalse(isColliding(composite, target))
        }

        @Test
        fun `composite with multiple colliders - one hits`() {
            val farSphere = MockSphere2(Vector3d(100.0, 0.0, 0.0), 1.0)
            val nearSphere = MockSphere2(Vector3d(0.0, 0.0, 0.0), 2.0)
            val composite = MockComposite(mutableListOf(farSphere, nearSphere))
            val target = MockSphere2(Vector3d(1.0, 0.0, 0.0), 1.0)
            assertTrue(isColliding(composite, target))
        }

        @Test
        fun `composite with all misses`() {
            val far1 = MockSphere2(Vector3d(100.0, 0.0, 0.0), 1.0)
            val far2 = MockSphere2(Vector3d(-100.0, 0.0, 0.0), 1.0)
            val composite = MockComposite(mutableListOf(far1, far2))
            val target = MockSphere2(Vector3d(0.0, 0.0, 0.0), 1.0)
            assertFalse(isColliding(composite, target))
        }

        @Test
        fun `dispatch composite via colliding function`() {
            val sphere = MockSphere2(Vector3d(0.0, 0.0, 0.0), 2.0)
            val composite: ICollider<MockTarget2> = MockComposite(mutableListOf(sphere))
            val target: ICollider<MockTarget2> = MockSphere2(Vector3d(1.0, 0.0, 0.0), 1.0)
            assertTrue(colliding(composite, target))
        }

        @Test
        fun `dispatch other vs composite via colliding function`() {
            val sphere = MockSphere2(Vector3d(0.0, 0.0, 0.0), 2.0)
            val composite: ICollider<MockTarget2> = MockComposite(mutableListOf(sphere))
            val target: ICollider<MockTarget2> = MockAABB2(Vector3d(1.0, 0.0, 0.0), Vector3d(1.0, 1.0, 1.0))
            assertTrue(colliding(target, composite))
        }
    }

    @Nested
    inner class GetClosestPointOBBDirect {
        @Test
        fun `point at center returns center`() {
            val obb = MockOBB2(Vector3d(3.0, 3.0, 3.0), Vector3d(1.0, 1.0, 1.0))
            val point = Vector3d(3.0, 3.0, 3.0)
            val closest = getClosestPointOBB(point, obb)
            assertEquals(3.0, closest.x, 1e-9)
            assertEquals(3.0, closest.y, 1e-9)
            assertEquals(3.0, closest.z, 1e-9)
        }

        @Test
        fun `point inside OBB returns itself`() {
            val obb = MockOBB2(Vector3d(0.0, 0.0, 0.0), Vector3d(5.0, 5.0, 5.0))
            val point = Vector3d(1.0, 1.0, 1.0)
            val closest = getClosestPointOBB(point, obb)
            assertEquals(1.0, closest.x, 1e-9)
            assertEquals(1.0, closest.y, 1e-9)
            assertEquals(1.0, closest.z, 1e-9)
        }
    }

    @Nested
    inner class NoneType {
        @Test
        fun `NONE type collider returns false`() {
            val none = object : ICollider<MockTarget2>, MockTarget2 {
                override val type = ColliderType.NONE
                override val fastCollider: IAABB<MockTarget2>? = null
                private var disabled = false
                override fun setDisable(d: Boolean) { disabled = d }
                override fun disable() = disabled
            }
            val sphere: ICollider<MockTarget2> = MockSphere2(Vector3d(0.0, 0.0, 0.0), 1.0)
            assertFalse(colliding(none, sphere))
            assertFalse(colliding(sphere, none))
        }
    }
}
