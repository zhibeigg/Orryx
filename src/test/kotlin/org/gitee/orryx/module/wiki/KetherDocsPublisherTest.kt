package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
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
        assertEquals(4, manifest.getValue("registryVersion").jsonPrimitive.content.toInt())
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
        assertFalse("registryVersion" in manifest)
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
            KetherDocsContracts.actionsSchema,
            KetherRegistryContracts.registryV4
        )) {
            assertTrue(Json.parseToJsonElement(contract).jsonObject.isNotEmpty())
        }
        assertEquals(
            "60b31dcb79ea5f280788de83d4b5ee6b70f49fd226fde93475f1775e743f8940",
            sha256Contract(KetherDocsContracts.releaseManifest)
        )
    }

    @Test
    fun `type graph accepted sets and raw input metadata are complete`() {
        assertEquals(Type.entries.size, Type.entries.map(Type::id).distinct().size)
        assertEquals(setOf(Type.ANY), Type.NULL.parents)
        for (type in Type.entries) {
            assertTrue(type.rawType.isNotBlank(), "${type.id} rawType")
            assertTrue(type.isAssignableFrom(type), "${type.id} must accept itself")
            type.parents.forEach { parent ->
                assertTrue(type in parent.children, "${type.id} parent/child mismatch: ${parent.id}")
                assertFalse(
                    type.parents.any { other -> other != parent && parent.isAssignableFrom(other) },
                    "${type.id} has redundant direct parent ${parent.id}"
                )
            }
            type.children.forEach { child ->
                assertTrue(type in child.parents, "${type.id} child/parent mismatch: ${child.id}")
            }
        }
        for (expected in Type.entries) {
            for (actual in Type.entries) {
                val inherited = generateSequence(setOf(actual)) { level ->
                    level.flatMapTo(linkedSetOf()) { it.parents }.takeIf(Set<Type>::isNotEmpty)
                }.flatten().toSet()
                assertEquals(expected in inherited, expected.isAssignableFrom(actual), "${expected.id} <- ${actual.id}")
            }
        }
        assertTrue(Type.NUMBER.isAssignableFrom(Type.INT))
        assertTrue(Type.TARGET.isAssignableFrom(Type.PLAYER))
        assertTrue(Type.ANY.isAssignableFrom(Type.SKILL_PARAMETER))
        assertEquals(
            setOf(Type.NUMBER, Type.STRING),
            Type.minimalAcceptedTypes(setOf(Type.INT, Type.NUMBER, Type.STRING))
        )
        assertThrows(IllegalArgumentException::class.java) {
            Type.minimalAcceptedTypes(setOf(Type.ANY, Type.STRING))
        }

        val unionEntry = Action.new("test", "contains", "contains")
            .addEntry("Iterable 或 String", Type.ANY, acceptedTypes = setOf(Type.ITERABLE, Type.STRING))
            .entries.single()
        val unionInput = ActionsSchemaGenerator.input(unionEntry, 0)
        assertEquals(
            setOf(Type.ITERABLE.id, Type.STRING.id),
            unionInput.getValue("acceptedTypes").jsonArray.map { it.jsonPrimitive.content }.toSet()
        )

        assertEquals(false, Type.PROFILE.ketherFillable)
        val rawEntry = Action.new("test", "profile", "profile").addEntry("档案", Type.PROFILE).entries.single()
        val rawInput = ActionsSchemaGenerator.input(rawEntry, 0)
        assertEquals("false", rawInput.getValue("ketherFillable").jsonPrimitive.content)
        assertEquals(Type.PROFILE.rawType, rawInput.getValue("rawType").jsonPrimitive.content)
        assertTrue(rawInput.getValue("inputHint").jsonPrimitive.content.contains("raw"))
    }

    @Test
    fun `aliases keywords and trigger entry metadata are structured`() {
        val wait = Action.new("Kether原生-延迟", "延迟", "wait")
        assertEquals(listOf("delay", "sleep"), KetherDocsContract.actionAliases(wait))

        val parameter = Action.new("上下文", "参数", "parameter/parm")
        assertEquals("parameter", KetherDocsContract.actionName(parameter))
        assertEquals(listOf("parm"), KetherDocsContract.actionAliases(parameter))

        val entry = Trigger.Entry(Type.STRING, "from/old", "旧值", writable = true, nullable = true)
        assertEquals("from", entry.key)
        assertEquals(listOf("old"), entry.aliases)
        assertTrue(entry.readable)
        assertTrue(entry.writable)
        assertTrue(entry.nullable)
    }

    @Test
    fun `release manifest v1 keeps editor schema v3 while attaching registry v4 assets`() {
        val metadata = KetherDocsContract.metadata(
            pluginId = "Orryx",
            version = "2.53.126",
            commit = commit,
            channel = "snapshot",
            generatedAt = Instant.parse("2026-07-15T00:00:00Z")
        )
        val manifest = Json.parseToJsonElement(
            KetherDocsPublisher.buildReleaseManifest(metadata, counts(), mapOf(
                "registry" to asset("kether-registry.json"),
                "registryContract" to asset("kether-registry.schema.json"),
                "schema" to asset("actions-schema.json"),
                "schemaContract" to asset("actions-schema.schema.json"),
                "markdown" to asset("docs.md", "text/markdown; charset=utf-8"),
                "changes" to asset("changes.json"),
                "checksums" to asset("checksums.json")
            ))
        ).jsonObject
        assertEquals(3, manifest.getValue("schemaVersion").jsonPrimitive.content.toInt())
        assertFalse("registryVersion" in manifest)
        assertTrue("registry" in manifest.getValue("assets").jsonObject)
        val compatibility = manifest.getValue("compatibility").jsonObject
        assertEquals(3, compatibility.getValue("minimumEditorSchemaVersion").jsonPrimitive.content.toInt())
        assertFalse("minimumEditorRegistryVersion" in compatibility)
    }

    @Test
    fun `version filenames are sanitized`() {
        assertEquals("2.43.114_build_1", KetherDocsPublisher.sanitizeVersion("2.43.114+build/1"))
    }

    private fun sha256Contract(content: String): String = MessageDigest.getInstance("SHA-256")
        .digest((content.trimEnd() + "\n").toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

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
