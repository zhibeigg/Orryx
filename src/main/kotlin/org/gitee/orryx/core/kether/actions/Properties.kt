package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.spirit.ISpiritManager
import taboolib.common.OpenResult
import taboolib.module.kether.KetherProperty
import taboolib.module.kether.ScriptProperty

object Properties {

    @KetherProperty(bind = IPlayerJob::class, true)
    private fun playerJobProperty() = object : ScriptProperty<IPlayerJob>("orryx.player.job.operator") {

        override fun read(instance: IPlayerJob, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
                "config" -> OpenResult.successful(instance.job)
                "player" -> OpenResult.successful(instance.player)
                "level" -> OpenResult.successful(instance.level)
                "maxLevel" -> OpenResult.successful(instance.maxLevel)
                "experienceOfLevel" -> OpenResult.successful(instance.experienceOfLevel)
                "maxExperienceOfLevel" -> OpenResult.successful(instance.maxExperienceOfLevel)
                "experience" -> OpenResult.successful(instance.experience)
                "binds" -> OpenResult.successful(instance.bindKeyOfGroup)
                "maxMana" -> OpenResult.successful(instance.getMaxMana())
                "regainMana" -> OpenResult.successful(instance.getRegainMana())
                "maxSpirit" -> OpenResult.successful(instance.getMaxSpirit())
                "regainSpirit" -> OpenResult.successful(instance.getRegainSpirit())
                "attributes" -> OpenResult.successful(instance.getAttributes())
                "mana" -> OpenResult.successful(IManaManager.INSTANCE.getMana(instance.player))
                "spirit" -> OpenResult.successful(ISpiritManager.INSTANCE.getSpirit(instance.player))
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IPlayerJob, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    @KetherProperty(bind = IPlayerSkill::class, true)
    private fun playerSkillProperty() = object : ScriptProperty<IPlayerSkill>("orryx.player.skill.operator") {

        override fun read(instance: IPlayerSkill, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
                "job" -> OpenResult.successful(instance.job)
                "player" -> OpenResult.successful(instance.player)
                "level" -> OpenResult.successful(instance.level)
                "config" -> OpenResult.successful(instance.skill)
                "locked" -> OpenResult.successful(instance.locked)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IPlayerSkill, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    @KetherProperty(bind = IPlayerProfile::class, true)
    private fun profileProperty() = object : ScriptProperty<IPlayerProfile>("orryx.player.profile.operator") {

        override fun read(instance: IPlayerProfile, key: String): OpenResult {
            return when(key) {
                "point" -> OpenResult.successful(instance.point)
                "job" -> OpenResult.successful(instance.job)
                "player" -> OpenResult.successful(instance.player)
                "flags" -> OpenResult.successful(instance.flags)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IPlayerProfile, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    @KetherProperty(bind = ISkill::class, true)
    private fun skillProperty() = object : ScriptProperty<ISkill>("orryx.skill.operator") {

        override fun read(instance: ISkill, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
                "name" -> OpenResult.successful(instance.name)
                "type" -> OpenResult.successful(instance.type)
                "minLevel" -> OpenResult.successful(instance.minLevel)
                "maxLevel" -> OpenResult.successful(instance.maxLevel)
                "icon" -> OpenResult.successful(instance.icon)
                "locked" -> OpenResult.successful(instance.isLocked)
                "sort" -> OpenResult.successful(instance.sort)
                "material" -> OpenResult.successful(instance.xMaterial)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: ISkill, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }

    @KetherProperty(bind = IJob::class, true)
    private fun jobProperty() = object : ScriptProperty<IJob>("orryx.player.profile.operator") {

        override fun read(instance: IJob, key: String): OpenResult {
            return when(key) {
                "key" -> OpenResult.successful(instance.key)
                "name" -> OpenResult.successful(instance.name)
                "experience" -> OpenResult.successful(instance.experience)
                "attributes" -> OpenResult.successful(instance.attributes)
                "skills" -> OpenResult.successful(instance.skills)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: IJob, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }
}