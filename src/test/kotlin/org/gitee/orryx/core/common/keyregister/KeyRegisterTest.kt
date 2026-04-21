package org.gitee.orryx.core.common.keyregister

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KeyRegisterTest {

    private lateinit var register: KeyRegister

    @BeforeEach
    fun setup() {
        val player = mockk<org.bukkit.entity.Player>(relaxed = true)
        register = KeyRegister(player)
    }

    @Nested
    inner class InitialState {
        @Test
        fun `new register has no key pressed`() {
            assertFalse(register.isKeyPress("W"))
        }

        @Test
        fun `new register reports key as released`() {
            assertTrue(register.isKeyRelease("W"))
        }

        @Test
        fun `press last is 0 for unknown key`() {
            assertEquals(0, register.getKeyPressLast("W"))
        }

        @Test
        fun `release last is 0 for unknown key`() {
            assertEquals(0, register.getKeyReleaseLast("W"))
        }
    }

    @Nested
    inner class PressAndRelease {
        @Test
        fun `keyPress marks key as pressed`() {
            register.keyPress("W")
            assertTrue(register.isKeyPress("W"))
            assertFalse(register.isKeyRelease("W"))
        }

        @Test
        fun `keyRelease marks key as released`() {
            register.keyPress("W")
            Thread.sleep(2) // 确保时间戳不同
            register.keyRelease("W")
            assertFalse(register.isKeyPress("W"))
            assertTrue(register.isKeyRelease("W"))
        }

        @Test
        fun `press updates timestamp`() {
            register.keyPress("W")
            assertTrue(register.getKeyPressLast("W") > 0)
        }

        @Test
        fun `release updates timestamp`() {
            register.keyRelease("W")
            assertTrue(register.getKeyReleaseLast("W") > 0)
        }

        @Test
        fun `multiple keys are independent`() {
            register.keyPress("W")
            register.keyRelease("S")
            assertTrue(register.isKeyPress("W"))
            assertTrue(register.isKeyRelease("S"))
            assertTrue(register.isKeyRelease("A"))
        }

        @Test
        fun `rapid press-release sequence`() {
            register.keyPress("W")
            Thread.sleep(2)
            register.keyRelease("W")
            Thread.sleep(2)
            register.keyPress("W")
            assertTrue(register.isKeyPress("W"))
        }
    }

    @Nested
    inner class TimeoutCheck {
        @Test
        fun `recently pressed key is in timeout`() {
            register.keyPress("W")
            assertTrue(register.isKeyInTimeout("W", 1000, IKeyRegister.ActionType.PRESS))
        }

        @Test
        fun `recently released key is in timeout`() {
            register.keyRelease("W")
            assertTrue(register.isKeyInTimeout("W", 1000, IKeyRegister.ActionType.RELEASE))
        }

        @Test
        fun `unknown key is not in timeout`() {
            assertFalse(register.isKeyInTimeout("X", 1000, IKeyRegister.ActionType.PRESS))
        }

        @Test
        fun `zero timeout still matches current press`() {
            register.keyPress("W")
            val now = System.currentTimeMillis()
            // 使用带时间戳的重载，确保 timeStamp == lastPress
            assertTrue(register.isKeyInTimeout("W", now, 1000, IKeyRegister.ActionType.PRESS))
        }

        @Test
        fun `expired key is not in timeout`() {
            register.keyPress("W")
            // 使用一个远未来的时间戳模拟过期
            val farFuture = System.currentTimeMillis() + 100_000
            assertFalse(register.isKeyInTimeout("W", farFuture, 50, IKeyRegister.ActionType.PRESS))
        }
    }

    @Nested
    inner class MultiKeyTimeout {
        @Test
        fun `all keys in timeout without sort`() {
            register.keyPress("W")
            register.keyPress("S")
            assertTrue(
                register.isKeysInTimeout(
                    listOf("W", "S"), 1000,
                    IKeyRegister.ActionType.PRESS, sort = false
                )
            )
        }

        @Test
        fun `one key missing fails without sort`() {
            register.keyPress("W")
            assertFalse(
                register.isKeysInTimeout(
                    listOf("W", "S"), 1000,
                    IKeyRegister.ActionType.PRESS, sort = false
                )
            )
        }

        @Test
        fun `empty key list returns true`() {
            assertTrue(
                register.isKeysInTimeout(
                    emptyList(), 1000,
                    IKeyRegister.ActionType.PRESS, sort = false
                )
            )
        }

        @Test
        fun `single key in timeout`() {
            register.keyPress("W")
            assertTrue(
                register.isKeysInTimeout(
                    listOf("W"), 1000,
                    IKeyRegister.ActionType.PRESS, sort = false
                )
            )
        }

        @Test
        fun `sorted keys all pressed recently`() {
            register.keyPress("W")
            register.keyPress("S")
            register.keyPress("A")
            assertTrue(
                register.isKeysInTimeout(
                    listOf("W", "S", "A"), 1000,
                    IKeyRegister.ActionType.PRESS, sort = true
                )
            )
        }

        @Test
        fun `sorted keys with missing key fails`() {
            register.keyPress("W")
            // S not pressed
            assertFalse(
                register.isKeysInTimeout(
                    listOf("W", "S"), 1000,
                    IKeyRegister.ActionType.PRESS, sort = true
                )
            )
        }
    }
}
