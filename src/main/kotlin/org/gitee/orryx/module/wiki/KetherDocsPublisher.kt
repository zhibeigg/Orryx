package org.gitee.orryx.module.wiki

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import org.bukkit.Bukkit
import org.gitee.orryx.core.kether.ScriptManager
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.submit
import taboolib.module.lang.sendLang
import java.io.File
import java.time.Instant

/**
 * 在 Gradle 文档模式下导出可部署到 GitHub Pages 的 Kether 文档供应链产物。
 *
 * 正常服务器不会设置 [OUTPUT_PROPERTY]，因此不会执行任何导出或关服逻辑。
 */
object KetherDocsPublisher {

    const val OUTPUT_PROPERTY = "orryx.ketherDocs.output"
    const val VERSION_PROPERTY = "orryx.ketherDocs.version"
    const val COMMIT_PROPERTY = "orryx.ketherDocs.commit"
    const val CHANNEL_PROPERTY = "orryx.ketherDocs.channel"
    const val GENERATED_AT_PROPERTY = "orryx.ketherDocs.generatedAt"
    const val PREVIOUS_SCHEMA_PROPERTY = "orryx.ketherDocs.previousSchema"
    const val PREVIOUS_RELEASE_ID_PROPERTY = "orryx.ketherDocs.previousReleaseId"
    const val PAGES_BASE_URL = KETHER_DOCS_BASE_URL

    data class RegistrationCounts(
        val actions: Int,
        val selectors: Int,
        val triggers: Int,
        val properties: Int
    )

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @Awake(LifeCycle.ENABLE)
    private fun publishWhenRequested() {
        val outputPath = System.getProperty(OUTPUT_PROPERTY)?.trim()?.takeIf(String::isNotEmpty) ?: return
        submit(delay = 20L) {
            try {
                val metadata = publish(File(outputPath))
                console().sendLang("kether-docs-published", metadata.releaseId, File(outputPath).absolutePath)
            } catch (ex: Throwable) {
                ex.printStackTrace()
                console().sendLang("kether-docs-publish-failed", ex.message ?: ex::class.java.simpleName)
            } finally {
                Bukkit.shutdown()
            }
        }
    }

    internal fun publish(siteDirectory: File): KetherDocsMetadata {
        val version = System.getProperty(VERSION_PROPERTY)?.trim()?.takeIf(String::isNotEmpty) ?: pluginVersion
        val commit = System.getProperty(COMMIT_PROPERTY)?.trim()?.takeIf(String::isNotEmpty)
            ?: "0000000000000000000000000000000000000000"
        val channel = System.getProperty(CHANNEL_PROPERTY)?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: "snapshot"
        val generatedAt = System.getProperty(GENERATED_AT_PROPERTY)?.trim()?.takeIf(String::isNotEmpty)
            ?.let(Instant::parse)
            ?: Instant.now()
        val previousReleaseId = System.getProperty(PREVIOUS_RELEASE_ID_PROPERTY)?.trim()?.takeIf(String::isNotEmpty)
        val metadata = KetherDocsContract.metadata(
            pluginId = pluginId,
            version = version,
            commit = commit,
            channel = channel,
            generatedAt = generatedAt,
            previousReleaseId = previousReleaseId
        )

        validateRegistry()
        if (siteDirectory.exists()) {
            check(siteDirectory.deleteRecursively()) { "无法清理旧文档目录: ${siteDirectory.absolutePath}" }
        }

        val ketherDirectory = File(siteDirectory, "kether")
        val bundleDirectory = File(ketherDirectory, metadata.relativeDirectory)
        val contractsDirectory = File(ketherDirectory, "contracts")
        val channelsDirectory = File(ketherDirectory, "channels")
        check(bundleDirectory.mkdirs() || bundleDirectory.isDirectory) {
            "无法创建 Kether 文档发布目录: ${bundleDirectory.absolutePath}"
        }

        val markdownFile = File(bundleDirectory, "docs.md")
        val schemaFile = File(bundleDirectory, "actions-schema.json")
        val schemaContractFile = File(bundleDirectory, "actions-schema.schema.json")
        val changesFile = File(bundleDirectory, "changes.json")
        val checksumsFile = File(bundleDirectory, "checksums.json")
        val releaseManifestFile = File(bundleDirectory, "manifest.json")

        MarkdownGenerator.generate(markdownFile)
        val schema = ActionsSchemaGenerator.generate(schemaFile, metadata)
        KetherDocsContract.writeUtf8(schemaContractFile, KetherDocsContracts.actionsSchema)

        val previousSchema = System.getProperty(PREVIOUS_SCHEMA_PROPERTY)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(::File)
        require((previousReleaseId == null) == (previousSchema == null)) {
            "上一版 releaseId 与 actions-schema.json 必须同时提供"
        }
        previousSchema?.let { require(it.isFile) { "上一版 Kether Schema 不存在: ${it.absolutePath}" } }
        val changes = KetherDocsDiff.generate(previousSchema, schema, previousReleaseId, metadata.releaseId)
        KetherDocsContract.writeUtf8(changesFile, KetherDocsDiff.encode(changes))

        val checksumInputs = linkedMapOf(
            schemaFile.name to KetherDocsContract.sha256(schemaFile),
            schemaContractFile.name to KetherDocsContract.sha256(schemaContractFile),
            markdownFile.name to KetherDocsContract.sha256(markdownFile),
            changesFile.name to KetherDocsContract.sha256(changesFile)
        )
        KetherDocsContract.writeUtf8(checksumsFile, buildChecksums(checksumInputs))

        val assets = linkedMapOf(
            "schema" to KetherDocsContract.asset(schemaFile, "application/json"),
            "schemaContract" to KetherDocsContract.asset(schemaContractFile, "application/schema+json"),
            "markdown" to KetherDocsContract.asset(markdownFile, "text/markdown; charset=utf-8"),
            "changes" to KetherDocsContract.asset(changesFile, "application/json"),
            "checksums" to KetherDocsContract.asset(checksumsFile, "application/json")
        )
        val counts = RegistrationCounts(
            actions = schema.getValue("actions").jsonArray.size,
            selectors = schema.getValue("selectors").jsonArray.size,
            triggers = schema.getValue("triggers").jsonArray.size,
            properties = schema.getValue("properties").jsonArray.size
        )

        KetherDocsContract.writeUtf8(
            releaseManifestFile,
            buildReleaseManifest(metadata, counts, assets)
        )
        KetherDocsContracts.writeGlobalContracts(contractsDirectory)
        KetherDocsContract.writeUtf8(
            File(channelsDirectory, "${metadata.channel}.json"),
            buildChannelManifest(metadata)
        )

        writeLegacyCompatibility(ketherDirectory, schemaFile, markdownFile, metadata, counts)
        KetherDocsContract.writeUtf8(File(siteDirectory, "index.html"), generateIndex(metadata))
        File(siteDirectory, ".nojekyll").apply {
            parentFile?.mkdirs()
            writeText("", Charsets.UTF_8)
        }

        validateOutput(siteDirectory, bundleDirectory, metadata, assets)
        return metadata
    }

