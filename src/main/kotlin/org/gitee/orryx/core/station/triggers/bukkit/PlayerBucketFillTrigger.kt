package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBucketFillEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerBucketFillTrigger: AbstractPlayerEventTrigger<PlayerBucketFillEvent>() {

    override val event: String = "Player Bucket Fill"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "blockClicked", "点击的方块位置")
            .addParm(Type.TARGET, "block", "水或者岩浆的位置")
            .addParm(Type.STRING, "bucket", "返回玩家手里的桶的类型")
            .description("桶装满时触发")

    override val clazz
        get() = PlayerBucketFillEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerBucketFillEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["blockClicked"] = LocationTarget(event.blockClicked.location)
        context["block"] = LocationTarget(event.block.location)
        context["bucket"] = event.bucket.name
    }
}