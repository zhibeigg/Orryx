package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.advancement.Advancement
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.module.kether.KetherLoader
import taboolib.module.kether.ScriptProperty

object PlayerAdvancementDoneTrigger: AbstractPropertyPlayerEventTrigger<PlayerAdvancementDoneEvent>("Player Advancement Done") {

    init {
        runCatching {
            KetherLoader.registerProperty(property(), Advancement::class.java, false)
        }
    }

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "advancement", "成就")
            .description("玩家成就完成")

    override val clazz: java
        get() = PlayerAdvancementDoneEvent::class.java

    override fun read(instance: PlayerAdvancementDoneEvent, key: String): OpenResult {
        return when(key) {
            "advancement" -> OpenResult.successful(instance.advancement)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerAdvancementDoneEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }

    private fun property() = object : ScriptProperty<Advancement>("orryx.advancement.operator") {

        override fun read(instance: Advancement, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key.key)
                "namespace" -> OpenResult.successful(instance.key.namespace)
                "icon" -> OpenResult.successful(instance.display?.icon)
                "type" -> OpenResult.successful(instance.display?.type?.name)
                "isHidden" -> OpenResult.successful(instance.display?.isHidden)
                "description" -> OpenResult.successful(instance.display?.description)
                "x" -> OpenResult.successful(instance.display?.x)
                "y" -> OpenResult.successful(instance.display?.y)
                "title" -> OpenResult.successful(instance.display?.title)
                "shouldShowToast" -> OpenResult.successful(instance.display?.shouldShowToast())
                "shouldAnnounceChat" -> OpenResult.successful(instance.display?.shouldAnnounceChat())
                "criteria" -> OpenResult.successful(instance.criteria)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: Advancement, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }
}