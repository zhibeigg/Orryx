package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.IEntitySelector
import taboolib.common.platform.Ghost
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Ghost
class MythicMobsOrryxOBBTargeter(mlc: MythicLineConfig): IEntitySelector(mlc) {

    private val length = mlc.getPlaceholderDouble(arrayOf("length", "l"), 3.0)
    private val width = mlc.getPlaceholderDouble(arrayOf("width", "w"), 1.0)
    private val height = mlc.getPlaceholderDouble(arrayOf("height", "h"), 2.0)
    private val forward = mlc.getPlaceholderDouble(arrayOf("forward", "f"), 0.0)
    private val up = mlc.getPlaceholderDouble(arrayOf("up", "u"), 0.0)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity?> {
        val am = data.caster
        val targets = HashSet<AbstractEntity?>()
        val casterLoc = am.entity.bukkitEntity.location

        val halfLength = length.get(data) / 2.0
        val halfWidth = width.get(data) / 2.0
        val halfHeight = height.get(data) / 2.0
        val fwd = forward.get(data)
        val upOffset = up.get(data)

        val yaw = Math.toRadians(-casterLoc.yaw.toDouble())
        val sinYaw = sin(yaw)
        val cosYaw = cos(yaw)

        // OBB 中心 = 施法者位置 + 前方偏移 + 上方偏移
        val centerX = casterLoc.x + sinYaw * fwd
        val centerY = casterLoc.y + upOffset
        val centerZ = casterLoc.z + cosYaw * fwd

        for (p in MythicMobs.inst().entityManager.getPlayers(am.entity.world)) {
            if (p.world != am.entity.world) continue
            val loc = p.bukkitEntity.location
            val dx = loc.x - centerX
            val dy = loc.y - centerY
            val dz = loc.z - centerZ
            // 转换到局部坐标系（前方为 local Z，右方为 local X）
            val localX = dx * cosYaw - dz * sinYaw
            val localZ = dx * sinYaw + dz * cosYaw
            if (abs(localX) > halfWidth) continue
            if (abs(dy) > halfHeight) continue
            if (abs(localZ) > halfLength) continue
            targets.add(p)
        }
        return targets
    }
}
