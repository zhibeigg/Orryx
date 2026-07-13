package org.gitee.orryx.core.message

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AimRequestProtocolTest {

    @Test
    fun `same skill receives a unique bounded wire id per request`() {
        val first = AimRequestProtocol.createWireSkillId("fireball", 1L)
        val second = AimRequestProtocol.createWireSkillId("fireball", 2L)
        val longSkill = AimRequestProtocol.createWireSkillId("s".repeat(256), Long.MAX_VALUE)

        assertNotEquals(first, second)
        assertTrue(first.startsWith("fireball~orryx~"))
        assertTrue(longSkill.length <= AimRequestProtocol.MAX_SKILL_ID_LENGTH)
    }

    @Test
    fun `request lifecycle requires confirmation and completes only once`() {
        val lifecycle = AimRequestLifecycle()

        assertEquals(AimRequestPhase.CREATED, lifecycle.currentPhase())
        assertFalse(lifecycle.complete())
        assertTrue(lifecycle.confirm())
        assertFalse(lifecycle.confirm())
        assertTrue(lifecycle.isConfirmed())
        assertTrue(lifecycle.complete())
        assertFalse(lifecycle.complete())
        assertFalse(lifecycle.cancel())
        assertEquals(AimRequestPhase.COMPLETED, lifecycle.currentPhase())
    }

    @Test
    fun `cancel prevents later confirmation or completion`() {
        val lifecycle = AimRequestLifecycle()

        assertTrue(lifecycle.cancel())
        assertFalse(lifecycle.confirm())
        assertFalse(lifecycle.complete())
    }
}
