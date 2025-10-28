package org.gitee.orryx.utils.raytrace

import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.gitee.orryx.utils.bukkit
import org.gitee.orryx.utils.joml
import org.gitee.orryx.utils.raytrace.FluidHandling.*
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.module.navigation.toBlock
import taboolib.module.nms.MinecraftVersion
import java.lang.reflect.Method

class SpigotWorld(private val world: World) : PlatformWorld {

    override fun rayTraceBlocks(
        start: Vector3dc,
        direction: Vector3dc,
        maxDistance: Double,
        fluidHandling: FluidHandling,
        checkAxisAlignedBB: Boolean,
        returnClosestPos: Boolean
    ): RayTraceResult? {
        if (MinecraftVersion.isHigher(MinecraftVersion.V1_12)) {
            val startVec = start.bukkit()
            val directionVec = direction.bukkit()

            val fluidMode = when(fluidHandling) {
                NONE -> FluidCollisionMode.NEVER
                SOURCE_ONLY -> FluidCollisionMode.SOURCE_ONLY
                ALWAYS -> FluidCollisionMode.ALWAYS
            }

            val result: org.bukkit.util.RayTraceResult = world.rayTraceBlocks(
                startVec.toLocation(world),
                directionVec,
                maxDistance,
                fluidMode,
                checkAxisAlignedBB
            ) ?: run {
                if (returnClosestPos) {
                    val vector = startVec.add(directionVec)
                    val block = vector.toBlock(world)
                    return RayTraceResult(
                        vector.joml(),
                        null,
                        block.let { Vector3i(it.x, it.y, it.z) },
                        block.blockData,
                        RayTraceResult.EnumMovingObjectType.MISS
                    )
                } else {
                    return null
                }
            }

            return RayTraceResult(
                Vector3d(result.hitPosition.x, result.hitPosition.y, result.hitPosition.z),
                result.hitBlockFace,
                Vector3i(result.hitBlock!!.x, result.hitBlock!!.y, result.hitBlock!!.z),
                if (result.hitBlock != null) { result.hitBlock!!.blockData } else null,
                if (result.hitBlock != null) {
                    RayTraceResult.EnumMovingObjectType.BLOCK
                } else if (result.hitEntity != null) {
                    RayTraceResult.EnumMovingObjectType.ENTITY
                } else {
                    RayTraceResult.EnumMovingObjectType.MISS
                }
            )
        } else {
            try {
                val startPos = vec3DClass.getConstructor(
                    Double::class.javaPrimitiveType,
                    Double::class.javaPrimitiveType,
                    Double::class.javaPrimitiveType
                ).newInstance(start.x(), start.y(), start.z())

                val endPos: Any = vec3DAddMethod.invoke(startPos, direction.x(), direction.y(), direction.z())
                val hitResult: Any = rayTraceMethod.invoke(craftWorldGetHandleMethod.invoke(world), startPos, endPos, fluidHandling == SOURCE_ONLY, checkAxisAlignedBB, returnClosestPos) ?: run {
                    if (returnClosestPos) {
                        val vector = start.add(direction, Vector3d()).bukkit()
                        val block = vector.toBlock(world)
                        return RayTraceResult(
                            vector.joml(),
                            null,
                            block.let { Vector3i(it.x, it.y, it.z) },
                            null,
                            RayTraceResult.EnumMovingObjectType.MISS
                        )
                    } else {
                        return null
                    }
                }

                val hitPosition = Vector3d(
                    hitResult.getProperty<Double>("pos/x")!!,
                    hitResult.getProperty<Double>("pos/y")!!,
                    hitResult.getProperty<Double>("pos/z")!!
                )

                val hitDirection = hitResult.javaClass.getField("direction")[hitResult]

                return RayTraceResult(
                    hitPosition,
                    getHitBlockFace(hitDirection),
                    null,
                    null,
                    RayTraceResult.EnumMovingObjectType.valueOf(hitResult.getProperty<String>("type/name")!!)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }

    companion object {

        private lateinit var craftWorldClass: Class<*>
        private lateinit var vec3DClass: Class<*>
        private lateinit var movingObjectPositionClass: Class<*>
        private lateinit var enumDirectionClass: Class<*>
        private lateinit var craftWorldGetHandleMethod: Method
        private lateinit var rayTraceMethod: Method
        private lateinit var vec3DAddMethod: Method

        init {
            if (MinecraftVersion.isLower(MinecraftVersion.V1_13)) {
                try {
                    val server = Bukkit.getServer().javaClass.getDeclaredMethod("getServer").invoke(Bukkit.getServer())
                    val nmsPackage = server.javaClass.getPackage().name
                    val bukkitPackage = Bukkit.getServer().javaClass.getPackage().name

                    craftWorldClass = Class.forName("$bukkitPackage.CraftWorld")
                    vec3DClass = Class.forName("$nmsPackage.Vec3D")
                    movingObjectPositionClass = Class.forName("$nmsPackage.MovingObjectPosition")
                    enumDirectionClass = Class.forName("$nmsPackage.EnumDirection")

                    craftWorldGetHandleMethod = craftWorldClass.getMethod("getHandle")
                    rayTraceMethod = Class.forName("$nmsPackage.World").getMethod(
                        "rayTrace", vec3DClass, vec3DClass,
                        Boolean::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType
                    )
                    vec3DAddMethod = vec3DClass.getMethod(
                        "add",
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType,
                        Double::class.javaPrimitiveType
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun getHitBlockFace(enumDirection: Any): BlockFace {
            try {
                when (enumDirection) {
                    enumDirectionClass.getField("NORTH")[null] -> return BlockFace.NORTH
                    enumDirectionClass.getField("SOUTH")[null] -> return BlockFace.SOUTH
                    enumDirectionClass.getField("EAST")[null] -> return BlockFace.EAST
                    enumDirectionClass.getField("WEST")[null] -> return BlockFace.WEST
                    enumDirectionClass.getField("UP")[null] -> return BlockFace.UP
                    enumDirectionClass.getField("DOWN")[null] -> return BlockFace.DOWN
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            throw IllegalStateException("Unexpected value: $enumDirection")
        }
    }
}