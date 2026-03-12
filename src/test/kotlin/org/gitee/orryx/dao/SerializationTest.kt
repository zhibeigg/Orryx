package org.gitee.orryx.dao

import kotlinx.serialization.json.Json
import org.gitee.orryx.core.profile.SerializableFlag
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class SerializationTest {

    private val json = Json { prettyPrint = false }

    @Nested
    inner class PlayerSkillPOTest {
        @Test fun `roundtrip serialization`() {
            val uuid = UUID.randomUUID()
            val skill = PlayerSkillPO(1, uuid, "warrior", "fireball", false, 5)
            val encoded = json.encodeToString(skill)
            val decoded = json.decodeFromString<PlayerSkillPO>(encoded)
            assertEquals(skill, decoded)
        }

        @Test fun `locked skill roundtrip`() {
            val skill = PlayerSkillPO(2, UUID.randomUUID(), "mage", "ice_bolt", true, 0)
            val decoded = json.decodeFromString<PlayerSkillPO>(json.encodeToString(skill))
            assertTrue(decoded.locked)
            assertEquals(0, decoded.level)
        }
    }

    @Nested
    inner class PlayerJobPOTest {
        @Test fun `roundtrip with empty maps`() {
            val job = PlayerJobPO(1, UUID.randomUUID(), "warrior", 100, "default", emptyMap())
            val decoded = json.decodeFromString<PlayerJobPO>(json.encodeToString(job))
            assertEquals(job, decoded)
        }

        @Test fun `roundtrip with nested maps`() {
            val bindKeys = mapOf(
                "group1" to mapOf("Q" to "fireball", "W" to null),
                "group2" to mapOf("E" to "heal")
            )
            val job = PlayerJobPO(1, UUID.randomUUID(), "mage", 500, "group1", bindKeys)
            val decoded = json.decodeFromString<PlayerJobPO>(json.encodeToString(job))
            assertEquals(job, decoded)
            assertNull(decoded.bindKeyOfGroup["group1"]?.get("W"))
        }
    }

    @Nested
    inner class PlayerProfilePOTest {
        @Test fun `roundtrip with null job`() {
            val profile = PlayerProfilePO(1, UUID.randomUUID(), null, 10, emptyMap())
            val decoded = json.decodeFromString<PlayerProfilePO>(json.encodeToString(profile))
            assertEquals(profile, decoded)
            assertNull(decoded.job)
        }

        @Test fun `roundtrip with flags`() {
            val flags = mapOf(
                "buff_speed" to SerializableFlag("DOUBLE", "1.5", true, 6000),
                "quest_done" to SerializableFlag("BOOLEAN", "true", false, 0)
            )
            val profile = PlayerProfilePO(1, UUID.randomUUID(), "warrior", 50, flags)
            val decoded = json.decodeFromString<PlayerProfilePO>(json.encodeToString(profile))
            assertEquals(profile, decoded)
            assertEquals("1.5", decoded.flags["buff_speed"]?.value)
            assertEquals(6000L, decoded.flags["buff_speed"]?.timeout)
        }
    }

    @Nested
    inner class SerializableFlagTest {
        @Test fun `roundtrip`() {
            val flag = SerializableFlag("STRING", "hello", true, 1000)
            val decoded = json.decodeFromString<SerializableFlag>(json.encodeToString(flag))
            assertEquals(flag, decoded)
        }

        @Test fun `data class equality`() {
            val a = SerializableFlag("INT", "42", false, 0)
            val b = SerializableFlag("INT", "42", false, 0)
            assertEquals(a, b)
            assertEquals(a.hashCode(), b.hashCode())
        }

        @Test fun `zero timeout`() {
            val flag = SerializableFlag("BOOLEAN", "true", false, 0)
            assertEquals(0L, flag.timeout)
        }
    }
}
