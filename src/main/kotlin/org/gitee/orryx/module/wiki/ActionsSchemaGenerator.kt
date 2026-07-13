package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gitee.orryx.core.kether.ScriptManager
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion
import java.io.File
import java.time.Instant
import java.util.Locale

/**
 * 从运行时完整注册表生成供 Orryx Editor 消费的 Kether Schema。
 *
 * 顶层保留 `version = 2` 兼容旧消费者，并通过 `schemaVersion = 3`
 * 声明稳定 ID、语法、属性和发布元数据扩展。
 */
object ActionsSchemaGenerator {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private fun mapTypeKey(type: Type): String = when (type) {
        Type.DOUBLE, Type.FLOAT -> "number"
        Type.INT, Type.SHORT, Type.BYTE -> "int"
        Type.LONG -> "long"
        Type.STRING -> "text"
        Type.BOOLEAN -> "boolean"
        Type.SYMBOL -> "keyword"
        Type.CONTAINER, Type.TARGET, Type.PLAYER -> "selector"
        Type.VECTOR, Type.QUATERNION -> "vector3"
        Type.MATRIX -> "matrix"
        Type.ITERABLE -> "list"
        Type.ANY, Type.NULL -> "any"
        else -> "any"
    }

    private fun mapActionCategory(group: String): String {
        val lower = group.lowercase(Locale.ROOT)
        return when {
            "kether原生-控制流" in lower || "kether原生-延迟" in lower || "kether原生-脚本" in lower -> "logic"
            "kether原生-循环" in lower -> "loop"
            "kether原生-输出" in lower -> "output"
            "kether原生-变量" in lower || "kether原生-类型转换" in lower || "kether原生-数组" in lower -> "variable"
            "kether原生-数学" in lower -> "math"
            "kether原生-时间" in lower -> "time"
            "kether原生-游戏" in lower -> "game"
            "kether原生-属性" in lower -> "variable"
            "damage" in lower || "伤害" in lower || "mana" in lower || "法力" in lower ||
                "spirit" in lower || "精力" in lower || "buff" in lower || "state" in lower ||
                "状态" in lower || "cooldown" in lower || "冷却" in lower || "hitbox" in lower ||
                "碰撞" in lower || "attribute" in lower || "属性系统" in lower || "astraxhero" in lower ||
                "attributeplus" in lower || "nodens" in lower || "skill" in lower || "技能" in lower ||
                "pressskill" in lower -> "combat"
            "move" in lower || "移动" in lower || "dash" in lower || "冲刺" in lower ||
                "传送" in lower || "projectile" in lower || "抛射" in lower -> "movement"
            "particle" in lower || "粒子" in lower || "effect" in lower || "特效" in lower -> "particle"
            "sound" in lower || "音效" in lower -> "sound"
            "entity" in lower || "实体" in lower || "ai" in lower || "智能" in lower -> "entity"
            "world" in lower || "世界" in lower -> "world"
            "coroutine" in lower || "协程" in lower || "pipe" in lower || "管式" in lower ||
                "station" in lower || "中转" in lower -> "logic"
            "variable" in lower || "变量" in lower || "flag" in lower || "标签" in lower ||
                "global" in lower || "全局" in lower || "上下文" in lower || "container" in lower ||
                "容器" in lower || "uuid" in lower -> "variable"
            "math" in lower || "数学" in lower || "calc" in lower -> "math"
            "selector" in lower || "选择" in lower || "raytrace" in lower || "光线" in lower -> "selector"
            "profile" in lower || "玩家信息" in lower || "orryx信息" in lower || "keysetting" in lower ||
                "按键" in lower || "money" in lower || "财富" in lower -> "player"
            "dragoncore" in lower || "germplugin" in lower || "arcartx" in lower || "cloudpick" in lower ||
                "mythicmobs" in lower || "gddtitle" in lower || "mod" in lower -> "compat"
            "game" in lower || "原版游戏" in lower -> "game"
            else -> "misc"
        }
    }

    private fun generateTypes(): JsonObject = buildJsonObject {
        put("number", type("number", "#4FC3F7", 0.1))
        put("int", type("number", "#4DB6AC", 1.0))
        put("long", type("number", "#4DB6AC", 1.0))
        put("text", type("text", "#FFB74D"))
        put("boolean", type("toggle", "#E57373"))
        put("keyword", type("text", "#9E9E9E"))
        put("selector", type("selector", "#BA68C8"))
        put("vector3", type("vector3", "#81C784"))
        put("matrix", type("text", "#A1887F"))
        put("list", type("list", "#7986CB"))
        put("duration", type("duration", "#FFD54F"))
        put("any", type("text", "#B0BEC5"))
        put("enum", type("select", "#F06292"))
        put("location", type("location", "#AED581"))
        put("port", type("port", "#90A4AE"))
    }

