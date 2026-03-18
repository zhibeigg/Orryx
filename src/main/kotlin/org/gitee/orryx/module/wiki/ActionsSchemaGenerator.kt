package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.*
import org.gitee.orryx.core.kether.ScriptManager
import taboolib.common.platform.function.pluginVersion
import java.io.File

/**
 * 根据 ACTIONS_SCHEMA 规范生成 JSON
 * 从 ScriptManager 收集所有 Kether 动作、触发器、选择器、属性元数据
 */
object ActionsSchemaGenerator {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * 将 wiki Type 映射到 ACTIONS_SCHEMA 的参数类型
     */
    private fun mapParamType(type: Type): String {
        return when (type) {
            Type.DOUBLE, Type.FLOAT, Type.INT, Type.LONG, Type.SHORT, Type.BYTE -> "number"
            Type.STRING -> "string"
            Type.BOOLEAN -> "boolean"
            Type.SYMBOL -> "string"
            Type.CONTAINER, Type.TARGET, Type.PLAYER -> "selector"
            Type.VECTOR, Type.MATRIX, Type.QUATERNION -> "vector"
            Type.ITEM_STACK -> "string"
            Type.ITERABLE -> "string"
            Type.ANY, Type.NULL -> "string"
            else -> "string"
        }
    }

    /**
     * 将 wiki Type 映射到 Property key 的值类型
     */
    private fun mapPropertyKeyType(type: Type): String {
        return when (type) {
            Type.DOUBLE -> "DOUBLE"
            Type.FLOAT -> "FLOAT"
            Type.INT, Type.SHORT, Type.BYTE -> "INT"
            Type.LONG -> "LONG"
            Type.STRING -> "STRING"
            Type.BOOLEAN -> "BOOLEAN"
            Type.VECTOR -> "VECTOR"
            else -> "ANY"
        }
    }

    /**
     * 将 wiki Type 映射到 Trigger variable 的类型
     */
    private fun mapTriggerVarType(type: Type): String {
        return when (type) {
            Type.DOUBLE, Type.FLOAT, Type.INT, Type.LONG, Type.SHORT, Type.BYTE -> "number"
            Type.STRING -> "string"
            Type.BOOLEAN -> "boolean"
            Type.ITEM_STACK -> "itemstack"
            Type.VECTOR -> "vector"
            else -> "string"
        }
    }

    /**
     * 将 wiki group 映射到 ACTIONS_SCHEMA 的 category
     */
    private fun mapActionCategory(group: String): String {
        val lower = group.lowercase()
        return when {
            // Kether 原生
            "kether原生-控制流" in lower || "kether原生-延迟" in lower || "kether原生-脚本" in lower -> "logic"
            "kether原生-循环" in lower -> "logic"
            "kether原生-输出" in lower -> "misc"
            "kether原生-变量" in lower || "kether原生-类型转换" in lower || "kether原生-数组" in lower -> "variable"
            "kether原生-数学" in lower -> "math"
            "kether原生-时间" in lower -> "misc"
            "kether原生-游戏" in lower -> "misc"
            "kether原生-属性" in lower -> "variable"
            // 战斗
            "damage" in lower || "伤害" in lower -> "combat"
            "mana" in lower || "法力" in lower -> "combat"
            "spirit" in lower || "精力" in lower -> "combat"
            "buff" in lower || "state" in lower || "状态" in lower -> "combat"
            "cooldown" in lower || "冷却" in lower -> "combat"
            "hitbox" in lower || "碰撞" in lower -> "combat"
            "attribute" in lower || "属性系统" in lower -> "combat"
            "astraxhero" in lower || "attributeplus" in lower || "nodens" in lower -> "combat"
            // 移动
            "move" in lower || "移动" in lower || "dash" in lower || "冲刺" in lower || "传送" in lower -> "movement"
            "projectile" in lower || "抛射" in lower -> "movement"
            // 粒子/音效
            "particle" in lower || "粒子" in lower || "effect" in lower || "特效" in lower -> "particle"
            "sound" in lower || "音效" in lower -> "sound"
            // 实体
            "entity" in lower || "实体" in lower -> "entity"
            // 世界
            "world" in lower || "世界" in lower -> "world"
            // 逻辑
            "coroutine" in lower || "协程" in lower -> "logic"
            "pipe" in lower || "管式" in lower -> "logic"
            "station" in lower || "中转" in lower -> "logic"
            // 变量
            "variable" in lower || "变量" in lower || "flag" in lower || "标签" in lower -> "variable"
            "global" in lower || "全局" in lower -> "variable"
            "上下文" in lower || "container" in lower || "容器" in lower -> "variable"
            // 数学
            "math" in lower || "数学" in lower || "calc" in lower -> "math"
            // 选择器
            "selector" in lower || "选择" in lower -> "selector"
            // 玩家信息
            "profile" in lower || "玩家信息" in lower || "orryx信息" in lower -> "misc"
            "keysetting" in lower || "按键" in lower -> "misc"
            "skill" in lower || "技能" in lower || "pressskill" in lower -> "combat"
            // 兼容插件
            "dragoncore" in lower || "germplugin" in lower || "arcartx" in lower || "cloudpick" in lower -> "misc"
            "mythicmobs" in lower -> "entity"
            "gddtitle" in lower -> "misc"
            "mod" in lower -> "misc"
            "ai" in lower || "智能" in lower -> "misc"
            "money" in lower || "财富" in lower -> "misc"
            "uuid" in lower -> "misc"
            "raytrace" in lower || "光线" in lower -> "selector"
            "game" in lower || "原版游戏" in lower -> "misc"
            "普通语句" in lower || "util" in lower || "工具" in lower -> "misc"
            else -> "misc"
        }
    }

