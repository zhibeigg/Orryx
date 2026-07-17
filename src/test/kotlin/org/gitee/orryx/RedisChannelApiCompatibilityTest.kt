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

    @Test
    fun `Orryx API declares its relocated coroutine runtime before initialization`() {
        val source = readSource("src/main/kotlin/org/gitee/orryx/api/OrryxAPI.kt")

        assertTrue("!org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1" in source)
        assertTrue("!kotlin2120x.coroutines1101.CoroutineExceptionHandler" in source)
        assertTrue("!kotlinx.coroutines." in source)
        assertTrue("!kotlin2120x.coroutines1101." in source)
    }

    private fun readSource(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8)
    }
}
