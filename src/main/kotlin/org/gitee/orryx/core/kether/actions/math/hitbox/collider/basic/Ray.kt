package org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic

import org.gitee.orryx.api.collider.IRay
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

class Ray<T : ITargetLocation<*>>(
    override val origin: Vector3d,
    override var length: Double,
    override var direction: Vector3d
) : IRay<T> {

    private var disable = false

    override val end: Vector3d
        get() = Vector3d(origin).add(direction.mul(length))

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    override fun toString(): String {
        return "Ray{" +
                "length=" + length +
                ", origin=" + origin +
                ", direction=" + direction +
                ", disable=" + disable +
                '}'
    }
}
