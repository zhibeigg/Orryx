package org.gitee.orryx.core.kether.actions.compat

import me.goudan.gddtitle.api.GDDTitleAPI
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.combinationParser
import org.gitee.orryx.utils.forEachInstance
import org.gitee.orryx.utils.orElse
import org.gitee.orryx.utils.self
import org.gitee.orryx.utils.theyContainer
import taboolib.module.kether.KetherParser
import taboolib.module.kether.player
import kotlin.Triple
import kotlin.arrayOf

object GddTitleActions {

    @KetherParser(["gddtitle"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun gddTitle() = combinationParser(
        Action.new("GddTitle", "发送龙核Hud Title", "gddtitle", true)
            .description("发送龙核Hud Title")
            .addEntry("title（@sender和@player会替换）", Type.STRING)
            .addEntry("淡入 停留 淡出", Type.INT, true, "0 20 0", "by/with")
            .addContainerEntry("目标玩家", true, "@self")
    ) {
        it.group(
            text(),
            command("by", "with", then = int().and(int(), int())).option().defaultsTo(Triple(0, 20, 0)),
            theyContainer(true)
        ).apply(it) { title, time, they ->
            val (i, s, o) = time
            now {
                val sender = player().name
                they.orElse(self()).forEachInstance<PlayerTarget> { player ->
                    GDDTitleAPI.sendTitle(player.getSource(), title.replace("@sender", sender).replace("@player", player.name), i*50, s*50, o*50)
                }
            }
        }
    }

    @KetherParser(["gddaction"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun gddAction() = combinationParser(
        Action.new("GddTitle", "发送龙核Hud Action", "gddaction", true)
            .description("发送龙核Hud Action")
            .addEntry("action（@sender和@player会替换）", Type.STRING)
            .addEntry("淡入 停留 淡出", Type.INT, true, "0 20 0", "by/with")
            .addContainerEntry("目标玩家", true, "@self")
    ) {
        it.group(
            text(),
            command("by", "with", then = int().and(int(), int())).option().defaultsTo(Triple(0, 20, 0)),
            theyContainer(true)
        ).apply(it) { action, time, they ->
            val (i, s, o) = time
            now {
                val sender = player().name
                they.orElse(self()).forEachInstance<PlayerTarget> { player ->
                    GDDTitleAPI.sendAction(player.getSource(), action.replace("@sender", sender).replace("@player", player.name), i*50, s*50, o*50)
                }
            }
        }
    }
}