package org.gitee.orryx.core.selector

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import taboolib.module.kether.ScriptContext

/**
 * 选择器流处理接口。
 */
interface ISelectorStream: ISelector, WikiSelector {

    /**
     * 将容器流进行加工。
     *
     * @param container 目标容器
     * @param context 脚本上下文
     * @param parameter 解析参数
     */
    fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry)
}
