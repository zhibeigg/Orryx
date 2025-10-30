package org.gitee.orryx.core.kether.actions.effect

import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.kether.actions.effect.EffectType.ARC
import org.joml.Matrix3d
import taboolib.common.util.unsafeLazy
import taboolib.library.xseries.XParticle

class EffectBuilder {

    var type: EffectType = ARC
    var particle: XParticle = XParticle.DUST
    var period: Long = 1
    var step: Double = 0.2
    var count: Int = 1
    var speed: Double = 1.0

    var offset: IVector = AbstractVector()
    var translate: IVector = AbstractVector()

    var startAngle: Double = 0.0
    var angle: Double = 30.0
    var radius: Double = 1.0
    var sample: Int = 100

    var width: Double = 1.0
    var height: Double = 1.0
    var length: Double = 1.0

    var xScaleRate: Double = 1.0
    var yScaleRate: Double = 1.0

    var vector: AbstractVector? = null
    var matrix: Matrix3d? = null

    var corner: Int = 5
    var side: Int = 3
    var maxLength: Double = 1.0
    var range: Double = 1.0

    //DustData
    var dustData: ParticleData.DustData? = null
    //DustTransitionData
    var dustTransitionData: ParticleData.DustTransitionData? = null
    //ItemData
    var itemData: ParticleData.ItemData? = null
    //BlockData
    var blockData: ParticleData.BlockData? = null
    //VibrationData
    var vibrationData: ParticleData.VibrationData? = null

    val data: ParticleData?
        get() = dustData ?: dustTransitionData ?: itemData ?: blockData ?: vibrationData

    val locations: getValue by unsafeLazy { mutableListOf<Pair<Int, EffectOrigin>>() }

}