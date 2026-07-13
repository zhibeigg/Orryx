package org.gitee.orryx.compat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CompatGuardTest {

    @Test
    fun `firstAvailable continues after candidate factory linkage error`() {
        val failures = mutableListOf<String>()
        val selected = CompatGuard.firstAvailable(
            default = { "default" },
            candidates = arrayOf(
                ({ true }) to { throw NoClassDefFoundError("broken bridge") },
                ({ true }) to { "second" },
            ),
        ) { name, _ -> failures += name }

        assertEquals("second", selected)
        assertEquals(listOf("可选依赖桥接"), failures)
    }

    @Test
    fun `firstAvailable continues after availability linkage error`() {
        val selected = CompatGuard.firstAvailable(
            default = { "default" },
            candidates = arrayOf(
                ({ throw NoClassDefFoundError("broken detector") }) to { "first" },
                ({ true }) to { "second" },
            ),
        ) { _, _ -> }

        assertEquals("second", selected)
    }

    @Test
    fun `degrades once when invocation throws linkage error`() {
        var primaryCalls = 0
        var fallbackCalls = 0
        var reports = 0
        val primary = {
            primaryCalls++
            throw NoSuchMethodError("removed api")
        }
        val fallback = {
            fallbackCalls++
            "fallback"
        }
        val bridge = OneTimeLinkageFallback(primary, fallback) { reports++ }

        assertEquals("fallback", bridge.invoke { it() })
        assertEquals("fallback", bridge.invoke { it() })
        assertEquals(1, primaryCalls)
        assertEquals(2, fallbackCalls)
        assertEquals(1, reports)
        assertSame(fallback, bridge.current())
    }

    @Test
    fun `does not swallow business exceptions`() {
        val failure = IllegalStateException("business failure")
        val primary = { throw failure }
        val fallback = { "fallback" }
        val bridge = OneTimeLinkageFallback(primary, fallback) { error("must not report") }

        val thrown = assertThrows(IllegalStateException::class.java) {
            bridge.invoke { it() }
        }

        assertSame(failure, thrown)
        assertSame(primary, bridge.current())
    }
}
