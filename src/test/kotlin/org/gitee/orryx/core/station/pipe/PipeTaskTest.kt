package org.gitee.orryx.core.station.pipe

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class PipeTaskTest {

    @Test
    fun `only one trigger can claim termination`() {
        var claims = 0
        val task = pausedTask(onBrock = PipeTaskCallback { CompletableFuture.completedFuture("broke") })

        val first = task.breakFromTrigger { claims++ }
        val second = task.breakFromTrigger { claims++ }

        assertNotNull(first)
        assertNull(second)
        assertEquals("broke", first?.get(1, TimeUnit.SECONDS))
        assertEquals(1, claims)
        assertTrue(task.isClosed)
    }

    @Test
    fun `callback failures complete result exceptionally`() {
        val failure = IllegalStateException("callback failed")
        val task = pausedTask(
            onComplete = PipeTaskCallback {
                CompletableFuture<Any?>().also { future -> future.completeExceptionally(failure) }
            }
        )

        val thrown = assertThrows(ExecutionException::class.java) {
            task.complete().get(1, TimeUnit.SECONDS)
        }
        assertEquals(failure, thrown.cause)
        assertTrue(task.isClosed)
    }

    @Test
    fun `cancelling public result closes the task`() {
        val task = pausedTask()

        task.result.cancel(false)

        assertTrue(task.result.isCancelled)
        assertTrue(task.isClosed)
    }

    private fun pausedTask(
        onBrock: PipeTaskCallback = PipeTaskCallback { CompletableFuture.completedFuture(null) },
        onComplete: PipeTaskCallback = PipeTaskCallback { CompletableFuture.completedFuture(null) },
    ): PipeTask {
        return PipeTask(
            uuid = UUID.randomUUID(),
            scriptContext = null,
            brokeTriggers = emptySet(),
            timeout = 20L,
            onBrock = onBrock,
            onComplete = onComplete,
            periodTask = null,
            autoStart = false,
            callbackExecutor = { block -> block() },
        )
    }
}
