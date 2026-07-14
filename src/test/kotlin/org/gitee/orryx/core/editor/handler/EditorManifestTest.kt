package org.gitee.orryx.core.editor.handler

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class EditorManifestTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `manifest is deterministic uses sha256 and ignores internal identity`() {
        val policy = EditorFilePolicy(root)
        policy.writeTextAtomic("skills/fire.yml", "abc")
        policy.writeTextAtomic("keys.yml", "key: value")

        Files.createDirectories(root.resolve(".editor"))
        Files.write(
            root.resolve(".editor/identity.json"),
            "{\"serverId\":\"not-remote\"}".toByteArray(StandardCharsets.UTF_8),
        )

        val first = policy.snapshotManifest()
        val second = policy.snapshotManifest()
        assertEquals("working-tree", first.manifestId)
        assertEquals(first, second)
        assertEquals(64, first.rootHash.length)
        val encoded = Json.encodeToJsonElement(ManifestSnapshotV1.serializer(), first).jsonObject
        assertEquals(setOf("manifestId", "revision", "files"), encoded.keys)
        assertTrue(first.files.none { it.path.startsWith(".editor") })

        val fire = first.files.single { it.path == "skills/fire.yml" }
        assertEquals(3L, fire.size)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", fire.revision)

        policy.writeTextAtomic("skills/fire.yml", "abcd", fire.revision)
        assertNotEquals(first.rootHash, policy.snapshotManifest().rootHash)
    }

    @Test
    fun `canonical root hash does not depend on input order`() {
        val a = ManifestEntryV1("skills/a.yml", "aa", 1L)
        val b = ManifestEntryV1("skills/b.yml", "bb", 1L)
        assertEquals(
            ManifestCanonicalHash.calculate(listOf(a, b)),
            ManifestCanonicalHash.calculate(listOf(b, a)),
        )
    }

    @Test
    fun `manifest and tree reject case insensitive collisions`() {
        val skills = root.resolve("skills")
        Files.createDirectories(skills)
        Files.write(skills.resolve("Fire.yml"), byteArrayOf(1))
        val secondCreated = runCatching {
            Files.write(skills.resolve("fire.yml"), byteArrayOf(2))
            Files.list(skills).use { stream -> stream.count() == 2L }
        }.getOrDefault(false)
        assumeTrue(secondCreated, "当前文件系统不支持仅大小写不同的两个文件")

        val policy = EditorFilePolicy(root)
        assertThrows(EditorFilePolicy.CaseConflictException::class.java) { policy.listTree(null) }
        assertThrows(EditorFilePolicy.CaseConflictException::class.java) { policy.snapshotManifest() }
    }
}
