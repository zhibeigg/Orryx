package org.gitee.orryx.core.wiki

import com.google.gson.JsonParser
import com.lark.oapi.Client
import com.lark.oapi.core.request.RequestOptions
import com.lark.oapi.core.utils.Jsons
import com.lark.oapi.service.auth.v3.model.InternalTenantAccessTokenReq
import com.lark.oapi.service.auth.v3.model.InternalTenantAccessTokenReqBody
import com.lark.oapi.service.docx.v1.enums.BlockBlockTypeEnum
import com.lark.oapi.service.docx.v1.model.*
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.utils.debug
import org.gitee.orryx.utils.gson
import taboolib.common.platform.function.info
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion
import taboolib.expansion.Chain
import taboolib.expansion.DurationType
import taboolib.expansion.submitChain
import taboolib.module.chat.colored
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets.UTF_8


object LarkSuite {

    private val appId
        get() = OrryxAPI.config.getString("LarkSuite.AppId")

    private val appSecret
        get() = OrryxAPI.config.getString("LarkSuite.AppSecret")

    private val folderToken
        get() = OrryxAPI.config.getString("LarkSuite.FolderToken")

    private val client: Client by lazy {
        Client.newBuilder(appId, appSecret).requestTimeout(30, TimeUnit.MINUTES).build()
    }

