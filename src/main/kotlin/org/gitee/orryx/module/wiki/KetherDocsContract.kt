package org.gitee.orryx.module.wiki

import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

internal const val KETHER_DOCS_FORMAT_VERSION = 1
internal const val KETHER_REGISTRY_VERSION = 4
internal const val KETHER_ACTIONS_SCHEMA_VERSION = 3
internal const val KETHER_DOCS_BASE_URL = "https://zhibeigg.github.io/Orryx/kether"

data class KetherDocsMetadata(
    val pluginId: String,
    val version: String,
    val commit: String,
    val channel: String,
    val generatedAt: Instant,
    val previousReleaseId: String? = null
) {
    val safeVersion: String = sanitizeVersion(version)
    val releaseId: String = "$pluginId@$version+$commit"
    val relativeDirectory: String = if (channel == "stable") {
        "releases/$safeVersion/$commit"
    } else {
        "snapshots/$commit"
    }
    val manifestUrl: String = "/Orryx/kether/$relativeDirectory/manifest.json"
}

data class KetherDocsAsset(
    val path: String,
    val mediaType: String,
    val bytes: Long,
    val sha256: String
)

internal object KetherDocsContract {

    private val versionPattern = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][0-9A-Za-z.-]+)?$")
    private val commitPattern = Regex("^[0-9a-f]{40}$")
    private val releaseIdPattern = Regex("^Orryx@.+\\+[0-9a-f]{40}$")
    private val identifierPartPattern = Regex("[^a-z0-9]+")

    fun metadata(
        pluginId: String,
        version: String,
        commit: String,
        channel: String,
        generatedAt: Instant,
        previousReleaseId: String? = null
    ): KetherDocsMetadata {
        require(pluginId == "Orryx") { "Kether 文档 pluginId 必须为 Orryx" }
        require(versionPattern.matches(version)) { "Kether 文档版本格式无效: $version" }
        val normalizedCommit = commit.lowercase(Locale.ROOT)
        require(commitPattern.matches(normalizedCommit)) { "Kether 文档 commit 必须为 40 位 Git SHA" }
        require(channel == "stable" || channel == "snapshot") { "Kether 文档 channel 仅支持 stable 或 snapshot" }
        require(previousReleaseId == null || releaseIdPattern.matches(previousReleaseId)) {
            "上一版 Kether 文档 releaseId 无效: $previousReleaseId"
        }
        return KetherDocsMetadata(pluginId, version, normalizedCommit, channel, generatedAt, previousReleaseId)
    }

    private val knownAliases = mapOf(
        "wait" to setOf("delay", "sleep"),
        "parameter" to setOf("parm"),
        "arcartx" to setOf("ax"),
        "cloudpick" to setOf("cp"),
        "dragoncore" to setOf("dragon"),
        "germplugin" to setOf("germ"),
        "mythicmobs" to setOf("mm")
    )

    fun actionName(action: Action): String = action.key.substringBefore('/').trim()

    fun requireActionDescriptions(actions: List<Action>) {
        val missing = actions.filter { it.description.isBlank() }
        require(missing.isEmpty()) {
            buildString {
                appendLine("以下 Kether Action 缺少简短中文简介：")
                missing.forEach { action ->
                    append("- ")
                    append(inferNamespace(action))
                    append(':')
                    append(actionName(action))
                    append(" | ")
                    append(action.group)
                    append(" | ")
                    appendLine(actionSyntax(action))
                }
            }.trimEnd()
        }
    }

    fun actionAliases(action: Action): List<String> {
        val name = actionName(action)
        val declared = action.key.split('/').drop(1) + action.aliases + knownAliases[name].orEmpty()
        val documented = Regex("别名\\s*([^，。；\\n]+)").findAll(action.description)
            .flatMap { match -> match.groupValues[1].split('/', '、', ',', ' ').asSequence() }
            .map(String::trim)
            .filter { it.matches(Regex("[A-Za-z0-9_\\$-]+")) }
            .toList()
        return (declared + documented)
            .filter(String::isNotBlank)
            .filterNot { it.equals(name, ignoreCase = true) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedBy { it.lowercase(Locale.ROOT) }
    }

    fun keywordAlternatives(value: String?): List<String> = value
        ?.split('/')
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        ?.distinct()
        .orEmpty()

    fun actionSyntax(action: Action): String {
        val tail = action.entries.joinToString(" ") { entry ->
            val (start, end) = if (entry.optional) "[" to "]" else "<" to ">"
            if (entry.type == Type.SYMBOL) {
                entry.head?.takeIf { it.isNotBlank() } ?: "${start}SYMBOL${end}"
            } else {
                buildString {
                    if (!entry.head.isNullOrBlank()) append('*').append(entry.head).append(' ')
                    append(start).append(entry.type.name)
                    if (entry.default != null) append('(').append(entry.default).append(')')
                    append(end)
                }
            }
        }
        val name = actionName(action)
        return if (tail.isBlank()) name else "$name $tail"
    }

    fun inputKey(entry: Action.Entry, index: Int): String = entry.head
        ?.takeIf(String::isNotBlank)
        ?: entry.description.takeIf(String::isNotBlank)
        ?: "p$index"

    fun selectorSyntax(selector: Selector): String {
        val tail = selector.entries.joinToString(" ") { entry ->
            buildString {
                append('[').append(entry.type.name)
                if (entry.default != null) append('(').append(entry.default).append(')')
                append(']')
            }
        }
        val head = "@${selector.keys.joinToString("/")}"
        return if (tail.isBlank()) head else "$head $tail"
    }

    fun inferNamespace(action: Action): String {
        action.namespace?.trim()?.takeIf(String::isNotEmpty)?.let { return it.lowercase(Locale.ROOT) }
        val group = action.group.lowercase(Locale.ROOT)
        return when {
            "kether原生" in group -> "kether"
            "nodens" in group -> "nodens"
            else -> "orryx"
        }
    }

    fun actionIds(actions: List<Action>): Map<Action, String> {
        val result = linkedMapOf<Action, String>()
        for (action in actions) {
            val base = "${inferNamespace(action)}.action.${slug(actionName(action))}"
            val qualifier = action.entries
                .filter { it.type == Type.SYMBOL && !it.head.isNullOrBlank() }
                .joinToString(".") { slug(it.head.orEmpty().substringBefore('/')) }
                .trim('.')
            val signature = sha256(actionIdentitySignature(action)).take(12)
            result[action] = if (qualifier.isBlank()) "$base.$signature" else "$base.$qualifier.$signature"
        }
        require(result.size == actions.size) { "Kether Action 文档存在重复注册项" }
        require(result.values.distinct().size == result.size) { "Kether Action 文档存在重复稳定 ID" }
        return result
    }

    fun selectorIds(selectors: List<Selector>): Map<Selector, String> = stableIds(
        selectors,
        base = { selector -> "orryx.selector.${slug(selector.keys.firstOrNull() ?: selector.name)}" },
        signature = { selector ->
            listOf(
                selector.type.name,
                selector.keys.joinToString("/"),
                selector.entries.joinToString("|") { "${it.type.name}:${it.default.orEmpty()}" }
            ).joinToString("|")
        }
    )

    fun triggerIds(triggers: List<Trigger>): Map<Trigger, String> = stableIds(
        triggers,
        base = { trigger -> "orryx.trigger.${slug(trigger.group.name)}.${slug(trigger.key)}" },
        signature = { trigger ->
            listOf(
                trigger.group.name,
                trigger.key,
                trigger.entries.joinToString("|") { "${it.key}:${it.type.name}" },
                trigger.specialKeyEntries.joinToString("|") { "${it.key}:${it.type.name}" }
            ).joinToString("|")
        }
    )

    fun propertyId(property: Property): String {
        val normalized = property.id
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('.', '-', '_')
        return when {
            normalized.startsWith("orryx.") || normalized.startsWith("kether.") -> normalized
            normalized.isNotBlank() -> "orryx.property.$normalized"
            else -> "orryx.property.${sha256("${property.group}|${property.name}").take(12)}"
        }
    }

    fun actionRequirements(action: Action): List<String> {
        val group = action.group.lowercase(Locale.ROOT)
        val inferred = linkedMapOf(
            "DragonCore" to listOf("dragoncore", "龙核"),
            "CloudPick" to listOf("cloudpick", "云拾"),
            "ArcartX" to listOf("arcartx"),
            "GermPlugin" to listOf("germplugin", "萌芽"),
            "MythicMobs" to listOf("mythicmobs"),
            "AttributePlus" to listOf("attributeplus"),
            "AstraXHero" to listOf("astraxhero"),
            "Nodens" to listOf("nodens"),
            "GDDTitle" to listOf("gddtitle")
        ).filterValues { markers -> markers.any { it in group } }.keys
        return (inferred + action.explicitRequirements).distinct().sorted()
    }

    fun actionSuspends(action: Action): Boolean {
        action.suspends?.let { return it }
        val key = actionName(action).lowercase(Locale.ROOT)
        return key in setOf("wait", "delay", "sleep", "await", "await_all", "await_any", "aichat") ||
            "等待" in action.description
    }

    fun actionThread(action: Action): ExecutionThread {
        if (action.executionThread != ExecutionThread.UNKNOWN) return action.executionThread
        val key = actionName(action).lowercase(Locale.ROOT)
        val group = action.group.lowercase(Locale.ROOT)
        if (key == "async") return ExecutionThread.ASYNC
        if (actionSuspends(action) || key in setOf("calc", "math", "random", "round", "scale", "type", "check")) {
            return ExecutionThread.ANY
        }
        if (listOf(
                "数学", "变量", "数组", "类型转换", "vector", "matrix", "quaternion", "util", "控制流",
                "时间", "循环", "脚本", "普通语句", "上下文", "flag", "global", "uuid", "coroutine",
                "协程", "pipe", "管式"
            ).any { it in group }) {
            return ExecutionThread.ANY
        }
        if (listOf(
                "游戏", "实体", "世界", "移动", "传送", "伤害", "技能", "状态", "碰撞", "粒子", "音效",
                "dragoncore", "cloudpick", "arcartx", "germplugin", "mythicmobs", "nodens", "attribute", "astraxhero",
                "orryx", "profile", "keysetting", "container", "cooldown", "mana", "spirit", "money",
                "station", "projectile", "raytrace", "gddtitle", "属性系统", "selector", "mod", "输出"
            ).any { it in group }) {
            return ExecutionThread.MAIN
        }
        return ExecutionThread.UNKNOWN
    }

    fun asset(file: File, mediaType: String): KetherDocsAsset {
        require(file.isFile) { "Kether 文档资产不存在: ${file.absolutePath}" }
        return KetherDocsAsset(file.name, mediaType, file.length(), sha256(file))
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex()
    }

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .toHex()

    fun writeUtf8(file: File, content: String) {
        file.parentFile?.mkdirs()
        file.writeText(content.trimEnd() + "\n", Charsets.UTF_8)
    }

    private fun actionIdentitySignature(action: Action): String = listOf(
        inferNamespace(action),
        actionName(action),
        action.entries.joinToString("|") { entry ->
            "${entry.head.orEmpty()}:${entry.acceptedTypes.joinToString(",") { it.id }}:${entry.optional}:${entry.default.orEmpty()}"
        },
        action.result.name,
        inferFlowType(action)
    ).joinToString("|")

    fun inferFlowType(action: Action): String {
        val key = actionName(action).lowercase(Locale.ROOT)
        val group = action.group.lowercase(Locale.ROOT)
        return when {
            key == "if" || key == "case" || key == "check" || key == "optional" -> "branch"
            key == "for" || key == "while" || key == "repeat" || key == "map" -> "loop"
            "循环" in group -> "loop"
            key == "async" || key == "seq" -> "container"
            else -> "normal"
        }
    }

    private fun <T : Any> stableIds(
        values: List<T>,
        base: (T) -> String,
        signature: (T) -> String
    ): Map<T, String> {
        val result = values.associateWithTo(linkedMapOf()) { value ->
            "${base(value)}.${sha256(signature(value)).take(12)}"
        }
        require(result.size == values.size) { "Kether 文档存在重复注册项" }
        require(result.values.distinct().size == result.size) { "Kether 文档存在重复稳定 ID" }
        return result
    }

    private fun slug(value: String): String {
        val normalized = value.lowercase(Locale.ROOT)
            .replace(identifierPartPattern, "-")
            .trim('-')
        return normalized.ifBlank { "item-${sha256(value).take(12)}" }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}

internal fun sanitizeVersion(version: String): String {
    require(version.isNotBlank()) { "文档版本不能为空" }
    return version.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
