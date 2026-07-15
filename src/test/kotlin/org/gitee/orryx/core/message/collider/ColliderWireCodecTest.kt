package org.gitee.orryx.core.message.collider

import com.google.common.io.ByteStreams
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import kotlin.math.PI

class ColliderWireCodecTest {

    @Test
    fun `OBB 按 Double 半轴和 Float 四元数编码`() {
        val collider = MockObb(
            center = Vector3d(1.0, 2.0, 3.0),
            halfExtents = Vector3d(4.0, 5.0, 6.0),
            rotation = Quaterniond().rotateY(PI / 2.0),
        )
        val snapshot = ColliderWireCodec.snapshot("obb", collider, ColliderRenderColor(1, 2, 3, 4))!!
        val output = ByteStreams.newDataOutput()
        ColliderWireCodec.writeShowPayload(output, snapshot)
        val input = DataInputStream(ByteArrayInputStream(output.toByteArray()))

        assertEquals("obb", input.readUTF())
        assertEquals(2, input.readInt())
        assertEquals(listOf(1, 2, 3, 4), List(4) { input.readInt() })
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), List(6) { input.readDouble() })
        assertEquals(0f, input.readFloat(), 1.0E-6f)
        assertEquals(0.70710677f, input.readFloat(), 1.0E-6f)
        assertEquals(0f, input.readFloat(), 1.0E-6f)
        assertEquals(0.70710677f, input.readFloat(), 1.0E-6f)
        assertEquals(0, input.available())
    }

    @Test
    fun `竖直胶囊使用 halfHeight 且倾斜胶囊使用类型 6`() {
        val vertical = MockCapsule(
            center = Vector3d(1.0, 2.0, 3.0),
            radius = 0.5,
            height = 4.0,
            rotation = Quaterniond(),
            directionValue = Vector3d(0.0, 1.0, 0.0),
        )
        val verticalShape = ColliderWireCodec.snapshot("vertical", vertical, ColliderRenderColor.WHITE)!!.shape
        val capsule = assertInstanceOf(ColliderWireShape.Capsule::class.java, verticalShape)
        assertEquals(2.0, capsule.halfHeight)

        val oriented = MockCapsule(
            center = Vector3d(),
            radius = 1.0,
            height = 6.0,
            rotation = Quaterniond().rotateZ(-PI / 2.0),
            directionValue = Vector3d(1.0, 0.0, 0.0),
        )
        val orientedShape = ColliderWireCodec.snapshot("oriented", oriented, ColliderRenderColor.WHITE)!!.shape
        val orientedCapsule = assertInstanceOf(ColliderWireShape.OrientedCapsule::class.java, orientedShape)
        assertEquals(6, orientedCapsule.typeId)
        assertEquals(3.0, orientedCapsule.halfHeight)
    }

    @Test
    fun `复合体写入稳定子 ID 和继承颜色`() {
        val nested = MockComposite(
            mutableListOf(
                MockAabb(Vector3d(4.0, 5.0, 6.0), Vector3d(1.0, 2.0, 3.0)),
            )
        )
        val root = MockComposite(
            mutableListOf(
                MockSphere(Vector3d(1.0, 2.0, 3.0), 2.0),
                nested,
            )
        )
        val color = ColliderRenderColor(10, 20, 30, 40)
        val snapshot = ColliderWireCodec.snapshot("root", root, color)!!
        val shape = assertInstanceOf(ColliderWireShape.Composite::class.java, snapshot.shape)
        assertEquals(listOf("0", "1"), shape.children.map { it.id })
        val nestedShape = assertInstanceOf(ColliderWireShape.Composite::class.java, shape.children[1].shape)
        assertEquals("1.0", nestedShape.children.single().id)
        assertEquals(color, nestedShape.children.single().color)

        val output = ByteStreams.newDataOutput()
        ColliderWireCodec.writeUpdatePayload(output, snapshot)
        val input = DataInputStream(ByteArrayInputStream(output.toByteArray()))
        assertEquals("root", input.readUTF())
        assertEquals(5, input.readInt())
        assertEquals(2, input.readInt())
        assertEquals("0", input.readUTF())
        assertEquals(0, input.readInt())
        assertEquals(listOf(10, 20, 30, 40), List(4) { input.readInt() })
    }

    @Test
    fun `拒绝非法 ID 和过深复合体`() {
        assertThrows(IllegalArgumentException::class.java) {
            ColliderWireCodec.snapshot(" ", MockSphere(Vector3d(), 1.0), ColliderRenderColor.WHITE)
        }

        val level4 = MockComposite(mutableListOf(MockSphere(Vector3d(), 1.0)))
        val level3 = MockComposite(mutableListOf(level4))
        val level2 = MockComposite(mutableListOf(level3))
        val level1 = MockComposite(mutableListOf(level2))
        val root = MockComposite(mutableListOf(level1))
        assertThrows(IllegalArgumentException::class.java) {
            ColliderWireCodec.snapshot("deep", root, ColliderRenderColor.WHITE)
        }
    }
}
