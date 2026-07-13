package org.gitee.orryx.core.skill

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class SkillCastCoordinatorTest {

    @Test
    fun `serializes the complete cast transaction per player`() {
        val player = UUID.randomUUID()
        val gate = CompletableFuture<Unit>()
        val starts = mutableListOf<String>()

        val first = SkillCastCoordinator.enqueue(player) {
            starts += "first"
            gate.thenApply { "first-result" }
        }
        val second = SkillCastCoordinator.enqueue(player) {
            starts += "second"
            CompletableFuture.completedFuture("second-result")
        }

        assertEquals(listOf("first"), starts)
        assertFalse(second.isDone)

        gate.complete(Unit)
        assertEquals("first-result", first.get(1, TimeUnit.SECONDS))
        assertEquals("second-result", second.get(1, TimeUnit.SECONDS))
        assertEquals(listOf("first", "second"), starts)
    }

    @Test
    fun `continues later casts after a failed transaction`() {
        val player = UUID.randomUUID()
        val failure = IllegalStateException("cast failed")
        val failedFuture = CompletableFuture<String>().also { it.completeExceptionally(failure) }

        val failed = SkillCastCoordinator.enqueue(player) { failedFuture }
        val continued = SkillCastCoordinator.enqueue(player) {
            CompletableFuture.completedFuture("ok")
        }

        val thrown = assertThrows(ExecutionException::class.java) {
            failed.get(1, TimeUnit.SECONDS)
        }
        assertEquals(failure, thrown.cause)
        assertEquals("ok", continued.get(1, TimeUnit.SECONDS))
    }
}
