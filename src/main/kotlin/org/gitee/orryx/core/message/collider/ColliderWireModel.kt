package org.gitee.orryx.core.message.collider

/** OrryxMod 碰撞箱线框颜色。 */
data class ColliderRenderColor(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int,
) {

    init {
        require(r in 0..255 && g in 0..255 && b in 0..255 && a in 0..255) {
            "碰撞箱颜色通道必须位于 0..255"
        }
    }

    companion object {

        val WHITE = ColliderRenderColor(255, 255, 255, 255)

        fun clamped(r: Int, g: Int, b: Int, a: Int): ColliderRenderColor {
            return ColliderRenderColor(
                r.coerceIn(0, 255),
                g.coerceIn(0, 255),
                b.coerceIn(0, 255),
                a.coerceIn(0, 255),
            )
        }
    }
}

/** 与 OrryxMod 18/19 号数据包一致的不可变几何快照。 */
sealed interface ColliderWireShape {

    val typeId: Int

    data class Sphere(
        val centerX: Double,
        val centerY: Double,
        val centerZ: Double,
        val radius: Double,
    ) : ColliderWireShape {
        override val typeId: Int = 0
    }

    data class Aabb(
        val centerX: Double,
        val centerY: Double,
        val centerZ: Double,
        val halfX: Double,
        val halfY: Double,
        val halfZ: Double,
    ) : ColliderWireShape {
        override val typeId: Int = 1
    }

    data class Obb(
        val centerX: Double,
        val centerY: Double,
        val centerZ: Double,
        val halfX: Double,
        val halfY: Double,
        val halfZ: Double,
        val quaternionX: Float,
        val quaternionY: Float,
        val quaternionZ: Float,
        val quaternionW: Float,
    ) : ColliderWireShape {
        override val typeId: Int = 2
    }

    /** 兼容现有协议的竖直胶囊，halfHeight 是两端球心到中心的距离。 */
    data class Capsule(
        val centerX: Double,
        val centerY: Double,
        val centerZ: Double,
        val radius: Double,
        val halfHeight: Double,
    ) : ColliderWireShape {
        override val typeId: Int = 3
    }

    data class Ray(
        val originX: Double,
        val originY: Double,
        val originZ: Double,
        val directionX: Double,
        val directionY: Double,
        val directionZ: Double,
        val length: Double,
    ) : ColliderWireShape {
        override val typeId: Int = 4
    }

    data class Composite(
        val children: List<ColliderWireChild>,
    ) : ColliderWireShape {
        override val typeId: Int = 5
    }

    /** 扩展类型；保持 0-5 的旧协议布局不变。 */
    data class OrientedCapsule(
        val centerX: Double,
        val centerY: Double,
        val centerZ: Double,
        val radius: Double,
        val halfHeight: Double,
        val quaternionX: Float,
        val quaternionY: Float,
        val quaternionZ: Float,
        val quaternionW: Float,
    ) : ColliderWireShape {
        override val typeId: Int = 6
    }
}

data class ColliderWireChild(
    val id: String,
    val color: ColliderRenderColor,
    val shape: ColliderWireShape,
)

data class ColliderWireSnapshot(
    val id: String,
    val color: ColliderRenderColor,
    val shape: ColliderWireShape,
)
