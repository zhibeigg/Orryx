package org.gitee.orryx.api.collider

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ColliderTypeTest {

    @Test
    fun `all types exist`() {
        val expected = setOf("OBB", "SPHERE", "CAPSULE", "AABB", "RAY", "COMPOSITE", "NONE")
        assertEquals(expected, ColliderType.entries.map { it.name }.toSet())
    }

    @Test
    fun `count is 7`() {
        assertEquals(7, ColliderType.entries.size)
    }

    @Test
    fun `valueOf roundtrip`() {
        ColliderType.entries.forEach {
            assertEquals(it, ColliderType.valueOf(it.name))
        }
    }
}
