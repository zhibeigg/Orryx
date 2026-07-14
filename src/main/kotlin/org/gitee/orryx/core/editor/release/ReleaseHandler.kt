package org.gitee.orryx.core.editor.release

import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.editor.EditorClient
import org.gitee.orryx.core.editor.EditorProtocol
import org.gitee.orryx.core.editor.handler.EditorRequestQueue
import org.gitee.orryx.core.reload.ReloadAPI
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.mainThreadFuture
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getDataFolder

/** V2 signed release.request 分发器。 */
object ReleaseHandler {

    private val manager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val dataFolder = getDataFolder().toPath()
        ReleaseTransactionManager(
            liveRoot = dataFolder,
            transactionsRoot = dataFolder.resolve(".editor/releases/transactions"),
            configProvider = ReleaseConfig::load,
        )
    }

    @Awake(LifeCycle.ENABLE)
    private fun recoverOnEnable() {
        OrryxAPI.ioScope.launch {
            runCatching {
                manager.recover { transactionId -> startReadiness(EditorClient.currentGeneration(), transactionId) }
            }.onFailure { failure ->
                consoleMessage("&c[Editor Release] 启动恢复扫描失败: ${EditorClient.sanitizeLogMessage(failure.message)}")
            }
        }
    }

    fun handle(generation: Long, id: String, data: JsonObject?) {
        val request = try {
            ReleaseRequest.parse(data)
        } catch (failure: Throwable) {
            EditorClient.sendError(
                generation,
                id,
                failure.message ?: "release.request 无效",
                (failure as? ReleaseException)?.code ?: "INVALID_RELEASE_REQUEST",
                EditorProtocol.RELEASE_REQUEST,
            )
            return
        }
        try {
            validateSession(generation)
        } catch (failure: Throwable) {
            sendResult(generation, id, manager.failure(request, failure))
            return
        }
        EditorRequestQueue.enqueue(generation, id, "发布事务执行失败") { requestGeneration ->
            val result = try {
                validateSession(requestGeneration)
                when (request.action) {
                    ReleaseAction.PREPARE -> manager.prepare(request)
                    ReleaseAction.COMMIT -> manager.commit(request)
                    ReleaseAction.STATUS -> manager.status(request)
                    ReleaseAction.ROLLBACK -> manager.rollback(request)
                }
            } catch (failure: Throwable) {
                manager.failure(request, failure)
            }
            sendResult(requestGeneration, id, result)
            if (request.action == ReleaseAction.COMMIT && result.pluginState == ReleaseState.READINESS_PENDING) {
                startReadiness(requestGeneration, request.transactionId)
            }
        }
    }

    private fun startReadiness(generation: Long, transactionId: String) {
        OrryxAPI.ioScope.launch {
            val result = runCatching {
                manager.completeReadiness(transactionId) {
                    val report = mainThreadFuture { ReloadAPI.reloadWithReport() }.await()
                    ReadinessReport(report.success, report.summary())
                }
            }.getOrNull() ?: return@launch
            sendResult(generation, "", result)
        }
    }

    private fun validateSession(generation: Long) {
        if (!EditorClient.isProtocolV2(generation)) {
            throw ReleaseException("RELEASE_REQUIRES_V2", "release.request 仅允许协商为 V2 的会话")
        }
        if (EditorClient.currentWorkspaceId() == null || EditorClient.currentSessionEpoch() == null) {
            throw ReleaseException("RELEASE_SESSION_INVALID", "V2 workspace/session 元数据缺失")
        }
        if (EditorProtocol.RELAY_RELEASE_CAPABILITY !in EditorClient.currentRelayCapabilities()) {
            throw ReleaseException("RELAY_CAPABILITY_MISSING", "relay 未声明 release.control.v1")
        }
        if (!ReleaseConfig.load().enabled) {
            throw ReleaseException("RELEASE_DISABLED", "Editor.Release.Enable 未开启")
        }
    }

    private fun sendResult(generation: Long, id: String, result: ReleaseResult) {
        EditorClient.sendMessage(generation, EditorProtocol.RELEASE_RESULT, id, buildJsonObject {
            put("action", result.action.wireName)
            put("transactionId", result.transactionId)
            put("releaseId", result.releaseId)
            put("commandId", result.commandId)
            put("success", result.success)
            put("pluginState", result.pluginState.wireName())
            put("eventId", result.eventId)
            put("eventSeq", result.eventSeq)
            result.observedManifestRevision?.let { put("observedManifestRevision", it) }
            result.resultManifestRevision?.let { put("resultManifestRevision", it) }
            result.errorCode?.let { put("errorCode", it) }
            result.message?.let { put("message", it.take(1000)) }
        })
    }
}
