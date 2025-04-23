package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.skill.PressSkillManager
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.QuestReader
import taboolib.module.kether.KetherParser
import taboolib.module.kether.ScriptAction
import taboolib.module.kether.actionFuture
import taboolib.module.kether.switch

object PressSkillActions {

    @KetherParser(["press"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionPress() = scriptParser(
        Action.new("PressSkill蓄力技能", "获取玩家正在蓄力的技能", "press", true)
            .description("获取玩家正在蓄力的技能")
            .addEntry("获取占位符", Type.SYMBOL, false, head = "get")
            .addContainerEntry("目标", true, "@self")
            .result("玩家正在蓄力的技能", Type.STRING)
    ) {
        it.switch {
            case("get") { getPressSkill(it) }
        }
    }

    fun getPressSkill(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()
        return actionFuture { future ->
            container(they, self()) {
                val player = it.firstInstanceOrNull<PlayerTarget>()
                if (player != null) {
                    future.complete(PressSkillManager.pressTaskMap[player.uniqueId]?.first)
                } else {
                    future.complete(null)
                }
            }
        }
    }
}