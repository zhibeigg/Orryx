package org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic

import org.gitee.orryx.api.collider.ICapsule
import org.gitee.orryx.api.collider.ICollideFunction
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

class Capsule<T : ITargetLocation<*>>(
    override var center: Vector3d,
    override var rotation: Quaterniond,
    override var radius: Double,
    override var height: Double
) : ICapsule<T> {

    private var disable = false

    override val direction: Vector3d
        get() = rotation.transform(Vector3d(0.0, 1.0, 0.0))

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    override fun toString(): String {
        return "Capsule{" +
                "height=" + height +
                ", radius=" + radius +
                ", center=" + center +
                ", rotation=" + rotation +
                ", disable=" + disable +
                '}'
    }
}
