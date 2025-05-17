package org.gitee.orryx.core.selector

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import taboolib.module.kether.ScriptContext

interface ISelectorStream: ISelector, WikiSelector {

    /**
     * 将容器流进行加工
     * */
    fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry)
}