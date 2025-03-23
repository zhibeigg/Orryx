package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

object PlayerBucketEmptyTrigger: AbstractEventTrigger<PlayerBucketEmptyEvent>() {

    override val event: String = "Player Bucket Empty"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "blockClicked", "点击的方块位置")
            .addParm(Type.TARGET, "block", "水或者岩浆的位置")
            .addParm(Type.STRING, "bucket", "返回玩家手里的桶的类型")
            .description("玩家用完一只桶后触发")

    override val clazz
        get() = PlayerBucketEmptyEvent::class.java

    override fun onJoin(event: PlayerBucketEmptyEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerBucketEmptyEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun onStart(context: ScriptContext, event: PlayerBucketEmptyEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["blockClicked"] = LocationTarget(event.blockClicked.location)
        context["block"] = LocationTarget(event.block.location)
        context["bucket"] = event.bucket.name
    }

}