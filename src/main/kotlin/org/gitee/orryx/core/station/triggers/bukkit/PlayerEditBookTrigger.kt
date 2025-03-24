package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerEditBookEvent
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerEditBookTrigger: AbstractPlayerEventTrigger<PlayerEditBookEvent>() {

    override val event: String = "Player Edit Book"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "newTitle", "获得新书标题")
            .addParm(Type.STRING, "newAuthor", "获得新书作者")
            .addParm(Type.ITERABLE, "newPages", "获得新书的内容")
            .addParm(Type.STRING, "newPageCount", "获得新书页数")
            .addParm(Type.STRING, "newGeneration", "获得新书的代次：COPY_OF_COPY/COPY_OF_ORIGINAL/ORIGINAL/TATTERED")
            .addParm(Type.STRING, "title", "获取老书标题")
            .addParm(Type.STRING, "author", "获取老书作者")
            .addParm(Type.ITERABLE, "pages", "获取老书的内容")
            .addParm(Type.STRING, "pageCount", "获取老书页数")
            .addParm(Type.STRING, "generation", "获取老书的代次：COPY_OF_COPY/COPY_OF_ORIGINAL/ORIGINAL/TATTERED")
            .addParm(Type.BOOLEAN, "isSigning", "检测书本是否正在被签名")
            .description("当玩家编辑或签名书与笔时触发。如果事件中断取消，书与笔的元数据不会改变。")

    override val clazz
        get() = PlayerEditBookEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerEditBookEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["newTitle"] = event.newBookMeta.title
        context["newAuthor"] = event.newBookMeta.author
        context["newPages"] = event.newBookMeta.pages
        context["newPageCount"] = event.newBookMeta.pageCount
        context["newGeneration"] = event.newBookMeta.generation?.name
        context["title"] = event.previousBookMeta.title
        context["author"] = event.previousBookMeta.author
        context["pages"] = event.previousBookMeta.pages
        context["pageCount"] = event.previousBookMeta.pageCount
        context["generation"] = event.previousBookMeta.generation?.name
        context["isSigning"] = event.isSigning
    }

}