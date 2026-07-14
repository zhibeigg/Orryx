package org.gitee.orryx.core.editor.release

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

class ReleaseCanonicalTest {

    @Test
    fun `canonical encoding matches frozen golden vector`() {
        val payload = ReleaseCanonical.Payload(
            keyId = "key-1",
            releaseId = "release-1",
            serverInstanceId = "server-instance-1",
            stableServerId = "stable-server-1",
            draftId = "draft-1",
            draftVersionId = "draft-version-1",
            expectedBaseManifestRevision = "a".repeat(64),
            targetManifestRevision = "b".repeat(64),
            createdAtEpochMillis = 1_748_736_000_123,
            files = listOf(
                ReleaseFile(1, "z.yml", null, "c".repeat(64), 7, "https://example.com/r/z"),
                ReleaseFile(0, "a.yml", "d".repeat(64), "e".repeat(64), 3, "https://example.com/r/a"),
            ),
        )

        assertEquals(
            "d313e0474d670a4f53c5394d07440e46d9f8dc77fc8c57f5a397f7f3cf66ab6d",
            ReleaseCanonical.sha256(payload),
        )
    }

    @Test
    fun `ed25519 rfc vector verifies and tampering fails`() {
        val publicKey = hex("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a")
        val signature = hex(
            "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e06522490155" +
                "5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
        )
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(signature)

        assertTrue(ReleaseSignatureVerifier.verify(publicKey, byteArrayOf(), encoded))
        assertFalse(ReleaseSignatureVerifier.verify(publicKey, byteArrayOf(1), encoded))
        val changed = signature.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        assertFalse(
            ReleaseSignatureVerifier.verify(
                publicKey,
                byteArrayOf(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(changed),
            ),
        )
    }

    private fun hex(value: String): ByteArray = ByteArray(value.length / 2) { index ->
        ((value[index * 2].digitToInt(16) shl 4) or value[index * 2 + 1].digitToInt(16)).toByte()
    }
}
