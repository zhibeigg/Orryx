package org.gitee.orryx.utils.raytrace

import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.joml.Vector3d
import org.joml.Vector3i

class RayTraceResult(
    val hitPosition: Vector3d,
    val blockFace: BlockFace?,
    val hitBlockPosition: Vector3i?,
    val hitBlock: BlockData?,
    val type: EnumMovingObjectType
) {
    enum class EnumMovingObjectType {
        MISS, BLOCK, ENTITY;
    }
}