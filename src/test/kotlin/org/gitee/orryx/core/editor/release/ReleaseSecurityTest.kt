package org.gitee.orryx.core.editor.release

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.Base64

class ReleaseSecurityTest {

    @Test
    fun `trusted key is raw base64 and key id is public key fingerprint`() {
        val key = ByteArray(32) { it.toByte() }
        val keyId = ReleaseCanonical.sha256(key)
        assertEquals(
            key.toList(),
            ReleaseConfig.decodeTrustedKey(keyId, Base64.getEncoder().encodeToString(key)).toList(),
        )
        assertThrows(ReleaseException::class.java) {
            ReleaseConfig.decodeTrustedKey("00".repeat(32), Base64.getEncoder().encodeToString(key))
        }
    }

    @Test
    fun `url policy rejects redirects by origin private hosts and path escape`() {
        val config = config(allowedHosts = setOf("release.example.test"))
        val policy = ReleaseUrlPolicy("https://release.example.test/releases/tx/operations.json", config)
        assertEquals(
            "https://release.example.test/releases/tx/content/1",
            policy.validateContentUrl("https://release.example.test/releases/tx/content/1").toString(),
        )
        assertThrows(ReleaseException::class.java) {
            policy.validateContentUrl("https://evil.example/releases/tx/content/1")
        }
        assertThrows(ReleaseException::class.java) {
            policy.validateContentUrl("https://release.example.test/other/content/1")
        }
        assertThrows(ReleaseException::class.java) {
            policy.validateContentUrl("https://release.example.test/releases/tx/%2e%2e/secret")
        }
        assertThrows(ReleaseException::class.java) {
            ReleaseUrlPolicy("https://127.0.0.1/releases/tx/operations.json", config())
        }
    }

    @Test
    fun `localhost http requires explicit switch`() {
        assertThrows(ReleaseException::class.java) {
            ReleaseUrlPolicy("http://localhost/releases/tx/operations.json", config())
        }
        ReleaseUrlPolicy(
            "http://localhost/releases/tx/operations.json",
            config(allowLocalhostHttp = true),
        )
    }

    private fun config(
        allowLocalhostHttp: Boolean = false,
        allowedHosts: Set<String> = emptySet(),
    ) = ReleaseConfig(
        enabled = true,
        requireSignature = true,
        maxStagedBytes = 64L * 1024L * 1024L,
        connectTimeoutMillis = 1_000,
        readTimeoutMillis = 1_000,
        callTimeoutMillis = 2_000,
        acceptedClockSkewMillis = 30_000,
        allowLocalhostHttp = allowLocalhostHttp,
        allowedHosts = allowedHosts,
        trustedKeys = emptyMap(),
    )
}
