package org.gitee.orryx.utils

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.api.adapters.AbstractEntity

/**
 *安全的将AbstractEntity转换成Bukkit Entity
 */
internal fun AbstractEntity.getBukkitEntity(): Entity? {
    return (this as? AbstractBukkitEntity)?.instance
}

/**
 *安全的将AbstractEntity转换成Bukkit LivingEntity
 */
internal fun AbstractEntity.getBukkitLivingEntity(): LivingEntity? {
    return (this as? AbstractBukkitEntity)?.instance as? LivingEntity
}

/**
 *安全的将AbstractEntity转换成Ady EntityInstance
 */
internal fun AbstractEntity.getAdyeshachEntity(): EntityInstance? {
    return (this as? AbstractAdyeshachEntity)?.instance
}
