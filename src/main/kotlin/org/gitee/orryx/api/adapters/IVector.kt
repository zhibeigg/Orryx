package org.gitee.orryx.api.adapters

import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.joml.Vector3d
import org.joml.Vector3dc

/**
 * 向量适配接口。
 *
 * @property joml 底层 JOML 向量实例
 */
interface IVector: Vector3dc {

    val joml: Vector3d

    /**
     * 与指定向量相加并返回新向量。
     *
     * @param vector3dc 参与运算的向量
     * @return 计算后的新向量
     */
    fun add(vector3dc: Vector3dc): AbstractVector

    /**
     * 返回当前向量的取反结果。
     *
     * @return 取反后的向量
     */
    fun negate(): IVector
}
