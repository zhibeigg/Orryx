package org.gitee.orryx.core.container

import org.gitee.orryx.core.targets.ITarget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private class ExtMockTarget(val id: String) : ITarget<String> {
    override fun getSource() = id
    override fun equals(other: Any?) = other is ExtMockTarget && id == other.id
    override fun hashCode() = id.hashCode()
    override fun toString() = "ExtMockTarget($id)"
}

class ContainerExtendedTest {

    private lateinit var container: Container

    @BeforeEach
    fun setup() {
        container = Container()
    }

    @Nested
    inner class EmptyContainerEdgeCases {
        @Test
        fun `drop on empty returns empty`() {
            assertEquals(0, container.drop(5).size)
        }

        @Test
        fun `take on empty returns empty`() {
            assertEquals(0, container.take(5).size)
        }

        @Test
        fun `foreach on empty does nothing`() {
            var count = 0
            container.foreach { count++ }
            assertEquals(0, count)
        }

        @Test
        fun `removeIf on empty does nothing`() {
            container.removeIf { true }
            assertTrue(container.targets.isEmpty())
        }

        @Test
        fun `merge empty with empty`() {
            container.merge(Container())
            assertTrue(container.targets.isEmpty())
        }

        @Test
        fun `clone empty container`() {
            val cloned = container.clone()
            assertEquals(container, cloned)
            assertTrue((cloned as Container).targets.isEmpty())
        }

        @Test
        fun `remove from empty container`() {
            container.remove(ExtMockTarget("x"))
            assertTrue(container.targets.isEmpty())
        }
    }

    @Nested
    inner class LargeDataSet {
        @Test
        fun `add 1000 unique targets`() {
            val targets = (1..1000).map { ExtMockTarget("t$it") }
            container.addAll(targets)
            assertEquals(1000, container.targets.size)
        }

        @Test
        fun `take preserves order`() {
            val targets = (1..100).map { ExtMockTarget("t$it") }
            container.addAll(targets)
            val taken = container.take(5)
            assertEquals(5, taken.size)
            assertEquals("t1", (taken[0] as ExtMockTarget).id)
            assertEquals("t5", (taken[4] as ExtMockTarget).id)
        }

        @Test
        fun `drop preserves remaining order`() {
            val targets = (1..10).map { ExtMockTarget("t$it") }
            container.addAll(targets)
            val dropped = container.drop(8)
            assertEquals(2, dropped.size)
            assertEquals("t9", (dropped[0] as ExtMockTarget).id)
            assertEquals("t10", (dropped[1] as ExtMockTarget).id)
        }
    }

    @Nested
    inner class MergeIfEdgeCases {
        @Test
        fun `mergeIf with always-false predicate adds nothing`() {
            container.add(ExtMockTarget("a"))
            val other = Container()
            other.addAll(listOf(ExtMockTarget("b"), ExtMockTarget("c")))
            container.mergeIf(other) { false }
            assertEquals(1, container.targets.size)
        }

        @Test
        fun `mergeIf with always-true predicate adds all`() {
            val other = Container()
            other.addAll(listOf(ExtMockTarget("a"), ExtMockTarget("b")))
            container.mergeIf(other) { true }
            assertEquals(2, container.targets.size)
        }

        @Test
        fun `mergeIf does not add duplicates`() {
            container.add(ExtMockTarget("a"))
            val other = Container()
            other.add(ExtMockTarget("a"))
            container.mergeIf(other) { true }
            assertEquals(1, container.targets.size)
        }
    }

    @Nested
    inner class RemoveIfEdgeCases {
        @Test
        fun `removeIf all`() {
            container.addAll(listOf(ExtMockTarget("a"), ExtMockTarget("b"), ExtMockTarget("c")))
            container.removeIf { true }
            assertTrue(container.targets.isEmpty())
        }

        @Test
        fun `removeIf none`() {
            container.addAll(listOf(ExtMockTarget("a"), ExtMockTarget("b")))
            container.removeIf { false }
            assertEquals(2, container.targets.size)
        }
    }

    @Nested
    inner class CloneIndependence {
        @Test
        fun `modifying clone does not affect original`() {
            container.addAll(listOf(ExtMockTarget("a"), ExtMockTarget("b")))
            val cloned = container.clone() as Container
            cloned.remove(ExtMockTarget("a"))
            assertEquals(2, container.targets.size)
            assertEquals(1, cloned.targets.size)
        }

        @Test
        fun `modifying original does not affect clone`() {
            container.add(ExtMockTarget("a"))
            val cloned = container.clone() as Container
            container.add(ExtMockTarget("b"))
            assertEquals(2, container.targets.size)
            assertEquals(1, cloned.targets.size)
        }
    }

    @Nested
    inner class DropAndTakeBoundary {
        @Test
        fun `take 0 returns empty`() {
            container.add(ExtMockTarget("a"))
            assertEquals(0, container.take(0).size)
        }

        @Test
        fun `drop 0 returns all`() {
            container.addAll(listOf(ExtMockTarget("a"), ExtMockTarget("b")))
            assertEquals(2, container.drop(0).size)
        }

        @Test
        fun `drop all returns empty`() {
            container.addAll(listOf(ExtMockTarget("a"), ExtMockTarget("b")))
            assertEquals(0, container.drop(2).size)
        }

        @Test
        fun `drop more than size returns empty`() {
            container.add(ExtMockTarget("a"))
            assertEquals(0, container.drop(100).size)
        }
    }
}