    private fun type(widget: String, color: String, step: Double? = null): JsonObject = buildJsonObject {
        put("widget", widget)
        put("color", color)
        if (step != null) put("step", if (step % 1.0 == 0.0) JsonPrimitive(step.toInt()) else JsonPrimitive(step))
    }

    private fun generateCategories(): JsonObject = buildJsonObject {
        put("logic", category("#42A5F5", "mdi-code-braces"))
        put("loop", category("#26C6DA", "mdi-sync"))
        put("output", category("#66BB6A", "mdi-message-text"))
        put("variable", category("#FFA726", "mdi-variable"))
        put("math", category("#AB47BC", "mdi-calculator"))
        put("time", category("#FFCA28", "mdi-clock-outline"))
        put("game", category("#78909C", "mdi-gamepad-variant"))
        put("combat", category("#EF5350", "mdi-sword-cross"))
        put("movement", category("#29B6F6", "mdi-run-fast"))
        put("particle", category("#EC407A", "mdi-sparkles"))
        put("sound", category("#7E57C2", "mdi-volume-high"))
        put("entity", category("#8D6E63", "mdi-account"))
        put("world", category("#26A69A", "mdi-earth"))
        put("selector", category("#5C6BC0", "mdi-target"))
        put("player", category("#42A5F5", "mdi-account-circle"))
        put("compat", category("#78909C", "mdi-puzzle"))
        put("misc", category("#BDBDBD", "mdi-dots-horizontal"))
    }

    private fun category(color: String, icon: String): JsonObject = buildJsonObject {
        put("color", color)
        put("icon", icon)
    }

    private fun generateActions(): JsonArray {
        val source = ScriptManager.wikiActions.toList()
        val ids = KetherDocsContract.actionIds(source)
        val actions = source.sortedBy(ids::get)
        return JsonArray(actions.map { action ->
            val flow = KetherDocsContract.inferFlowType(action)
            buildJsonObject {
                put("id", ids.getValue(action))
                put("name", action.key)
                put("aliases", JsonArray(emptyList()))
                put("category", mapActionCategory(action.group))
                put("namespace", KetherDocsContract.inferNamespace(action))
                put("visibility", if (action.sharded) "public" else "private")
                put("description", action.description.ifBlank { action.name })
                put("syntax", KetherDocsContract.actionSyntax(action))
                put("deprecated", JsonNull)
                if (action.sharded) put("builtin", true)
                put("flow", flow)
                put("inputs", JsonArray(action.entries.mapIndexed { index, entry -> input(entry, index) }))
                put("output", output(action))
                put("examples", JsonArray(action.example.map(::JsonPrimitive)))
                if (action.example.isNotEmpty()) put("example", action.example.joinToString("\n"))
                put("execution", buildJsonObject {
                    put("thread", "any")
                    put("suspends", KetherDocsContract.actionSuspends(action))
                })
                put("requirements", JsonArray(KetherDocsContract.actionRequirements(action).map(::JsonPrimitive)))
                put("source", buildJsonObject {
                    put("symbol", action.key)
                    put("group", action.group)
                })
                addFlowMetadata(this, action, flow)
            }
        })
    }

    private fun input(entry: Action.Entry, index: Int): JsonObject = buildJsonObject {
        put("name", entry.description.ifBlank { entry.head ?: entry.type.name.lowercase(Locale.ROOT) })
        put("key", entry.head?.takeIf(String::isNotBlank) ?: "p$index")
        if (entry.type == Type.SYMBOL) {
            put("type", "keyword")
            put("required", true)
            put("default", JsonPrimitive(entry.head ?: ""))
            entry.head?.let { put("keyword", it) }
        } else {
            put("type", mapTypeKey(entry.type))
            put("required", !entry.optional)
            if (entry.default == null) put("default", JsonNull) else put("default", JsonPrimitive(entry.default))
            if (entry.description.isNotBlank()) put("description", entry.description)
            entry.head?.takeIf(String::isNotBlank)?.let { put("keyword", it) }
        }
    }

    private fun output(action: Action) = if (action.result == Type.NULL) {
        JsonNull
    } else {
        buildJsonObject {
            put("type", mapTypeKey(action.result))
            action.resultDescription?.let { put("description", it) }
        }
    }

    private fun addFlowMetadata(target: JsonObjectBuilder, action: Action, flow: String) {
        when (flow) {
            "branch" -> target.put("slots", JsonArray(listOf(
                buildJsonObject { put("name", "then"); put("label", "条件为真"); put("multiple", true) },
                buildJsonObject { put("name", "else"); put("label", "条件为假"); put("multiple", true); put("optional", true) }
            )))
            "loop" -> target.put("slots", JsonArray(listOf(
                buildJsonObject { put("name", "body"); put("label", "循环体"); put("multiple", true) }
            )))
            "container" -> target.put("slots", JsonArray(listOf(
                buildJsonObject { put("name", "children"); put("label", "子动作"); put("multiple", true) }
            )))
        }
        if (flow == "loop" && action.key.lowercase(Locale.ROOT) in setOf("for", "map")) {
            target.put("provides", JsonArray(listOf(buildJsonObject {
                put("name", "迭代变量")
                put("key", "it")
                put("type", "any")
                put("description", "当前迭代元素")
            })))
        }
    }

