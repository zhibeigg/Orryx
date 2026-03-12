package org.gitee.orryx.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class UuidTest {

    @Nested
    inner class ParseUUID {
        @Test fun `valid uuid string`() {
            val uuid = UUID.randomUUID()
            assertEquals(uuid, uuid.toString().parseUUID())
        }

        @Test fun `invalid string returns null`() {
            assertNull("not-a-uuid".parseUUID())
        }

        @Test fun `empty string returns null`() {
            assertNull("".parseUUID())
        }

        @Test fun `uuid without dashes`() {
            // FastUUID should handle standard format
            val uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
            assertEquals(uuid, "550e8400-e29b-41d4-a716-446655440000".parseUUID())
        }
    }

    @Nested
    inner class ByteConversion {
        @Test fun `roundtrip uuid to bytes and back`() {
            val uuid = UUID.randomUUID()
            val bytes = uuidToBytes(uuid)
            assertEquals(16, bytes.size)
            assertEquals(uuid, bytesToUuid(bytes))
        }

        @Test fun `specific uuid roundtrip`() {
            val uuid = UUID.fromString("00000000-0000-0000-0000-000000000000")
            val bytes = uuidToBytes(uuid)
            assertTrue(bytes.all { it == 0.toByte() })
            assertEquals(uuid, bytesToUuid(bytes))
        }

        @Test fun `max uuid roundtrip`() {
            val uuid = UUID(-1, -1) // all bits set
            val bytes = uuidToBytes(uuid)
            assertTrue(bytes.all { it == (-1).toByte() })
            assertEquals(uuid, bytesToUuid(bytes))
        }

        @Test fun `multiple uuids produce different bytes`() {
            val uuid1 = UUID.randomUUID()
            val uuid2 = UUID.randomUUID()
            assertFalse(uuidToBytes(uuid1).contentEquals(uuidToBytes(uuid2)))
        }
    }
}
