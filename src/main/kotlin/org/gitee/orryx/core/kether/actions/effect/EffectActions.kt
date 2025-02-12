package org.gitee.orryx.core.kether.actions.effect

import org.gitee.orryx.utils.ORRYX_NAMESPACE
import taboolib.module.kether.KetherParser

object EffectActions {

    /**
     * ```
     * set m matrix
     * effect parm e {
     *   draw particle "@type DRAGON_BREATH @color 0 0 0 @count 1 @size 1.5"
     *   draw movement "@pattern LINEAR @speed 1 @density 5 @duration 20"
     *   draw matrix &m
     * }
     * effect show &e they "@self" viewer "@self" onhit {
     *   tell &target
     * }
     *
     * or
     *
     * set m matrix
     *
     * effect show effect temp {
     *   draw particle "@type DRAGON_BREATH @color 0 0 0 @count 1 @size 1.5"
     *   draw movement "@pattern LINEAR @speed 1 @density 5 @duration 20"
     *   draw matrix &m
     * } duration 20 tick 2 they "@self" viewer "@self" onhit {
     *   tell &target
     * }
     * ```
     * */
    @KetherParser(["effect"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun effect() {

    }

}