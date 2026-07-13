package org.gitee.orryx.dao.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class PlayerWriteCoordinatorTest {

    @Test
    fun `serializes writes for the same player`() {
        val player = UUID.randomUUID()
        val gate = CompletableFuture<Unit>()
        val starts = mutableListOf<String>()

        val first = PlayerWriteCoordinator.enqueue(player, "profile") {
            starts += "first"
            gate
        }
        val second = PlayerWriteCoordinator.enqueue(player, "job") {
            starts += "second"
            CompletableFuture.completedFuture(Unit)
        }

        assertEquals(listOf("first"), starts)
        assertFalse(second.isDone)

        gate.complete(Unit)
        CompletableFuture.allOf(first, second).get(1, TimeUnit.SECONDS)
        assertEquals(listOf("first", "second"), starts)
        PlayerWriteCoordinator.release(player).get(1, TimeUnit.SECONDS)
    }

    @Test
    fun `coalesces pending writes with the same key to the newest operation`() {
        val player = UUID.randomUUID()
        val gate = CompletableFuture<Unit>()
        val starts = mutableListOf<String>()

        val blocker = PlayerWriteCoordinator.enqueue(player, "profile") {
            starts += "blocker"
            gate
        }
        val superseded = PlayerWriteCoordinator.enqueue(player, "skill") {
            starts += "superseded"
            CompletableFuture.completedFuture(Unit)
        }
        val newest = PlayerWriteCoordinator.enqueue(player, "skill") {
            starts += "newest"
            CompletableFuture.completedFuture(Unit)
        }

        gate.complete(Unit)
        CompletableFuture.allOf(blocker, superseded, newest).get(1, TimeUnit.SECONDS)

        assertEquals(listOf("blocker", "newest"), starts)
        assertTrue(superseded.isDone)
        assertTrue(newest.isDone)
        PlayerWriteCoordinator.release(player).get(1, TimeUnit.SECONDS)
    }

    @Test
    fun `propagates failures and continues draining later writes`() {
        val player = UUID.randomUUID()
        val failure = IllegalStateException("write failed")
        val failedOperation = CompletableFuture<Unit>().also { it.completeExceptionally(failure) }

        val failed = PlayerWriteCoordinator.enqueue(player, "profile") { failedOperation }
        val continued = PlayerWriteCoordinator.enqueue(player, "job") {
            CompletableFuture.completedFuture(Unit)
        }

        val thrown = assertThrows(ExecutionException::class.java) {
            failed.get(1, TimeUnit.SECONDS)
        }
        assertEquals(failure, thrown.cause)
        continued.get(1, TimeUnit.SECONDS)
        assertThrows(ExecutionException::class.java) {
            PlayerWriteCoordinator.release(player).get(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `does not serialize independent players`() {
        val firstPlayer = UUID.randomUUID()
        val secondPlayer = UUID.randomUUID()
        val firstGate = CompletableFuture<Unit>()
        val secondGate = CompletableFuture<Unit>()
        var firstStarted = false
        var secondStarted = false

        val first = PlayerWriteCoordinator.enqueue(firstPlayer, "profile") {
            firstStarted = true
            firstGate
        }
        val second = PlayerWriteCoordinator.enqueue(secondPlayer, "profile") {
            secondStarted = true
            secondGate
        }

        assertTrue(firstStarted)
        assertTrue(secondStarted)
        firstGate.complete(Unit)
        secondGate.complete(Unit)
        CompletableFuture.allOf(first, second).get(1, TimeUnit.SECONDS)
        PlayerWriteCoordinator.release(firstPlayer).get(1, TimeUnit.SECONDS)
        PlayerWriteCoordinator.release(secondPlayer).get(1, TimeUnit.SECONDS)
    }
}
