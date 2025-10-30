package org.gitee.orryx.module.wiki

import com.google.gson.JsonParser
import com.lark.oapi.Client
import com.lark.oapi.core.request.RequestOptions
import com.lark.oapi.core.utils.Jsons
import com.lark.oapi.service.auth.v3.model.InternalTenantAccessTokenReq
import com.lark.oapi.service.auth.v3.model.InternalTenantAccessTokenReqBody
import com.lark.oapi.service.docx.v1.enums.BlockBlockTypeEnum
import com.lark.oapi.service.docx.v1.model.*
import com.lark.oapi.service.wiki.v2.model.MoveDocsToWikiSpaceNodeReq
import com.lark.oapi.service.wiki.v2.model.MoveDocsToWikiSpaceNodeReqBody
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets.UTF_8


object LarkSuite {

    private val appId
        get() = Orryx.config.getString("LarkSuite.AppId")

    private val appSecret
        get() = Orryx.config.getString("LarkSuite.AppSecret")

    private val parentWikiToken
        get() = Orryx.config.getString("LarkSuite.ParentWikiToken")

    private val spaceId
        get() = Orryx.config.getString("LarkSuite.SpaceId")

    private val client: Client by lazy {
        Client.newBuilder(appId, appSecret).requestTimeout(30, TimeUnit.MINUTES).build()
    }

    internal var ioScope: CoroutineScope? = null

    @Awake(LifeCycle.DISABLE)
    private fun disable() {
        ioScope?.cancel("服务器关闭")
    }

