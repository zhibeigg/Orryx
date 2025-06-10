package org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic

import org.gitee.orryx.api.collider.ICollideFunction
import org.gitee.orryx.api.collider.ISphere
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

class Sphere<T : ITargetLocation<*>>(
    override var center: Vector3d,
    override var radius: Double
) : ISphere<T> {

    private var disable = false

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    override fun toString(): String {
        return "Sphere{" +
                "radius=" + radius +
                ", center=" + center +
                ", disable=" + disable +
                '}'
    }
}