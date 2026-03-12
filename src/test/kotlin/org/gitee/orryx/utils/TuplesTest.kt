package org.gitee.orryx.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TuplesTest {

    @Nested
    inner class Tuple2Tests {
        @Test fun `toString format`() {
            assertEquals("(a, 1)", Tuple2("a", 1).toString())
        }

        @Test fun `paired infix creates tuple`() {
            val t = "key" paired 42
            assertEquals("key", t.first)
            assertEquals(42, t.second)
        }

        @Test fun `toList returns both elements`() {
            val list = Tuple2("a", "b").toList()
            assertEquals(listOf("a", "b"), list)
        }

        @Test fun `data class equality`() {
            assertEquals(Tuple2(1, 2), Tuple2(1, 2))
            assertNotEquals(Tuple2(1, 2), Tuple2(1, 3))
        }

        @Test fun `data class copy`() {
            val t = Tuple2("a", 1)
            val t2 = t.copy(second = 2)
            assertEquals(Tuple2("a", 2), t2)
        }

        @Test fun `null values`() {
            val t = Tuple2<String?, Int?>(null, null)
            assertNull(t.first)
            assertNull(t.second)
            assertEquals("(null, null)", t.toString())
        }
    }

    @Nested
    inner class Tuple3Tests {
        @Test fun `toString format`() {
            assertEquals("(a, 1, true)", Tuple3("a", 1, true).toString())
        }

        @Test fun `toList returns all elements`() {
            val list = Tuple3(1, 2, 3).toList()
            assertEquals(listOf(1, 2, 3), list)
        }

        @Test fun `data class equality`() {
            assertEquals(Tuple3(1, 2, 3), Tuple3(1, 2, 3))
            assertNotEquals(Tuple3(1, 2, 3), Tuple3(1, 2, 4))
        }

        @Test fun `destructuring`() {
            val (a, b, c) = Tuple3("x", "y", "z")
            assertEquals("x", a)
            assertEquals("y", b)
            assertEquals("z", c)
        }
    }
}
