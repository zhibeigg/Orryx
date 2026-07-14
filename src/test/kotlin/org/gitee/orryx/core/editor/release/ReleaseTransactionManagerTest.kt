package org.gitee.orryx.core.editor.release

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.gitee.orryx.core.editor.handler.EditorFilePolicy
import org.gitee.orryx.core.editor.handler.ManifestCanonicalHash
import org.gitee.orryx.core.editor.handler.ManifestEntryV1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

class ReleaseTransactionManagerTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `prepare verifies complete stage manifest signature and content hashes`() = runTest {
        val fixture = fixture(root.resolve("valid"))
        val result = fixture.manager.prepare(fixture.prepareRequest)

        assertTrue(result.success)
        assertEquals(ReleaseState.PREPARED, result.pluginState)
        assertEquals(fixture.targetRevision, result.resultManifestRevision)

        val tampered = fixture(root.resolve("tampered"), tamperPath = "skills/old.yml")
        val failed = tampered.manager.prepare(tampered.prepareRequest)
        assertFalse(failed.success)
        assertEquals(ReleaseState.FAILED, failed.pluginState)
        assertEquals("CONTENT_SHA256_MISMATCH", failed.errorCode)
    }

    @Test
    fun `readiness is separate from commit and failure rolls back base`() = runTest {
        val fixture = fixture(root.resolve("readiness"))
        assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)

        val pending = fixture.manager.commit(fixture.commitRequest)
        assertEquals(ReleaseState.READINESS_PENDING, pending.pluginState)
        assertEquals("new", String(Files.readAllBytes(fixture.live.resolve("skills/old.yml"))))
        assertTrue(Files.exists(fixture.live.resolve("keys.yml")))

        val rolledBack = fixture.manager.completeReadiness(fixture.transactionId) {
            ReadinessReport(false, "synthetic reload failure")
        }
        assertFalse(rolledBack.success)
        assertEquals(ReleaseState.ROLLED_BACK, rolledBack.pluginState)
        assertEquals("base", String(Files.readAllBytes(fixture.live.resolve("skills/old.yml"))))
        assertFalse(Files.exists(fixture.live.resolve("keys.yml")))
        assertEquals(fixture.baseRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
    }

    @Test
    fun `every filesystem exchange checkpoint recovers to ready`() = runTest {
        val expectedCheckpointCount = 11
        for (crashAt in 1..expectedCheckpointCount) {
            val directory = root.resolve("crash-$crashAt")
            val counter = AtomicInteger()
            val fixture = fixture(directory) { _, _ ->
                if (counter.incrementAndGet() == crashAt) throw SimulatedCrash()
            }
            assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)
            runCatching { fixture.manager.commit(fixture.commitRequest) }
                .onSuccess { error("checkpoint $crashAt 未触发模拟崩溃") }
                .onFailure { assertTrue(it is SimulatedCrash) }

            val recoveredManager = ReleaseTransactionManager(
                liveRoot = fixture.live,
                transactionsRoot = fixture.transactions,
                configProvider = { fixture.config },
                downloader = fixture.downloader,
            )
            val pending = mutableListOf<String>()
            recoveredManager.recover { pending += it }
            assertEquals(listOf(fixture.transactionId), pending, "checkpoint=$crashAt")
            val ready = recoveredManager.completeReadiness(fixture.transactionId) {
                ReadinessReport(true, "ready")
            }
            assertEquals(ReleaseState.READY, ready.pluginState, "checkpoint=$crashAt")
            assertEquals(fixture.targetRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
        }
    }

    private fun fixture(
        directory: Path,
        tamperPath: String? = null,
        checkpoint: (String, String) -> Unit = { _, _ -> },
    ): Fixture {
        val live = directory.resolve("live")
        val transactions = live.resolve(".editor/releases/transactions")
        Files.createDirectories(live.resolve("skills"))
        Files.write(live.resolve("skills/old.yml"), "base".toByteArray())
        val livePolicy = EditorFilePolicy(live)
        val base = livePolicy.snapshotManifest()

        val targetContents = linkedMapOf(
            "skills/old.yml" to "new".toByteArray(),
            "keys.yml" to "key: value".toByteArray(),
        )
        val files = targetContents.entries.mapIndexed { index, (path, bytes) ->
            ReleaseFile(
                ordinal = index,
                path = path,
                baseRevision = base.files.singleOrNull { it.path == path }?.revision,
                contentRevision = ReleaseCanonical.sha256(bytes),
                size = bytes.size.toLong(),
                contentUrl = "https://release.example.test/releases/tx/content/$index",
            )
        }
        val targetRevision = ManifestCanonicalHash.calculate(
            files.map { ManifestEntryV1(it.path, it.contentRevision, it.size) },
        )
        val privateKey = Ed25519PrivateKeyParameters(SecureRandom())
        val publicKey = privateKey.generatePublicKey().encoded
        val keyId = ReleaseCanonical.sha256(publicKey)
        val transactionId = "11111111-1111-4111-8111-${directory.fileName.toString().hashCode().toUInt().toString(16).padStart(12, '0').takeLast(12)}"
        val releaseId = "22222222-2222-4222-8222-222222222222"
        val commandId = ReleaseCanonical.sha256(directory.toString().toByteArray())
        val createdAt = 1_748_736_000_123L
        val payload = ReleaseCanonical.Payload(
            keyId = keyId,
            releaseId = releaseId,
            serverInstanceId = "33333333-3333-4333-8333-333333333333",
            stableServerId = "stable-server-test",
            draftId = "44444444-4444-4444-8444-444444444444",
            draftVersionId = "55555555-5555-4555-8555-555555555555",
            expectedBaseManifestRevision = base.revision,
            targetManifestRevision = targetRevision,
            createdAtEpochMillis = createdAt,
            files = files,
        )
        val canonical = ReleaseCanonical.encode(payload)
        val signer = Ed25519Signer().apply {
            init(true, privateKey)
            update(canonical, 0, canonical.size)
        }
        val signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.generateSignature())
        val config = ReleaseConfig(
            enabled = true,
            requireSignature = true,
            maxStagedBytes = 64L * 1024L * 1024L,
            connectTimeoutMillis = 1_000,
            readTimeoutMillis = 1_000,
            callTimeoutMillis = 2_000,
            acceptedClockSkewMillis = 30_000,
            allowLocalhostHttp = false,
            allowedHosts = setOf("release.example.test"),
            trustedKeys = mapOf(keyId to publicKey),
        )
        val contentsByUrl = files.associate { file ->
            val original = targetContents.getValue(file.path)
            file.contentUrl to if (file.path == tamperPath) original.copyOf().also { it[0]++ } else original
        }
        val operations = ReleaseOperations(
            canonicalVersion = ReleaseCanonical.VERSION,
            canonicalPayloadSha256 = ReleaseCanonical.sha256(canonical),
            signingKeyId = keyId,
            signature = signature,
            releaseId = releaseId,
            serverInstanceId = payload.serverInstanceId,
            stableServerId = payload.stableServerId,
            draftId = payload.draftId,
            draftVersionId = payload.draftVersionId,
            expectedManifestRevision = base.revision,
            targetManifestRevision = targetRevision,
            createdAt = createdAt,
            fileCount = files.size,
            totalBytes = files.sumOf { it.size },
            files = files,
        )
        val downloader = FakeDownloader(operations, contentsByUrl)
        val manager = ReleaseTransactionManager(
            liveRoot = live,
            transactionsRoot = transactions,
            configProvider = { config },
            downloader = downloader,
            checkpoint = checkpoint,
        )
        val prepare = ReleaseRequest(
            action = ReleaseAction.PREPARE,
            transactionId = transactionId,
            releaseId = releaseId,
            commandId = commandId,
            canonicalVersion = ReleaseCanonical.VERSION,
            canonicalPayloadSha256 = ReleaseCanonical.sha256(canonical),
            signingKeyId = keyId,
            signature = signature,
            expectedManifestRevision = base.revision,
            targetManifestRevision = targetRevision,
            fileCount = files.size,
            totalBytes = files.sumOf { it.size },
            operationsUrl = "https://release.example.test/releases/tx/operations.json",
            transferToken = "t".repeat(40),
            transferExpiresAt = System.currentTimeMillis() + 60_000,
        )
        val commit = ReleaseRequest(
            action = ReleaseAction.COMMIT,
            transactionId = transactionId,
            releaseId = releaseId,
            commandId = ReleaseCanonical.sha256("commit:$directory".toByteArray()),
            readinessDeadline = System.currentTimeMillis() + 60_000,
        )
        return Fixture(
            live,
            transactions,
            manager,
            downloader,
            config,
            prepare,
            commit,
            transactionId,
            base.revision,
            targetRevision,
        )
    }

    private data class Fixture(
        val live: Path,
        val transactions: Path,
        val manager: ReleaseTransactionManager,
        val downloader: ReleaseDownloader,
        val config: ReleaseConfig,
        val prepareRequest: ReleaseRequest,
        val commitRequest: ReleaseRequest,
        val transactionId: String,
        val baseRevision: String,
        val targetRevision: String,
    )

    private class FakeDownloader(
        operations: ReleaseOperations,
        private val contents: Map<String, ByteArray>,
    ) : ReleaseDownloader {
        private val metadata = Json.encodeToString(ReleaseOperations.serializer(), operations).toByteArray()

        override suspend fun metadata(uri: URI, token: String, config: ReleaseConfig): ByteArray = metadata

        override suspend fun file(
            uri: URI,
            token: String,
            config: ReleaseConfig,
            target: Path,
            expectedSize: Long,
        ): DownloadedFile {
            val bytes = requireNotNull(contents[uri.toString()])
            Files.createDirectories(target.parent)
            Files.write(target, bytes)
            return DownloadedFile(bytes.size.toLong(), ReleaseCanonical.sha256(bytes))
        }
    }

    private class SimulatedCrash : RuntimeException()
}
