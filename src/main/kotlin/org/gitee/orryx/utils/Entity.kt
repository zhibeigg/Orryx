package org.gitee.orryx.utils

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.targets.PlayerTarget

/**
 *安全的将AbstractEntity转换成Bukkit Entity
 */
internal fun IEntity.getBukkitEntity(): Entity? {
    return (this as? AbstractBukkitEntity ?: this as? PlayerTarget)?.getSource()
}

/**
 *安全的将AbstractEntity转换成Bukkit LivingEntity
 */
internal fun IEntity.getBukkitLivingEntity(): LivingEntity? {
    return getBukkitEntity() as? LivingEntity
}

/**
 *安全的将AbstractEntity转换成Ady EntityInstance
 */
internal fun IEntity.getAdyeshachEntity(): EntityInstance? {
    return (this as? AbstractAdyeshachEntity)?.getSource()
}
