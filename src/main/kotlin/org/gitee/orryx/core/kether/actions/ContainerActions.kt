package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.OpenResult
import taboolib.common5.cbool
import taboolib.module.kether.*

object ContainerActions {

    init {
        KetherLoader.registerProperty(containerProperty(), IContainer::class.java, false)
    }

    @KetherParser(["container"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionContainer() = combinationParser(
        Action.new("Container容器", "目标容器", "container", true)
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

    @KetherParser(["merge"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionMerge() = combinationParser(
        Action.new("Container容器", "合并目标容器", "merge", true)
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

    @KetherParser(["mergeIf"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionMergeIf() = combinationParser(
        Action.new("Container容器", "合并如果目标容器", "mergeIf", true)
            .description("合并两个Container容器，剔除被合并容器中返回false的目标，可读取@Target参数")
            .addEntry("合并到的Container容器", Type.CONTAINER)
            .addContainerEntry("被合并的Container容器")
            .addEntry("方法体返回布尔类型", Type.BOOLEAN)
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

    @KetherParser(["removeIf"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionRemoveIf() = combinationParser(
        Action.new("Container容器", "删除如果目标容器", "removeIf", true)
            .description("检测Container容器，删除返回值为true的Target，可读取@Target参数")
            .addContainerEntry("被检测的Container容器")
            .addEntry("方法体返回布尔类型", Type.BOOLEAN)
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

    @KetherParser(["contain"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionContain() = combinationParser(
        Action.new("Container容器", "检测包含", "contain", true)
            .description("检测Container容器中是否包含另一个容器中的目标，或是否包含实体")
            .addEntry("实体或容器", Type.CONTAINER)
            .addContainerEntry("被检测的容器")
            .result("是否包含", Type.BOOLEAN)
    ) {
        it.group(
            container(),
            theyContainer(true)
        ).apply(it) { entities, container ->
            now {
                val container = container.orElse(self())
                entities?.all<ITargetEntity<*>> { entity ->
                    container.any<ITargetEntity<*>> { targetEntity ->
                        targetEntity.entity.uniqueId == entity.entity.uniqueId
                    }
                } ?: true
            }
        }
    }

    @KetherParser(["stream"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionStream() = combinationParser(
        Action.new("Container容器", "流过流式选择器", "stream", true)
            .description("将指定容器流过流式选择器")
            .addEntry("选择器文本", Type.STRING)
            .addContainerEntry("被检测的Container容器")
            .result("经过流后的容器", Type.CONTAINER)
    ) {
        it.group(
            text(),
            theyContainer(false)
        ).apply(it) { selector, container ->
            now {
                StringParser(selector).stream(container!!, script())
            }
        }
    }

    private fun containerProperty() = object : ScriptProperty<IContainer>("orryx.container.operator") {

        override fun read(instance: IContainer, key: String): OpenResult {
            return when (key) {
                "list" -> OpenResult.successful(instance.targets)
                "length", "size" -> OpenResult.successful(instance.targets.size)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IContainer, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }
}