    fun createDocument() {
        submitChain {
            info("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
            info("&e┣&7新文档$pluginId-$pluginVersion-(自生成) 开始创建".colored())
            val documentId = async {
                // 创建请求对象
                val req = CreateDocumentReq.newBuilder()
                    .createDocumentReqBody(
                        CreateDocumentReqBody.newBuilder()
                            .folderToken(folderToken)
                            .title("$pluginId-$pluginVersion-(自生成)")
                            .build()
                    )
                    .build()

                // 发起请求
                val resp = client.docx().v1().document().create(
                    req,
                    RequestOptions.newBuilder()
                        .userAccessToken(getToken() ?: return@async null)
                        .build()
                )

                // 处理服务端错误
                if (!resp.success()) {
                    println(
                        String.format(
                            "code:%s,msg:%s,reqId:%s, resp:%s",
                            resp.code, resp.msg, resp.requestId, Jsons.createGSON(true, false).toJson(
                                JsonParser().parse(
                                    String(resp.rawResponse.body, UTF_8)
                                )
                            )
                        )
                    )
                    return@async null
                }
                info("&e┣&7空文档 创建成功 &a√".colored())
                JsonParser().parse(String(resp.rawResponse.body, UTF_8)).asJsonObject["data"].asJsonObject["document"].asJsonObject["document_id"].asString
            }
            documentId?.let { createDocumentBlocks(it, this) }

            info("&e┣&7新文档已创建成功 &a√".colored())
            info("&e┣&7访问地址 &fhttps://www.feishu.cn/docx/$documentId".colored())
            info("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━".colored())
        }
    }

    private fun getToken(): String? {
        // 创建请求对象
        val req = InternalTenantAccessTokenReq.newBuilder()
            .internalTenantAccessTokenReqBody(
                InternalTenantAccessTokenReqBody.newBuilder()
                    .appId(appId)
                    .appSecret(appSecret)
                    .build()
            )
            .build()

        // 发起请求
        val resp = client.auth().v3().tenantAccessToken().internal(req)

        // 处理服务端错误
        if (!resp.success()) {
            println(
                String.format(
                    "code:%s,msg:%s,reqId:%s, resp:%s",
                    resp.code,
                    resp.msg,
                    resp.requestId,
                    gson.toJson(String(resp.rawResponse.body, UTF_8))
                )
            )
            return null
        }

        val token = JsonParser().parse(String(resp.rawResponse.body, UTF_8)).asJsonObject.get("tenant_access_token").asString
        return token
    }

    private suspend fun createDocumentBlocks(documentId: String, chain: Chain<*>) {
        val group = ScriptManager.wikiActions.values.groupBy { it.group }
        debug(group.mapValues { it.value.map { it.name } })
        group.forEach { (g, u) ->
            createGroup(g, u, documentId, chain)
        }
    }

    private suspend fun createGroup(group: String, list: List<Action>, documentId: String, chain: Chain<*>) {
        val keyGroup = list.groupBy { it.key }
        chain.async {
            val req = CreateDocumentBlockChildrenReq.newBuilder()
                .documentId(documentId)
                .blockId(documentId)
                .documentRevisionId(-1)
                .createDocumentBlockChildrenReqBody(
                    CreateDocumentBlockChildrenReqBody.newBuilder()
                        .children(
                            arrayOf(
                                Block.newBuilder()
                                    .blockId("${group}_heading1")
                                    .children(arrayOf())
                                    .blockType(BlockBlockTypeEnum.HEADING1)
                                    .heading1(
                                        Text.newBuilder().elements(
                                            arrayOf(
                                                TextElement.newBuilder()
                                                    .textRun(
                                                        TextRun.newBuilder().content(group).build()
                                                    ).build()
                                            )
                                        ).build()
                                    )
                                    .build()
                            )
                        )
                        .index(-1)
                        .build()
                )
                .build()

            // 发起请求
            val resp = client.docx().v1().documentBlockChildren().create(
                req, RequestOptions.newBuilder()
                    .userAccessToken(getToken() ?: return@async)
                    .build()
            )

            // 处理服务端错误
            if (!resp.success()) {
                println(
                    String.format(
                        "code:%s,msg:%s,reqId:%s, resp:%s",
                        resp.code,
                        resp.msg,
                        resp.requestId,
                        String(resp.rawResponse.body, UTF_8)
                    )
                )
                return@async
            }
            info("&e┣┳&7Group: $group 创建成功 &a√".colored())
        }
        val l = keyGroup.toList()
        l.forEach {
            if (l.last() == it) {
                createKey(it.first, it.second, documentId, chain, l.size > 1, true)
            } else {
                createKey(it.first, it.second, documentId, chain, l.size > 1, false)
            }
        }
    }

    private suspend fun createKey(key: String, list: List<Action>, documentId: String, chain: Chain<*>, change: Boolean, last: Boolean) {
        chain.async {
            val req = CreateDocumentBlockChildrenReq.newBuilder()
                .documentId(documentId)
                .blockId(documentId)
                .documentRevisionId(-1)
                .createDocumentBlockChildrenReqBody(
                    CreateDocumentBlockChildrenReqBody.newBuilder()
                        .children(
                            arrayOf(
                                Block.newBuilder()
                                    .blockId("${key}_heading2")
                                    .children(arrayOf())
                                    .blockType(BlockBlockTypeEnum.HEADING2)
                                    .heading2(
                                        Text.newBuilder().elements(
                                            arrayOf(
                                                TextElement.newBuilder()
                                                    .textRun(
                                                        TextRun.newBuilder().content(key).build()
                                                    ).build()
                                            )
                                        ).build()
                                    )
                                    .build()
                            )
                        )
                        .index(-1)
                        .build()
                )
                .build()

            // 发起请求
            val resp = client.docx().v1().documentBlockChildren().create(
                req, RequestOptions.newBuilder()
                    .userAccessToken(getToken() ?: return@async)
                    .build()
            )

            // 处理服务端错误
            if (!resp.success()) {
                println(
                    String.format(
                        "code:%s,msg:%s,reqId:%s, resp:%s",
                        resp.code,
                        resp.msg,
                        resp.requestId,
                        String(resp.rawResponse.body, UTF_8)
                    )
                )
                return@async
            }
            if (last) {
                info("&e┃┗┳&7Key: $key 创建成功 &a√".colored())
            } else {
                info("&e┃┣┳&7Key: $key 创建成功 &a√".colored())
            }
        }
        list.forEach {
            createAction(it, documentId, chain)
            if (change && !last) {
                if (list.last() == it) {
                    info("&e┃┃┗&7Action: ${it.name} 创建成功 &a√".colored())
                } else {
                    info("&e┃┃┣&7Action: ${it.name} 创建成功 &a√".colored())
                }
            } else {
                if (list.last() == it) {
                    info("&e┃ ┗&7Action: ${it.name} 创建成功 &a√".colored())
                } else {
                    info("&e┃ ┣&7Action: ${it.name} 创建成功 &a√".colored())
                }
            }
        }
    }

    private suspend fun createAction(action: Action, documentId: String, chain: Chain<*>) {
        chain.wait(1000, DurationType.MILLIS)
        chain.async {
            val blocks = action.createBlocks()
            val req = CreateDocumentBlockDescendantReq.newBuilder()
                .documentId(documentId)
                .blockId(documentId)
                .documentRevisionId(-1)
                .createDocumentBlockDescendantReqBody(
                    CreateDocumentBlockDescendantReqBody.newBuilder()
                        .childrenId(blocks.second.toTypedArray())
                        .index(-1)
                        .descendants(blocks.first.toTypedArray())
                        .build()
                )
                .build()

            // 发起请求
            val resp = client.docx().v1().documentBlockDescendant().create(
                req, RequestOptions.newBuilder()
                    .userAccessToken(getToken() ?: return@async)
                    .build()
            )

            // 处理服务端错误
            if (!resp.success()) {
                println(
                    String.format(
                        "code:%s,msg:%s,reqId:%s, resp:%s",
                        resp.code,
                        resp.msg,
                        resp.requestId,
                        String(resp.rawResponse.body, UTF_8)
                    )
                )
                return@async
            }
        }
    }

}