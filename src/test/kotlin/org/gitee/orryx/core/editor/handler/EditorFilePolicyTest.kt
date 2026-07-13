package org.gitee.orryx.core.editor.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}
