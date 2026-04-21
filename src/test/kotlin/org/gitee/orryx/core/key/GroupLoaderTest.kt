package org.gitee.orryx.core.key

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GroupLoaderTest {

    @Nested
    inner class BasicProperties {
        @Test
        fun `key property returns constructor value`() {
            val group = GroupLoader("main")
            assertEquals("main", group.key)
        }

        @Test
        fun `implements IGroup`() {
            val group: IGroup = GroupLoader("test")
            assertEquals("test", group.key)
        }
    }

    @Nested
    inner class Equality {
        @Test
        fun `same key equals`() {
            val a = GroupLoader("combat")
            val b = GroupLoader("combat")
            assertEquals(a, b)
        }

        @Test
        fun `different key not equals`() {
            val a = GroupLoader("combat")
            val b = GroupLoader("magic")
            assertNotEquals(a, b)
        }

        @Test
        fun `equals self`() {
            val group = GroupLoader("test")
            assertEquals(group, group)
        }

        @Test
        fun `not equals null`() {
            val group = GroupLoader("test")
            assertNotEquals(group, null)
        }

        @Test
        fun `not equals other type`() {
            val group = GroupLoader("test")
            assertNotEquals(group, "test")
        }

        @Test
        fun `not equals non-GroupLoader`() {
            val group = GroupLoader("test")
            assertFalse(group.equals(42))
        }
    }

    @Nested
    inner class HashCode {
        @Test
        fun `same key same hashCode`() {
            val a = GroupLoader("combat")
            val b = GroupLoader("combat")
            assertEquals(a.hashCode(), b.hashCode())
        }

        @Test
        fun `different key likely different hashCode`() {
            val a = GroupLoader("combat")
            val b = GroupLoader("magic")
            assertNotEquals(a.hashCode(), b.hashCode())
        }

        @Test
        fun `hashCode equals key hashCode`() {
            val group = GroupLoader("test")
            assertEquals("test".hashCode(), group.hashCode())
        }
    }

    @Nested
    inner class ToString {
        @Test
        fun `toString format`() {
            val group = GroupLoader("main")
            assertEquals("GroupLoader(key=main)", group.toString())
        }

        @Test
        fun `toString with empty key`() {
            val group = GroupLoader("")
            assertEquals("GroupLoader(key=)", group.toString())
        }

        @Test
        fun `toString with special characters`() {
            val group = GroupLoader("combat-1")
            assertEquals("GroupLoader(key=combat-1)", group.toString())
        }
    }

    @Nested
    inner class SetBehavior {
        @Test
        fun `can be used in Set with deduplication`() {
            val set = setOf(GroupLoader("a"), GroupLoader("a"), GroupLoader("b"))
            assertEquals(2, set.size)
        }

        @Test
        fun `can be used as Map key`() {
            val map = mapOf(GroupLoader("main") to 1, GroupLoader("alt") to 2)
            assertEquals(1, map[GroupLoader("main")])
            assertEquals(2, map[GroupLoader("alt")])
        }
    }
}
