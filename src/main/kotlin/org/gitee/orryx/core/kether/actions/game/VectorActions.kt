package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import org.gitee.orryx.utils.raytrace.FluidHandling
import org.gitee.orryx.utils.raytrace.RayTraceResult
import org.joml.Vector3d
import taboolib.module.kether.KetherParser
import taboolib.module.kether.script

object VectorActions {

    @KetherParser(["lookAt"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionLookAt() = combinationParser(
        Action.new("Game原版游戏", "让实体看向某处", "lookAt", true)
            .description("让实体看向某处")
            .addEntry("目标处的世界原点向量", Type.VECTOR, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            vector(),
            theyContainer(true)
        ).apply(it) { vector, container ->
            now {
                ensureSync {
                    container.orElse(self()).forEachInstance<ITargetEntity<*>> { entity ->
                        entity.entity.teleport(
                            entity.entity.location.setDirection(vector.sub(entity.entity.eyeLocation.joml(), Vector3d()).bukkit())
                        )
                    }
                }
            }
        }
    }

    @KetherParser(["flash"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionFlash() = combinationParser(
        Action.new("Game原版游戏", "给予视角瞬移量", "flash", true)
            .description("给予视角瞬移量")
            .addEntry("视角前方移动距离", Type.DOUBLE, false)
            .addEntry("视角上方移动距离", Type.DOUBLE, false)
            .addEntry("视角右方移动距离", Type.DOUBLE, false)
            .addEntry("视角向量", Type.DOUBLE, true, "保留")
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            double(),
            double(),
            double(),
            vector().option(),
            theyContainer(true)
        ).apply(it) { x, y, z, direction, container ->
            now {
                ensureSync {
                    container.orElse(self()).forEachInstance<ITargetEntity<*>> { entity ->
                        val dir = entity.direction(x, y, z, true)

                        val result = entity.entity.world.rayTraceBlocks(
                            entity.entity.eyeLocation.joml(),
                            dir.joml(),
                            1.0,
                            FluidHandling.NONE,
                            checkAxisAlignedBB = true,
                            returnClosestPos = true
                        )
                        result?.hitPosition?.toLocation(entity.entity.world)?.let { loc ->
                            entity.entity.teleport(loc.setDirection(direction?.bukkit() ?: entity.entity.location.direction).apply {
                                if (result.type == RayTraceResult.EnumMovingObjectType.MISS) {
                                    add(0.0, -1.0, 0.0)
                                }
                            })
                        }
                    }
                }
            }
        }
    }

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
                ensureSync {
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
                ensureSync {
                    container.readContainer(script()).orElse(self()).forEachInstance<ITargetEntity<*>> { target ->
                        target.entity.velocity = bukkit
                    }
                }
            }
        }
    }

}