package org.gitee.orryx.api.adapters

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector
import java.util.*

/**
 * 实体适配接口。
 *
 * @property uniqueId 实体唯一标识
 * @property isDead 实体是否死亡
 * @property isValid 实体是否有效
 * @property world 实体所在世界
 * @property type 实体类型字符串
 * @property bukkitType Bukkit 实体类型，若无则为 null
 * @property width 实体宽度
 * @property height 实体高度
 * @property vehicle 当前载具实体
 * @property name 实体名称
 * @property customName 自定义名称
 * @property location 实体位置
 * @property eyeLocation 实体视线位置
 * @property velocity 实体速度向量
 * @property entityId 实体数值 ID
 * @property gravity 是否受重力影响
 * @property moveSpeed 实体移动速度
 * @property isOnGround 是否在地面
 * @property isFrozen 是否冻结
 * @property isFired 是否着火
 * @property isInsideVehicle 是否在载具内
 * @property isSilent 是否静默
 * @property isCustomNameVisible 自定义名称是否可见
 * @property isGlowing 是否发光
 * @property isInWater 是否在水中
 * @property isInvulnerable 是否无敌
 */
interface IEntity {

    val uniqueId: UUID

    val isDead: Boolean

    val isValid: Boolean

    val world: World

    val type: String

    val bukkitType: EntityType?

    val width: Double

    val height: Double

    val vehicle: IEntity?

    val name: String

    var customName: String?

    val location: Location

    val eyeLocation: Location

    var velocity: Vector

    val entityId: Int

    val gravity: Boolean

    val moveSpeed: Double

    val isOnGround: Boolean

    val isFrozen: Boolean

    val isFired: Boolean

    val isInsideVehicle: Boolean

    val isSilent: Boolean

    val isCustomNameVisible: Boolean

    val isGlowing: Boolean

    val isInWater: Boolean

    val isInvulnerable: Boolean

    /**
     * 传送实体到指定位置。
     *
     * @param location 目标位置
     */
    fun teleport(location: Location)

    /**
     * 移除实体。
     */
    fun remove()
}
