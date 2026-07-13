package org.gitee.orryx.dao.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MemoryCachePolicyTest {

    @Test
    fun `resize updates capacity and access expiry`() {
        val cache = Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .buildAsync<String, String> { key, _ -> CompletableFuture.completedFuture(key) }

        resizeCachePolicy(cache, 25, 5)

        assertEquals(25, cache.synchronous().policy().eviction().get().maximum)
        assertEquals(
            5,
            cache.synchronous().policy().expireAfterAccess().get().getExpiresAfter(TimeUnit.MINUTES)
        )
    }
}
