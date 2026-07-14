package org.gitee.orryx.core.editor.release

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ReleaseJournalStoreTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `journal replacement is atomic durable and preserves state`() {
        val store = ReleaseJournalStore(root.resolve("transactions"))
        val transactionId = "11111111-1111-4111-8111-111111111111"
        val initial = journal(transactionId, ReleaseState.PREPARING)
        store.save(initial)
        store.save(initial.copy(state = ReleaseState.COMMITTING, backupMoved = setOf("skills"), eventSeq = 4))

        val loaded = store.load(transactionId)
        assertEquals(ReleaseState.COMMITTING, loaded?.state)
        assertEquals(setOf("skills"), loaded?.backupMoved)
        assertEquals(4, loaded?.eventSeq)
        assertTrue(Files.isRegularFile(store.journalPath(transactionId)))
        Files.newDirectoryStream(store.transactionDir(transactionId)).use { stream ->
            assertFalse(stream.any { it.fileName.toString().endsWith(".tmp") })
        }
    }

    private fun journal(transactionId: String, state: ReleaseState) = ReleaseJournal(
        transactionId = transactionId,
        releaseId = "22222222-2222-4222-8222-222222222222",
        commandId = "11".repeat(32),
        prepareCommandId = "11".repeat(32),
        state = state,
        canonicalPayloadSha256 = "22".repeat(32),
        signingKeyId = "33".repeat(32),
        expectedManifestRevision = "44".repeat(32),
        targetManifestRevision = "55".repeat(32),
        fileCount = 0,
        totalBytes = 0,
        operationsUrl = "https://example.com/releases/ops.json",
        transferExpiresAt = Long.MAX_VALUE,
    )
}
