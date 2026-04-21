package org.gitee.orryx.utils

import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AABBCollisionTest {

    @Nested
    inner class AABBConstruction {
        @Test
        fun `vector constructor properties`() {
            val aabb = AABB(Vector3d(1.0, 2.0, 3.0), Vector3d(4.0, 5.0, 6.0))
            assertEquals(1.0, aabb.minX)
            assertEquals(2.0, aabb.minY)
            assertEquals(3.0, aabb.minZ)
            assertEquals(4.0, aabb.maxX)
            assertEquals(5.0, aabb.maxY)
            assertEquals(6.0, aabb.maxZ)
        }

        @Test
        fun `double constructor properties`() {
            val aabb = AABB(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
            assertEquals(1.0, aabb.minX)
            assertEquals(2.0, aabb.minY)
            assertEquals(3.0, aabb.minZ)
            assertEquals(4.0, aabb.maxX)
            assertEquals(5.0, aabb.maxY)
            assertEquals(6.0, aabb.maxZ)
        }

        @Test
        fun `data class equality`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(Vector3d(0.0, 0.0, 0.0), Vector3d(1.0, 1.0, 1.0))
            assertEquals(a, b)
        }

        @Test
        fun `data class inequality`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(0.0, 0.0, 0.0, 2.0, 2.0, 2.0)
            assertNotEquals(a, b)
        }

        @Test
        fun `zero-size AABB`() {
            val aabb = AABB(5.0, 5.0, 5.0, 5.0, 5.0, 5.0)
            assertEquals(aabb.minX, aabb.maxX)
            assertEquals(aabb.minY, aabb.maxY)
            assertEquals(aabb.minZ, aabb.maxZ)
        }

        @Test
        fun `negative coordinates`() {
            val aabb = AABB(-3.0, -2.0, -1.0, 0.0, 0.0, 0.0)
            assertEquals(-3.0, aabb.minX)
            assertEquals(-2.0, aabb.minY)
            assertEquals(-1.0, aabb.minZ)
        }
    }

    @Nested
    inner class Collision {
        @Test
        fun `overlapping AABBs collide`() {
            val a = AABB(0.0, 0.0, 0.0, 2.0, 2.0, 2.0)
            val b = AABB(1.0, 1.0, 1.0, 3.0, 3.0, 3.0)
            assertTrue(areAABBsColliding(a, b))
        }

        @Test
        fun `separated AABBs do not collide`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(2.0, 2.0, 2.0, 3.0, 3.0, 3.0)
            assertFalse(areAABBsColliding(a, b))
        }

        @Test
        fun `touching faces collide`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(1.0, 0.0, 0.0, 2.0, 1.0, 1.0)
            assertTrue(areAABBsColliding(a, b))
        }

        @Test
        fun `touching edges collide`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(1.0, 1.0, 0.0, 2.0, 2.0, 1.0)
            assertTrue(areAABBsColliding(a, b))
        }

        @Test
        fun `touching corners collide`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(1.0, 1.0, 1.0, 2.0, 2.0, 2.0)
            assertTrue(areAABBsColliding(a, b))
        }

        @Test
        fun `one contains the other`() {
            val outer = AABB(0.0, 0.0, 0.0, 10.0, 10.0, 10.0)
            val inner = AABB(2.0, 2.0, 2.0, 5.0, 5.0, 5.0)
            assertTrue(areAABBsColliding(outer, inner))
            assertTrue(areAABBsColliding(inner, outer))
        }

        @Test
        fun `same AABB collides with itself`() {
            val aabb = AABB(1.0, 1.0, 1.0, 3.0, 3.0, 3.0)
            assertTrue(areAABBsColliding(aabb, aabb))
        }

        @Test
        fun `separated on X axis only`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(2.0, 0.0, 0.0, 3.0, 1.0, 1.0)
            assertFalse(areAABBsColliding(a, b))
        }

        @Test
        fun `separated on Y axis only`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(0.0, 2.0, 0.0, 1.0, 3.0, 1.0)
            assertFalse(areAABBsColliding(a, b))
        }

        @Test
        fun `separated on Z axis only`() {
            val a = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            val b = AABB(0.0, 0.0, 2.0, 1.0, 1.0, 3.0)
            assertFalse(areAABBsColliding(a, b))
        }

        @Test
        fun `partial overlap on two axes but separated on third`() {
            val a = AABB(0.0, 0.0, 0.0, 2.0, 2.0, 2.0)
            val b = AABB(1.0, 1.0, 3.0, 3.0, 3.0, 5.0)
            assertFalse(areAABBsColliding(a, b))
        }

        @Test
        fun `collision is commutative`() {
            val a = AABB(0.0, 0.0, 0.0, 2.0, 2.0, 2.0)
            val b = AABB(1.0, 1.0, 1.0, 3.0, 3.0, 3.0)
            assertEquals(areAABBsColliding(a, b), areAABBsColliding(b, a))
        }

        @Test
        fun `zero-size AABB inside another`() {
            val point = AABB(1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
            val box = AABB(0.0, 0.0, 0.0, 2.0, 2.0, 2.0)
            assertTrue(areAABBsColliding(point, box))
        }

        @Test
        fun `zero-size AABB outside another`() {
            val point = AABB(5.0, 5.0, 5.0, 5.0, 5.0, 5.0)
            val box = AABB(0.0, 0.0, 0.0, 2.0, 2.0, 2.0)
            assertFalse(areAABBsColliding(point, box))
        }

        @Test
        fun `negative coordinate AABBs`() {
            val a = AABB(-3.0, -3.0, -3.0, -1.0, -1.0, -1.0)
            val b = AABB(-2.0, -2.0, -2.0, 0.0, 0.0, 0.0)
            assertTrue(areAABBsColliding(a, b))
        }

        @Test
        fun `large AABBs`() {
            val a = AABB(-1000.0, -1000.0, -1000.0, 1000.0, 1000.0, 1000.0)
            val b = AABB(999.0, 999.0, 999.0, 2000.0, 2000.0, 2000.0)
            assertTrue(areAABBsColliding(a, b))
        }
    }
}
