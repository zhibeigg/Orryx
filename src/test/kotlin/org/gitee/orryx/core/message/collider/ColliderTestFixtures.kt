package org.gitee.orryx.core.message.collider

import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.api.collider.ICapsule
import org.gitee.orryx.api.collider.ICollider
import org.gitee.orryx.api.collider.IComposite
import org.gitee.orryx.api.collider.IOBB
import org.gitee.orryx.api.collider.IRay
import org.gitee.orryx.api.collider.ISphere
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

internal interface ColliderMockTarget : ITargetLocation<Unit> {
    override fun getSource() = Unit
    override val world get() = throw UnsupportedOperationException()
    override val location get() = throw UnsupportedOperationException()
    override val eyeLocation get() = throw UnsupportedOperationException()
}

internal abstract class MockCollider : ICollider<ColliderMockTarget>, ColliderMockTarget {

    private var disabled = false

    override fun setDisable(disable: Boolean) {
        disabled = disable
    }

    override fun disable(): Boolean = disabled
}

internal class MockSphere(
    override var center: Vector3d,
    override var radius: Double,
) : MockCollider(), ISphere<ColliderMockTarget>

internal class MockAabb(
    override var center: Vector3d,
    override var halfExtents: Vector3d,
) : MockCollider(), IAABB<ColliderMockTarget> {
    override val min: Vector3d get() = Vector3d(center).sub(halfExtents)
    override val max: Vector3d get() = Vector3d(center).add(halfExtents)
}

internal class MockObb(
    override var center: Vector3d,
    override var halfExtents: Vector3d,
    override var rotation: Quaterniond,
) : MockCollider(), IOBB<ColliderMockTarget> {
    override val vertices: Array<Vector3d> = emptyArray()
    override val axes: Array<Vector3d> = emptyArray()
}

internal class MockCapsule(
    override var center: Vector3d,
    override var radius: Double,
    override var height: Double,
    override var rotation: Quaterniond,
    var directionValue: Vector3d,
) : MockCollider(), ICapsule<ColliderMockTarget> {
    override val direction: Vector3d get() = directionValue
}

internal class MockRay(
    override val origin: Vector3d,
    override var direction: Vector3d,
    override var length: Double,
) : MockCollider(), IRay<ColliderMockTarget> {
    override val end: Vector3d get() = Vector3d(direction).mul(length).add(origin)
}

internal class MockComposite(
    private val children: MutableList<ICollider<ColliderMockTarget>> = mutableListOf(),
) : MockCollider(), IComposite<ColliderMockTarget, ICollider<ColliderMockTarget>> {

    override val collidersCount: Int get() = children.size

    override fun getCollider(index: Int): ICollider<ColliderMockTarget> = children[index]

    override fun setCollider(index: Int, collider: ICollider<ColliderMockTarget>) {
        children[index] = collider
    }

    override fun addCollider(collider: ICollider<ColliderMockTarget>) {
        children += collider
    }

    override fun removeCollider(index: Int) {
        children.removeAt(index)
    }
}
