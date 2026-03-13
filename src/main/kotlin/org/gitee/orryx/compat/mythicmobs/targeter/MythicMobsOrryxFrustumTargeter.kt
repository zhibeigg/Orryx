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
import kotlin.math.sqrt

@Ghost
class MythicMobsOrryxFrustumTargeter(mlc: MythicLineConfig): IEntitySelector(mlc) {

    private val topRadius = mlc.getPlaceholderDouble(arrayOf("topradius", "tr"), 1.0)
    private val bottomRadius = mlc.getPlaceholderDouble(arrayOf("bottomradius", "br"), 3.0)
    private val angle = mlc.getPlaceholderDouble(arrayOf("angle", "a"), 45.0)
    private val yawOffset = mlc.getPlaceholderDouble(arrayOf("yaw", "y"), 0.0)
    private val offsetY = mlc.getPlaceholderDouble(arrayOf("offsety", "oy"), 0.0)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity?> {
        val am = data.caster
        val targets = HashSet<AbstractEntity?>()
        val casterLoc = am.entity.bukkitEntity.location

        val topR = topRadius.get(data)
        val bottomR = bottomRadius.get(data)
        val ang = Math.toRadians(angle.get(data))
        val oy = offsetY.get(data)

        // 圆台轴方向由 pitch(angle) 和 yaw 决定
        val yaw = Math.toRadians(-(casterLoc.yaw.toDouble() + yawOffset.get(data)))
        val pitch = ang

        // 轴方向向量（从底部到顶部）
        val axisX = sin(yaw) * cos(pitch)
        val axisY = sin(pitch)
        val axisZ = cos(yaw) * cos(pitch)

        // 圆台高度 = 沿轴方向的总长度
        val frustumHeight = (bottomR + topR) / (2.0 * kotlin.math.tan(ang / 2.0).coerceAtLeast(0.001))

        val baseX = casterLoc.x
        val baseY = casterLoc.y + oy
        val baseZ = casterLoc.z

        for (p in MythicMobs.inst().entityManager.getPlayers(am.entity.world)) {
            if (p.world != am.entity.world) continue
            val loc = p.bukkitEntity.location
            val dx = loc.x - baseX
            val dy = loc.y - baseY
            val dz = loc.z - baseZ

            // 投影到轴上
            val proj = dx * axisX + dy * axisY + dz * axisZ
            if (proj < 0 || proj > frustumHeight) continue

            // 到轴的垂直距离
            val perpX = dx - proj * axisX
            val perpY = dy - proj * axisY
            val perpZ = dz - proj * axisZ
            val perpDist = sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ)

            // 在该高度处的半径（线性插值）
            val t = proj / frustumHeight
            val radiusAtHeight = bottomR + (topR - bottomR) * t
            if (perpDist > radiusAtHeight) continue

            targets.add(p)
        }
        return targets
    }
}
