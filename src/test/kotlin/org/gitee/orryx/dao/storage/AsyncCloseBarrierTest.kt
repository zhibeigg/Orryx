package org.gitee.orryx.dao.storage

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class AsyncCloseBarrierTest {

    @Test
    fun `close waits accepted operations and rejects new work`() {
        val barrier = AsyncCloseBarrier("closing")
        val operationGate = CompletableFuture<String>()
        val operation = barrier.submit { operationGate }
        var closeCalled = false

        val close = barrier.close {
            closeCalled = true
            CompletableFuture.completedFuture(Unit)
        }
        val rejected = barrier.submit { CompletableFuture.completedFuture("late") }

        assertFalse(close.isDone)
        assertFalse(closeCalled)
        assertThrows(ExecutionException::class.java) { rejected.get(1, TimeUnit.SECONDS) }

        operationGate.complete("ok")
        operation.get(1, TimeUnit.SECONDS)
        close.get(1, TimeUnit.SECONDS)
        assertTrue(closeCalled)
    }

    @Test
    fun `close still runs and propagates an in-flight failure`() {
        val barrier = AsyncCloseBarrier("closing")
        val operationGate = CompletableFuture<Unit>()
        val operation = barrier.submit { operationGate }
        val failure = IllegalStateException("write failed")
        var closeCalled = false

        val close = barrier.close {
            closeCalled = true
            CompletableFuture.completedFuture(Unit)
        }
        operationGate.completeExceptionally(failure)

        assertThrows(ExecutionException::class.java) { operation.get(1, TimeUnit.SECONDS) }
        val thrown = assertThrows(ExecutionException::class.java) { close.get(1, TimeUnit.SECONDS) }
        assertSame(failure, thrown.cause)
        assertTrue(closeCalled)
    }

    @Test
    fun `close is idempotent`() {
        val barrier = AsyncCloseBarrier("closing")
        val first = barrier.close { CompletableFuture.completedFuture(Unit) }
        val second = barrier.close { CompletableFuture.completedFuture(Unit) }

        assertSame(first, second)
        first.get(1, TimeUnit.SECONDS)
    }
}
