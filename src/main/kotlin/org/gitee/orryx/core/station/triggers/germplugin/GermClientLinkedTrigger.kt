package org.gitee.orryx.core.station.triggers.germplugin

import com.germ.germplugin.api.event.GermClientLinkedEvent
import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

@Plugin("GermPlugin")
object GermClientLinkedTrigger: AbstractPropertyEventTrigger<GermClientLinkedEvent>("Germ Client Linked") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.GERM_PLUGIN, event)
            .addParm(Type.STRING, "ip", "ip")
            .addParm(Type.STRING, "machineCode", "机器代码")
            .addParm(Type.STRING, "modVersion", "萌芽mod版本")
            .description("玩家进服后萌芽加载完毕")

    override val clazz
        get() = GermClientLinkedEvent::class.java

    override fun onJoin(event: GermClientLinkedEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: GermClientLinkedEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: GermClientLinkedEvent, key: String): OpenResult {
        return when(key) {
            "ip" -> OpenResult.successful(instance.ip)
            "machineCode" -> OpenResult.successful(instance.machineCode)
            "modVersion" -> OpenResult.successful(instance.modVersion)
            "qq" -> OpenResult.successful(instance.qq)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: GermClientLinkedEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}