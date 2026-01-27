package org.gitee.orryx.core.selector

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITarget
import taboolib.common.util.Location
import taboolib.module.kether.ScriptContext

/**
 * 几何选择器接口。
 */
interface ISelectorGeometry: ISelector, WikiSelector {

    /**
     * 获取目标列表。
     *
     * @param context 脚本上下文
     * @param parameter 解析参数
     * @return 目标列表
     */
    fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>>

    /**
     * 获取一帧可视粒子渲染位置列表。
     *
     * @param context 脚本上下文
     * @param parameter 解析参数
     * @return 渲染位置列表
     */
    fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location>

}
