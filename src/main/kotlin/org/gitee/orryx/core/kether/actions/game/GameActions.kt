package org.gitee.orryx.core.kether.actions.game

import org.bukkit.entity.Entity
import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object GameActions {

    @KetherParser(["sprint"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSprint() = combinationParser(
        Action.new("Game原版游戏", "设置跑步状态", "sprint", true)
            .description("设置跑步状态")
            .addEntry("是否跑步", Type.BOOLEAN, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            bool(),
            theyContainer(true)
        ).apply(it) { isSprint, container ->
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        player.getSource().isSprinting = isSprint
                    }
                }
            }
        }
    }

    @KetherParser(["specialTarget"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionSpecialTarget() = combinationParser(
        Action.new("Game原版游戏", "设置旁观者模式下的附着视角", "specialTarget", true)
            .description("设置旁观者模式下的附着视角")
            .addEntry("目标实体", Type.CONTAINER)
            .addContainerEntry("玩家", optional = true, default = "@self")
    ) {
        it.group(
            container(),
            theyContainer(false)
        ).apply(it) { target, container ->
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                        target?.firstInstanceOrNull<ITargetEntity<Entity>>()?.getSource()
                            ?.let { entity -> player.getSource().spectatorTarget = entity }
                    }
                }
            }
        }
    }

}