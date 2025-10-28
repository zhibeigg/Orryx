package org.gitee.orryx.utils.raytrace

enum class FluidHandling {

    /**
     * 忽略流体
     * */
    NONE,
    /**
     * 仅与源流体块碰撞
     * */
    SOURCE_ONLY,
    /**
     * 与所有流体碰撞（低于1.13无效）
     * */
    ALWAYS;
}