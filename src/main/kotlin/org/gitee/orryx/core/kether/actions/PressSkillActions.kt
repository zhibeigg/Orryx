package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.scriptParser
import taboolib.module.kether.KetherParser
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

        }
    }
}