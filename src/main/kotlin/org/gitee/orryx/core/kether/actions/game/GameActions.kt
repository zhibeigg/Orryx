package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser

object GameActions {

    @KetherParser(["sprint"], namespace = NAMESPACE, shared = true)
    fun actionSprint() = combinationParser(
        Action.new("sprint", true)
            .description("设置跑步状态")
            .addEntry("是否跑步", Type.BOOLEAN, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            bool(),
            theyContainer(true)
        ).apply(it) { isSprint, container ->
            now {
                container.orElse(self()).forEachInstance<PlayerTarget> { player ->
                    player.player.isSprinting = isSprint
                }
            }
        }
    }

    @KetherParser(["launch"], namespace = NAMESPACE, shared = true)
    fun actionLaunch() = combinationParser(
        Action.new("launch", true)
            .description("设置冲量")
            .addEntry("视角前方冲量大小", Type.DOUBLE, false)
            .addEntry("视角上方冲量大小", Type.DOUBLE, false)
            .addEntry("视角右方冲量大小", Type.DOUBLE, false)
            .addEntry("是否叠加原冲量", Type.BOOLEAN, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            double(),
            double(),
            double(),
            bool(),
            theyContainer(true)
        ).apply(it) { x, y, z, additional, container ->
            now {
                container.orElse(self()).forEachInstance<AbstractBukkitEntity> { entity ->
                    if (additional) {
                        entity.velocity = entity.direction(x, y, z).add(entity.velocity)
                    } else {
                        entity.velocity = entity.direction(x, y, z)
                    }
                }.forEachInstance<PlayerTarget> { player ->
                    if (additional) {
                        player.player.velocity = player.direction(x, y, z).add(player.player.velocity)
                    } else {
                        player.player.velocity = player.direction(x, y, z)
                    }
                }.apply {
                    if (AdyeshachEnabled) {
                        forEachInstance<AbstractAdyeshachEntity> { entity ->
                            if (additional) {
                                entity.velocity = entity.direction(x, y, z).add(entity.velocity)
                            } else {
                                entity.velocity = entity.direction(x, y, z)
                            }
                        }
                    }
                }
            }
        }
    }


}