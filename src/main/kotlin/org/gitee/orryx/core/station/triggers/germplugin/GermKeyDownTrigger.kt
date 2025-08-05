package org.gitee.orryx.core.station.triggers.germplugin

import com.germ.germplugin.api.KeyType
import com.germ.germplugin.api.bean.KeyBinding
import com.germ.germplugin.api.event.GermKeyDownEvent
import org.bukkit.inventory.meta.BookMeta
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.core.station.triggers.bukkit.PlayerEditBookTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cint
import taboolib.module.kether.KetherLoader
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty

@Plugin("GermPlugin")
object GermKeyDownTrigger: AbstractPropertyEventTrigger<GermKeyDownEvent>("Germ Key Down") {

    init {
        runCatching {
            KetherLoader.registerProperty(property(), KeyBinding::class.java, false)
        }
    }

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.GERM_PLUGIN, event)
            .addParm(Type.STRING, "key", "按下的按键")
            .addSpecialKey(Type.STRING, "Keys", "按键，可写列表/单个")
            .description("玩家按下按键事件")

    override val clazz
        get() = GermKeyDownEvent::class.java

    override val specialKeys = arrayOf("keys")

    override fun onJoin(event: GermKeyDownEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(station: IStation, event: GermKeyDownEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && ((map["Keys"] as? List<*>)?.contains(event.keyType.simpleKey) ?: (map["keys"] == event.keyType.simpleKey))
    }

    override fun onCheck(pipeTask: IPipeTask, event: GermKeyDownEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["Keys"] as? List<*>)?.contains(event.keyType.simpleKey) ?: (map["keys"] == event.keyType.simpleKey))
    }

    override fun read(instance: GermKeyDownEvent, key: String): OpenResult {
        return when(key) {
            "key" -> OpenResult.successful(instance.keyType.simpleKey)
            "keyBinding" -> OpenResult.successful(instance.keyBinding)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: GermKeyDownEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }

    private fun property() = object : ScriptProperty<KeyBinding>("orryx.germ.keybinding.operator") {

        override fun read(instance: KeyBinding, key: String): OpenResult {
            return when(key) {
                "name" -> OpenResult.successful(instance.name)
                "index" -> OpenResult.successful(instance.index)
                "defaultKey" -> OpenResult.successful(instance.defaultKey)
                "category" -> OpenResult.successful(instance.category)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: KeyBinding, key: String, value: Any?): OpenResult {
            return when(key) {
                "name" -> {
                    instance.name = value.toString()
                    OpenResult.successful()
                }
                "index" -> {
                    instance.index = value.toString()
                    OpenResult.successful()
                }
                "defaultKey" -> {
                    instance.defaultKey = KeyType.getKeyTypeFromKeyId(value.cint)
                    OpenResult.successful()
                }
                "category" -> {
                    instance.category = value.toString()
                    OpenResult.successful()
                }
                else -> OpenResult.failed()
            }
        }
    }
}