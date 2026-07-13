package org.gitee.orryx.core.kether.actions.game.projectile

internal class ProjectileLifetime(period: Long, private val timeout: Long) {
    val period = period.coerceAtLeast(1L)
    var ticked = -this.period
        private set

    fun advance(): Boolean {
        ticked = if (Long.MAX_VALUE - period < ticked) Long.MAX_VALUE else ticked + period
        return ticked <= timeout
    }
}
