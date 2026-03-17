package org.gitee.orryx.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.util.Vector
import org.joml.Vector3dc
import taboolib.common5.Coerce
import taboolib.module.nms.MinecraftVersion
import java.lang.reflect.Method

internal fun Any.toLocation(world: World? = null): Location {
    return when (this) {
        is Location -> this.also { it.world = world }
        is String -> {
            val split = split(",")
            Location(
                world ?: Bukkit.getWorld(split[0]),
                Coerce.toDouble(split[1]),
                Coerce.toDouble(split[2]),
                Coerce.toDouble(split[3])
            )
        }
        is Vector3dc -> Location(world, x(), y(), z())
        is Vector -> Location(world, x, y, z)
        else -> Location(world, 0.0, 0.0, 0.0)
    }
}

fun floor(origin: Location, distance: Double): Pair<Location, Int> {
    origin.y = kotlin.math.floor(origin.y)
    fun isPassable(block: Block): Boolean {
        return if (MinecraftVersion.isHigher(MinecraftVersion.V1_12)) {
            block.isPassable
        } else {
            try {
                val blockPositionClass = LegacyNmsReflection.blockPositionClass ?: return true
                val pos = blockPositionClass.getConstructor(
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                ).newInstance(block.x, block.y, block.z)

                val craftChunkClass = LegacyNmsReflection.craftChunkClass ?: return true
                val nmsChunk = craftChunkClass.getMethod("getHandle").invoke(block.chunk)
                val blockData = nmsChunk.javaClass.getMethod("getBlockData", blockPositionClass).invoke(nmsChunk, pos)

                val craftWorldClass = LegacyNmsReflection.craftWorldClass ?: return true
                val nmsWorld = craftWorldClass.getMethod("getHandle").invoke(block.world)

                val dMethod = LegacyNmsReflection.getBlockDataBoundingBoxMethod(blockData.javaClass, blockPositionClass)
                val box = dMethod?.invoke(blockData, nmsWorld, pos)
                box != null
            } catch (e: Exception) {
                e.printStackTrace()
                true
            }
        }
    }
    var deep = 0
    while (!isPassable(origin.block) && deep <= distance) {
        origin.add(0.0, -1.0, 0.0)
        deep++
    }
    return origin.add(0.0, 1.0, 0.0) to deep - 1
}

/**
 * 1.12 NMS 反射缓存，避免硬编码 CraftBukkit 版本化 import
 */
private object LegacyNmsReflection {

    val craftWorldClass: Class<*>?
    val craftChunkClass: Class<*>?
    val blockPositionClass: Class<*>?

    init {
        if (MinecraftVersion.isLower(MinecraftVersion.V1_13)) {
            val bukkitPackage = Bukkit.getServer().javaClass.getPackage().name
            val server = Bukkit.getServer().javaClass.getDeclaredMethod("getServer").invoke(Bukkit.getServer())
            val nmsPackage = server.javaClass.getPackage().name

            craftWorldClass = runCatching { Class.forName("$bukkitPackage.CraftWorld") }.getOrNull()
            craftChunkClass = runCatching { Class.forName("$bukkitPackage.CraftChunk") }.getOrNull()
            blockPositionClass = runCatching { Class.forName("$nmsPackage.BlockPosition") }.getOrNull()
        } else {
            craftWorldClass = null
            craftChunkClass = null
            blockPositionClass = null
        }
    }

    fun getBlockDataBoundingBoxMethod(blockDataClass: Class<*>, blockPositionClass: Class<*>): Method? {
        return try {
            // IBlockData.d(IBlockAccess, BlockPosition) 在 1.12 中返回碰撞箱
            blockDataClass.getMethod("d", Class.forName(blockDataClass.getPackage().name + ".IBlockAccess"), blockPositionClass)
        } catch (e: Exception) {
            // 尝试其他方法名
            try {
                blockDataClass.getMethod("c", Class.forName(blockDataClass.getPackage().name + ".IBlockAccess"), blockPositionClass)
            } catch (_: Exception) {
                null
            }
        }
    }
}
