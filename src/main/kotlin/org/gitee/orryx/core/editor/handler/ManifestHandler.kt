package org.gitee.orryx.core.editor.handler

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gitee.orryx.core.editor.EditorClient
import org.gitee.orryx.core.editor.EditorProtocol
import taboolib.common.platform.function.getDataFolder

/** V2 working-tree manifest 快照处理器。 */
object ManifestHandler {

    private val policy: EditorFilePolicy by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        EditorFilePolicy(getDataFolder().toPath())
    }

    fun handle(generation: Long, id: String) {
        EditorRequestQueue.enqueue(generation, id, "生成 manifest 快照失败") { requestGeneration ->
            val snapshot = policy.snapshotManifest()
            EditorClient.sendMessage(requestGeneration, EditorProtocol.MANIFEST_SNAPSHOT, id, buildJsonObject {
                put("manifestId", snapshot.manifestId)
                put("revision", snapshot.revision)
                put("files", JsonArray(snapshot.files.map { entry ->
                    buildJsonObject {
                        put("path", entry.path)
                        put("revision", entry.revision)
                        put("size", entry.size)
                    }
                }))
            })
        }
    }
}
