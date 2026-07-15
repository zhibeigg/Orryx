package org.gitee.orryx.core.editor.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FileHandlerTest {

    private val revisionA = "a".repeat(64)
    private val revisionB = "b".repeat(64)

    @Test
    fun `accepts expectedRevision and baseRevision aliases`() {
        assertEquals(revisionA, FileHandler.resolveWriteRevision(revisionA, null, protocolV2 = true, force = false))
        assertEquals(revisionA, FileHandler.resolveWriteRevision(null, revisionA, protocolV2 = true, force = false))
        assertEquals(revisionA, FileHandler.resolveWriteRevision(revisionA, revisionA, protocolV2 = true, force = false))
        assertNull(FileHandler.resolveWriteRevision(null, null, protocolV2 = false, force = false))
        assertNull(FileHandler.resolveWriteRevision(null, "42", protocolV2 = false, force = false))
        assertNull(FileHandler.resolveWriteRevision("not-a-sha", "43", protocolV2 = false, force = false))
        assertNull(FileHandler.resolveWriteRevision(revisionA, revisionA, protocolV2 = true, force = true))
    }

    @Test
    fun `rejects mismatched dual fields and invalid v2 revisions`() {
        val mismatch = assertThrows(FileHandler.WriteRevisionException::class.java) {
            FileHandler.resolveWriteRevision(revisionA, revisionB, protocolV2 = true, force = false)
        }
        assertEquals("REVISION_FIELDS_MISMATCH", mismatch.code)

        val missing = assertThrows(FileHandler.WriteRevisionException::class.java) {
            FileHandler.resolveWriteRevision(null, null, protocolV2 = true, force = false)
        }
        assertEquals("REVISION_REQUIRED", missing.code)

        val invalid = assertThrows(FileHandler.WriteRevisionException::class.java) {
            FileHandler.resolveWriteRevision("ABC", null, protocolV2 = true, force = false)
        }
        assertEquals("INVALID_REVISION", invalid.code)
    }
}
