package org.gitee.orryx.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class RedisReadFutureTest {

    private class Commands(val response: CompletableFuture<String?>)

    @Test
    fun `decodes cache hits and refreshes expiry`() {
        var refreshed = false
        var fallbackCalled = false
        val commands = Commands(CompletableFuture.completedFuture("42"))

        val result = redisReadFuture(
            useCommands = { consumer ->
                consumer(commands)
                CompletableFuture.completedFuture(Unit)
            },
            request = { it.response },
            refreshExpiry = { refreshed = true },
            decode = String::toInt,
            fallback = {
                fallbackCalled = true
                CompletableFuture.completedFuture(0)
            },
        )

        assertEquals(42, result.get(1, TimeUnit.SECONDS))
        assertTrue(refreshed)
        assertFalse(fallbackCalled)
    }

    @Test
    fun `synchronous client failures fall back and warm the cache`() {
        var warmed = 0
        val result = redisReadFuture<Commands, Int>(
            useCommands = { throw IllegalStateException("client unavailable") },
            request = { it.response },
            refreshExpiry = {},
            decode = String::toInt,
            fallback = { CompletableFuture.completedFuture(7) },
            warmCache = { warmed = it },
        )

        assertEquals(7, result.get(1, TimeUnit.SECONDS))
        assertEquals(7, warmed)
    }

    @Test
    fun `decode failures use storage fallback`() {
        val commands = Commands(CompletableFuture.completedFuture("invalid"))
        val result = redisReadFuture(
            useCommands = { consumer ->
                consumer(commands)
                CompletableFuture.completedFuture(Unit)
            },
            request = { it.response },
            refreshExpiry = {},
            decode = String::toInt,
            fallback = { CompletableFuture.completedFuture(9) },
        )

        assertEquals(9, result.get(1, TimeUnit.SECONDS))
    }

    @Test
    fun `synchronous storage failures become exceptional futures`() {
        val result = redisReadFuture<Commands, Int>(
            useCommands = { throw IllegalStateException("redis failed") },
            request = { it.response },
            refreshExpiry = {},
            decode = String::toInt,
            fallback = { throw IllegalArgumentException("storage failed") },
        )

        val thrown = assertThrows(ExecutionException::class.java) {
            result.get(1, TimeUnit.SECONDS)
        }
        assertEquals("storage failed", thrown.cause?.message)
    }

    @Test
    fun `shared redis and storage failures still complete the future`() {
        val failure = IllegalStateException("shared failure")
        val storage = CompletableFuture<Int>().also { it.completeExceptionally(failure) }
        val result = redisReadFuture<Commands, Int>(
            useCommands = { throw failure },
            request = { it.response },
            refreshExpiry = {},
            decode = String::toInt,
            fallback = { storage },
        )

        val thrown = assertThrows(ExecutionException::class.java) {
            result.get(1, TimeUnit.SECONDS)
        }
        assertEquals(failure, thrown.cause)
    }

    @Test
    fun `skipped async command context falls back instead of hanging`() {
        val result = redisReadFuture<Commands, Int>(
            useCommands = { CompletableFuture.completedFuture(Unit) },
            request = { it.response },
            refreshExpiry = {},
            decode = String::toInt,
            fallback = { CompletableFuture.completedFuture(13) },
        )

        assertEquals(13, result.get(1, TimeUnit.SECONDS))
    }

    @Test
    fun `redis command observes skipped outer future`() {
        val result = redisCommandFuture<Commands>(
            useCommands = { CompletableFuture.completedFuture(Unit) },
            command = { it.response },
        )

        val thrown = assertThrows(ExecutionException::class.java) {
            result.get(1, TimeUnit.SECONDS)
        }
        assertEquals("Redis 异步命令上下文未执行", thrown.cause?.message)
    }

    @Test
    fun `redis command observes outer connection failures`() {
        val failure = IllegalStateException("connection unavailable")
        val outer = CompletableFuture<Unit>().also { it.completeExceptionally(failure) }
        val result = redisCommandFuture<Commands>(
            useCommands = { outer },
            command = { it.response },
        )

        val thrown = assertThrows(ExecutionException::class.java) {
            result.get(1, TimeUnit.SECONDS)
        }
        assertEquals(failure, thrown.cause)
    }
}
