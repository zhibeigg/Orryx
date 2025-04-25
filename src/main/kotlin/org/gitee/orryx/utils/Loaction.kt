package org.gitee.orryx.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld
import org.bukkit.util.Vector
import org.joml.Vector3dc
import taboolib.common5.Coerce
import taboolib.module.nms.MinecraftVersion

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
            val pos = net.minecraft.server.v1_12_R1.BlockPosition(block.x, block.y, block.z)
            val box = (block.chunk as org.bukkit.craftbukkit.v1_12_R1.CraftChunk).handle.getBlockData(pos).d((block.world as CraftWorld).handle, pos)
            box != null
        }
    }
    var deep = 0
    while (!isPassable(origin.block) && deep <= distance) {
        origin.add(0.0, -1.0, 0.0)
        deep++
    }
    return origin.add(0.0, 1.0, 0.0) to deep - 1
}