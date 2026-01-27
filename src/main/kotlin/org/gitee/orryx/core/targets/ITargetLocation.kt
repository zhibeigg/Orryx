package org.gitee.orryx.core.targets

import org.bukkit.Location
import org.bukkit.World

/**
 * 带位置信息的目标对象接口。
 *
 * @param T 目标的源对象类型
 * @property world 目标所在世界
 * @property location 目标位置
 * @property eyeLocation 目标视线位置
 */
interface ITargetLocation<T>: ITarget<T> {

    val world: World

    val location: Location

    val eyeLocation: Location
}
