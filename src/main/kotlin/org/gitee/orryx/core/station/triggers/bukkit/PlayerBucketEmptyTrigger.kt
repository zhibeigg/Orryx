package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult

object PlayerBucketEmptyTrigger: AbstractPropertyPlayerEventTrigger<PlayerBucketEmptyEvent>("Player Bucket Empty") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "blockClicked", "点击的方块位置")
            .addParm(Type.TARGET, "block", "水或者岩浆的位置")
            .addParm(Type.STRING, "bucket", "返回玩家手里的桶的类型")
            .description("玩家用完一只桶后触发")

    override val clazz
        get() = PlayerBucketEmptyEvent::class.java

    override fun read(instance: PlayerBucketEmptyEvent, key: String): OpenResult {
        return when(key) {
            "blockClicked" -> OpenResult.successful(instance.blockClicked.location.toTarget())
            "block" -> OpenResult.successful(instance.block.location.toTarget())
            "bucket" -> OpenResult.successful(instance.bucket.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerBucketEmptyEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}