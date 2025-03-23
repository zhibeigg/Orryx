package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBucketEntityEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

object PlayerBucketEntityTrigger: AbstractEventTrigger<PlayerBucketEntityEvent>() {

    override val event: String = "Player Bucket Entity"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "originalBucket", "捕获前的桶")
            .addParm(Type.ITEM_STACK, "entityBucket", "捕获后的桶")
            .addParm(Type.TARGET, "entity", "获取将要放入桶中的实体")
            .description("玩家捕获存储桶中的实体时触发")

    override val clazz
        get() = PlayerBucketEntityEvent::class.java

    override fun onJoin(event: PlayerBucketEntityEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerBucketEntityEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun onStart(context: ScriptContext, event: PlayerBucketEntityEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["originalBucket"] = event.originalBucket
        context["entityBucket"] = event.entityBucket
        context["entity"] = AbstractBukkitEntity(event.entity)
    }

}