package org.gitee.orryx.core.editor.handler

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.editor.EditorClient

/**
 * Editor 请求的有界顺序队列。
 *
 * 队列中的请求始终绑定接收它的连接 generation；旧连接请求在开始执行前会被丢弃，响应也只能发送回
 * 同一 generation。使用单一消费者避免为每个请求创建等待锁的协程。
 */
object EditorRequestQueue {

    private const val QUEUE_CAPACITY = 64
    private const val QUEUE_FULL_MESSAGE = "Editor 操作队列已满，请稍后重试"

    private data class Request(
        val generation: Long,
        val id: String,
        val failureMessage: String,
        val block: suspend (Long) -> Unit,
    )

    private val requests = Channel<Request>(QUEUE_CAPACITY)
    private val worker by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OrryxAPI.ioScope.launch {
            for (request in requests) {
                if (!EditorClient.isGenerationCurrent(request.generation)) continue
                try {
                    request.block(request.generation)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: EditorFilePolicy.PolicyException) {
                    EditorClient.sendError(request.generation, request.id, e.message ?: request.failureMessage)
                } catch (e: Exception) {
                    EditorClient.sendError(
                        request.generation,
                        request.id,
                        "${request.failureMessage}: ${e.message ?: e.javaClass.simpleName}",
                    )
                }
            }
        }
    }

    fun enqueue(
        generation: Long,
        id: String,
        failureMessage: String,
        block: suspend (Long) -> Unit,
    ): Boolean {
        if (!EditorClient.isGenerationCurrent(generation)) return false
        worker
        val result = requests.trySend(Request(generation, id, failureMessage, block))
        if (result.isSuccess) return true
        EditorClient.sendError(generation, id, QUEUE_FULL_MESSAGE)
        return false
    }
}