    /**
     * 将 TriggerGroup + key 映射到 ACTIONS_SCHEMA 的 trigger category
     */
    private fun mapTriggerCategory(group: TriggerGroup, key: String): String {
        val lower = key.lowercase()
        return when (group) {
            TriggerGroup.BUKKIT -> when {
                "block" in lower -> "bukkit-block"
                "entity" in lower || "projectile" in lower -> "bukkit-entity"
                else -> "bukkit-player"
            }
            TriggerGroup.ORRYX -> when {
                "skill" in lower || "cast" in lower || "check" in lower -> "orryx-skill"
                "job" in lower -> "orryx-job"
                "flag" in lower -> "orryx-flag"
                "mana" in lower || "spirit" in lower || "level" in lower || "exp" in lower || "point" in lower -> "orryx-player"
                "key" in lower -> "orryx-player"
                else -> "orryx-player"
            }
            TriggerGroup.DRAGONCORE -> "third-party"
            TriggerGroup.GERM_PLUGIN -> "third-party"
            TriggerGroup.ARCARTX -> "third-party"
            TriggerGroup.MYTHIC_MOBS -> "third-party"
            TriggerGroup.DUNGEON_PLUS -> "third-party"
        }
    }

    /**
     * 将 SelectorType + name 映射到 ACTIONS_SCHEMA 的 selector category
     */
    private fun mapSelectorCategory(type: SelectorType, name: String): String {
        val lower = name.lowercase()
        return when (type) {
            SelectorType.GEOMETRY -> when {
                "位置" in lower || "坐标" in lower || "地面" in lower || "偏移" in lower -> "location"
                "最近" in lower || "视线" in lower -> "entity"
                else -> "geometry"
            }
            SelectorType.STREAM -> when {
                "sender" in lower || "玩家" in lower || "全服" in lower -> "entity"
                "位置" in lower || "坐标" in lower || "原点" in lower -> "location"
                else -> "filter"
            }
        }
    }

    /**
     * 将 Property group 映射到 ACTIONS_SCHEMA 的 property category
     */
    private fun mapPropertyCategory(group: String): String {
        val lower = group.lowercase()
        return when {
            "kether原生" in lower -> "game"
            "game" in lower || "原版" in lower || "location" in lower -> "game"
            "hitbox" in lower || "碰撞" in lower -> "orryx-combat"
            "orryx" in lower || "核心" in lower -> "orryx-player"
            "key" in lower || "按键" in lower -> "keysetting"
            else -> "game"
        }
    }

