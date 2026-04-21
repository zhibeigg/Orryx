package org.gitee.orryx.dao

import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class PlayerKeySettingPOTest {

    private val json = Json { prettyPrint = false }

    @Test
    fun `roundtrip serialization`() {
        val po = PlayerKeySettingPO(
            id = 1,
            player = UUID.randomUUID(),
            bindKeyMap = mapOf("Q" to "fireball", "W" to "heal"),
            aimConfirmKey = "MOUSE_LEFT",
            aimCancelKey = "MOUSE_RIGHT",
            generalAttackKey = "MOUSE_LEFT",
            blockKey = "V",
            dodgeKey = "SHIFT",
            extKeyMap = mapOf("F" to "interact")
        )
        val decoded = json.decodeFromString<PlayerKeySettingPO>(json.encodeToString(po))
        assertEquals(po, decoded)
    }

    @Test
    fun `roundtrip with empty maps`() {
        val po = PlayerKeySettingPO(
            id = 2,
            player = UUID.randomUUID(),
            bindKeyMap = emptyMap(),
            aimConfirmKey = "A",
            aimCancelKey = "B",
            generalAttackKey = "C",
            blockKey = "D",
            dodgeKey = "E",
            extKeyMap = emptyMap()
        )
        val decoded = json.decodeFromString<PlayerKeySettingPO>(json.encodeToString(po))
        assertEquals(po, decoded)
        assertTrue(decoded.bindKeyMap.isEmpty())
        assertTrue(decoded.extKeyMap.isEmpty())
    }

    @Test
    fun `data class equality`() {
        val uuid = UUID.randomUUID()
        val a = PlayerKeySettingPO(1, uuid, mapOf("Q" to "skill"), "A", "B", "C", "D", "E", emptyMap())
        val b = PlayerKeySettingPO(1, uuid, mapOf("Q" to "skill"), "A", "B", "C", "D", "E", emptyMap())
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `data class inequality`() {
        val uuid = UUID.randomUUID()
        val a = PlayerKeySettingPO(1, uuid, emptyMap(), "A", "B", "C", "D", "E", emptyMap())
        val b = PlayerKeySettingPO(2, uuid, emptyMap(), "A", "B", "C", "D", "E", emptyMap())
        assertNotEquals(a, b)
    }
}
