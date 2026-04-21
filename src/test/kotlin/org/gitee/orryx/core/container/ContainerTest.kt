package org.gitee.orryx.core.container

import org.gitee.orryx.core.targets.ITarget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private class MockTarget(val id: String) : ITarget<String> {
    override fun getSource() = id
    override fun equals(other: Any?) = other is MockTarget && id == other.id
    override fun hashCode() = id.hashCode()
    override fun toString() = "MockTarget($id)"
}

class ContainerTest {

    private lateinit var container: Container
    private val t1 = MockTarget("a")
    private val t2 = MockTarget("b")
    private val t3 = MockTarget("c")

    @BeforeEach
    fun setup() {
        container = Container()
    }

    @Nested
    inner class BasicOperations {
        @Test
        fun `empty container`() {
            assertTrue(container.targets.isEmpty())
        }

        @Test
        fun `add single target`() {
            container.add(t1)
            assertEquals(1, container.targets.size)
            assertTrue(container.targets.contains(t1))
        }

        @Test
        fun `add duplicate target`() {
            container.add(t1)
            container.add(t1)
            assertEquals(1, container.targets.size)
        }

        @Test
        fun `addAll targets`() {
            container.addAll(listOf(t1, t2, t3))
            assertEquals(3, container.targets.size)
        }

        @Test
        fun `remove target`() {
            container.addAll(listOf(t1, t2))
            container.remove(t1)
            assertEquals(1, container.targets.size)
            assertFalse(container.targets.contains(t1))
        }

        @Test
        fun `remove nonexistent target`() {
            container.add(t1)
            container.remove(t2)
            assertEquals(1, container.targets.size)
        }
    }

    @Nested
    inner class MergeOperations {
        @Test
        fun `merge two containers`() {
            container.add(t1)
            val other = Container()
            other.add(t2)
            container.merge(other)
            assertEquals(2, container.targets.size)
        }

        @Test
        fun `and infix operator`() {
            container.add(t1)
            val other = Container()
            other.add(t2)
            val result = container and other
            assertSame(container, result)
            assertEquals(2, container.targets.size)
        }

        @Test
        fun `remove container`() {
            container.addAll(listOf(t1, t2, t3))
            val toRemove = Container()
            toRemove.addAll(listOf(t1, t3))
            container.remove(toRemove)
            assertEquals(1, container.targets.size)
            assertTrue(container.targets.contains(t2))
        }

        @Test
        fun `mergeIf with predicate`() {
            container.add(t1)
            val other = Container()
            other.addAll(listOf(t2, t3))
            container.mergeIf(other) { (it as MockTarget).id == "b" }
            assertEquals(2, container.targets.size)
            assertTrue(container.targets.contains(t2))
            assertFalse(container.targets.contains(t3))
        }
    }

    @Nested
    inner class QueryOperations {
        @Test
        fun `first returns first element`() {
            container.addAll(listOf(t1, t2))
            assertEquals(t1, container.first())
        }

        @Test
        fun `first on empty throws`() {
            assertThrows(NoSuchElementException::class.java) {
                container.first()
            }
        }

        @Test
        fun `firstOrNull on empty returns null`() {
            assertNull(container.firstOrNull())
        }

        @Test
        fun `firstOrNull returns first`() {
            container.add(t1)
            assertEquals(t1, container.firstOrNull())
        }

        @Test
        fun `take returns subset`() {
            container.addAll(listOf(t1, t2, t3))
            val taken = container.take(2)
            assertEquals(2, taken.size)
        }

        @Test
        fun `drop skips elements`() {
            container.addAll(listOf(t1, t2, t3))
            val dropped = container.drop(1)
            assertEquals(2, dropped.size)
        }

        @Test
        fun `take more than size returns all`() {
            container.add(t1)
            assertEquals(1, container.take(10).size)
        }
    }

    @Nested
    inner class RemoveIfOperation {
        @Test
        fun `removeIf removes matching`() {
            container.addAll(listOf(t1, t2, t3))
            container.removeIf { (it as MockTarget).id == "a" }
            assertEquals(2, container.targets.size)
            assertFalse(container.targets.contains(t1))
        }

        @Test
        fun `removeIf with no match`() {
            container.addAll(listOf(t1, t2))
            container.removeIf { false }
            assertEquals(2, container.targets.size)
        }
    }

    @Nested
    inner class ForeachOperation {
        @Test
        fun `foreach visits all targets`() {
            container.addAll(listOf(t1, t2, t3))
            val visited = mutableListOf<String>()
            container.foreach { visited.add((it as MockTarget).id) }
            assertEquals(3, visited.size)
        }
    }

    @Nested
    inner class CloneAndEquality {
        @Test
        fun `clone creates independent copy`() {
            container.addAll(listOf(t1, t2))
            val cloned = container.clone()
            assertEquals(container, cloned)
            (cloned as Container).add(t3)
            assertEquals(2, container.targets.size)
            assertEquals(3, cloned.targets.size)
        }

        @Test
        fun `equals same targets`() {
            val a = Container(linkedSetOf(t1, t2))
            val b = Container(linkedSetOf(t1, t2))
            assertEquals(a, b)
            assertEquals(a.hashCode(), b.hashCode())
        }

        @Test
        fun `not equals different targets`() {
            val a = Container(linkedSetOf(t1))
            val b = Container(linkedSetOf(t2))
            assertNotEquals(a, b)
        }

        @Test
        fun `equals self`() {
            assertEquals(container, container)
        }

        @Test
        fun `not equals other type`() {
            assertNotEquals(container, "string")
        }
    }

    @Nested
    inner class ToStringTest {
        @Test
        fun `toString format`() {
            container.addAll(listOf(t1, t2))
            val str = container.toString()
            assertTrue(str.startsWith("["))
            assertTrue(str.endsWith("]"))
            assertTrue(str.contains("a"))
            assertTrue(str.contains("b"))
        }

        @Test
        fun `empty toString`() {
            assertEquals("[]", container.toString())
        }
    }

    @Nested
    inner class FluentApi {
        @Test
        fun `add returns same container`() {
            val result = container.add(t1)
            assertSame(container, result)
        }

        @Test
        fun `addAll returns same container`() {
            val result = container.addAll(listOf(t1))
            assertSame(container, result)
        }

        @Test
        fun `remove returns same container`() {
            val result = container.remove(t1)
            assertSame(container, result)
        }

        @Test
        fun `merge returns same container`() {
            val result = container.merge(Container())
            assertSame(container, result)
        }

        @Test
        fun `removeIf returns same container`() {
            val result = container.removeIf { false }
            assertSame(container, result)
        }
    }
}
