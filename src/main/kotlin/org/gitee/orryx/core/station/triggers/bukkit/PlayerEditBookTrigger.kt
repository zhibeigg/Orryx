package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.inventory.meta.BookMeta
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common5.cint
import taboolib.module.kether.KetherLoader
import taboolib.module.kether.ScriptProperty
import taboolib.module.kether.isInt

object PlayerEditBookTrigger: AbstractPropertyPlayerEventTrigger<PlayerEditBookEvent>("Player Edit Book") {

    init {
        runCatching {
            KetherLoader.registerProperty(property(), BookMeta::class.java, false)
        }
    }

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ANY, "newBookMeta", "新书的数据")
            .addParm(Type.ANY, "previousBookMeta", "旧书的数据")
            .addParm(Type.BOOLEAN, "isSigning", "检测书本是否正在被签名")
            .description("当玩家编辑或签名书与笔时触发。如果事件中断取消，书与笔的元数据不会改变。")

    override val clazz
        get() = PlayerEditBookEvent::class.java

    override fun read(instance: PlayerEditBookEvent, key: String): OpenResult {
        return when(key) {
            "newBookMeta" -> OpenResult.successful(instance.newBookMeta)
            "previousBookMeta" -> OpenResult.successful(instance.previousBookMeta)
            "isSigning" -> OpenResult.successful(instance.isSigning)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerEditBookEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }

    private fun property() = object : ScriptProperty<BookMeta>("orryx.bookmeta.operator") {

        override fun read(instance: BookMeta, key: String): OpenResult {
            return when(key) {
                "title" -> OpenResult.successful(instance.title)
                "lore" -> OpenResult.successful(instance.lore)
                "author" -> OpenResult.successful(instance.author)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: BookMeta, key: String, value: Any?): OpenResult {
            return when {
                key.isInt() -> {
                    instance.setPage(key.cint, value.toString())
                    OpenResult.successful()
                }
                key == "title" -> {
                    instance.title = value.toString()
                    OpenResult.successful()
                }
                key == "author" -> {
                    instance.author = value.toString()
                    OpenResult.successful()
                }
                else -> OpenResult.failed()
            }
        }
    }
}