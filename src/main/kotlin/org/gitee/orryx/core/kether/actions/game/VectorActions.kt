package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.module.kether.KetherParser
import taboolib.module.kether.script

object VectorActions {

    @KetherParser(["launch"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionLaunch() = combinationParser(
        Action.new("Game原版游戏", "给予视角冲量", "launch", true)
            .description("给予视角冲量")
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
                container.orElse(self()).forEachInstance<ITargetEntity<*>> { entity ->
                    if (additional) {
                        entity.entity.velocity = entity.direction(x, y, z).add(entity.entity.velocity)
                    } else {
                        entity.entity.velocity = entity.direction(x, y, z)
                    }
                }
            }
        }
    }

    @KetherParser(["velocity"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionVelocity() = combinationParser(
        Action.new("Game原版游戏", "改变目标速度", "velocity", true)
            .description("改变目标速度")
            .addEntry("矢量", Type.VECTOR, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            vector(),
            theyContainer(true)
        ).apply(it) { vector, container ->
            now {
                val bukkit = vector?.bukkit() ?: return@now null
                container.readContainer(script()).orElse(self()).forEachInstance<ITargetEntity<*>> { target ->
                    target.entity.velocity = bukkit
                }
            }
        }
    }

}