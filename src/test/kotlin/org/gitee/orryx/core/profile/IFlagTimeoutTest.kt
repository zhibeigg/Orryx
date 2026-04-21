package org.gitee.orryx.core.profile

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 测试 IFlag.isTimeout() 的纯时间逻辑。
 * 使用 MockK mock sealed interface IFlag，仅测试 isTimeout 默认实现。
 */
class IFlagTimeoutTest {

    private fun createFlag(
        timestamp: Long = System.currentTimeMillis(),
        timeout: Long = 0L
    ): IFlag {
        val flag = mockk<Flag<String>>()
        every { flag.timestamp } returns timestamp
        every { flag.timeout } returns timeout
        every { flag.isTimeout() } answers { callOriginal() }
        return flag
    }

    @Nested
    inner class TimeoutZero {
        @Test
        fun `zero timeout never expires`() {
            val flag = createFlag(timeout = 0L)
            assertFalse(flag.isTimeout())
        }

        @Test
        fun `zero timeout with old timestamp never expires`() {
            val flag = createFlag(timestamp = 0L, timeout = 0L)
            assertFalse(flag.isTimeout())
        }
    }

    @Nested
    inner class TimeoutExpired {
        @Test
        fun `expired flag returns true`() {
            // timestamp=0, timeout=1 => 0+1=1 < currentTimeMillis => expired
            val flag = createFlag(timestamp = 0L, timeout = 1L)
            assertTrue(flag.isTimeout())
        }

        @Test
        fun `flag expired 1 second ago`() {
            val now = System.currentTimeMillis()
            val flag = createFlag(timestamp = now - 2000, timeout = 1000)
            assertTrue(flag.isTimeout())
        }

        @Test
        fun `flag expired long ago`() {
            val flag = createFlag(timestamp = 1000L, timeout = 1000L)
            // 1000 + 1000 = 2000 < currentTimeMillis
            assertTrue(flag.isTimeout())
        }
    }

    @Nested
    inner class TimeoutNotExpired {
        @Test
        fun `flag with future expiry is not timed out`() {
            val now = System.currentTimeMillis()
            val flag = createFlag(timestamp = now, timeout = 60_000)
            assertFalse(flag.isTimeout())
        }

        @Test
        fun `flag with very large timeout is not timed out`() {
            val flag = createFlag(timestamp = System.currentTimeMillis(), timeout = Long.MAX_VALUE / 2)
            assertFalse(flag.isTimeout())
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `timeout of 1ms with current timestamp does not throw`() {
            val now = System.currentTimeMillis()
            val flag = createFlag(timestamp = now, timeout = 1L)
            // 不做断言，只确保不抛异常
            flag.isTimeout()
        }

        @Test
        fun `negative timestamp with positive timeout`() {
            val flag = createFlag(timestamp = -1000L, timeout = 500L)
            // -1000 + 500 = -500 < currentTimeMillis => expired
            assertTrue(flag.isTimeout())
        }
    }
}