    private fun validateRegistry() {
        require(ScriptManager.wikiActions.isNotEmpty()) { "未注册任何 Kether Action 文档" }
        require(ScriptManager.wikiSelectors.isNotEmpty()) { "未注册任何 Selector 文档" }
        require(ScriptManager.wikiTriggers.isNotEmpty()) { "未注册任何 Trigger 文档" }
        require(ScriptManager.wikiProperties.isNotEmpty()) { "未注册任何 Property 文档" }
    }

    fun sanitizeVersion(version: String): String = org.gitee.orryx.module.wiki.sanitizeVersion(version)

    fun buildChannelManifest(metadata: KetherDocsMetadata): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("${'$'}schema", "$PAGES_BASE_URL/contracts/channel-manifest-v1.schema.json")
            put("formatVersion", KETHER_DOCS_FORMAT_VERSION)
            put("channel", metadata.channel)
            put("releaseId", metadata.releaseId)
            put("pluginVersion", metadata.version)
            put("commit", metadata.commit)
            put("publishedAt", metadata.generatedAt.toString())
            put("releaseManifest", metadata.manifestUrl)
        }
    )

    fun buildReleaseManifest(
        metadata: KetherDocsMetadata,
        counts: RegistrationCounts,
        assets: Map<String, KetherDocsAsset>
    ): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("${'$'}schema", "$PAGES_BASE_URL/contracts/release-manifest-v1.schema.json")
            put("formatVersion", KETHER_DOCS_FORMAT_VERSION)
            put("releaseId", metadata.releaseId)
            put("channel", metadata.channel)
            put("plugin", buildJsonObject {
                put("id", metadata.pluginId)
                put("version", metadata.version)
                put("commit", metadata.commit)
            })
            put("schemaVersion", KETHER_SCHEMA_VERSION)
            put("generatedAt", metadata.generatedAt.toString())
            if (metadata.previousReleaseId == null) put("previousReleaseId", JsonNull)
            else put("previousReleaseId", metadata.previousReleaseId)
            put("assets", buildJsonObject {
                assets.forEach { (name, asset) -> put(name, asset.toJson()) }
            })
            put("counts", counts.toJson())
            put("compatibility", buildJsonObject {
                put("minimumEditorManifestFormat", 1)
                put("minimumEditorSchemaVersion", 2)
            })
        }
    )

    fun buildManifest(
        pluginName: String,
        version: String,
        safeVersion: String = sanitizeVersion(version),
        commit: String? = null,
        generatedAt: Instant = Instant.now(),
        counts: RegistrationCounts
    ): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("pluginId", pluginName)
            put("pluginVersion", version)
            put("version", version)
            put("schemaVersion", KETHER_SCHEMA_VERSION)
            put("generatedAt", generatedAt.toString())
            commit?.let { put("commit", it) }
            put("latest", "$PAGES_BASE_URL/latest.md")
            put("versioned", "$PAGES_BASE_URL/versions/$safeVersion.md")
            put("schema", "$PAGES_BASE_URL/actions-schema.json")
            put("stableChannel", "$PAGES_BASE_URL/channels/stable.json")
            put("snapshotChannel", "$PAGES_BASE_URL/channels/snapshot.json")
            put("counts", counts.toJson())
            put("files", buildJsonObject {
                put("markdown", "./latest.md")
                put("versionedMarkdown", "./versions/$safeVersion.md")
                put("actionsSchema", "./actions-schema.json")
            })
        }
    )

    private fun writeLegacyCompatibility(
        ketherDirectory: File,
        schemaFile: File,
        markdownFile: File,
        metadata: KetherDocsMetadata,
        counts: RegistrationCounts
    ) {
        val versionsDirectory = File(ketherDirectory, "versions").apply { mkdirs() }
        schemaFile.copyTo(File(ketherDirectory, "actions-schema.json"), overwrite = true)
        markdownFile.copyTo(File(ketherDirectory, "latest.md"), overwrite = true)
        markdownFile.copyTo(File(versionsDirectory, "${metadata.safeVersion}.md"), overwrite = true)
        KetherDocsContract.writeUtf8(
            File(ketherDirectory, "manifest.json"),
            buildManifest(
                pluginName = metadata.pluginId,
                version = metadata.version,
                safeVersion = metadata.safeVersion,
                commit = metadata.commit,
                generatedAt = metadata.generatedAt,
                counts = counts
            )
        )
    }

    private fun buildChecksums(files: Map<String, String>): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("formatVersion", 1)
            put("algorithm", "SHA-256")
            put("files", buildJsonObject {
                files.forEach { (path, digest) -> put(path, digest) }
            })
        }
    )

    private fun RegistrationCounts.toJson(): JsonObject = buildJsonObject {
        put("actions", actions)
        put("selectors", selectors)
        put("triggers", triggers)
        put("properties", properties)
    }

    private fun KetherDocsAsset.toJson(): JsonObject = buildJsonObject {
        put("path", path)
        put("mediaType", mediaType)
        put("bytes", bytes)
        put("sha256", sha256)
    }

    private fun validateOutput(
        siteDirectory: File,
        bundleDirectory: File,
        metadata: KetherDocsMetadata,
        assets: Map<String, KetherDocsAsset>
    ) {
        require(File(siteDirectory, ".nojekyll").isFile) { "GitHub Pages 标记缺失" }
        require(File(siteDirectory, "index.html").length() > 0L) { "index.html 为空" }
        require(File(siteDirectory, "kether/channels/${metadata.channel}.json").length() in 1..32L * 1024) {
            "channel manifest 为空或超过 32 KiB"
        }
        require(File(bundleDirectory, "manifest.json").length() in 1..64L * 1024) {
            "release manifest 为空或超过 64 KiB"
        }
        val budgets = mapOf(
            "schema" to 4L * 1024 * 1024,
            "schemaContract" to 512L * 1024,
            "markdown" to 8L * 1024 * 1024,
            "changes" to 2L * 1024 * 1024,
            "checksums" to 128L * 1024
        )
        assets.forEach { (name, asset) ->
            require(asset.bytes in 1..budgets.getValue(name)) { "$name 资产为空或超过预算" }
        }
    }

    private fun generateIndex(metadata: KetherDocsMetadata): String {
        val escapedVersion = metadata.version.htmlEscape()
        val escapedReleaseId = metadata.releaseId.htmlEscape()
        return """<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Orryx Kether 文档</title>
    <style>
        body { max-width: 840px; margin: 64px auto; padding: 0 24px; font-family: system-ui, sans-serif; line-height: 1.7; color: #1f2937; }
        h1 { margin-bottom: 8px; }
        .version { color: #6b7280; }
        ul { padding-left: 22px; }
        a { color: #2563eb; }
        code { padding: 2px 6px; border-radius: 4px; background: #f3f4f6; overflow-wrap: anywhere; }
    </style>
</head>
<body>
    <h1>Orryx Kether 文档</h1>
    <p class="version">当前生成版本：<code>$escapedVersion</code></p>
    <p class="version">发布标识：<code>$escapedReleaseId</code></p>
    <ul>
        <li><a href="kether/channels/stable.json">Stable Channel</a></li>
        <li><a href="kether/channels/snapshot.json">Snapshot Channel</a></li>
        <li><a href="kether/latest.md">兼容 Markdown 文档</a></li>
        <li><a href="kether/actions-schema.json">兼容 Actions Schema JSON</a></li>
        <li><a href="kether/manifest.json">兼容 Manifest</a></li>
    </ul>
</body>
</html>"""
    }

    private fun String.htmlEscape(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
