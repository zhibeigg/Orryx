package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.ColliderType
import org.gitee.orryx.api.collider.local.ILocalCollider
import org.gitee.orryx.core.targets.ITargetLocation

class None() : ILocalCollider<ITargetLocation<*>> {

    override val type: ColliderType
        get() = ColliderType.NONE

    override fun disable(): Boolean {
        return true
    }

    override fun setDisable(disable: Boolean) {
    }

    override fun update() {
    }
}