package org.gitee.orryx.core.targets

/**
 * 目标对象包装接口。
 *
 * @param T 目标的源对象类型
 */
interface ITarget<T> {

    /**
     * 获取目标的源对象。
     *
     * @return 源对象
     */
    fun getSource(): T
}
