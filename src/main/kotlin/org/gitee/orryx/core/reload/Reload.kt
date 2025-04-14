package org.gitee.orryx.core.reload

/**
 * 按照权重从小到大执行
 * @param weight 权重
 * */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reload(val weight: Int)
