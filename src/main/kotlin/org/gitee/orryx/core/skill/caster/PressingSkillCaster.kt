package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressStartEvent
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressStopEvent
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressTickEvent
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.PressSkillManager
import org.gitee.orryx.core.skill.skills.PressingSkill
import org.gitee.orryx.core.station.pipe.PipeBuilder
import org.gitee.orryx.utils.consume
import org.gitee.orryx.utils.paired
import org.gitee.orryx.utils.runCustomAction
import org.gitee.orryx.utils.runSkillAction
import taboolib.common5.clong
import taboolib.module.kether.orNull
import taboolib.platform.util.sendLang
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 蓄力技能释放器。
 *
 * 处理 [PressingSkill] 类型的技能释放，支持蓄力时间和打断机制。
 */
object PressingSkillCaster : ISkillCaster {

    override fun supports(skill: ISkill): Boolean {
        return skill is PressingSkill
    }

    override fun cast(skill: ISkill, player: Player, parameter: SkillParameter, consume: Boolean) {
        skill as PressingSkill
        if (PressSkillManager.pressTaskMap.containsKey(player.uniqueId)) return

        val maxPressTick = parameter.runCustomAction(skill.maxPressTickAction).orNull().clong
        val time = System.currentTimeMillis()

        PressSkillManager.pressTaskMap[player.uniqueId] = skill.key paired PipeBuilder()
            .uuid(UUID.randomUUID())
            .timeout(maxPressTick)
            .brokeTriggers(*skill.pressBrockTriggers)
            .periodTask(skill.period) {
                parameter.runCustomAction(
                    skill.pressPeriodAction,
                    mapOf(Pair("pressTick", (System.currentTimeMillis() - time) / 50))
                )
                OrryxPlayerPressTickEvent(
                    player, skill, skill.period,
                    (System.currentTimeMillis() - time) / 50, maxPressTick
                ).call()
            }.onComplete {
                if (consume) skill.consume(player, parameter)
                val tick = (System.currentTimeMillis() - time) / 50
                parameter.runSkillAction(mapOf(Pair("pressTick", tick)))
                PressSkillManager.pressTaskMap.remove(player.uniqueId)
                OrryxPlayerPressStopEvent(player, skill, tick, maxPressTick).call()
                CompletableFuture.completedFuture(null)
            }.onBrock {
                player.sendLang("pressing-broke", skill.name)
                PressSkillManager.pressTaskMap.remove(player.uniqueId)
                OrryxPlayerPressStopEvent(
                    player, skill,
                    (System.currentTimeMillis() - time) / 50, maxPressTick
                ).call()
                CompletableFuture.completedFuture(null)
            }.build()

        OrryxPlayerPressStartEvent(player, skill, maxPressTick).call()
    }
}
