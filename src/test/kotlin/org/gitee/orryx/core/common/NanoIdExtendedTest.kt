package org.gitee.orryx.core.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Random

class NanoIdExtendedTest {

    @Nested
    inner class Distribution {
        @Test
        fun `default alphabet characters are reasonably distributed`() {
            val counts = mutableMapOf<Char, Int>()
            repeat(10000) {
                val id = NanoId.generate(size = 21)
                id.forEach { c ->
                    counts[c] = (counts[c] ?: 0) + 1
                }
            }
            // 64 字符的字母表，210000 个字符，每个字符期望出现 ~3281 次
            // 允许 50% 偏差
            val totalChars = 10000 * 21
            val alphabetSize = 64
            val expected = totalChars.toDouble() / alphabetSize
            counts.values.forEach { count ->
                assertTrue(count > expected * 0.5, "Character appeared too few times: $count (expected ~$expected)")
                assertTrue(count < expected * 1.5, "Character appeared too many times: $count (expected ~$expected)")
            }
        }

        @Test
        fun `small alphabet covers all characters`() {
            val alphabet = "AB"
            val ids = (1..100).map { NanoId.generate(size = 50, alphabet = alphabet) }
            val allChars = ids.flatMap { it.toList() }.toSet()
            assertEquals(setOf('A', 'B'), allChars)
        }
    }

    @Nested
    inner class Determinism {
        @Test
        fun `same seed produces same sequence of IDs`() {
            val ids1 = (1..10).map { NanoId.generate(random = Random(123)) }
            val ids2 = (1..10).map { NanoId.generate(random = Random(123)) }
            assertEquals(ids1, ids2)
        }

        @Test
        fun `different seeds produce different IDs`() {
            val id1 = NanoId.generate(random = Random(1))
            val id2 = NanoId.generate(random = Random(2))
            assertNotEquals(id1, id2)
        }
    }

    @Nested
    inner class EdgeSizes {
        @Test
        fun `size 1 produces single character`() {
            val id = NanoId.generate(size = 1)
            assertEquals(1, id.length)
        }

        @Test
        fun `size 256 produces correct length`() {
            val id = NanoId.generate(size = 256)
            assertEquals(256, id.length)
        }

        @Test
        fun `size 1000 produces correct length`() {
            val id = NanoId.generate(size = 1000)
            assertEquals(1000, id.length)
        }
    }

    @Nested
    inner class CustomAlphabet {
        @Test
        fun `numeric only alphabet`() {
            val id = NanoId.generate(size = 100, alphabet = "0123456789")
            assertTrue(id.all { it.isDigit() })
        }

        @Test
        fun `hex alphabet`() {
            val hexAlphabet = "0123456789abcdef"
            val id = NanoId.generate(size = 100, alphabet = hexAlphabet)
            assertTrue(id.all { it in hexAlphabet })
        }

        @Test
        fun `two character alphabet`() {
            val id = NanoId.generate(size = 10, alphabet = "XY")
            assertTrue(id.all { it == 'X' || it == 'Y' })
            assertEquals(10, id.length)
        }

        @Test
        fun `alphabet with special characters`() {
            val alphabet = "!@#$%"
            val id = NanoId.generate(size = 50, alphabet = alphabet)
            assertTrue(id.all { it in alphabet })
        }
    }

    @Nested
    inner class Uniqueness {
        @Test
        fun `100000 IDs are all unique`() {
            val ids = (1..100_000).map { NanoId.generate() }.toSet()
            assertEquals(100_000, ids.size)
        }

        @Test
        fun `short IDs with large alphabet still unique in small batch`() {
            val ids = (1..100).map { NanoId.generate(size = 10) }.toSet()
            assertEquals(100, ids.size)
        }
    }

    @Nested
    inner class CalculationFunctions {
        @Test
        fun `calculateMask for power of 2 alphabet`() {
            // 64 字符 => mask = 63
            val alphabet64 = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val mask = NanoId.calculateMask(alphabet64)
            assertEquals(63, mask)
        }

        @Test
        fun `calculateMask for non-power of 2`() {
            // 10 字符 => 最近的 2^n - 1 >= 9 => 15
            val mask = NanoId.calculateMask("0123456789")
            assertEquals(15, mask)
        }

        @Test
        fun `calculateStep returns positive value`() {
            val alphabet64 = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val step = NanoId.calculateStep(21, alphabet64)
            assertTrue(step > 0)
        }

        @Test
        fun `calculateAdditionalBytesFactor returns at least 1`() {
            val alphabet64 = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val factor = NanoId.calculateAdditionalBytesFactor(alphabet64)
            assertTrue(factor >= 1)
        }
    }
}
