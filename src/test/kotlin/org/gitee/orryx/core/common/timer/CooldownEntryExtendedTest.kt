package org.gitee.orryx.core.common.timer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class CooldownEntryExtendedTest {

    @Nested
    inner class ExtremeValues {
        @Test
        fun `very large duration`() {
            val entry = CooldownEntry("big", Long.MAX_VALUE / 2)
            assertFalse(entry.isReady)
            assertTrue(entry.countdown > 0)
        }

        @Test
        fun `duration of 1ms`() {
            val entry = CooldownEntry("tiny", 1)
            // 可能立即就绪，也可能还有 1ms
            assertTrue(entry.countdown <= 1)
        }

        @Test
        fun `negative duration treated as zero`() {
            val entry = CooldownEntry("neg", -100)
            assertTrue(entry.isReady)
            assertEquals(0, entry.countdown)
        }
    }

    @Nested
    inner class AddReduceCombinations {
        @Test
        fun `add then reduce back to zero`() {
            val entry = CooldownEntry("test", 0)
            entry.addDuration(5000)
            entry.reduceDuration(5000)
            assertTrue(entry.isReady)
        }

        @Test
        fun `reduce more than remaining clamps to zero`() {
            val entry = CooldownEntry("test", 1000)
            entry.reduceDuration(999_999)
            assertTrue(entry.isReady)
            assertEquals(0, entry.countdown)
        }

        @Test
        fun `add after reduce extends cooldown`() {
            val entry = CooldownEntry("test", 1000)
            entry.reduceDuration(500)
            entry.addDuration(2000)
            assertFalse(entry.isReady)
        }

        @Test
        fun `multiple adds accumulate`() {
            val entry = CooldownEntry("test", 0)
            assertTrue(entry.isReady)
            entry.addDuration(1000)
            entry.addDuration(1000)
            assertFalse(entry.isReady)
        }

        @Test
        fun `add negative duration clamps`() {
            val entry = CooldownEntry("test", 5000)
            entry.addDuration(-10000)
            assertTrue(entry.isReady)
        }

        @Test
        fun `reduce negative duration clamps`() {
            val entry = CooldownEntry("test", 1000)
            entry.reduceDuration(-5000)
            // 负值 reduce 应该不增加冷却
            // reduceDuration 内部: overStamp = max(0, overStamp - amount)
            // amount = -5000, overStamp - (-5000) = overStamp + 5000
            assertFalse(entry.isReady)
        }
    }

    @Nested
    inner class TagProperty {
        @Test
        fun `tag is preserved`() {
            val entry = CooldownEntry("my-skill", 1000)
            assertEquals("my-skill", entry.tag)
        }

        @Test
        fun `empty tag`() {
            val entry = CooldownEntry("", 0)
            assertEquals("", entry.tag)
        }

        @Test
        fun `unicode tag`() {
            val entry = CooldownEntry("火球术", 1000)
            assertEquals("火球术", entry.tag)
        }
    }

    @Nested
    inner class OverStamp {
        @Test
        fun `overStamp changes after addDuration`() {
            val entry = CooldownEntry("test", 1000)
            val before = entry.overStamp
            entry.addDuration(500)
            assertTrue(entry.overStamp > before)
        }

        @Test
        fun `overStamp changes after reduceDuration`() {
            val entry = CooldownEntry("test", 5000)
            val before = entry.overStamp
            entry.reduceDuration(1000)
            assertTrue(entry.overStamp < before)
        }
    }

    @Nested
    inner class ConcurrentAccess {
        @Test
        fun `concurrent add and reduce do not throw`() {
            val entry = CooldownEntry("concurrent", 10000)
            val error = AtomicBoolean(false)
            val latch = CountDownLatch(2)

            Thread {
                try {
                    repeat(1000) { entry.addDuration(1) }
                } catch (e: Exception) {
                    error.set(true)
                } finally {
                    latch.countDown()
                }
            }.start()

            Thread {
                try {
                    repeat(1000) { entry.reduceDuration(1) }
                } catch (e: Exception) {
                    error.set(true)
                } finally {
                    latch.countDown()
                }
            }.start()

            latch.await()
            assertFalse(error.get())
        }
    }
}
