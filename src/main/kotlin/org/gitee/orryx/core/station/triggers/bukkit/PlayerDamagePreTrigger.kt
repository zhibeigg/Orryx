package org.gitee.orryx.core.station.triggers.bukkit

import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.deepVars
import taboolib.module.kether.extend

object PlayerDamagePreTrigger: AbstractEventTrigger<OrryxDamageEvents.Pre>() {

    override val event: String = "Player Damage Pre"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.DOUBLE, "damage", "伤害")
            .addParm(Type.TARGET, "defender", "防御者")
            .addParm(Type.STRING, "type", "攻击类型：PHYSICS/MAGIC/FIRE/REAL/SELF/CONSOLE/CUSTOM")
            .description("当玩家攻击时发生，如果攻击来自于Or技能，那将会继承技能环境中的参数")

    override val clazz
        get() = OrryxDamageEvents.Pre::class.java

    override fun onCheck(station: IStation, event: OrryxDamageEvents.Pre, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.attackPlayer() != null
    }

    override fun onJoin(event: OrryxDamageEvents.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.attackPlayer()!!)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxDamageEvents.Pre, map: Map<String, Any?>): Boolean {
        return (pipeTask.scriptContext?.sender?.origin == event.attackPlayer())
    }

    override fun onStart(context: ScriptContext, event: OrryxDamageEvents.Pre, map: Map<String, Any?>) {
        event.context?.let { context.extend(it.rootFrame().deepVars()) }
        super.onStart(context, event, map)
        context["damage"] = event.damage
        context["defender"] = event.defender.abstract()
        context["type"] = event.type.name
    }
}