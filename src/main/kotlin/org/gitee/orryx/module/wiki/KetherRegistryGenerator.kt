package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion
import java.io.File
import java.time.Instant
import java.util.Locale

/**
 * Schema v4 Kether Registry。
 *
 * v4 由与 v3 actions-schema 相同的运行时注册表生成；v3 继续作为兼容资产发布。
 */
object KetherRegistryGenerator {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private fun runtimeMetadata(): KetherDocsMetadata = KetherDocsContract.metadata(
        pluginId = pluginId,
        version = pluginVersion,
        commit = System.getProperty(KetherDocsPublisher.COMMIT_PROPERTY)
            ?.takeIf { Regex("^[0-9a-fA-F]{40}$").matches(it) }
            ?.lowercase(Locale.ROOT)
            ?: "0000000000000000000000000000000000000000",
        channel = "snapshot",
        generatedAt = Instant.EPOCH
    )

    fun generateRegistry(): JsonObject = generateRegistry(runtimeMetadata())

    fun generateRegistry(metadata: KetherDocsMetadata): JsonObject {
        val legacy = ActionsSchemaGenerator.generateSchema(metadata)
        val actions = legacy.getValue("actions").jsonArray.map { actionElement ->
            val action = actionElement.jsonObject
            buildJsonObject {
                action.forEach { (key, value) ->
                    if (key !in setOf("aliases", "output", "outputStatus", "requirements", "syntax", "inputs")) {
                        put(key, value)
                    }
                }
                put("aliases", JsonArray(action.getValue("aliases").jsonArray.map { alias ->
                    buildJsonObject {
                        put("name", alias.jsonPrimitive.content)
                        put("kind", "parser")
                    }
                }))
                put("grammar", buildJsonObject {
                    put("syntax", action.getValue("syntax"))
                    put("inputs", action.getValue("inputs"))
                    put("variants", JsonArray(listOf(buildJsonObject {
                        put("id", "default")
                        put("syntax", action.getValue("syntax"))
                        put("inputs", action.getValue("inputs"))
                    })))
                })
                val output = action.getValue("output")
                put("output", if (output is JsonNull) {
                    buildJsonObject { put("status", "none") }
                } else {
                    buildJsonObject {
                        put("status", "declared")
                        output.jsonObject.forEach { (key, value) -> put(key, value) }
                    }
                })
                put("requirements", JsonArray(action.getValue("requirements").jsonArray.map { requirement ->
                    buildJsonObject {
                        put("id", requirement.jsonPrimitive.content)
                        put("required", true)
                    }
                }))
            }
        }
        val namespaces = actions.map { it.getValue("namespace").jsonPrimitive.content }.distinct().sorted()
        return buildJsonObject {
            put("${'$'}schema", "$KETHER_DOCS_BASE_URL/contracts/kether-registry-v4.schema.json")
            put("registryVersion", KETHER_REGISTRY_VERSION)
            put("schemaVersion", KETHER_REGISTRY_VERSION)
            put("plugin", legacy.getValue("plugin"))
            put("pluginId", metadata.pluginId)
            put("pluginVersion", metadata.version)
            put("commit", metadata.commit)
            put("namespaces", JsonArray(namespaces.map(::JsonPrimitive)))
            put("types", legacy.getValue("types"))
            put("categories", legacy.getValue("categories"))
            put("actions", JsonArray(actions))
            put("selectors", legacy.getValue("selectors"))
            put("triggers", legacy.getValue("triggers"))
            put("properties", legacy.getValue("properties"))
            put("compatibility", buildJsonObject {
                put("actionsSchemaVersion", KETHER_ACTIONS_SCHEMA_VERSION)
                put("actionsSchemaAsset", "actions-schema.json")
            })
        }
    }

    fun generateJsonString(metadata: KetherDocsMetadata): String =
        json.encodeToString(JsonObject.serializer(), generateRegistry(metadata))

    fun generate(outputFile: File, metadata: KetherDocsMetadata): JsonObject {
        val registry = generateRegistry(metadata)
        KetherDocsContract.writeUtf8(outputFile, json.encodeToString(JsonObject.serializer(), registry))
        return registry
    }
}
