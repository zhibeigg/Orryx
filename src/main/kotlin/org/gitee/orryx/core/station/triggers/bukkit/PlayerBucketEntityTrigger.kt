package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketEntityEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.module.kether.ScriptContext

object PlayerBucketEntityTrigger: AbstractPropertyPlayerEventTrigger<PlayerBucketEntityEvent>("Player Bucket Entity") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "originalBucket", "捕获前的桶")
            .addParm(Type.ITEM_STACK, "entityBucket", "捕获后的桶")
            .addParm(Type.TARGET, "entity", "获取将要放入桶中的实体")
            .description("玩家捕获存储桶中的实体时触发")

    override val clazz
        get() = PlayerBucketEntityEvent::class.java

    override fun read(instance: PlayerBucketEntityEvent, key: String): OpenResult {
        return when(key) {
            "originalBucket" -> OpenResult.successful(instance.originalBucket)
            "entityBucket" -> OpenResult.successful(instance.entityBucket)
            "entity" -> OpenResult.successful(AbstractBukkitEntity(instance.entity))
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerBucketEntityEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}