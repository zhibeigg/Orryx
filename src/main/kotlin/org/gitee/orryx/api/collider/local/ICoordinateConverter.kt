package org.gitee.orryx.api.collider.local

import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * 坐标转换器接口。
 *
 * @property position 当前坐标
 * @property rotation 当前旋转
 */
interface ICoordinateConverter {

    /**
     * 获取位置版本号。
     *
     * @return 位置版本
     */
    fun positionVersion(): Short

    val position: Vector3d

    /**
     * 获取旋转版本号。
     *
     * @return 旋转版本
     */
    fun rotationVersion(): Short

    val rotation: Quaterniond

    /**
     * 更新缓存。
     */
    fun update()
}
