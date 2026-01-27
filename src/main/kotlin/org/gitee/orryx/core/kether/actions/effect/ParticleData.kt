package org.gitee.orryx.core.kether.actions.effect

import taboolib.common.util.Location
import java.awt.Color
import java.util.*

/**
 * 粒子数据接口。
 */
interface ParticleData {

    open class DustData(val color: Color, val size: Float) : ParticleData

    class DustTransitionData(color: Color, val toColor: Color, size: Float) : DustData(color, size)

    class ItemData(
        val material: String,
        val data: Int = 0,
        val name: String = "",
        val lore: List<String> = emptyList(),
        val customModelData: Int = -1,
    ) : ParticleData

    class BlockData(val material: String, val data: Int = 0) : ParticleData

    class VibrationData(val origin: Location, val destination: Destination, val arrivalTime: Int) : ParticleData {

        /**
         * 震动目标类型接口。
         */
        sealed interface Destination

        class EntityDestination(val entity: UUID) : Destination

        class LocationDestination(val location: Location) : Destination
    }

}
