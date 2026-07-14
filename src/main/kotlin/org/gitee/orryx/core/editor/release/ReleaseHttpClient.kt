package org.gitee.orryx.core.editor.release

import com.lark.oapi.okhttp.Call
import com.lark.oapi.okhttp.Callback
import com.lark.oapi.okhttp.OkHttpClient
import com.lark.oapi.okhttp.Request
import com.lark.oapi.okhttp.Response
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class DownloadedFile(val size: Long, val sha256: String)

/** OkHttp callback 到 suspend 的非阻塞桥接；响应体消费发生在 OkHttp I/O 回调线程。 */
internal object ReleaseHttpClient {

    private val baseClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    suspend fun fetchMetadata(uri: URI, bearerToken: String, config: ReleaseConfig): ByteArray {
        return execute(uri, bearerToken, config) { response ->
            val body = response.body() ?: throw ReleaseException("EMPTY_HTTP_RESPONSE", "operations metadata 响应为空")
            val declared = body.contentLength()
            if (declared > MAX_METADATA_BYTES) {
                throw ReleaseException("OPERATIONS_TOO_LARGE", "operations metadata 超过大小限制")
            }
            body.byteStream().use { input ->
                val output = ByteArrayOutputStream(if (declared in 1..MAX_METADATA_BYTES) declared.toInt() else 8192)
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    total += count
                    if (total > MAX_METADATA_BYTES) {
                        throw ReleaseException("OPERATIONS_TOO_LARGE", "operations metadata 超过大小限制")
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        }
    }

    suspend fun download(
        uri: URI,
        bearerToken: String,
        config: ReleaseConfig,
        target: Path,
        expectedSize: Long,
    ): DownloadedFile {
        return execute(uri, bearerToken, config) { response ->
            val body = response.body() ?: throw ReleaseException("EMPTY_HTTP_RESPONSE", "文件下载响应为空")
            val declared = body.contentLength()
            if (declared >= 0L && declared != expectedSize) {
                throw ReleaseException("CONTENT_SIZE_MISMATCH", "文件 Content-Length 与清单不一致")
            }
            Files.createDirectories(target.parent)
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            body.byteStream().use { input ->
                FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        total += count
                        if (total > expectedSize) {
                            throw ReleaseException("CONTENT_SIZE_MISMATCH", "下载文件超过清单大小")
                        }
                        digest.update(buffer, 0, count)
                        var bytes = ByteBuffer.wrap(buffer, 0, count)
                        while (bytes.hasRemaining()) output.write(bytes)
                    }
                    output.force(true)
                }
            }
            if (total != expectedSize) {
                throw ReleaseException("CONTENT_SIZE_MISMATCH", "下载文件大小与清单不一致")
            }
            DownloadedFile(total, ReleaseCanonical.sha256DigestToHex(digest.digest()))
        }
    }

    private suspend fun <T> execute(
        uri: URI,
        bearerToken: String,
        config: ReleaseConfig,
        reader: (Response) -> T,
    ): T = suspendCancellableCoroutine { continuation ->
        val client = baseClient.newBuilder()
            .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMillis, TimeUnit.MILLISECONDS)
            .callTimeout(config.callTimeoutMillis, TimeUnit.MILLISECONDS)
            .build()
        val request = Request.Builder()
            .url(uri.toASCIIString())
            .header("Accept", "application/json, application/octet-stream")
            .header("Authorization", "Bearer $bearerToken")
            .get()
            .build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { closed ->
                    if (!continuation.isActive) return
                    try {
                        if (closed.isRedirect) {
                            throw ReleaseException("HTTP_REDIRECT_REJECTED", "发布下载不允许重定向")
                        }
                        if (!closed.isSuccessful) {
                            throw ReleaseException("HTTP_DOWNLOAD_FAILED", "发布下载失败: HTTP ${closed.code()}")
                        }
                        continuation.resume(reader(closed))
                    } catch (throwable: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(throwable)
                    }
                }
            }
        })
    }

    private const val MAX_METADATA_BYTES = 4L * 1024L * 1024L
}
