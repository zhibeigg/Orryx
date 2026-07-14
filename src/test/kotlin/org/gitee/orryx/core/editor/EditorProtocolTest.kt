package org.gitee.orryx.core.editor

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class EditorProtocolTest {

    @Test
    fun `editor endpoints are fixed to the official center`() {
        assertEquals("orryx.mcwar.cn", EditorTokenManager.CENTER_HOST)
        assertEquals("https://orryx.mcwar.cn", EditorTokenManager.PUBLIC_URL)
        assertEquals("wss://orryx.mcwar.cn/ws/server", EditorTokenManager.SERVER_URL)
    }

    @Test
    fun `editor token url uses the fixed connect route and fragment credential`() {
        assertEquals(
            "https://orryx.mcwar.cn/connect#token=secret-token",
            EditorTokenManager.buildEditorUrl("secret-token"),
        )
    }

    @Test
    fun `editor command supports console url output while player chat stays click only`() {
        val commandSource = String(
            Files.readAllBytes(Paths.get("src/main/kotlin/org/gitee/orryx/command/OrryxCommand.kt")),
            StandardCharsets.UTF_8,
        )
        val languageSource = String(
            Files.readAllBytes(Paths.get("src/main/resources/lang/zh_CN.yml")),
            StandardCharsets.UTF_8,
        )

        assertEquals("CONSOLE", EditorTokenManager.CONSOLE_ACTOR)
        assertTrue("exec<ProxyCommandSender>" in commandSource)
        assertTrue("sender.castSafely<Player>()" in commandSource)
        assertTrue("sender.sendLang(\"editor-open-console\", url)" in commandSource)
        assertTrue("player.sendLang(\"editor-open\", url)" in commandSource)
        assertTrue("editor-open-console:" in languageSource)
        assertTrue("一次性网址" in languageSource)
        assertEquals(
            "https://orryx.mcwar.cn/connect#token=***",
            EditorClient.sanitizeLogMessage("https://orryx.mcwar.cn/connect#token=secret-token"),
        )
    }

    @Test
    fun `registration includes stable identity negotiation and nonce fields`() {
        val request = ServerRegisterRequest(
            license = "secret-license",
            serverName = "Test Server",
            serverId = "123e4567-e89b-12d3-a456-426614174000",
            pluginVersion = "2.51.124",
            protocolVersions = EditorProtocol.supportedProtocols(v2Enabled = true),
            preferredProtocol = EditorProtocol.preferredProtocol(v2Enabled = true),
            capabilities = EditorProtocol.V2_CAPABILITIES,
            connectionNonce = "nonce-1",
        )
        val data = EditorProtocol.registrationData(request)

        assertEquals(request.serverId, data.getValue("serverId").jsonPrimitive.content)
        assertEquals(request.pluginVersion, data.getValue("pluginVersion").jsonPrimitive.content)
        assertEquals(listOf("v2", "v1"), data.getValue("protocolVersions").jsonArray.map { it.jsonPrimitive.content })
        assertEquals("v2", data.getValue("preferredProtocol").jsonPrimitive.content)
        assertEquals("nonce-1", data.getValue("connectionNonce").jsonPrimitive.content)
        val capabilities = data.getValue("capabilities").jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertTrue(capabilities.containsAll(setOf(
            "release.transaction.v1",
            "release.signature.ed25519",
            "release.readiness.async",
            "release.recovery.v1",
            "release.http-pull.v1",
        )))
    }

    @Test
    fun `legacy registration result explicitly falls back to v1`() {
        val result = EditorProtocol.parseRegisterResult(buildJsonObject {
            put("success", true)
            put("message", "ok")
        })

        assertTrue(result.success)
        assertEquals(EditorProtocol.PROTOCOL_V1, result.negotiatedProtocol)
        assertNull(result.sessionEpoch)
        assertNull(result.workspaceId)
        assertTrue(EditorProtocol.validateNegotiatedProtocol(result, listOf("v2", "v1")))
    }

    @Test
    fun `v2 registration result retains relay session metadata`() {
        val workspaceId = "a".repeat(64)
        val result = EditorProtocol.parseRegisterResult(buildJsonObject {
            put("success", true)
            put("negotiatedProtocol", "v2")
            put("serverId", "123e4567-e89b-12d3-a456-426614174000")
            put("sessionEpoch", 42L)
            put("workspaceId", workspaceId)
            put("connectionNonce", "nonce-1")
            put("relayCapabilities", kotlinx.serialization.json.buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive("revision.sha256"))
                add(kotlinx.serialization.json.JsonPrimitive("release.control.v1"))
            })
        })

        assertEquals("v2", result.negotiatedProtocol)
        assertEquals("123e4567-e89b-12d3-a456-426614174000", result.serverId)
        assertEquals(42L, result.sessionEpoch)
        assertEquals(workspaceId, result.workspaceId)
        assertTrue(EditorProtocol.isSha256Revision(result.workspaceId))
        assertFalse(EditorProtocol.isSha256Revision(workspaceId.uppercase()))
        assertEquals(listOf("revision.sha256", "release.control.v1"), result.relayCapabilities)
        assertEquals("nonce-1", result.connectionNonce)
        assertFalse(EditorProtocol.validateNegotiatedProtocol(result, listOf("v1")))
    }

    @Test
    fun `registration validation enforces v2 identity nonce and sha metadata`() {
        val request = ServerRegisterRequest(
            license = "secret-license",
            serverName = "Test Server",
            serverId = "123e4567-e89b-12d3-a456-426614174000",
            pluginVersion = "2.51.124",
            protocolVersions = listOf("v2", "v1"),
            preferredProtocol = "v2",
            capabilities = EditorProtocol.V2_CAPABILITIES,
            connectionNonce = "nonce-1",
        )
        val valid = ServerRegisterResult(
            success = true,
            message = "ok",
            negotiatedProtocol = "v2",
            serverId = request.serverId,
            sessionEpoch = 1L,
            workspaceId = "a".repeat(64),
            relayCapabilities = listOf("revision.sha256", "release.control.v1"),
            connectionNonce = request.connectionNonce,
        )

        assertTrue(EditorProtocol.validateRegisterResult(valid, request).accepted)
        assertFalse(EditorProtocol.validateRegisterResult(valid.copy(serverId = "other"), request).accepted)
        assertFalse(EditorProtocol.validateRegisterResult(valid.copy(connectionNonce = null), request).accepted)
        assertFalse(EditorProtocol.validateRegisterResult(valid.copy(workspaceId = "invalid"), request).accepted)
        assertFalse(EditorProtocol.validateRegisterResult(valid.copy(sessionEpoch = 0L), request).accepted)
        assertFalse(EditorProtocol.validateRegisterResult(valid.copy(relayCapabilities = emptyList()), request).accepted)
        assertFalse(
            EditorProtocol.validateRegisterResult(valid.copy(relayCapabilities = listOf("revision.sha256")), request).accepted,
        )

        val legacy = valid.copy(
            negotiatedProtocol = "v1",
            serverId = null,
            sessionEpoch = null,
            workspaceId = null,
            relayCapabilities = emptyList(),
            connectionNonce = null,
        )
        assertTrue(EditorProtocol.validateRegisterResult(legacy, request).accepted)
    }

    @Test
    fun `bundled relay contract matches plugin allowlists`() {
        val bytes = requireNotNull(javaClass.classLoader.getResourceAsStream("editor-relay-contract-v2.json")) {
            "缺少 editor-relay-contract-v2.json"
        }.use { it.readBytes() }
        val manifest = Json.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
        val directions = manifest.getValue("directions").jsonObject

        assertEquals(directions.stringSet("relayToPlugin"), EditorProtocol.allowedInboundTypes())
        assertEquals(directions.stringSet("pluginToRelay"), EditorProtocol.allowedOutboundTypes())
        assertEquals(setOf("v1", "v2"), manifest.stringSet("protocolVersions"))
        val reserved = manifest.stringSet("reservedUnroutedTypes")
        assertTrue(reserved.intersect(EditorProtocol.allowedInboundTypes()).isEmpty())
        assertTrue(reserved.intersect(EditorProtocol.allowedOutboundTypes()).isEmpty())
    }

    @Test
    fun `direction allowlist matches relay routes and rejects reserved messages`() {
        assertEquals(InboundDisposition.ACCEPT, EditorProtocol.inboundDisposition("file.read"))
        assertEquals(InboundDisposition.ACCEPT, EditorProtocol.inboundDisposition("token.revoke.result"))
        assertEquals(InboundDisposition.ACCEPT, EditorProtocol.inboundDisposition(EditorProtocol.RELEASE_REQUEST))
        assertEquals(InboundDisposition.WRONG_DIRECTION, EditorProtocol.inboundDisposition("file.content"))
        assertEquals(InboundDisposition.UNKNOWN, EditorProtocol.inboundDisposition(EditorProtocol.MANIFEST_GET))
        assertEquals(InboundDisposition.UNKNOWN, EditorProtocol.inboundDisposition("actions.schema"))
        assertEquals(InboundDisposition.UNKNOWN, EditorProtocol.inboundDisposition("center.future.command"))
        assertTrue(EditorProtocol.isServerToCenter("error"))
        assertTrue(EditorProtocol.isServerToCenter("token.revoke"))
        assertTrue(EditorProtocol.isServerToCenter("file.changed"))
        assertTrue(EditorProtocol.isServerToCenter("server.info"))
        assertTrue(EditorProtocol.isServerToCenter(EditorProtocol.RELEASE_RESULT))
        assertFalse(EditorProtocol.isSupportedForProtocol(EditorProtocol.RELEASE_REQUEST, EditorProtocol.PROTOCOL_V1))
        assertTrue(EditorProtocol.isSupportedForProtocol(EditorProtocol.RELEASE_REQUEST, EditorProtocol.PROTOCOL_V2))
        assertFalse(EditorProtocol.isServerToCenter(EditorProtocol.MANIFEST_SNAPSHOT))
        assertFalse(EditorProtocol.isServerToCenter("file.read"))
    }

    private fun JsonObject.stringSet(key: String): Set<String> =
        (getValue(key) as JsonArray).mapNotNull { it.jsonPrimitive.contentOrNull }.toSet()
}
