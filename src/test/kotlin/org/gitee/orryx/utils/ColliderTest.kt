package org.gitee.orryx.utils

import org.gitee.orryx.api.collider.*
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private interface MockTarget : ITargetLocation<Unit> {
    override fun getSource() = Unit
    override val world get() = throw UnsupportedOperationException()
    override val location get() = throw UnsupportedOperationException()
    override val eyeLocation get() = throw UnsupportedOperationException()
}

private class MockSphere(
    override var center: Vector3d, override var radius: Double
) : ISphere<MockTarget>, MockTarget {
    private var disabled = false
    override val fastCollider: IAABB<MockTarget>? = null
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

private class MockAABB(
    override var center: Vector3d, override var halfExtents: Vector3d
) : IAABB<MockTarget>, MockTarget {
    private var disabled = false
    override val min get() = Vector3d(center).sub(halfExtents)
    override val max get() = Vector3d(center).add(halfExtents)
    override val fastCollider: IAABB<MockTarget>? = null
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

private class MockOBB(
    override var center: Vector3d, override var halfExtents: Vector3d,
    override var rotation: Quaterniond = Quaterniond()
) : IOBB<MockTarget>, MockTarget {
    private var disabled = false
    override val fastCollider: IAABB<MockTarget>? = null
    override val vertices: Array<Vector3d> get() {
        val hx = halfExtents.x; val hy = halfExtents.y; val hz = halfExtents.z
        return arrayOf(
            Vector3d(-hx, -hy, -hz), Vector3d(hx, -hy, -hz),
            Vector3d(hx, hy, -hz),   Vector3d(-hx, hy, -hz),
            Vector3d(-hx, -hy, hz),  Vector3d(hx, -hy, hz),
            Vector3d(hx, hy, hz),    Vector3d(-hx, hy, hz)
        ).map { it.rotate(rotation).add(center) }.toTypedArray()
    }
    override val axes: Array<Vector3d> get() = arrayOf(
        Vector3d(1.0, 0.0, 0.0).rotate(rotation),
        Vector3d(0.0, 1.0, 0.0).rotate(rotation),
        Vector3d(0.0, 0.0, 1.0).rotate(rotation)
    )
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

private class MockRay(
    private val _origin: Vector3d, override var direction: Vector3d, override var length: Double
) : IRay<MockTarget>, MockTarget {
    private var disabled = false
    override val origin get() = Vector3d(_origin)
    override val end get() = Vector3d(_origin).add(Vector3d(direction).mul(length))
    override val fastCollider: IAABB<MockTarget>? = null
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

private class MockCapsule(
    override var center: Vector3d, override var radius: Double,
    override var height: Double, override var rotation: Quaterniond = Quaterniond()
) : ICapsule<MockTarget>, MockTarget {
    private var disabled = false
    override val direction: Vector3d get() = rotation.transform(Vector3d(0.0, 1.0, 0.0))
    override val fastCollider: IAABB<MockTarget>? = null
    override fun setDisable(d: Boolean) { disabled = d }
    override fun disable() = disabled
}

class ColliderTest {

    // --- Sphere vs Sphere ---
    @Nested inner class SphereSphere {
        @Test fun `overlapping`() {
            assertTrue(isColliding(MockSphere(Vector3d(0.0,0.0,0.0),1.0), MockSphere(Vector3d(1.0,0.0,0.0),1.0)))
        }
        @Test fun `touching`() {
            assertTrue(isColliding(MockSphere(Vector3d(0.0,0.0,0.0),1.0), MockSphere(Vector3d(2.0,0.0,0.0),1.0)))
        }
        @Test fun `separated`() {
            assertFalse(isColliding(MockSphere(Vector3d(0.0,0.0,0.0),1.0), MockSphere(Vector3d(3.0,0.0,0.0),1.0)))
        }
        @Test fun `concentric different radius`() {
            assertTrue(isColliding(MockSphere(Vector3d(0.0,0.0,0.0),2.0), MockSphere(Vector3d(0.1,0.0,0.0),1.0)))
        }
    }

    // --- Sphere vs AABB ---
    @Nested inner class SphereAABB {
        @Test fun `inside`() {
            assertTrue(isColliding(MockSphere(Vector3d(0.0,0.0,0.0),0.5), MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(2.0,2.0,2.0))))
        }
        @Test fun `outside`() {
            assertFalse(isColliding(MockSphere(Vector3d(5.0,5.0,5.0),0.5), MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `touching edge`() {
            assertTrue(isColliding(MockSphere(Vector3d(2.0,0.0,0.0),1.0), MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
    }

    // --- AABB vs AABB ---
    @Nested inner class AABBAABB {
        @Test fun `overlapping`() {
            assertTrue(isColliding(MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0)), MockAABB(Vector3d(1.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `separated`() {
            assertFalse(isColliding(MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0)), MockAABB(Vector3d(5.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `touching`() {
            assertTrue(isColliding(MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0)), MockAABB(Vector3d(2.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
    }

    // --- Sphere vs OBB ---
    @Nested inner class SphereOBB {
        @Test fun `inside`() {
            assertTrue(isColliding(MockSphere(Vector3d(0.0,0.0,0.0),0.5), MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(2.0,2.0,2.0))))
        }
        @Test fun `outside`() {
            assertFalse(isColliding(MockSphere(Vector3d(10.0,10.0,10.0),0.5), MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
    }

    // --- OBB vs OBB ---
    @Nested inner class OBBOBB {
        @Test fun `overlapping`() {
            assertTrue(isColliding(MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0)), MockOBB(Vector3d(1.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `separated`() {
            assertFalse(isColliding(MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0)), MockOBB(Vector3d(5.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `rotated overlap`() {
            val rot = Quaterniond().rotateY(Math.toRadians(45.0))
            assertTrue(isColliding(MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0)), MockOBB(Vector3d(1.5,0.0,0.0),Vector3d(1.0,1.0,1.0),rot)))
        }
    }

    // --- OBB vs AABB ---
    @Nested inner class OBBAABB {
        @Test fun `collision`() {
            assertTrue(isColliding(MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0)), MockAABB(Vector3d(1.5,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `no collision`() {
            assertFalse(isColliding(MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0)), MockAABB(Vector3d(5.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
    }

    // --- Ray vs Sphere ---
    @Nested inner class RaySphere {
        @Test fun `through center`() {
            assertTrue(isColliding(MockRay(Vector3d(-5.0,0.0,0.0),Vector3d(1.0,0.0,0.0),10.0), MockSphere(Vector3d(0.0,0.0,0.0),1.0)))
        }
        @Test fun `miss`() {
            assertFalse(isColliding(MockRay(Vector3d(-5.0,5.0,0.0),Vector3d(1.0,0.0,0.0),10.0), MockSphere(Vector3d(0.0,0.0,0.0),1.0)))
        }
        @Test fun `too short`() {
            assertFalse(isColliding(MockRay(Vector3d(-5.0,0.0,0.0),Vector3d(1.0,0.0,0.0),2.0), MockSphere(Vector3d(0.0,0.0,0.0),1.0)))
        }
    }

    // --- Ray vs AABB ---
    @Nested inner class RayAABB {
        @Test fun `through`() {
            assertTrue(isColliding(MockRay(Vector3d(-5.0,0.0,0.0),Vector3d(1.0,0.0,0.0),10.0), MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `miss`() {
            assertFalse(isColliding(MockRay(Vector3d(-5.0,5.0,0.0),Vector3d(1.0,0.0,0.0),10.0), MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
    }

    // --- Ray vs OBB ---
    @Nested inner class RayOBB {
        @Test fun `through`() {
            assertTrue(isColliding(MockRay(Vector3d(-5.0,0.0,0.0),Vector3d(1.0,0.0,0.0),10.0), MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `miss`() {
            assertFalse(isColliding(MockRay(Vector3d(-5.0,5.0,0.0),Vector3d(1.0,0.0,0.0),10.0), MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `origin inside`() {
            assertTrue(isColliding(MockRay(Vector3d(0.0,0.0,0.0),Vector3d(1.0,0.0,0.0),5.0), MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(2.0,2.0,2.0))))
        }
        @Test fun `backward`() {
            assertFalse(isColliding(MockRay(Vector3d(-5.0,0.0,0.0),Vector3d(-1.0,0.0,0.0),10.0), MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
    }

    // --- Capsule tests ---
    @Nested inner class Capsule {
        @Test fun `capsule vs capsule overlap`() {
            assertTrue(isColliding(MockCapsule(Vector3d(0.0,0.0,0.0),1.0,4.0), MockCapsule(Vector3d(1.0,0.0,0.0),1.0,4.0)))
        }
        @Test fun `capsule vs capsule separated`() {
            assertFalse(isColliding(MockCapsule(Vector3d(0.0,0.0,0.0),0.5,2.0), MockCapsule(Vector3d(5.0,0.0,0.0),0.5,2.0)))
        }
        @Test fun `capsule vs sphere overlap`() {
            assertTrue(isColliding(MockCapsule(Vector3d(0.0,0.0,0.0),1.0,4.0), MockSphere(Vector3d(1.5,0.0,0.0),1.0)))
        }
        @Test fun `capsule vs sphere separated`() {
            assertFalse(isColliding(MockCapsule(Vector3d(0.0,0.0,0.0),0.5,2.0), MockSphere(Vector3d(5.0,0.0,0.0),0.5)))
        }
        @Test fun `capsule vs OBB overlap`() {
            assertTrue(isColliding(MockCapsule(Vector3d(0.0,0.0,0.0),1.0,4.0), MockOBB(Vector3d(1.5,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `capsule vs AABB overlap`() {
            assertTrue(isColliding(MockCapsule(Vector3d(0.0,0.0,0.0),1.0,4.0), MockAABB(Vector3d(1.5,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `capsule vs AABB separated`() {
            assertFalse(isColliding(MockCapsule(Vector3d(0.0,0.0,0.0),0.5,2.0), MockAABB(Vector3d(5.0,0.0,0.0),Vector3d(1.0,1.0,1.0))))
        }
        @Test fun `ray vs capsule hit`() {
            assertTrue(isColliding(MockRay(Vector3d(-5.0,0.0,0.0),Vector3d(1.0,0.0,0.0),10.0), MockCapsule(Vector3d(0.0,0.0,0.0),1.0,4.0)))
        }
        @Test fun `ray vs capsule miss`() {
            assertFalse(isColliding(MockRay(Vector3d(-5.0,10.0,0.0),Vector3d(1.0,0.0,0.0),10.0), MockCapsule(Vector3d(0.0,0.0,0.0),1.0,4.0)))
        }
    }

    // --- Utility functions ---
    @Nested inner class UtilFunctions {
        @Test fun `closest point on segment - before start`() {
            val c = getClosestPointOnSegment(Vector3d(0.0,0.0,0.0), Vector3d(10.0,0.0,0.0), Vector3d(-5.0,0.0,0.0))
            assertEquals(0.0, c.x, 1e-9); assertEquals(0.0, c.y, 1e-9); assertEquals(0.0, c.z, 1e-9)
        }
        @Test fun `closest point on segment - after end`() {
            val c = getClosestPointOnSegment(Vector3d(0.0,0.0,0.0), Vector3d(10.0,0.0,0.0), Vector3d(15.0,0.0,0.0))
            assertEquals(10.0, c.x, 1e-9)
        }
        @Test fun `closest point on segment - perpendicular`() {
            val c = getClosestPointOnSegment(Vector3d(0.0,0.0,0.0), Vector3d(10.0,0.0,0.0), Vector3d(5.0,3.0,0.0))
            assertEquals(5.0, c.x, 1e-9); assertEquals(0.0, c.y, 1e-9)
        }
        @Test fun `segment distance - parallel`() {
            assertEquals(9.0, getClosestDistanceBetweenSegmentsSqr(
                Vector3d(0.0,0.0,0.0), Vector3d(10.0,0.0,0.0),
                Vector3d(0.0,3.0,0.0), Vector3d(10.0,3.0,0.0)), 1e-6)
        }
        @Test fun `segment distance - intersecting`() {
            assertEquals(0.0, getClosestDistanceBetweenSegmentsSqr(
                Vector3d(0.0,0.0,0.0), Vector3d(10.0,0.0,0.0),
                Vector3d(5.0,-5.0,0.0), Vector3d(5.0,5.0,0.0)), 1e-6)
        }
        @Test fun `segment distance - skew`() {
            assertEquals(9.0, getClosestDistanceBetweenSegmentsSqr(
                Vector3d(0.0,0.0,0.0), Vector3d(10.0,0.0,0.0),
                Vector3d(5.0,0.0,3.0), Vector3d(5.0,10.0,3.0)), 1e-6)
        }
    }

    // --- Dispatch & edge cases ---
    @Nested inner class Dispatch {
        @Test fun `disabled collider`() {
            val a = MockSphere(Vector3d(0.0,0.0,0.0),1.0); a.setDisable(true)
            assertFalse(colliding(a, MockSphere(Vector3d(0.5,0.0,0.0),1.0)))
        }
        @Test fun `null other`() {
            val other: ICollider<MockTarget>? = null
            assertFalse(colliding(MockSphere(Vector3d(0.0,0.0,0.0),1.0), other))
        }
        @Test fun `same reference`() {
            val a = MockSphere(Vector3d(0.0,0.0,0.0),1.0)
            assertTrue(colliding(a, a))
        }
        @Test fun `dispatch sphere vs aabb`() {
            val a: ICollider<MockTarget> = MockSphere(Vector3d(0.0,0.0,0.0),1.0)
            val b: ICollider<MockTarget> = MockAABB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))
            assertTrue(colliding(a, b))
        }
        @Test fun `dispatch ray vs obb`() {
            val a: ICollider<MockTarget> = MockRay(Vector3d(-5.0,0.0,0.0),Vector3d(1.0,0.0,0.0),10.0)
            val b: ICollider<MockTarget> = MockOBB(Vector3d(0.0,0.0,0.0),Vector3d(1.0,1.0,1.0))
            assertTrue(colliding(a, b))
        }
        @Test fun `dispatch capsule vs capsule`() {
            val a: ICollider<MockTarget> = MockCapsule(Vector3d(0.0,0.0,0.0),1.0,4.0)
            val b: ICollider<MockTarget> = MockCapsule(Vector3d(1.0,0.0,0.0),1.0,4.0)
            assertTrue(colliding(a, b))
        }
    }
}
