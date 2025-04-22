package org.gitee.orryx.utils

import com.eatthepath.uuid.FastUUID
import java.util.UUID

fun String.parseUUID(): UUID? {
    return runCatching { FastUUID.parseUUID(this) }.getOrNull()
}