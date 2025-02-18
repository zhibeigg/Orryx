package org.gitee.orryx.compat.mythicmobs.targeter

import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity
import io.lumine.xikage.mythicmobs.io.MythicLineConfig
import io.lumine.xikage.mythicmobs.skills.SkillMetadata
import io.lumine.xikage.mythicmobs.skills.targeters.IEntitySelector
import taboolib.common.platform.Ghost

@Ghost
class MythicMobsMobInRangeTargeter(mlc: MythicLineConfig) : IEntitySelector(mlc) {

    private val range = mlc.getDouble(arrayOf("range", "r"), 5.0)
    private val ignoreType = mlc.getString(arrayOf("type", "t"), null)

    override fun getEntities(data: SkillMetadata): HashSet<AbstractEntity> {
        val am = data.caster
        val location = am.location

        val types = ignoreType?.split(",")?.map { MythicMobs.inst().mobManager.getMythicMob(it) } ?: emptyList()

        val entities = MythicMobs.inst().mobManager.activeMobs.filter {
            it.location.world == am.location.world && it.location.distance(location) <= range && it.type !in types
        }

        return HashSet<AbstractEntity>().apply {
            addAll(entities.map { it.entity })
            remove(am.entity)
        }
    }

}