package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.*
import org.gitee.orryx.core.kether.ScriptManager
import taboolib.common.platform.function.pluginVersion
import java.io.File

/**
 * 根据 ActionsSchemaV2 规范生成 JSON
 * 从 ScriptManager 收集所有 Kether 动作、触发器、选择器元数据
 */
object ActionsSchemaGenerator {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // ==================== 类型映射 ====================

    /**
     * 将 wiki Type 映射到 V2 type key
     */
    private fun mapTypeKey(type: Type): String {
        return when (type) {
            Type.DOUBLE -> "number"
            Type.FLOAT -> "number"
            Type.INT -> "int"
            Type.LONG -> "long"
            Type.SHORT, Type.BYTE -> "int"
            Type.STRING -> "text"
            Type.BOOLEAN -> "boolean"
            Type.SYMBOL -> "keyword"
            Type.CONTAINER, Type.TARGET, Type.PLAYER -> "selector"
            Type.VECTOR -> "vector3"
            Type.MATRIX -> "matrix"
            Type.QUATERNION -> "vector3"
            Type.ITEM_STACK -> "text"
            Type.ITERABLE -> "list"
            Type.ANY, Type.NULL -> "any"
            else -> "any"
        }
    }

    /**
     * 将 wiki group 映射到 V2 category key
     */
    private fun mapActionCategory(group: String): String {
        val lower = group.lowercase()
        return when {
            "kether原生-控制流" in lower || "kether原生-延迟" in lower || "kether原生-脚本" in lower -> "logic"
            "kether原生-循环" in lower -> "loop"
            "kether原生-输出" in lower -> "output"
            "kether原生-变量" in lower || "kether原生-类型转换" in lower || "kether原生-数组" in lower -> "variable"
            "kether原生-数学" in lower -> "math"
            "kether原生-时间" in lower -> "time"
            "kether原生-游戏" in lower -> "game"
            "kether原生-属性" in lower -> "variable"
            "damage" in lower || "伤害" in lower -> "combat"
            "mana" in lower || "法力" in lower -> "combat"
            "spirit" in lower || "精力" in lower -> "combat"
            "buff" in lower || "state" in lower || "状态" in lower -> "combat"
            "cooldown" in lower || "冷却" in lower -> "combat"
            "hitbox" in lower || "碰撞" in lower -> "combat"
            "attribute" in lower || "属性系统" in lower -> "combat"
            "astraxhero" in lower || "attributeplus" in lower || "nodens" in lower -> "combat"
            "move" in lower || "移动" in lower || "dash" in lower || "冲刺" in lower || "传送" in lower -> "movement"
            "projectile" in lower || "抛射" in lower -> "movement"
            "particle" in lower || "粒子" in lower || "effect" in lower || "特效" in lower -> "particle"
            "sound" in lower || "音效" in lower -> "sound"
            "entity" in lower || "实体" in lower -> "entity"
            "world" in lower || "世界" in lower -> "world"
            "coroutine" in lower || "协程" in lower -> "logic"
            "pipe" in lower || "管式" in lower -> "logic"
            "station" in lower || "中转" in lower -> "logic"
            "variable" in lower || "变量" in lower || "flag" in lower || "标签" in lower -> "variable"
            "global" in lower || "全局" in lower -> "variable"
            "上下文" in lower || "container" in lower || "容器" in lower -> "variable"
            "math" in lower || "数学" in lower || "calc" in lower -> "math"
            "selector" in lower || "选择" in lower -> "selector"
            "profile" in lower || "玩家信息" in lower || "orryx信息" in lower -> "player"
            "keysetting" in lower || "按键" in lower -> "player"
            "skill" in lower || "技能" in lower || "pressskill" in lower -> "combat"
            "dragoncore" in lower || "germplugin" in lower || "arcartx" in lower || "cloudpick" in lower -> "compat"
            "mythicmobs" in lower -> "compat"
            "gddtitle" in lower -> "compat"
            "mod" in lower -> "compat"
            "ai" in lower || "智能" in lower -> "entity"
            "money" in lower || "财富" in lower -> "player"
            "uuid" in lower -> "variable"
            "raytrace" in lower || "光线" in lower -> "selector"
            "game" in lower || "原版游戏" in lower -> "game"
            "普通语句" in lower || "util" in lower || "工具" in lower -> "misc"
            else -> "misc"
        }
    }

