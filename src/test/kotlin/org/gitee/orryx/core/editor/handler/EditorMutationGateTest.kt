package org.gitee.orryx.core.editor.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EditorMutationGateTest {

    @Test
    fun `blocks ordinary mutations until release transaction is cleared`() {
        val gate = EditorMutationGate()
        assertNull(gate.begin("tx-1", "COMMITTING"))

        val blocked = assertThrows(EditorMutationGate.MutationBlockedException::class.java) {
            gate.checkAllowed(EditorMutationOperation.FILE_WRITE)
        }
        assertEquals("tx-1", blocked.blocker.transactionId)
        assertEquals("COMMITTING", blocked.blocker.state)

        gate.hold("tx-1", "RECOVERY_REQUIRED")
        assertThrows(EditorMutationGate.MutationBlockedException::class.java) {
            gate.checkAllowed(EditorMutationOperation.RELOAD)
        }

        gate.release("tx-1")
        gate.checkAllowed(EditorMutationOperation.FILE_DELETE)
    }

    @Test
    fun `does not allow a second release to enter an active gate`() {
        val gate = EditorMutationGate()
        assertNull(gate.begin("tx-1", "READINESS_PENDING"))
        val conflict = gate.begin("tx-2", "COMMITTING")
        assertEquals("tx-1", conflict?.transactionId)
        assertEquals(listOf(EditorMutationGate.Blocker("tx-1", "READINESS_PENDING")), gate.currentBlockers())
    }
}
