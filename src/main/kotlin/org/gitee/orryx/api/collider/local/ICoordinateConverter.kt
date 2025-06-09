package org.gitee.orryx.api.collider.local

import org.joml.Quaterniond
import org.joml.Vector3d

interface ICoordinateConverter {

    /** 位置版本 */
    fun positionVersion(): Short

    /** 位置 */
    val position: Vector3d

    /** 旋转版本 */
    fun rotationVersion(): Short

    /** 旋转 */
    val rotation: Quaterniond
}