    fun createDocument() {
        if (ioScope == null) { ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob()) }
        ioScope!!.launch {
            consoleMessage("&e┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            consoleMessage("&e┣&7新文档$pluginId-$pluginVersion-(自生成) 开始创建")

            val token = getToken() ?: return@launch
            val documentId = async {
                // 创建请求对象
                val req = CreateDocumentReq.newBuilder()
                    .createDocumentReqBody(
                        CreateDocumentReqBody.newBuilder()
                            .title("$pluginId-$pluginVersion-(自生成)")
                            .build()
                    )
                    .build()

                // 发起请求
                val resp = client.docx().v1().document().create(
                    req,
                    RequestOptions.newBuilder()
                        .userAccessToken(token)
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
                consoleMessage("&e┣&7空文档 创建成功 &a√")
                JsonParser().parse(String(resp.rawResponse.body, UTF_8)).asJsonObject["data"].asJsonObject["document"].asJsonObject["document_id"].asString
            }.await()
            documentId?.let {
                move(token, it)
                createDocumentBlocks(token, it)
            }

            consoleMessage("&e┣&7新文档已创建成功 &a√")
            consoleMessage("&e┣&7访问地址 &fhttps://www.feishu.cn/docx/$documentId")
            consoleMessage("&e┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    
    private suspend fun CoroutineScope.move(token: String, documentId: String) {
        async {
            val req = MoveDocsToWikiSpaceNodeReq.newBuilder()
                .spaceId(spaceId)
                .moveDocsToWikiSpaceNodeReqBody(
                    MoveDocsToWikiSpaceNodeReqBody.newBuilder()
                        .parentWikiToken(parentWikiToken)
                        .objType("docx")
                        .objToken(documentId)
                        .build()
                ).build()

            // 发起请求
            val resp = client.wiki().v2().spaceNode().moveDocsToWiki(
                req, RequestOptions.newBuilder()
                    .userAccessToken(token)
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
            }
        }.await()
    }

    private suspend fun CoroutineScope.getToken(): String? {
        return async {
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
                        Json.encodeToJsonElement(String(resp.rawResponse.body, UTF_8))
                    )
                )
                return@async null
            }

            JsonParser().parse(String(resp.rawResponse.body, UTF_8)).asJsonObject.get("tenant_access_token").asString
        }.await()
    }

    private suspend fun CoroutineScope.createDocumentBlocks(token: String, documentId: String) {
        val actionGroup = ScriptManager.wikiActions.groupBy { it.group }
        val selectorsGroup = ScriptManager.wikiSelectors.groupBy { it.type }
        val triggersGroup = ScriptManager.wikiTriggers.groupBy { it.group }
        debug(actionGroup.mapValues { it.value.map { action -> action.name } })
        debug(selectorsGroup.mapValues { it.value.map { selector -> selector.name } })
        debug(triggersGroup.mapValues { it.value.map { trigger -> trigger.key } })
        createPs(token, documentId)
        actionGroup.forEach { (g, u) ->
            createGroup(token, g, u, documentId)
        }
        createSelectorHanging(token, documentId)
        selectorsGroup.forEach { (g, u) ->
            createSelectorType(token, g, u, documentId)
        }
        createTriggerHanging(token, documentId)
        triggersGroup.forEach { (g, u) ->
            createTriggerType(token, g, u, documentId)
        }
    }

    private suspend fun CoroutineScope.createPs(token: String, documentId: String) {
        async {
            val req = CreateDocumentBlockChildrenReq.newBuilder()
                .documentId(documentId)
                .blockId(documentId)
                .documentRevisionId(-1)
                .createDocumentBlockChildrenReqBody(
                    CreateDocumentBlockChildrenReqBody.newBuilder()
                        .children(
                            arrayOf(
                                Block.newBuilder()
                                    .blockId("ps_text_0")
                                    .children(arrayOf())
                                    .blockType(BlockBlockTypeEnum.TEXT)
                                    .text(
                                        Text.newBuilder().elements(
                                            arrayOf(
                                                TextElement.newBuilder()
                                                    .textRun(
                                                        TextRun.newBuilder().content("更多原生Kether语句请查看 https://kether.tabooproject.org/list.html").build()
                                                    ).build()
                                            )
                                        ).build()
                                    )
                                    .build(),
                                Block.newBuilder()
                                    .blockId("ps_text_1")
                                    .children(arrayOf())
                                    .blockType(BlockBlockTypeEnum.TEXT)
                                    .text(
                                        Text.newBuilder().elements(
                                            arrayOf(
                                                TextElement.newBuilder()
                                                    .textRun(
                                                        TextRun.newBuilder().content("[*] 代表可选 <*> 代表必选 () 代表默认值 前缀*代表先导词").build()
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
                    .userAccessToken(token)
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
        }.await()
    }

    private suspend fun CoroutineScope.createGroup(token: String, group: String, list: List<Action>, documentId: String) {
        val keyGroup = list.groupBy { it.key }
        async {
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
                    .userAccessToken(token)
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
            consoleMessage("&e┣┳&7Group: $group 创建成功 &a√")
            val l = keyGroup.toList()
            l.forEach {
                if (l.last() == it) {
                    createKey(token, it.first, it.second, documentId, l.size > 1, true)
                } else {
                    createKey(token, it.first, it.second, documentId, l.size > 1, false)
                }
            }
        }.await()
    }

    private suspend fun CoroutineScope.createKey(
        token: String,
        key: String,
        list: List<Action>,
        documentId: String,
        change: Boolean,
        last: Boolean
    ) {
        async {
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
                    .userAccessToken(token)
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
                consoleMessage("&e┃┗┳&7Key: $key 创建成功 &a√")
            } else {
                consoleMessage("&e┃┣┳&7Key: $key 创建成功 &a√")
            }
        }.await()
        list.forEach {
            async { createAction(token, it, documentId) }.await()
            if (change && !last) {
                if (list.last() == it) {
                    consoleMessage("&e┃┃┗&7Action: ${it.name} 创建成功 &a√")
                } else {
                    consoleMessage("&e┃┃┣&7Action: ${it.name} 创建成功 &a√")
                }
            } else {
                if (list.last() == it) {
                    consoleMessage("&e┃ ┗&7Action: ${it.name} 创建成功 &a√")
                } else {
                    consoleMessage("&e┃ ┣&7Action: ${it.name} 创建成功 &a√")
                }
            }
        }
    }

    private suspend fun CoroutineScope.createAction(token: String, action: Action, documentId: String) {
        delay(300)
        async {
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
                    .userAccessToken(token)
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
        }.await()
    }

    private suspend fun CoroutineScope.createSelectorHanging(token: String, documentId: String) {
        async {
            val req = CreateDocumentBlockChildrenReq.newBuilder()
                .documentId(documentId)
                .blockId(documentId)
                .documentRevisionId(-1)
                .createDocumentBlockChildrenReqBody(
                    CreateDocumentBlockChildrenReqBody.newBuilder()
                        .children(
                            arrayOf(
                                Block.newBuilder()
                                    .blockId("Selector_hanging1")
                                    .children(arrayOf())
                                    .blockType(BlockBlockTypeEnum.HEADING1)
                                    .heading1(
                                        Text.newBuilder().elements(
                                            arrayOf(
                                                TextElement.newBuilder()
                                                    .textRun(
                                                        TextRun.newBuilder().content("Selector选择器列表").build()
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
                    .userAccessToken(token)
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
        }.await()
    }

    private suspend fun CoroutineScope.createSelectorType(token: String, type: SelectorType, list: List<Selector>, documentId: String) {
        async {
            val req = CreateDocumentBlockChildrenReq.newBuilder()
                .documentId(documentId)
                .blockId(documentId)
                .documentRevisionId(-1)
                .createDocumentBlockChildrenReqBody(
                    CreateDocumentBlockChildrenReqBody.newBuilder()
                        .children(
                            arrayOf(
                                Block.newBuilder()
                                    .blockId("${type.name}_heading2")
                                    .children(arrayOf())
                                    .blockType(BlockBlockTypeEnum.HEADING2)
                                    .heading2(
                                        Text.newBuilder().elements(
                                            arrayOf(
                                                TextElement.newBuilder()
                                                    .textRun(
                                                        TextRun.newBuilder().content(
                                                            when(type) {
                                                                SelectorType.STREAM -> "流式选择器"
                                                                SelectorType.GEOMETRY -> "几何选择器"
                                                            }
                                                        ).build()
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
                    .userAccessToken(token)
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
            consoleMessage("&e┣┳&7SelectorType: ${type.name} 创建成功 &a√")
        }.await()
        list.forEach {
            if (list.last() == it) {
                createSelector(token, it, documentId, true)
            } else {
                createSelector(token, it, documentId, false)
            }
        }
    }

    private suspend fun CoroutineScope.createSelector(token: String, selector: Selector, documentId: String, last: Boolean) {
        delay(300)
        async {
            val blocks = selector.createBlocks()
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
                    .userAccessToken(token)
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
                consoleMessage("&e┃┗&7Selector: ${selector.name} 创建成功 &a√")
            } else {
                consoleMessage("&e┃┣&7Selector: ${selector.name} 创建成功 &a√")
            }
        }.await()
    }

    private suspend fun CoroutineScope.createTriggerHanging(token: String, documentId: String) {
       async {
            val req = CreateDocumentBlockChildrenReq.newBuilder()
                .documentId(documentId)
                .blockId(documentId)
                .documentRevisionId(-1)
                .createDocumentBlockChildrenReqBody(
                    CreateDocumentBlockChildrenReqBody.newBuilder()
                        .children(
                            arrayOf(
                                Block.newBuilder()
                                    .blockId("Trigger_hanging1")
                                    .children(arrayOf())
                                    .blockType(BlockBlockTypeEnum.HEADING1)
                                    .heading1(
                                        Text.newBuilder().elements(
                                            arrayOf(
                                                TextElement.newBuilder()
                                                    .textRun(
                                                        TextRun.newBuilder().content("Trigger触发器列表").build()
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
                    .userAccessToken(token)
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
        }.await()
    }

    private suspend fun CoroutineScope.createTriggerType(token: String, group: TriggerGroup, list: List<Trigger>, documentId: String) {
        async {
            val req = CreateDocumentBlockChildrenReq.newBuilder()
                .documentId(documentId)
                .blockId(documentId)
                .documentRevisionId(-1)
                .createDocumentBlockChildrenReqBody(
                    CreateDocumentBlockChildrenReqBody.newBuilder()
                        .children(
                            arrayOf(
                                Block.newBuilder()
                                    .blockId("Trigger_${group}_heading2")
                                    .children(arrayOf())
                                    .blockType(BlockBlockTypeEnum.HEADING2)
                                    .heading2(
                                        Text.newBuilder().elements(
                                            arrayOf(
                                                TextElement.newBuilder()
                                                    .textRun(
                                                        TextRun.newBuilder().content(
                                                            group.value
                                                        ).build()
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
                    .userAccessToken(token)
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
            consoleMessage("&e┣┳&7TriggerGroup: ${group.value} 创建成功 &a√")
        }.await()
        list.forEach {
            if (list.last() == it) {
                createTrigger(token, it, documentId, true)
            } else {
                createTrigger(token, it, documentId, false)
            }
        }
    }

    private suspend fun CoroutineScope.createTrigger(token: String, trigger: Trigger, documentId: String, last: Boolean) {
        delay(300)
        async {
            val blocks = trigger.createBlocks()
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
                    .userAccessToken(token)
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
                consoleMessage("&e┃┗&7Trigger: ${trigger.key} 创建成功 &a√")
            } else {
                consoleMessage("&e┃┣&7Trigger: ${trigger.key} 创建成功 &a√")
            }
        }.await()
    }
}