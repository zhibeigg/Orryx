package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.bukkit.Bukkit
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.submit
import java.io.File
import java.time.Instant

/**
 * 在 Gradle 文档模式下导出可部署到 GitHub Pages 的 Kether 文档。
 *
 * 正常服务器不会设置 [OUTPUT_PROPERTY]，因此不会执行任何导出或关服逻辑。
 */
object KetherDocsPublisher {

    const val OUTPUT_PROPERTY = "orryx.ketherDocs.output"
    const val VERSION_PROPERTY = "orryx.ketherDocs.version"
    const val COMMIT_PROPERTY = "orryx.ketherDocs.commit"
    const val PAGES_BASE_URL = "https://zhibeigg.github.io/Orryx/kether"

    data class RegistrationCounts(
        val actions: Int,
        val selectors: Int,
        val triggers: Int,
        val properties: Int
    )

    private val json = Json {
        prettyPrint = true
    }

    @Awake(LifeCycle.ENABLE)
    private fun publishWhenRequested() {
        val outputPath = System.getProperty(OUTPUT_PROPERTY)?.trim()?.takeIf { it.isNotEmpty() } ?: return

        // 等待所有 ENABLE 生命周期注册完成，再生成运行时完整文档。
        submit(delay = 20L) {
            try {
                publish(File(outputPath))
                consoleMessage("&e┣&7Kether Pages 文档已生成 &a√")
                consoleMessage("&e┣&7路径: &f${File(outputPath).absolutePath}")
            } catch (ex: Throwable) {
                ex.printStackTrace()
                consoleMessage("&c[Orryx] Kether Pages 文档生成失败: ${ex.message}")
            } finally {
                Bukkit.shutdown()
            }
        }
    }

    internal fun publish(siteDirectory: File) {
        val version = System.getProperty(VERSION_PROPERTY)?.trim()?.takeIf { it.isNotEmpty() } ?: pluginVersion
        val commit = System.getProperty(COMMIT_PROPERTY)?.trim()?.takeIf { it.isNotEmpty() }
        val safeVersion = sanitizeVersion(version)

        validateRegistry()

        if (siteDirectory.exists()) {
            check(siteDirectory.deleteRecursively()) { "无法清理旧文档目录: ${siteDirectory.absolutePath}" }
        }

        val ketherDirectory = File(siteDirectory, "kether")
        val versionDirectory = File(ketherDirectory, "versions")
        check(versionDirectory.mkdirs() || versionDirectory.isDirectory) {
            "无法创建文档目录: ${versionDirectory.absolutePath}"
        }

        val latestMarkdown = File(ketherDirectory, "latest.md")
        val versionedMarkdown = File(versionDirectory, "$safeVersion.md")
        val schemaFile = File(ketherDirectory, "actions-schema.json")
        val manifestFile = File(ketherDirectory, "manifest.json")

        MarkdownGenerator.generate(latestMarkdown)
        latestMarkdown.copyTo(versionedMarkdown, overwrite = true)
        ActionsSchemaGenerator.generate(schemaFile)
        manifestFile.writeText(
            buildManifest(
                pluginName = pluginId,
                version = version,
                safeVersion = safeVersion,
                commit = commit,
                generatedAt = Instant.now(),
                counts = RegistrationCounts(
                    actions = ScriptManager.wikiActions.size,
                    selectors = ScriptManager.wikiSelectors.size,
                    triggers = ScriptManager.wikiTriggers.size,
                    properties = ScriptManager.wikiProperties.size
                )
            ),
            Charsets.UTF_8
        )

        File(siteDirectory, "index.html").writeText(generateIndex(version), Charsets.UTF_8)
        File(siteDirectory, ".nojekyll").writeText("", Charsets.UTF_8)

        require(latestMarkdown.length() > 0L) { "latest.md 为空" }
        require(versionedMarkdown.length() > 0L) { "版本化 Markdown 为空" }
        require(schemaFile.length() > 0L) { "actions-schema.json 为空" }
        require(manifestFile.length() > 0L) { "manifest.json 为空" }
    }

    private fun validateRegistry() {
        require(ScriptManager.wikiActions.isNotEmpty()) { "未注册任何 Kether Action 文档" }
        require(ScriptManager.wikiSelectors.isNotEmpty()) { "未注册任何 Selector 文档" }
        require(ScriptManager.wikiTriggers.isNotEmpty()) { "未注册任何 Trigger 文档" }
        require(ScriptManager.wikiProperties.isNotEmpty()) { "未注册任何 Property 文档" }
    }

    fun sanitizeVersion(version: String): String {
        require(version.isNotBlank()) { "文档版本不能为空" }
        return version.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    fun buildManifest(
        pluginName: String,
        version: String,
        safeVersion: String = sanitizeVersion(version),
        commit: String? = null,
        generatedAt: Instant = Instant.now(),
        counts: RegistrationCounts
    ): String {
        val manifest = buildJsonObject {
            put("pluginId", pluginName)
            put("pluginVersion", version)
            put("version", version)
            put("schemaVersion", 2)
            put("generatedAt", generatedAt.toString())
            commit?.let { put("commit", it) }
            put("latest", "$PAGES_BASE_URL/latest.md")
            put("versioned", "$PAGES_BASE_URL/versions/$safeVersion.md")
            put("schema", "$PAGES_BASE_URL/actions-schema.json")
            put("counts", buildJsonObject {
                put("actions", counts.actions)
                put("selectors", counts.selectors)
                put("triggers", counts.triggers)
                put("properties", counts.properties)
            })
            put("files", buildJsonObject {
                put("markdown", "./latest.md")
                put("versionedMarkdown", "./versions/$safeVersion.md")
                put("actionsSchema", "./actions-schema.json")
            })
        }
        return json.encodeToString(JsonObject.serializer(), manifest)
    }

    private fun generateIndex(version: String): String {
        val escapedVersion = version
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        return """<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Orryx Kether 文档</title>
    <style>
        body { max-width: 760px; margin: 64px auto; padding: 0 24px; font-family: system-ui, sans-serif; line-height: 1.7; color: #1f2937; }
        h1 { margin-bottom: 8px; }
        .version { color: #6b7280; }
        ul { padding-left: 22px; }
        a { color: #2563eb; }
        code { padding: 2px 6px; border-radius: 4px; background: #f3f4f6; }
    </style>
</head>
<body>
    <h1>Orryx Kether 文档</h1>
    <p class="version">当前版本：<code>$escapedVersion</code></p>
    <ul>
        <li><a href="kether/latest.md">最新 Markdown 文档</a></li>
        <li><a href="kether/actions-schema.json">Actions Schema JSON</a></li>
        <li><a href="kether/manifest.json">版本清单 Manifest</a></li>
    </ul>
</body>
</html>
"""
    }
}
