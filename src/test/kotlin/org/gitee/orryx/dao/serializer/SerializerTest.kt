package org.gitee.orryx.dao.serializer

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

@Serializable
private data class DateWrapper(@Serializable(with = DateSerializer::class) val date: Date)

@Serializable
private data class UUIDWrapper(@Serializable(with = UUIDSerializer::class) val uuid: UUID)

class SerializerTest {

    private val json = Json { prettyPrint = false }

    @Nested
    inner class DateSerializerTest {

        @Test
        fun `roundtrip current date`() {
            val now = Date()
            val wrapper = DateWrapper(now)
            val encoded = json.encodeToString(wrapper)
            val decoded = json.decodeFromString<DateWrapper>(encoded)
            assertEquals(now.time, decoded.date.time)
        }

        @Test
        fun `roundtrip epoch`() {
            val epoch = Date(0)
            val wrapper = DateWrapper(epoch)
            val decoded = json.decodeFromString<DateWrapper>(json.encodeToString(wrapper))
            assertEquals(0L, decoded.date.time)
        }

        @Test
        fun `roundtrip specific timestamp`() {
            val specific = Date(1700000000000L)
            val wrapper = DateWrapper(specific)
            val decoded = json.decodeFromString<DateWrapper>(json.encodeToString(wrapper))
            assertEquals(1700000000000L, decoded.date.time)
        }

        @Test
        fun `descriptor name`() {
            assertEquals("orryx.Date", DateSerializer.descriptor.serialName)
        }
    }

    @Nested
    inner class InstantSerializerTest {

        @Test
        fun `SerializableInstant roundtrip`() {
            val si = InstantSerializer.SerializableInstant(1700000000L, 123456789)
            val encoded = json.encodeToString(si)
            val decoded = json.decodeFromString<InstantSerializer.SerializableInstant>(encoded)
            assertEquals(1700000000L, decoded.seconds)
            assertEquals(123456789, decoded.nanos)
        }

        @Test
        fun `SerializableInstant epoch`() {
            val si = InstantSerializer.SerializableInstant(0L, 0)
            val decoded = json.decodeFromString<InstantSerializer.SerializableInstant>(json.encodeToString(si))
            assertEquals(0L, decoded.seconds)
            assertEquals(0, decoded.nanos)
        }

        @Test
        fun `SerializableInstant data class equality`() {
            val a = InstantSerializer.SerializableInstant(100L, 200)
            val b = InstantSerializer.SerializableInstant(100L, 200)
            assertEquals(a, b)
            assertEquals(a.hashCode(), b.hashCode())
        }

        @Test
        fun `descriptor name`() {
            assertEquals("orryx.Instant", InstantSerializer.descriptor.serialName)
        }
    }

    @Nested
    inner class UUIDSerializerTest {

        @Test
        fun `roundtrip random uuid`() {
            val uuid = UUID.randomUUID()
            val wrapper = UUIDWrapper(uuid)
            val decoded = json.decodeFromString<UUIDWrapper>(json.encodeToString(wrapper))
            assertEquals(uuid, decoded.uuid)
        }

        @Test
        fun `roundtrip nil uuid`() {
            val nil = UUID(0L, 0L)
            val wrapper = UUIDWrapper(nil)
            val decoded = json.decodeFromString<UUIDWrapper>(json.encodeToString(wrapper))
            assertEquals(nil, decoded.uuid)
        }

        @Test
        fun `roundtrip max uuid`() {
            val max = UUID(-1L, -1L)
            val wrapper = UUIDWrapper(max)
            val decoded = json.decodeFromString<UUIDWrapper>(json.encodeToString(wrapper))
            assertEquals(max, decoded.uuid)
        }

        @Test
        fun `serialized form is string`() {
            val uuid = UUID.fromString("12345678-1234-1234-1234-123456789abc")
            val wrapper = UUIDWrapper(uuid)
            val encoded = json.encodeToString(wrapper)
            assertTrue(encoded.contains("12345678-1234-1234-1234-123456789abc"))
        }

        @Test
        fun `descriptor name`() {
            assertEquals("orryx.UUID", UUIDSerializer.descriptor.serialName)
        }
    }
}
