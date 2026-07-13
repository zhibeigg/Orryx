package org.gitee.orryx.module.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SerialFutureQueueTest {

    @Test
    fun `cancelled queued result does not let later operation overtake the running node`() {
        val queue = SerialFutureQueue<String>()
        val gate = CompletableFuture<Unit>()
        val firstResult = CompletableFuture<String>()
        val secondResult = CompletableFuture<String>()
        val thirdResult = CompletableFuture<String>()
        val started = mutableListOf<String>()

        queue.enqueue("conversation", firstResult) {
            started += "first"
            gate.thenApply {
                firstResult.complete("first")
                Unit
            }
        }
        queue.enqueue("conversation", secondResult) {
            started += "second"
            secondResult.complete("second")
            CompletableFuture.completedFuture(Unit)
        }
        queue.enqueue("conversation", thirdResult) {
            started += "third"
            thirdResult.complete("third")
            CompletableFuture.completedFuture(Unit)
        }

        secondResult.cancel(false)
        assertEquals(listOf("first"), started)
        assertFalse(thirdResult.isDone)

        gate.complete(Unit)
        assertEquals("third", thirdResult.get(1, TimeUnit.SECONDS))
        assertEquals(listOf("first", "third"), started)
    }

    @Test
    fun `failed operation releases the next node`() {
        val queue = SerialFutureQueue<String>()
        val failed = CompletableFuture<String>()
        val next = CompletableFuture<String>()
        val calls = AtomicInteger()

        queue.enqueue("conversation", failed) {
            calls.incrementAndGet()
            CompletableFuture<Unit>().also { it.completeExceptionally(IllegalStateException("boom")) }
        }
        queue.enqueue("conversation", next) {
            calls.incrementAndGet()
            next.complete("ok")
            CompletableFuture.completedFuture(Unit)
        }

        assertEquals("ok", next.get(1, TimeUnit.SECONDS))
        assertTrue(failed.isCompletedExceptionally)
        assertEquals(2, calls.get())
    }

    @Test
    fun `bounded history preserves system message and newest turns`() {
        val history = listOf("system", "u1", "a1", "u2", "a2", "u3")
        val result = appendBoundedConversation(history, "a3", 6) { it == "system" }

        assertEquals(listOf("system", "a1", "u2", "a2", "u3", "a3"), result)
    }

    @Test
    fun `deadline rejects completion at the exact cutoff`() {
        assertFalse(isOpenAIRequestExpired(deadlineNanos = 100, nowNanos = 99))
        assertTrue(isOpenAIRequestExpired(deadlineNanos = 100, nowNanos = 100))
        assertTrue(isOpenAIRequestExpired(deadlineNanos = 100, nowNanos = 101))
    }
}
