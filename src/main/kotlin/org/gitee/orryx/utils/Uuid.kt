package org.gitee.orryx.utils

import com.eatthepath.uuid.FastUUID
import java.nio.ByteBuffer
import java.util.UUID

fun String.parseUUID(): UUID? {
    return runCatching { FastUUID.parseUUID(this) }.getOrNull()
}

fun uuidToBytes(uuid: UUID): ByteArray {
    val buffer = ByteBuffer.wrap(ByteArray(16))
    buffer.putLong(uuid.mostSignificantBits)
    buffer.putLong(uuid.leastSignificantBits)
    return buffer.array()
}

fun bytesToUuid(bytes: ByteArray): UUID {
    val buffer = ByteBuffer.wrap(bytes)
    val mostSigBits = buffer.long
    val leastSigBits = buffer.long
    return UUID(mostSigBits, leastSigBits)
}