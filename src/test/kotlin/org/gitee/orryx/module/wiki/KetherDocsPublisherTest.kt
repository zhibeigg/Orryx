package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class KetherDocsPublisherTest {

    private val commit = "a66cd572be3f1855570ae0ae593220e28c84f4bc"

    @Test
    fun `legacy manifest keeps stable downloads and advertises channels`() {
        val manifest = Json.parseToJsonElement(
            KetherDocsPublisher.buildManifest(
                pluginName = "Orryx",
                version = "2.43.114",
                commit = commit,
                generatedAt = Instant.parse("2026-07-13T03:59:59Z"),
                counts = counts()
            )
        ).jsonObject

        assertEquals("Orryx", manifest.getValue("pluginId").jsonPrimitive.content)
        assertEquals("2.43.114", manifest.getValue("version").jsonPrimitive.content)
        assertEquals(3, manifest.getValue("schemaVersion").jsonPrimitive.content.toInt())
        assertEquals("$KETHER_DOCS_BASE_URL/actions-schema.json", manifest.getValue("schema").jsonPrimitive.content)
        assertEquals("$KETHER_DOCS_BASE_URL/channels/stable.json", manifest.getValue("stableChannel").jsonPrimitive.content)
        assertEquals(10, manifest.getValue("counts").jsonObject.getValue("actions").jsonPrimitive.content.toInt())
    }

    @Test
    fun `channel and release manifests bind an immutable release`() {
        val metadata = KetherDocsContract.metadata(
            pluginId = "Orryx",
            version = "2.43.114",
            commit = commit,
            channel = "stable",
            generatedAt = Instant.parse("2026-07-13T03:59:59Z"),
            previousReleaseId = "Orryx@2.42.113+0000000000000000000000000000000000000000"
        )
        val channel = Json.parseToJsonElement(KetherDocsPublisher.buildChannelManifest(metadata)).jsonObject
        assertEquals(1, channel.getValue("formatVersion").jsonPrimitive.content.toInt())
        assertEquals("stable", channel.getValue("channel").jsonPrimitive.content)
        assertEquals("/Orryx/kether/releases/2.43.114/$commit/manifest.json", channel.getValue("releaseManifest").jsonPrimitive.content)

        val assets = mapOf(
            "schema" to asset("actions-schema.json"),
            "schemaContract" to asset("actions-schema.schema.json"),
            "markdown" to asset("docs.md", "text/markdown; charset=utf-8"),
            "changes" to asset("changes.json"),
            "checksums" to asset("checksums.json")
        )
        val manifest = Json.parseToJsonElement(
            KetherDocsPublisher.buildReleaseManifest(metadata, counts(), assets)
        ).jsonObject
        assertEquals(metadata.releaseId, manifest.getValue("releaseId").jsonPrimitive.content)
        assertEquals(3, manifest.getValue("schemaVersion").jsonPrimitive.content.toInt())
        assertEquals(commit, manifest.getValue("plugin").jsonObject.getValue("commit").jsonPrimitive.content)
        assertEquals(64, manifest.getValue("assets").jsonObject.getValue("schema").jsonObject.getValue("sha256").jsonPrimitive.content.length)
    }

    @Test
    fun `overloaded actions receive deterministic distinct ids`() {
        val set = Action.new("Cooldown冷却", "设置冷却", "cooldown")
            .addEntry("设置标识符", Type.SYMBOL, head = "set/to")
            .addEntry("冷却值", Type.LONG)
        val reset = Action.new("Cooldown冷却", "重置冷却", "cooldown")
            .addEntry("重置标识符", Type.SYMBOL, head = "reset")

        val singleId = KetherDocsContract.actionIds(listOf(set)).getValue(set)
        val first = KetherDocsContract.actionIds(listOf(set, reset))
        val second = KetherDocsContract.actionIds(listOf(set, reset))
        assertEquals(first, second)
        assertEquals(singleId, first.getValue(set))
        assertNotEquals(first.getValue(set), first.getValue(reset))
        assertTrue(first.getValue(set).startsWith("orryx.action.cooldown."))
        assertEquals("cooldown set/to <LONG>", KetherDocsContract.actionSyntax(set))
        assertEquals("冷却值", KetherDocsContract.inputKey(set.entries[1], 1))
    }

    @Test
    fun `published JSON contracts are valid JSON`() {
        for (contract in listOf(
            KetherDocsContracts.channelManifest,
            KetherDocsContracts.releaseManifest,
            KetherDocsContracts.actionsSchema
        )) {
            assertTrue(Json.parseToJsonElement(contract).jsonObject.isNotEmpty())
        }
    }

    @Test
    fun `version filenames are sanitized`() {
        assertEquals("2.43.114_build_1", KetherDocsPublisher.sanitizeVersion("2.43.114+build/1"))
    }

    private fun counts() = KetherDocsPublisher.RegistrationCounts(
        actions = 10,
        selectors = 20,
        triggers = 30,
        properties = 40
    )

    private fun asset(path: String, mediaType: String = "application/json") = KetherDocsAsset(
        path = path,
        mediaType = mediaType,
        bytes = 100,
        sha256 = "0".repeat(64)
    )
}
