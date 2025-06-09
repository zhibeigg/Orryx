package org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic

import org.gitee.orryx.api.collider.ICollideFunction
import org.gitee.orryx.api.collider.ICollider
import org.gitee.orryx.api.collider.IComposite
import org.gitee.orryx.core.targets.ITargetLocation

class Composite<T : ITargetLocation<*>, C : ICollider<T>>(
    private val colliders: MutableList<C>,
    override val onCollideFunction: ICollideFunction
) : IComposite<T, C> {

    private var disable = false

    override val collidersCount: Int
        get() = colliders.size

    override fun getCollider(index: Int): C {
        return colliders[index]
    }

    override fun setCollider(index: Int, collider: C) {
        colliders[index] = collider
    }

    override fun addCollider(collider: C) {
        colliders.add(collider)
    }

    override fun removeCollider(index: Int) {
        colliders.removeAt(index)
    }

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    override fun toString(): String {
        return "Composite{" +
                "colliders=" + colliders +
                ", disable=" + disable +
                '}'
    }
}
