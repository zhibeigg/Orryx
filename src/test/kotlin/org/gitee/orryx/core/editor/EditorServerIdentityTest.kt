package org.gitee.orryx.core.editor

import org.gitee.orryx.core.editor.handler.EditorFileAllowlistDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class EditorServerIdentityTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `creates stable canonical identity without secrets`() {
        val expectedId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val store = EditorServerIdentityStore(root) { expectedId }

        val first = store.loadOrCreate()
        val second = EditorServerIdentityStore(root) { UUID.randomUUID() }.loadOrCreate()

        assertEquals(expectedId.toString(), first.serverId)
        assertEquals(first, second)
        assertEquals(root.resolve(".editor/identity.json").toAbsolutePath().normalize(), store.identityPath)
        val content = String(Files.readAllBytes(store.identityPath), StandardCharsets.UTF_8)
        assertTrue(content.contains(expectedId.toString()))
        assertFalse(content.contains("license", ignoreCase = true))
        assertFalse(content.contains("private", ignoreCase = true))
        assertFalse(Files.list(store.identityPath.parent).use { stream ->
            stream.anyMatch { it.fileName.toString().startsWith(".identity-") }
        })
    }

    @Test
    fun `does not silently replace invalid existing identity`() {
        val identityPath = root.resolve(".editor/identity.json")
        Files.createDirectories(identityPath.parent)
        Files.write(identityPath, "{\"schemaVersion\":1,\"serverId\":\"invalid\"}".toByteArray(StandardCharsets.UTF_8))

        assertThrows(EditorServerIdentityStore.IdentityException::class.java) {
            EditorServerIdentityStore(root).loadOrCreate()
        }
        assertTrue(String(Files.readAllBytes(identityPath), StandardCharsets.UTF_8).contains("invalid"))
    }

    @Test
    fun `identity directory is never part of remote allowlist`() {
        val descriptor = EditorFileAllowlistDescriptor.ORRYX_CONFIG
        assertFalse(descriptor.allowsTopLevel(".editor"))
        assertFalse(descriptor.allowsTopLevel("identity.json"))
    }
}
