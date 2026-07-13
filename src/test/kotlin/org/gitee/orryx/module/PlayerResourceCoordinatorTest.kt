package org.gitee.orryx.module

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class PlayerResourceCoordinatorTest {

    @Test
    fun `serializes complete resource transactions for one player`() {
        val player = UUID.randomUUID()
        val gate = CompletableFuture<Unit>()
        val starts = mutableListOf<String>()

        val first = PlayerResourceCoordinator.enqueue(player) {
            starts += "mana"
            gate.thenApply { "mana-done" }
        }
        val second = PlayerResourceCoordinator.enqueue(player) {
            starts += "spirit"
            CompletableFuture.completedFuture("spirit-done")
        }

        assertEquals(listOf("mana"), starts)
        assertFalse(second.isDone)
        gate.complete(Unit)

        assertEquals("mana-done", first.get(1, TimeUnit.SECONDS))
        assertEquals("spirit-done", second.get(1, TimeUnit.SECONDS))
        assertEquals(listOf("mana", "spirit"), starts)
    }

    @Test
    fun `allows different players to progress independently`() {
        val firstPlayer = UUID.randomUUID()
        val secondPlayer = UUID.randomUUID()
        val gate = CompletableFuture<Unit>()
        var secondStarted = false

        PlayerResourceCoordinator.enqueue(firstPlayer) { gate }
        val second = PlayerResourceCoordinator.enqueue(secondPlayer) {
            secondStarted = true
            CompletableFuture.completedFuture(Unit)
        }

        assertEquals(Unit, second.get(1, TimeUnit.SECONDS))
        assertEquals(true, secondStarted)
        gate.complete(Unit)
    }

    @Test
    fun `continues later transactions after failure`() {
        val player = UUID.randomUUID()
        val failure = IllegalStateException("write failed")
        val failedStage = CompletableFuture<Unit>().also { it.completeExceptionally(failure) }

        val failed = PlayerResourceCoordinator.enqueue(player) { failedStage }
        val continued = PlayerResourceCoordinator.enqueue(player) {
            CompletableFuture.completedFuture("ok")
        }

        val thrown = assertThrows(ExecutionException::class.java) {
            failed.get(1, TimeUnit.SECONDS)
        }
        assertEquals(failure, thrown.cause)
        assertEquals("ok", continued.get(1, TimeUnit.SECONDS))
    }
}
