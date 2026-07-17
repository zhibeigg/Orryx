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
        val source = readSource("src/main/kotlin/org/gitee/orryx/dao/cache/ISyncCacheManager.kt")

        assertTrue("e.cluster" in source)
        assertFalse("RedisChannelPlugin.Type" in source)
        assertFalse("RedisChannelPlugin.type" in source)
    }

    @Test
    fun `cache managers only use RedisChannel API v2`() {
        val singleSource = readSource("src/main/kotlin/org/gitee/orryx/dao/cache/RedisManager.kt")
        val clusterSource = readSource("src/main/kotlin/org/gitee/orryx/dao/cache/ClusterRedisManager.kt")
        val buildScript = readSource("build.gradle.kts")

        assertTrue("api.executeAsync" in singleSource)
        assertTrue("api.executeClusterAsync" in clusterSource)
        assertFalse("useAsyncCommands" in singleSource)
        assertFalse("useAsyncCommands" in clusterSource)
        assertTrue("RedisChannel:2.15.14:api" in buildScript)
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8)
    }
}
