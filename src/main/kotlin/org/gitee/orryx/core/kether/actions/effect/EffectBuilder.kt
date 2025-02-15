package org.gitee.orryx.core.kether.actions.effect

import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.kether.actions.effect.EffectType.ARC
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Matrix4d
import taboolib.common.platform.ProxyParticle

class EffectBuilder {

    var type: EffectType = ARC
    var particle: ProxyParticle = ProxyParticle.DUST
    var period: Long = 1
    var step: Double = 0.2
    var count: Int = 1
    var speed: Double = 1.0

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
    var matrix: Matrix4d? = null

    var corner: Int = 5
    var side: Int = 3
    var maxLength: Double = 1.0
    var range: Double = 1.0

    //DustData
    var dustData: ProxyParticle.DustData? = null
    //DustTransitionData
    var dustTransitionData: ProxyParticle.DustTransitionData? = null
    //ItemData
    var itemData: ProxyParticle.ItemData? = null
    //BlockData
    var blockData: ProxyParticle.BlockData? = null
    //VibrationData
    var vibrationData: ProxyParticle.VibrationData? = null

    val locations by lazy { mutableListOf<Pair<Int, ITargetLocation<*>>>() }

}