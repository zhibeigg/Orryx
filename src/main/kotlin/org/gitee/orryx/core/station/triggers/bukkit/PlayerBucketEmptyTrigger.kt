package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerBucketEmptyTrigger: AbstractPlayerEventTrigger<PlayerBucketEmptyEvent>() {

    override val event: String = "Player Bucket Empty"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "blockClicked", "点击的方块位置")
            .addParm(Type.TARGET, "block", "水或者岩浆的位置")
            .addParm(Type.STRING, "bucket", "返回玩家手里的桶的类型")
            .description("玩家用完一只桶后触发")

    override val clazz
        get() = PlayerBucketEmptyEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerBucketEmptyEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["blockClicked"] = LocationTarget(event.blockClicked.location)
        context["block"] = LocationTarget(event.block.location)
        context["bucket"] = event.bucket.name
    }
}