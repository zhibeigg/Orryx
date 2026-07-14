package org.gitee.orryx

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class RedisChannelApiCompatibilityTest {

    @Test
    fun `cache startup uses event deployment flag instead of removed plugin type api`() {
        val source = String(
            Files.readAllBytes(Paths.get("src/main/kotlin/org/gitee/orryx/dao/cache/ISyncCacheManager.kt")),
            StandardCharsets.UTF_8
        )

        assertTrue("e.cluster" in source)
        assertFalse("RedisChannelPlugin.Type" in source)
        assertFalse("RedisChannelPlugin.type" in source)
    }
}
