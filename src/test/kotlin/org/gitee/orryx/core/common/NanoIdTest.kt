package org.gitee.orryx.core.common

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

class NanoIdTest {

    @Test
    fun `generate returns string of default length 21`() {
        val id = NanoId.generate()
        assertEquals(21, id.length)
    }

    @Test
    fun `generate returns string of custom length`() {
        for (size in listOf(1, 5, 10, 50, 100)) {
            val id = NanoId.generate(size = size)
            assertEquals(size, id.length, "Expected length $size")
        }
    }

    @Test
    fun `generate uses only characters from alphabet`() {
        val alphabet = "abc123"
        val id = NanoId.generate(size = 100, alphabet = alphabet)
        assertTrue(id.all { it in alphabet }, "ID contains characters outside alphabet: $id")
    }

    @Test
    fun `generate with default alphabet uses valid characters`() {
        val defaultAlphabet = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val id = NanoId.generate(size = 1000)
        assertTrue(id.all { it in defaultAlphabet })
    }

    @Test
    fun `generate produces unique ids`() {
        val ids = (1..10000).map { NanoId.generate() }.toSet()
        assertEquals(10000, ids.size, "Expected all unique IDs")
    }

    @Test
    fun `generate with custom random is deterministic`() {
        val id1 = NanoId.generate(random = Random(42))
        val id2 = NanoId.generate(random = Random(42))
        assertEquals(id1, id2)
    }

    @Test
    fun `generate throws on empty alphabet`() {
        assertThrows<IllegalArgumentException> {
            NanoId.generate(alphabet = "")
        }
    }

    @Test
    fun `generate throws on alphabet too large`() {
        val bigAlphabet = (0..255).map { it.toChar() }.joinToString("")
        assertThrows<IllegalArgumentException> {
            NanoId.generate(alphabet = bigAlphabet)
        }
    }

    @Test
    fun `generate throws on zero size`() {
        assertThrows<IllegalArgumentException> {
            NanoId.generate(size = 0)
        }
    }

    @Test
    fun `generate throws on negative size`() {
        assertThrows<IllegalArgumentException> {
            NanoId.generate(size = -1)
        }
    }

    @Test
    fun `generate throws on additionalBytesFactor less than 1`() {
        assertThrows<IllegalArgumentException> {
            NanoId.generate(additionalBytesFactor = 0.5)
        }
    }

    @Test
    fun `calculateMask returns correct mask for various alphabets`() {
        // alphabet size 2 -> mask = 1
        assertEquals(1, NanoId.calculateMask("ab"))
        // alphabet size 4 -> mask = 3
        assertEquals(3, NanoId.calculateMask("abcd"))
        // alphabet size 64 -> mask = 63
        val alpha64 = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        assertEquals(63, NanoId.calculateMask(alpha64))
    }

    @Test
    fun `calculateStep returns positive value`() {
        val step = NanoId.calculateStep(21, "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
        assertTrue(step > 0)
    }

    @Test
    fun `calculateAdditionalBytesFactor returns value gte 1`() {
        val factor = NanoId.calculateAdditionalBytesFactor("abc")
        assertTrue(factor >= 1.0)
    }

    @Test
    fun `round extension function works correctly`() {
        with(NanoId) {
            assertEquals(1.23, 1.234.round(2))
            assertEquals(1.24, 1.235.round(2))
            assertEquals(1.0, 1.0.round(2))
            assertEquals(0.0, 0.0.round(5))
        }
    }

    @Test
    fun `generate is thread safe`() {
        val ids = ConcurrentHashMap.newKeySet<String>()
        val threads = 10
        val idsPerThread = 1000
        val latch = CountDownLatch(threads)

        repeat(threads) {
            Thread {
                repeat(idsPerThread) {
                    ids.add(NanoId.generate())
                }
                latch.countDown()
            }.start()
        }
        latch.await()
        assertEquals(threads * idsPerThread, ids.size, "Concurrent generation should produce unique IDs")
    }

    @Test
    fun `generateOptimized with single char alphabet`() {
        val id = NanoId.generate(size = 10, alphabet = "ab")
        assertEquals(10, id.length)
        assertTrue(id.all { it == 'a' || it == 'b' })
    }
}
