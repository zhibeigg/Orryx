package org.gitee.orryx.core.kether.actions.game.entity

import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.NAMESPACE
import org.gitee.orryx.utils.containerOrSelf
import org.gitee.orryx.utils.firstInstance
import org.gitee.orryx.utils.nextTheyContainer
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.*

object EntityActions {

    @KetherParser(["entity"], namespace = NAMESPACE)
    private fun entityParser() = scriptParser {
        arrayOf(
            Action.new("Entity实体操作", "获取实体参数", "entity", true)
                .description("获取实体参数")
                .addEntry("实体参数key", Type.STRING, default = "uuid")
                .result("指定参数", Type.ANY),
        )
        it.switch {
            other {
                fieldGet(this)
            }
        }
    }

    private fun fieldGet(reader: QuestReader): ScriptAction<Any?> {
        return try {
            reader.mark()
            val expect = reader.expects(*EntityField.fields().toTypedArray())
            val they = reader.nextTheyContainer()
            actionTake {
                containerOrSelf(they) {
                    val entity = it.firstInstance<ITargetEntity<*>>().entity
                    EntityField.valueOf(expect.uppercase(Locale.getDefault())).get(entity)
                }
            }
        } catch (_: Throwable) {
            reader.reset()
            error("entity field not found")
        }
    }

}