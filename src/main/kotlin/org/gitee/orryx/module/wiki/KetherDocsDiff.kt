package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

internal object KetherDocsDiff {

    private val json = Json { prettyPrint = true }

    fun generate(
        previousSchemaFile: File?,
        currentSchema: JsonObject,
        previousReleaseId: String?,
        currentReleaseId: String
    ): JsonObject {
        val previousSchema = previousSchemaFile?.let { file ->
            require(file.isFile) { "上一版 Kether Schema 不存在: ${file.absolutePath}" }
            runCatching { Json.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject }
                .getOrElse { throw IllegalArgumentException("无法解析上一版 Kether Schema: ${file.absolutePath}", it) }
        }

        val actionDiff = diffCollection(
            previousSchema?.array("actions"),
            currentSchema.array("actions"),
            ::actionIdentity
        )
        val selectorDiff = diffCollection(
            previousSchema?.array("selectors"),
            currentSchema.array("selectors"),
            { item -> item.string("id") ?: "selector:${item.string("name").orEmpty()}" }
        )
        val triggerDiff = diffCollection(
            previousSchema?.array("triggers"),
            currentSchema.array("triggers"),
            { item -> item.string("id") ?: "trigger:${item.string("category").orEmpty()}:${item.string("name").orEmpty()}" }
        )
        val propertyDiff = diffCollection(
            previousSchema?.array("properties"),
            currentSchema.array("properties"),
            { item -> item.string("id") ?: "property:${item.string("name").orEmpty()}" }
        )

        return buildJsonObject {
            put("formatVersion", 1)
            if (previousReleaseId == null) put("fromReleaseId", JsonNull) else put("fromReleaseId", previousReleaseId)
            put("toReleaseId", currentReleaseId)
            put("breaking", actionDiff.removed.isNotEmpty())
            put("summary", buildJsonObject {
                put("actionsAdded", actionDiff.added.size)
                put("actionsRemoved", actionDiff.removed.size)
                put("actionsChanged", actionDiff.changed.size)
                put("selectorsChanged", selectorDiff.totalChanges)
                put("triggersChanged", triggerDiff.totalChanges)
                put("propertiesChanged", propertyDiff.totalChanges)
            })
            put("actions", actionDiff.toJson())
            put("selectors", selectorDiff.toJson())
            put("triggers", triggerDiff.toJson())
            put("properties", propertyDiff.toJson())
        }
    }

    fun encode(value: JsonObject): String = json.encodeToString(JsonObject.serializer(), value)

    private data class CollectionDiff(
        val added: List<String>,
        val removed: List<String>,
        val changed: List<String>
    ) {
        val totalChanges: Int get() = added.size + removed.size + changed.size

        fun toJson(): JsonObject = buildJsonObject {
            put("added", JsonArray(added.map(::JsonPrimitive)))
            put("removed", JsonArray(removed.map(::JsonPrimitive)))
            put("changed", JsonArray(changed.map(::JsonPrimitive)))
        }
    }

    private fun diffCollection(
        previous: JsonArray?,
        current: JsonArray,
        identity: (JsonObject) -> String
    ): CollectionDiff {
        val previousMap = previous.orEmpty().map(JsonElement::jsonObject).associateBy(identity)
        val currentMap = current.map(JsonElement::jsonObject).associateBy(identity)
        val added = currentMap.keys.minus(previousMap.keys).sorted().map { currentMap.getValue(it).displayId(it) }
        val removed = previousMap.keys.minus(currentMap.keys).sorted().map { previousMap.getValue(it).displayId(it) }
        val changed = previousMap.keys.intersect(currentMap.keys)
            .filter { key -> canonical(previousMap.getValue(key)) != canonical(currentMap.getValue(key)) }
            .sorted()
            .map { currentMap.getValue(it).displayId(it) }
        return CollectionDiff(added, removed, changed)
    }

    private fun actionIdentity(item: JsonObject): String {
        item.string("id")?.let { return "id:$it" }
        val inputs = item.array("inputs").mapIndexed { index, element ->
            val input = element.jsonObject
            listOf(
                index.toString(),
                input.string("keyword").orEmpty(),
                input.string("type").orEmpty(),
                input["required"]?.jsonPrimitive?.contentOrNull.orEmpty()
            ).joinToString(":")
        }
        val outputType = (item["output"] as? JsonObject)?.string("type").orEmpty()
        return listOf(
            item.string("namespace").orEmpty(),
            item.string("name").orEmpty(),
            inputs.joinToString("|"),
            outputType,
            item.string("flow").orEmpty()
        ).joinToString("|")
    }

    private fun canonical(item: JsonObject): String = item.entries
        .filterNot { (key, _) -> key == "source" }
        .sortedBy { (key, _) -> key }
        .joinToString("|") { (key, value) -> "$key=${canonical(value)}" }

    private fun canonical(value: JsonElement): String = when (value) {
        is JsonObject -> canonical(value)
        is JsonArray -> value.joinToString(prefix = "[", postfix = "]") { canonical(it) }
        else -> value.toString()
    }

    private fun JsonObject.displayId(fallback: String): String = string("id") ?: fallback

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.array(key: String): JsonArray = this[key]?.jsonArray ?: JsonArray(emptyList())

    private fun List<JsonElement>?.orEmpty(): List<JsonElement> = this ?: emptyList()
}
