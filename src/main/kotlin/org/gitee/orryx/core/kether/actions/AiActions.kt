package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.ai.OpenAI
import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.bukkitPlayer
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.expansion.submitChain
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.kether.KetherParser

object AiActions {

    @Config("npc.yml")
    lateinit var config: ConfigFile

    class Npc(val key: String, val name: String, val system: String, val model: String, val maxTokens: Int, val temperature: Double)

    private val npcMap by lazy { mutableMapOf<String, Npc>() }

    @Awake(LifeCycle.ENABLE)
    @Reload(1)
    private fun reload() {
        npcMap.clear()
        config.reload()
        config.getKeys(false).forEach { key ->
            val name = config.getString("$key.name")!!.colored()
            val system = config.getString("$key.system")!!
            val model = config.getString("$key.model")!!
            val maxTokens = config.getInt("$key.maxTokens", 64)
            val temperature = config.getDouble("$key.temperature")
            npcMap[key] = Npc(key, name, system, model, maxTokens, temperature)
        }
        info("&e┣&7Npc loaded &e${npcMap.size} &a√".colored())
    }

    @KetherParser(["aiChat"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun aiChat() = combinationParser(
        Action.new("AI智能", "模拟Npc对话", "npc", true)
            .description("模拟Npc对话，不同Npc人设请在npc.yml中配置，等待直到AI返回内容")
            .addEntry("模拟的npc", Type.STRING)
            .addEntry("对话信息", Type.STRING)
            .result("回复信息", Type.STRING)
    ) {
        it.group(
            text(),
            text()
        ).apply(it) { key, message ->
            future {
                val npc = npcMap[key] ?: error("not found npc $key")
                submitChain {
                    async {
                        OpenAI.npcChat(bukkitPlayer().name, npc.name, npc.system, message, npc.model, npc.maxTokens, npc.temperature)
                    }
                }
            }
        }
    }

}