    /**
     * 构建单个 Action 的语法模板
     */
    private fun buildActionSyntax(action: Action): String {
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
     * 构建单个 Selector 的语法模板
     */
    private fun buildSelectorSyntax(selector: Selector): String {
        return "@${selector.keys.joinToString("/")} " + selector.entries.joinToString(" ") { entry ->
            var s = "[${entry.type.name}"
            if (entry.default != null) s += "(${entry.default})"
            s += "]"
            s
        }
    }

    /**
     * 生成 actions 列表
     */
    private fun generateActions(): JsonArray {
        return JsonArray(ScriptManager.wikiActions.map { action ->
            buildJsonObject {
                put("name", action.key)
                put("category", mapActionCategory(action.group))
                put("description", action.description.ifBlank { action.name })
                put("returnType", if (action.result != Type.NULL) mapParamType(action.result) else "void")

                if (action.entries.isNotEmpty()) {
                    put("params", JsonArray(action.entries.mapNotNull { entry ->
                        if (entry.type == Type.SYMBOL) return@mapNotNull null
                        buildJsonObject {
                            put("name", entry.head ?: entry.description.take(20))
                            put("type", mapParamType(entry.type))
                            put("required", !entry.optional)
                            entry.default?.let { put("default", it) }
                            if (entry.description.isNotBlank()) {
                                put("description", entry.description)
                            }
                        }
                    }))
                }

                put("syntax", buildActionSyntax(action))

                if (action.example.isNotEmpty()) {
                    put("examples", JsonArray(action.example.map { JsonPrimitive(it) }))
                }
            }
        })
    }

    /**
     * 生成 triggers 列表
     */
    private fun generateTriggers(): JsonArray {
        return JsonArray(ScriptManager.wikiTriggers.map { trigger ->
            buildJsonObject {
                put("name", trigger.key)
                put("category", mapTriggerCategory(trigger.group, trigger.key))
                put("description", trigger.description.ifBlank { trigger.key })

                if (trigger.entries.isNotEmpty()) {
                    put("variables", JsonArray(trigger.entries.map { entry ->
                        buildJsonObject {
                            put("name", entry.key)
                            put("type", mapTriggerVarType(entry.type))
                            if (entry.description.isNotBlank()) {
                                put("description", entry.description)
                            }
                        }
                    }))
                }
            }
        })
    }

    /**
     * 生成 selectors 列表
     */
    private fun generateSelectors(): JsonArray {
        return JsonArray(ScriptManager.wikiSelectors.map { selector ->
            buildJsonObject {
                put("name", selector.name)
                if (selector.keys.size > 1) {
                    put("aliases", JsonArray(selector.keys.drop(1).map { JsonPrimitive(it) }))
                }
                put("category", mapSelectorCategory(selector.type, selector.name))
                put("description", selector.description.ifBlank { selector.name })

                if (selector.entries.isNotEmpty()) {
                    put("params", JsonArray(selector.entries.map { entry ->
                        buildJsonObject {
                            put("type", entry.type.name)
                            put("description", entry.description)
                            entry.default?.let { put("default", it) }
                        }
                    }))
                }

                put("syntax", buildSelectorSyntax(selector))

                if (selector.example.isNotEmpty()) {
                    put("examples", JsonArray(selector.example.map { JsonPrimitive(it) }))
                }
            }
        })
    }

    /**
     * 生成 properties 列表
     */
    private fun generateProperties(): JsonArray {
        return JsonArray(ScriptManager.wikiProperties.map { property ->
            buildJsonObject {
                put("name", property.name)
                put("id", property.id)
                put("category", mapPropertyCategory(property.group))
                if (property.description.isNotBlank()) {
                    put("description", property.description)
                }
                put("usage", "&变量名[key]")

                put("keys", JsonArray(property.entries.map { entry ->
                    buildJsonObject {
                        put("name", entry.key)
                        put("type", mapPropertyKeyType(entry.type))
                        put("writable", entry.writable)
                        if (entry.description.isNotBlank()) {
                            put("description", entry.description)
                        }
                    }
                }))
            }
        })
    }

    /**
     * 生成完整的 schema JsonObject
     */
    fun generateSchema(): JsonObject {
        return buildJsonObject {
            put("version", "1.0")
            put("pluginVersion", pluginVersion)
            put("actions", generateActions())
            put("triggers", generateTriggers())
            put("selectors", generateSelectors())
            put("properties", generateProperties())
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
