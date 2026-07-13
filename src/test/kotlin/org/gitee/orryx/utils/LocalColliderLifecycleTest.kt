package org.gitee.orryx.utils

import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.LocalComposite
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.LocalOBB
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.LocalSphere
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

private interface LocalMockTarget : ITargetLocation<Unit> {
    override fun getSource() = Unit
    override val world get() = throw UnsupportedOperationException()
    override val location get() = throw UnsupportedOperationException()
    override val eyeLocation get() = throw UnsupportedOperationException()
}

private class MutableCoordinateConverter : ICoordinateConverter {
    private var positionVersion = 0.toShort()
    private var rotationVersion = 0.toShort()
    override val position = Vector3d()
    override val rotation = Quaterniond()

    override fun positionVersion() = positionVersion
    override fun rotationVersion() = rotationVersion
    override fun update() = Unit

    fun moveTo(x: Double, y: Double, z: Double) {
        position.set(x, y, z)
        positionVersion++
    }

    fun rotateTo(value: Quaterniond) {
        rotation.set(value)
        rotationVersion++
    }
}

class LocalColliderLifecycleTest {

    @Test
    fun `first named collider keeps index zero`() {
        val parent = MutableCoordinateConverter()
        val composite = LocalComposite<LocalMockTarget, LocalSphere<LocalMockTarget>>(
            Vector3d(), Quaterniond(), parent
        )
        val sphere = LocalSphere<LocalMockTarget>(Vector3d(), 1.0, parent)

        composite.addCollider("first", sphere)

        assertSame(sphere, composite.getCollider("first"))
        assertEquals(0, composite.getColliderIndex("first"))
    }

    @Test
    fun `indexed insert reindexes every named collider`() {
        val parent = MutableCoordinateConverter()
        val composite = LocalComposite<LocalMockTarget, LocalSphere<LocalMockTarget>>(
            Vector3d(), Quaterniond(), parent
        )
        val first = LocalSphere<LocalMockTarget>(Vector3d(), 1.0, parent)
        val second = LocalSphere<LocalMockTarget>(Vector3d(), 2.0, parent)
        val inserted = LocalSphere<LocalMockTarget>(Vector3d(), 3.0, parent)
        composite.addCollider("first", first)
        composite.addCollider("second", second)

        composite.addCollider(1, "inserted", inserted)

        assertEquals(0, composite.getColliderIndex("first"))
        assertEquals(1, composite.getColliderIndex("inserted"))
        assertEquals(2, composite.getColliderIndex("second"))
        assertSame(inserted, composite.getCollider("inserted"))
        assertSame(second, composite.getCollider("second"))
    }

    @Test
    fun `composite child versions advance only on actual changes`() {
        val parent = MutableCoordinateConverter()
        val composite = LocalComposite<LocalMockTarget, LocalSphere<LocalMockTarget>>(
            Vector3d(), Quaterniond(), parent
        )
        val child = composite.converter

        child.update()
        val initialPositionVersion = child.positionVersion()
        val initialRotationVersion = child.rotationVersion()
        child.update()

        assertEquals(initialPositionVersion, child.positionVersion())
        assertEquals(initialRotationVersion, child.rotationVersion())

        parent.moveTo(2.0, 0.0, 0.0)
        child.update()
        assertEquals((initialPositionVersion + 1).toShort(), child.positionVersion())
        assertEquals(initialRotationVersion, child.rotationVersion())
    }

    @Test
    fun `local composite rotation does not rotate its local position`() {
        val parent = MutableCoordinateConverter()
        val composite = LocalComposite<LocalMockTarget, LocalSphere<LocalMockTarget>>(
            Vector3d(1.0, 0.0, 0.0),
            Quaterniond().rotateZ(Math.PI / 2.0),
            parent
        )

        assertEquals(1.0, composite.position.x, 1e-9)
        assertEquals(0.0, composite.position.y, 1e-9)
    }

    @Test
    fun `local OBB getters refresh after parent movement`() {
        val parent = MutableCoordinateConverter()
        val obb = LocalOBB<LocalMockTarget>(
            Vector3d(1.0, 1.0, 1.0),
            Vector3d(),
            Quaterniond(),
            parent
        )
        assertEquals(0.0, obb.center.x, 1e-9)

        parent.moveTo(4.0, 5.0, 6.0)

        assertEquals(Vector3d(4.0, 5.0, 6.0), obb.center)
        assertEquals(Vector3d(5.0, 6.0, 7.0), obb.vertices.first())
    }

    @Test
    fun `local OBB rotation does not rotate its local center`() {
        val parent = MutableCoordinateConverter()
        val obb = LocalOBB<LocalMockTarget>(
            Vector3d(1.0, 1.0, 1.0),
            Vector3d(1.0, 0.0, 0.0),
            Quaterniond().rotateZ(Math.PI / 2.0),
            parent
        )

        assertEquals(1.0, obb.center.x, 1e-9)
        assertEquals(0.0, obb.center.y, 1e-9)
    }
}
