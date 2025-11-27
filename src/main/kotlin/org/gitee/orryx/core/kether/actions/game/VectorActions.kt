package org.gitee.orryx.core.kether.actions.game

import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import org.gitee.orryx.utils.raytrace.FluidHandling
import org.gitee.orryx.utils.raytrace.RayTraceResult
import org.joml.Matrix3d
import org.joml.Vector3d
import taboolib.module.kether.KetherParser
import taboolib.module.kether.script
import taboolib.module.navigation.set
import java.util.concurrent.CompletableFuture

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
            future {
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
            .addEntry("视角向量", Type.VECTOR, false)
            .addEntry("偏移yaw和pitch(os 90.0 90.0)", Type.DOUBLE, head = "offset/os")
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            double(),
            double(),
            double(),
            vector(),
            command("offset", "os", then = double().and(double())).option().defaultsTo(Pair(0.0, 0.0)),
            theyContainer(true)
        ).apply(it) { x, y, z, direction, offset, container ->
            future {
                val (yaw, pitch) = offset
                ensureSync {
                    container.orElse(self()).forEachInstance<ITargetEntity<*>> { entity ->
                        val dir = entity.direction(x, y, z, true).also { v ->
                            val joml = Vector3d(v.x, 0.0, v.z)
                            val z = joml.cross(0.0, 1.0, 0.0)
                            val y = z.cross(Vector3d(v.x, v.y, v.z), Vector3d())
                            val matrix = Matrix3d().rotate(Math.toRadians(yaw), y).rotate(Math.toRadians(pitch), z)
                            Vector3d(v.x, v.y, v.z).apply {
                                mul(matrix, this)
                                v.set(this.x, this.y, this.z)
                            }
                        }

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
                                if (result.type != RayTraceResult.EnumMovingObjectType.MISS) {
                                    add(dir.normalize().multiply(-0.1))
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
        Action.new("Game原版游戏", "给予视角冲量（不参考pitch）", "launch", true)
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
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<ITargetEntity<*>> { entity ->
                        if (additional) {
                            entity.entity.velocity = entity.direction(x, y, z, false).add(entity.entity.velocity)
                        } else {
                            entity.entity.velocity = entity.direction(x, y, z, false)
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["direct"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionDirect() = combinationParser(
        Action.new("Game原版游戏", "给予视角冲量（参考pitch）", "direct", true)
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
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<ITargetEntity<*>> { entity ->
                        if (additional) {
                            entity.entity.velocity = entity.direction(x, y, z, true).add(entity.entity.velocity)
                        } else {
                            entity.entity.velocity = entity.direction(x, y, z, true)
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["drag"], namespace = ORRYX_NAMESPACE, shared = false)
    private fun actionDrag() = combinationParser(
        Action.new("Game原版游戏", "向原点聚拢", "drag", false)
            .description("给予向原点聚拢的冲量")
            .addEntry("冲量大小系数", Type.DOUBLE, false)
            .addEntry("是否叠加原冲量", Type.BOOLEAN, false)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            double(),
            bool(),
            theyContainer(true)
        ).apply(it) { mul, additional, container ->
            future {
                val origin = script().getParameter().origin ?: return@future completedFuture(null)
                ensureSync {
                    container.orElse(self()).forEachInstance<ITargetEntity<*>> { entity ->
                        val vector = origin.location.toVector().subtract(entity.entity.location.toVector()).multiply(mul)
                        if (additional) {
                            entity.entity.velocity = vector.add(entity.entity.velocity)
                        } else {
                            entity.entity.velocity = vector
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
            future {
                val bukkit = vector?.bukkit() ?: return@future CompletableFuture.completedFuture(null)
                ensureSync {
                    container.orElse(self()).forEachInstance<ITargetEntity<*>> { target ->
                        target.entity.velocity = bukkit
                    }
                }
            }
        }
    }

    @KetherParser(["teleport"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionTeleport() = combinationParser(
        Action.new("Game原版游戏", "传送到指向向量点", "teleport", true)
            .description("传送到指向向量点")
            .addEntry("指向向量", Type.VECTOR, false)
            .addEntry("是否保留原朝向", Type.BOOLEAN)
            .addContainerEntry(optional = true, default = "@self")
    ) {
        it.group(
            vector(),
            bool(),
            theyContainer(true)
        ).apply(it) { vector, keepFace, container ->
            future {
                ensureSync {
                    container.orElse(self()).forEachInstance<ITargetEntity<*>> { entity ->
                        val result = entity.entity.world.rayTraceBlocks(
                            entity.entity.location.joml(),
                            vector.joml(),
                            1.0,
                            FluidHandling.NONE,
                            checkAxisAlignedBB = true,
                            returnClosestPos = true
                        )
                        result?.hitPosition?.toLocation(entity.entity.world)?.let { loc ->
                            if (keepFace) {
                                loc.direction = entity.entity.location.direction
                            }
                            entity.entity.teleport(loc)
                        }
                    }
                }
            }
        }
    }
}