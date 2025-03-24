package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerAdvancementDoneTrigger: AbstractPlayerEventTrigger<PlayerAdvancementDoneEvent>() {

    override val event: String = "Player Advancement Done"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "advancementKey", "成就的键名")
            .addParm(Type.STRING, "advancementNamespace", "成就的命名空间")
            .description("玩家成就完成")

    override val clazz
        get() = PlayerAdvancementDoneEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerAdvancementDoneEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["advancementKey"] = event.advancement.key.key
        context["advancementNamespace"] = event.advancement.key.namespace
    }

}