    /**
     * 推断 Action 的 flow 类型
     */
    private fun inferFlowType(action: Action): String {
        val key = action.key.lowercase()
        val group = action.group.lowercase()
        return when {
            key == "if" || key == "case" || key == "check" || key == "optional" -> "branch"
            key == "for" || key == "while" || key == "repeat" || key == "map" -> "loop"
            "循环" in group -> "loop"
            key == "async" || key == "seq" -> "container"
            else -> "normal"
        }
    }

    /**
     * 推断 Action 的 namespace
     */
    private fun inferNamespace(action: Action): String {
        val group = action.group.lowercase()
        return when {
            "kether原生" in group -> "kether"
            else -> "orryx"
        }
    }

    // ==================== 顶层 types ====================

    private fun generateTypes(): JsonObject {
        return buildJsonObject {
            put("number", buildJsonObject {
                put("widget", "number")
                put("color", "#4FC3F7")
                put("step", 0.1)
            })
            put("int", buildJsonObject {
                put("widget", "number")
                put("color", "#4DB6AC")
                put("step", 1)
            })
            put("long", buildJsonObject {
                put("widget", "number")
                put("color", "#4DB6AC")
                put("step", 1)
            })
            put("text", buildJsonObject {
                put("widget", "text")
                put("color", "#FFB74D")
            })
            put("boolean", buildJsonObject {
                put("widget", "toggle")
                put("color", "#E57373")
            })
            put("keyword", buildJsonObject {
                put("widget", "text")
                put("color", "#9E9E9E")
            })
            put("selector", buildJsonObject {
                put("widget", "selector")
                put("color", "#BA68C8")
            })
            put("vector3", buildJsonObject {
                put("widget", "vector3")
                put("color", "#81C784")
            })
            put("matrix", buildJsonObject {
                put("widget", "text")
                put("color", "#A1887F")
            })
            put("list", buildJsonObject {
                put("widget", "list")
                put("color", "#7986CB")
            })
            put("duration", buildJsonObject {
                put("widget", "duration")
                put("color", "#FFD54F")
            })
            put("any", buildJsonObject {
                put("widget", "text")
                put("color", "#B0BEC5")
            })
            put("enum", buildJsonObject {
                put("widget", "select")
                put("color", "#F06292")
            })
            put("location", buildJsonObject {
                put("widget", "location")
                put("color", "#AED581")
            })
            put("port", buildJsonObject {
                put("widget", "port")
                put("color", "#90A4AE")
            })
        }
    }

    // ==================== 顶层 categories ====================

    private fun generateCategories(): JsonObject {
        return buildJsonObject {
            put("logic", buildJsonObject { put("color", "#42A5F5"); put("icon", "mdi-code-braces") })
            put("loop", buildJsonObject { put("color", "#26C6DA"); put("icon", "mdi-sync") })
            put("output", buildJsonObject { put("color", "#66BB6A"); put("icon", "mdi-message-text") })
            put("variable", buildJsonObject { put("color", "#FFA726"); put("icon", "mdi-variable") })
            put("math", buildJsonObject { put("color", "#AB47BC"); put("icon", "mdi-calculator") })
            put("time", buildJsonObject { put("color", "#FFCA28"); put("icon", "mdi-clock-outline") })
            put("game", buildJsonObject { put("color", "#78909C"); put("icon", "mdi-gamepad-variant") })
            put("combat", buildJsonObject { put("color", "#EF5350"); put("icon", "mdi-sword-cross") })
            put("movement", buildJsonObject { put("color", "#29B6F6"); put("icon", "mdi-run-fast") })
            put("particle", buildJsonObject { put("color", "#EC407A"); put("icon", "mdi-sparkles") })
            put("sound", buildJsonObject { put("color", "#7E57C2"); put("icon", "mdi-volume-high") })
            put("entity", buildJsonObject { put("color", "#8D6E63"); put("icon", "mdi-account") })
            put("world", buildJsonObject { put("color", "#26A69A"); put("icon", "mdi-earth") })
            put("selector", buildJsonObject { put("color", "#5C6BC0"); put("icon", "mdi-target") })
            put("player", buildJsonObject { put("color", "#42A5F5"); put("icon", "mdi-account-circle") })
            put("compat", buildJsonObject { put("color", "#78909C"); put("icon", "mdi-puzzle") })
            put("misc", buildJsonObject { put("color", "#BDBDBD"); put("icon", "mdi-dots-horizontal") })
        }
    }

