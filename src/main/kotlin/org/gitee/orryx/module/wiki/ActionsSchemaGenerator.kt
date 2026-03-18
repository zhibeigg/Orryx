package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.*
import org.gitee.orryx.core.kether.ScriptManager
import taboolib.common.platform.function.pluginVersion
import java.io.File

/**
 * 根据 ACTIONS_SCHEMA 规范生成 JSON
 * 从 ScriptManager.wikiActions 收集所有 Kether 动作元数据
 */
object ActionsSchemaGenerator {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * 将 wiki Type 映射到 ACTIONS_SCHEMA 的参数类型
     */
    private fun mapType(type: Type): String {
        return when (type) {
            Type.DOUBLE, Type.FLOAT, Type.INT, Type.LONG, Type.SHORT, Type.BYTE -> "number"
            Type.STRING -> "string"
            Type.BOOLEAN -> "boolean"
            Type.SYMBOL -> "string"
            Type.CONTAINER, Type.TARGET, Type.PLAYER -> "selector"
            Type.VECTOR, Type.MATRIX, Type.QUATERNION -> "vector"
            Type.ANY, Type.NULL -> "string"
            else -> "string"
        }
    }

    /**
     * 将 wiki group 映射到 ACTIONS_SCHEMA 的 category
     */
    private fun mapCategory(group: String): String {
        val lower = group.lowercase()
        return when {
            "combat" in lower || "伤害" in lower || "damage" in lower -> "combat"
            "mana" in lower || "法力" in lower || "spirit" in lower || "精力" in lower -> "combat"
            "buff" in lower || "状态" in lower || "state" in lower || "effect" in lower -> "combat"
            "move" in lower || "移动" in lower || "dash" in lower || "冲刺" in lower || "传送" in lower -> "movement"
            "particle" in lower || "粒子" in lower || "特效" in lower -> "particle"
            "sound" in lower || "音效" in lower -> "sound"
            "entity" in lower || "实体" in lower -> "entity"
            "world" in lower || "世界" in lower -> "world"
            "logic" in lower || "逻辑" in lower || "条件" in lower || "循环" in lower -> "logic"
            "variable" in lower || "变量" in lower -> "variable"
            "math" in lower || "数学" in lower || "calc" in lower -> "math"
            "selector" in lower || "选择" in lower -> "selector"
            else -> "misc"
        }
    }

    /**
     * 构建单个 Action 的语法模板
     */
    private fun buildSyntax(action: Action): String {
        return "${action.key} " + action.entries.joinToString(" ") { entry ->
            if (entry.type == Type.SYMBOL) {
                entry.head ?: ""
            } else {
                val (start, end) = if (entry.optional) "[" to "]" else "<" to ">"
                val typeName = entry.type.name.lowercase()
                val prefix = if (entry.head != null) "${entry.head} " else ""
                "$start$prefix$typeName$end"
            }
        }
    }

    /**
     * 生成完整的 actions schema JsonObject
     */
    fun generateSchema(): JsonObject {
        val actions = ScriptManager.wikiActions.map { action ->
            buildJsonObject {
                put("name", action.name)
                put("category", mapCategory(action.group))
                put("description", action.description.ifBlank { action.name })
                put("returnType", if (action.result != Type.NULL) mapType(action.result) else "void")

                if (action.entries.isNotEmpty()) {
                    put("params", JsonArray(action.entries.mapNotNull { entry ->
                        if (entry.type == Type.SYMBOL) return@mapNotNull null
                        buildJsonObject {
                            put("name", entry.head ?: entry.description.take(20))
                            put("type", mapType(entry.type))
                            put("required", !entry.optional)
                            entry.default?.let { put("default", it) }
                            if (entry.description.isNotBlank()) {
                                put("description", entry.description)
                            }
                        }
                    }))
                }

                put("syntax", buildSyntax(action))

                if (action.example.isNotEmpty()) {
                    put("examples", JsonArray(action.example.map { JsonPrimitive(it) }))
                }
            }
        }

        return buildJsonObject {
            put("version", "1.0")
            put("pluginVersion", pluginVersion)
            put("actions", JsonArray(actions))
        }
    }

    /**
     * 生成 JSON 字符串
     */
    fun generateJsonString(): String {
        return json.encodeToString(JsonObject.serializer(), generateSchema())
    }

    /**
     * 生成 JSON 文件
     */
    fun generate(outputFile: File) {
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(generateJsonString(), Charsets.UTF_8)
    }
}