    private fun generateSelectors(): JsonArray {
        val source = ScriptManager.wikiSelectors.toList()
        val ids = KetherDocsContract.selectorIds(source)
        return JsonArray(source.sortedBy(ids::get).map { selector ->
            buildJsonObject {
                put("id", ids.getValue(selector))
                put("name", selector.keys.first())
                put("aliases", JsonArray(selector.keys.drop(1).map(::JsonPrimitive)))
                put("description", selector.description.ifBlank { selector.name })
                put("syntax", KetherDocsContract.selectorSyntax(selector))
                put("params", JsonArray(selector.entries.mapIndexed { index, entry ->
                    buildJsonObject {
                        put("name", entry.description.ifBlank { "参数${index + 1}" })
                        put("key", "p$index")
                        put("type", mapTypeKey(entry.type))
                        entry.default?.let { put("default", it) }
                    }
                }))
                put("examples", JsonArray(selector.example.map(::JsonPrimitive)))
            }
        })
    }

    private fun generateTriggers(): JsonArray {
        val source = ScriptManager.wikiTriggers.toList()
        val ids = KetherDocsContract.triggerIds(source)
        return JsonArray(source.sortedBy(ids::get).map { trigger ->
            buildJsonObject {
                put("id", ids.getValue(trigger))
                put("name", trigger.key)
                put("category", mapTriggerCategory(trigger.group, trigger.key))
                put("description", trigger.description.ifBlank { trigger.key })
                put("variables", JsonArray(trigger.entries.map { entry ->
                    buildJsonObject {
                        put("name", entry.key)
                        put("type", mapTypeKey(entry.type))
                        put("description", entry.description)
                    }
                }))
                put("specialKeys", JsonArray(trigger.specialKeyEntries.map { entry ->
                    buildJsonObject {
                        put("name", entry.key)
                        put("type", mapTypeKey(entry.type))
                        put("description", entry.description)
                    }
                }))
            }
        })
    }

    private fun generateProperties(): JsonArray = JsonArray(
        ScriptManager.wikiProperties
            .sortedBy(KetherDocsContract::propertyId)
            .map { property ->
                buildJsonObject {
                    put("id", KetherDocsContract.propertyId(property))
                    put("name", property.name)
                    put("group", property.group)
                    put("description", property.description)
                    put("usage", "&变量名[key]")
                    put("keys", JsonArray(property.entries.map { entry ->
                        buildJsonObject {
                            put("name", entry.key)
                            put("type", mapTypeKey(entry.type))
                            put("readable", true)
                            put("writable", entry.writable)
                            put("description", entry.description)
                        }
                    }))
                }
            }
    )

    private fun mapTriggerCategory(group: TriggerGroup, key: String): String {
        val lower = key.lowercase(Locale.ROOT)
        return when (group) {
            TriggerGroup.BUKKIT -> when {
                "block" in lower -> "bukkit-block"
                "entity" in lower || "projectile" in lower -> "bukkit-entity"
                else -> "bukkit-player"
            }
            TriggerGroup.ORRYX -> when {
                "skill" in lower || "cast" in lower || "check" in lower -> "orryx-skill"
                "job" in lower -> "orryx-job"
                else -> "orryx-player"
            }
            else -> "third-party"
        }
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

    fun generateSchema(): JsonObject = generateSchema(runtimeMetadata())

    fun generateSchema(metadata: KetherDocsMetadata): JsonObject = buildJsonObject {
        put("${'$'}schema", "$KETHER_DOCS_BASE_URL/contracts/actions-schema-v3.schema.json")
        put("version", 2)
        put("schemaVersion", KETHER_SCHEMA_VERSION)
        put("pluginId", metadata.pluginId)
        put("pluginVersion", metadata.version)
        put("commit", metadata.commit)
        put("plugin", buildJsonObject {
            put("id", metadata.pluginId)
            put("version", metadata.version)
            put("commit", metadata.commit)
        })
        put("types", generateTypes())
        put("categories", generateCategories())
        put("actions", generateActions())
        put("selectors", generateSelectors())
        put("triggers", generateTriggers())
        put("properties", generateProperties())
    }

    fun generateJsonString(): String = generateJsonString(runtimeMetadata())

    fun generateJsonString(metadata: KetherDocsMetadata): String =
        json.encodeToString(JsonObject.serializer(), generateSchema(metadata))

    fun generate(outputFile: File): JsonObject = generate(outputFile, runtimeMetadata())

    fun generate(outputFile: File, metadata: KetherDocsMetadata): JsonObject {
        val schema = generateSchema(metadata)
        KetherDocsContract.writeUtf8(outputFile, json.encodeToString(JsonObject.serializer(), schema))
        return schema
    }
}