    // ==================== actions ====================

    private fun generateActions(): JsonArray {
        return JsonArray(ScriptManager.wikiActions.map { action ->
            buildJsonObject {
                put("name", action.key)
                put("category", mapActionCategory(action.group))
                put("namespace", inferNamespace(action))
                put("description", action.description.ifBlank { action.name })
                if (action.sharded) put("builtin", true)
                put("flow", inferFlowType(action))

                // inputs
                val inputs = action.entries.map { entry ->
                    buildJsonObject {
                        put("name", entry.description.ifBlank { entry.head ?: entry.type.name.lowercase() })
                        put("key", entry.head ?: entry.description.take(20).replace(" ", "_"))
                        if (entry.type == Type.SYMBOL) {
                            put("type", "keyword")
                            put("required", true)
                            put("default", JsonPrimitive(entry.head ?: ""))
                            entry.head?.let { put("keyword", it) }
                        } else {
                            put("type", mapTypeKey(entry.type))
                            put("required", !entry.optional)
                            if (entry.default != null) {
                                put("default", JsonPrimitive(entry.default))
                            } else {
                                put("default", JsonNull)
                            }
                            if (entry.description.isNotBlank()) {
                                put("description", entry.description)
                            }
                        }
                    }
                }
                put("inputs", JsonArray(inputs))

                // output
                if (action.result != Type.NULL) {
                    put("output", buildJsonObject {
                        put("type", mapTypeKey(action.result))
                        action.resultDescription?.let { put("description", it) }
                    })
                } else {
                    put("output", JsonNull)
                }

                // example
                if (action.example.isNotEmpty()) {
                    put("example", action.example.joinToString("\n"))
                }

                // slots for flow types
                val flow = inferFlowType(action)
                if (flow == "branch") {
                    put("slots", JsonArray(listOf(
                        buildJsonObject {
                            put("name", "then"); put("label", "条件为真"); put("multiple", true)
                        },
                        buildJsonObject {
                            put("name", "else"); put("label", "条件为假"); put("multiple", true); put("optional", true)
                        }
                    )))
                } else if (flow == "loop") {
                    put("slots", JsonArray(listOf(
                        buildJsonObject {
                            put("name", "body"); put("label", "循环体"); put("multiple", true)
                        }
                    )))
                } else if (flow == "container") {
                    put("slots", JsonArray(listOf(
                        buildJsonObject {
                            put("name", "children"); put("label", "子动作"); put("multiple", true)
                        }
                    )))
                }

                // provides for loop
                if (flow == "loop" && action.key.lowercase() in listOf("for", "map")) {
                    put("provides", JsonArray(listOf(
                        buildJsonObject {
                            put("name", "迭代变量"); put("key", "it"); put("type", "any")
                            put("description", "当前迭代元素")
                        }
                    )))
                }
            }
        })
    }

    // ==================== selectors ====================

    private fun generateSelectors(): JsonArray {
        return JsonArray(ScriptManager.wikiSelectors.map { selector ->
            buildJsonObject {
                put("name", selector.keys.first())
                if (selector.keys.size > 1) {
                    put("aliases", JsonArray(selector.keys.drop(1).map { JsonPrimitive(it) }))
                }
                put("description", selector.description.ifBlank { selector.name })

                put("params", JsonArray(selector.entries.mapIndexed { index, entry ->
                    buildJsonObject {
                        put("name", entry.description.ifBlank { "参数${index + 1}" })
                        put("key", "p$index")
                        put("type", mapTypeKey(entry.type))
                        entry.default?.let { put("default", JsonPrimitive(it)) }
                    }
                }))
            }
        })
    }

    // ==================== triggers ====================

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
                            put("type", mapTypeKey(entry.type))
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
     * 将 TriggerGroup + key 映射到 trigger category
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

    // ==================== 入口 ====================

    /**
     * 生成完整的 V2 schema JsonObject
     */
    fun generateSchema(): JsonObject {
        return buildJsonObject {
            put("version", 2)
            put("types", generateTypes())
            put("categories", generateCategories())
            put("actions", generateActions())
            put("selectors", generateSelectors())
            put("triggers", generateTriggers())
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
