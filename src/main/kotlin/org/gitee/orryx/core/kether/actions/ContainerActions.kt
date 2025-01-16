package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.NAMESPACE
import org.gitee.orryx.utils.container
import org.gitee.orryx.utils.orElse
import org.gitee.orryx.utils.theyContainer
import taboolib.common5.cbool
import taboolib.module.kether.KetherParser
import taboolib.module.kether.orNull
import taboolib.module.kether.run

object ContainerActions {

    @KetherParser(["container"], namespace = NAMESPACE, shared = true)
    fun actionContainer() = combinationParser(
        Action.new("container", true)
            .description("创建/复制 一个Container容器，用于储存各类Target")
            .addContainerEntry("被复制的容器", optional = true)
            .result("创建的容器", Type.CONTAINER)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            now { container?.clone().orElse(Container()) }
        }
    }

    @KetherParser(["merge"], namespace = NAMESPACE, shared = true)
    fun actionMerge() = combinationParser(
        Action.new("merge", true)
            .description("合并两个Container容器")
            .addEntry("合并到的Container容器", Type.CONTAINER)
            .addContainerEntry("被合并的Container容器")
            .result("合并后的容器", Type.CONTAINER)
    ) {
        it.group(
            container(),
            theyContainer(false)
        ).apply(it) { container1, container2 ->
            now { container1?.merge(container2!!) }
        }
    }

    @KetherParser(["mergeIf"], namespace = NAMESPACE, shared = true)
    fun actionMergeIf() = combinationParser(
        Action.new("mergeIf", true)
            .description("合并两个Container容器，剔除被合并容器中返回false的目标，可读取@Target参数")
            .addEntry("合并到的Container容器", Type.CONTAINER)
            .addContainerEntry("被合并的Container容器")
            .result("合并后的容器", Type.CONTAINER)
    ) {
        it.group(
            container(),
            theyContainer(false),
            action()
        ).apply(it) { container1, container2, bool ->
            now {
                container1?.mergeIf(container2!!) { target ->
                    variables()["@Target"] = target
                    val result = run(bool).orNull().cbool
                    variables().remove("@Target")
                    result
                }
            }
        }
    }

    @KetherParser(["removeIf"], namespace = NAMESPACE, shared = true)
    fun actionRemoveIf() = combinationParser(
        Action.new("removeIf", true)
            .description("检测Container容器，删除返回值为true的Target，可读取@Target参数")
            .addContainerEntry("被检测的Container容器")
            .result("被删除后的容器", Type.CONTAINER)
    ) {
        it.group(
            theyContainer(false),
            action()
        ).apply(it) { container, bool ->
            now {
                container?.removeIf { target ->
                    variables()["@Target"] = target
                    val result = run(bool).orNull().cbool
                    variables().remove("@Target")
                    result
                }
            }
        }
    }

}