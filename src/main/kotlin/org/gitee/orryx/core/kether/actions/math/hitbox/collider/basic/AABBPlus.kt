package org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic

import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.api.collider.ICollideFunction
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

class AABBPlus<T : ITargetLocation<*>>(override var halfExtents: Vector3d, override var center: Vector3d) : IAABB<T> {

    private var disable = false

    override val min: Vector3d
        get() = center.sub(halfExtents, Vector3d())

    override val max: Vector3d
        get() = center.add(halfExtents, Vector3d())

    override fun toString(): String {
        return "AABBPlus{" +
                "halfExtents=" + halfExtents +
                ", center=" + center +
                ", disable=" + disable +
                '}'
    }

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }
}
