package org.gitee.orryx.core.editor.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class EditorFilePolicyTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `rejects traversal absolute paths and root mutation`() {
        val policy = EditorFilePolicy(root)
        assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.readText("../secret.yml") }
        assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.readText("C:/secret.yml") }
        assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.delete("") }
        assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.create("a//b.yml", false) }
        assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.writeTextAtomic("skills/.env", "secret") }
        assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.writeTextAtomic("jobs/private.pem", "secret") }
        assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.writeTextAtomic("ui/cache.db", "data") }
    }

    @Test
    fun `writes atomically and reads utf8 content`() {
        val policy = EditorFilePolicy(root)
        policy.writeTextAtomic("skills/fire.yml", "技能: 火球")
        assertEquals("技能: 火球", policy.readText("skills/fire.yml"))
        assertFalse(Files.list(root.resolve("skills")).use { stream ->
            stream.anyMatch { it.fileName.toString().startsWith(".orryx-editor-") }
        })
    }

    @Test
    fun `enforces file and entry quotas`() {
        val policy = EditorFilePolicy(root, maxFileBytes = 4, maxTreeEntries = 1, maxTreeDepth = 4)
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            policy.writeTextAtomic("large.yml", "12345")
        }
        policy.create("bloom.yml", false)
        assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.create("keys.yml", false) }
    }

    @Test
    fun `rejects symbolic links`() {
        val outside = Files.createTempDirectory("orryx-editor-outside")
        val link = root.resolve("linked")
        val linked = runCatching { Files.createSymbolicLink(link, outside) }.isSuccess
        try {
            assumeTrue(linked, "当前环境不允许创建符号链接")
            val policy = EditorFilePolicy(root)
            assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.listTree(null) }
            assertThrows(EditorFilePolicy.PolicyException::class.java) { policy.writeTextAtomic("linked/file.yml", "x") }
        } finally {
            Files.deleteIfExists(link)
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun `renames and deletes without touching root`() {
        val policy = EditorFilePolicy(root)
        policy.writeTextAtomic("jobs/a.yml", "a")
        assertTrue(policy.rename("jobs/a.yml", "jobs/b.yml"))
        assertEquals("a", policy.readText("jobs/b.yml"))
        assertTrue(policy.delete("jobs"))
        assertTrue(Files.exists(root))
    }

    @Test
    fun `counts implicit parents against entry quota`() {
        val allowedRoot = root.resolve("allowed")
        val allowed = EditorFilePolicy(allowedRoot, maxTreeEntries = 3, maxTreeDepth = 4)
        allowed.writeTextAtomic("skills/nested/fire.yml", "x")
        assertEquals("x", allowed.readText("skills/nested/fire.yml"))

        val rejectedRoot = root.resolve("rejected")
        val rejected = EditorFilePolicy(rejectedRoot, maxTreeEntries = 2, maxTreeDepth = 4)
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            rejected.create("skills/nested/fire.yml", false)
        }
        assertFalse(Files.exists(rejectedRoot.resolve("skills")))
    }

    @Test
    fun `enforces mutation and renamed subtree depth`() {
        val depthRoot = root.resolve("depth")
        val policy = EditorFilePolicy(depthRoot, maxTreeDepth = 2)
        policy.writeTextAtomic("skills/fire.yml", "x")
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            policy.writeTextAtomic("skills/nested/fire.yml", "x")
        }
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            policy.create("jobs/nested/job.yml", false)
        }

        val renameRoot = root.resolve("rename-depth")
        val seed = EditorFilePolicy(renameRoot, maxTreeDepth = 4)
        seed.writeTextAtomic("skills/group/fire.yml", "x")
        val limited = EditorFilePolicy(renameRoot, maxTreeDepth = 2)
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            limited.rename("skills/group", "jobs/group")
        }
        assertTrue(Files.exists(renameRoot.resolve("skills/group/fire.yml")))
    }

    @Test
    fun `validates rename capacity including implicit target parents`() {
        val renameRoot = root.resolve("rename-capacity")
        val seed = EditorFilePolicy(renameRoot, maxTreeEntries = 10, maxTreeDepth = 5)
        seed.writeTextAtomic("jobs/a.yml", "a")

        val limited = EditorFilePolicy(renameRoot, maxTreeEntries = 3, maxTreeDepth = 5)
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            limited.rename("jobs/a.yml", "skills/nested/a.yml")
        }
        assertTrue(Files.exists(renameRoot.resolve("jobs/a.yml")))
        assertFalse(Files.exists(renameRoot.resolve("skills")))

        val allowed = EditorFilePolicy(renameRoot, maxTreeEntries = 4, maxTreeDepth = 5)
        assertTrue(allowed.rename("jobs/a.yml", "skills/nested/a.yml"))
        assertEquals("a", allowed.readText("skills/nested/a.yml"))
    }

    @Test
    fun `manifest file limit rejects snapshots and mutations beyond relay contract`() {
        val policyRoot = root.resolve("manifest-limit")
        val policy = EditorFilePolicy(policyRoot, maxManifestFiles = 2)
        policy.writeTextAtomic("skills/a.yml", "a")
        policy.writeTextAtomic("skills/b.yml", "b")
        assertEquals(2, policy.snapshotManifest().files.size)
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            policy.create("skills/c.yml", false)
        }

        val externalRoot = root.resolve("manifest-external")
        Files.createDirectories(externalRoot.resolve("skills"))
        Files.write(externalRoot.resolve("skills/a.yml"), "a".toByteArray())
        Files.write(externalRoot.resolve("skills/b.yml"), "b".toByteArray())
        Files.write(externalRoot.resolve("skills/c.yml"), "c".toByteArray())
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            EditorFilePolicy(externalRoot, maxManifestFiles = 2).snapshotManifest()
        }
    }

    @Test
    fun `supports revisions and rejects stale writes`() {
        val policy = EditorFilePolicy(root.resolve("revision"))
        val firstRevision = policy.writeTextAtomic("skills/fire.yml", "first")
        val read = policy.readTextWithRevision("skills/fire.yml")
        assertEquals("first", read.content)
        assertEquals(firstRevision, read.revision)
        assertEquals(64, read.revision.length)

        val secondRevision = policy.writeTextAtomic("skills/fire.yml", "second", firstRevision)
        assertEquals(secondRevision, policy.readTextWithRevision("skills/fire.yml").revision)
        val stale = assertThrows(EditorFilePolicy.RevisionConflictException::class.java) {
            policy.writeTextAtomic("skills/fire.yml", "stale", firstRevision)
        }
        assertEquals("REVISION_CONFLICT", stale.errorCode)
        assertEquals("skills/fire.yml", stale.safePath)
        assertEquals(secondRevision, stale.currentRevision)
        val missing = assertThrows(EditorFilePolicy.RevisionConflictException::class.java) {
            policy.writeTextAtomic("skills/missing.yml", "new", firstRevision)
        }
        assertEquals("skills/missing.yml", missing.safePath)
        assertNull(missing.currentRevision)
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            policy.writeTextAtomic("skills/fire.yml", "invalid", firstRevision.uppercase())
        }
        assertThrows(EditorFilePolicy.PolicyException::class.java) {
            policy.writeTextAtomic("skills/fire.yml", "invalid", "abc")
        }
        assertEquals("second", policy.readText("skills/fire.yml"))
    }

    @Test
    fun `supports explicit present and absent mutation preconditions`() {
        val policy = EditorFilePolicy(root.resolve("preconditions"))
        assertEquals(
            EditorFilePolicy.EMPTY_FILE_REVISION,
            policy.create("skills/a.yml", false, ExpectedPathState.ABSENT),
        )
        val createConflict = assertThrows(EditorFilePolicy.PreconditionFailedException::class.java) {
            policy.create("skills/a.yml", false, ExpectedPathState.ABSENT)
        }
        assertEquals("PRECONDITION_FAILED", createConflict.errorCode)
        assertEquals("skills/a.yml", createConflict.safePath)
        assertEquals(EditorFilePolicy.EMPTY_FILE_REVISION, createConflict.currentRevision)
        assertThrows(EditorFilePolicy.PreconditionFailedException::class.java) {
            policy.delete("skills/a.yml", ExpectedPathState.ABSENT)
        }

        policy.writeTextAtomic("jobs/b.yml", "b")
        assertThrows(EditorFilePolicy.PreconditionFailedException::class.java) {
            policy.rename(
                "jobs/b.yml",
                "jobs/c.yml",
                ExpectedPathState.ABSENT,
                ExpectedPathState.ABSENT,
            )
        }
        assertTrue(
            policy.rename(
                "jobs/b.yml",
                "jobs/c.yml",
                ExpectedPathState.PRESENT,
                ExpectedPathState.ABSENT,
            ),
        )
        assertTrue(policy.delete("jobs/c.yml", ExpectedPathState.PRESENT))
    }

    @Test
    fun `exposes allowlist descriptor without internal editor directory`() {
        val policy = EditorFilePolicy(root.resolve("descriptor"))
        assertTrue(policy.allowlist.allowsTopLevel("skills"))
        assertTrue(policy.allowlist.allowsTopLevel("KEYS.YML"))
        assertFalse(policy.allowlist.allowsTopLevel("config.yml"))
        assertFalse(policy.allowlist.allowsTopLevel(".editor"))
    }
}
