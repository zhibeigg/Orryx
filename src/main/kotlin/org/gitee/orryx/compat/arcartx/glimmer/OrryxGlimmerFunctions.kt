package org.gitee.orryx.compat.arcartx.glimmer

import org.gitee.orryx.api.Orryx
import priv.seventeen.artist.arcartx.glimmer.annotations.GlimmerInvokeHandler
import priv.seventeen.artist.arcartx.glimmer.annotations.GlimmerNamespace
import priv.seventeen.artist.arcartx.glimmer.callable.InvocationData
import taboolib.common.platform.function.adaptPlayer

@GlimmerNamespace("Orryx")
object OrryxGlimmerFunctions {

    @GlimmerInvokeHandler("getMana")
    @JvmStatic
    fun getMana(data: InvocationData): Double {
        val player = data.playerArgument() ?: return 0.0
        return Orryx.api().consumptionValueAPI.manaInstance.getMana(player)
            .glimmerNow(0.0, "getMana")
    }

    @GlimmerInvokeHandler("getMaxMana")
    @JvmStatic
    fun getMaxMana(data: InvocationData): Double {
        val player = data.playerArgument() ?: return 0.0
        return Orryx.api().consumptionValueAPI.manaInstance.getMaxMana(player)
            .glimmerNow(0.0, "getMaxMana")
    }

    @GlimmerInvokeHandler("setMana")
    @JvmStatic
    fun setMana(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val mana = data.finiteDoubleArgument(1)?.takeIf { it >= 0.0 } ?: return
        glimmerFireAndForget("setMana") {
            Orryx.api().consumptionValueAPI.manaInstance.setMana(player, mana)
        }
    }

    @GlimmerInvokeHandler("giveMana")
    @JvmStatic
    fun giveMana(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val mana = data.finiteDoubleArgument(1)?.takeIf { it >= 0.0 } ?: return
        glimmerFireAndForget("giveMana") {
            Orryx.api().consumptionValueAPI.manaInstance.giveMana(player, mana)
        }
    }

    @GlimmerInvokeHandler("takeMana")
    @JvmStatic
    fun takeMana(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val mana = data.finiteDoubleArgument(1)?.takeIf { it >= 0.0 } ?: return
        glimmerFireAndForget("takeMana") {
            Orryx.api().consumptionValueAPI.manaInstance.takeMana(player, mana)
        }
    }

    @GlimmerInvokeHandler("getSpirit")
    @JvmStatic
    fun getSpirit(data: InvocationData): Double {
        val player = data.playerArgument() ?: return 0.0
        return Orryx.api().consumptionValueAPI.spiritInstance.getSpirit(player)
    }

    @GlimmerInvokeHandler("getMaxSpirit")
    @JvmStatic
    fun getMaxSpirit(data: InvocationData): Double {
        val player = data.playerArgument() ?: return 0.0
        return Orryx.api().consumptionValueAPI.spiritInstance.getMaxSpirit(player)
            .glimmerNow(0.0, "getMaxSpirit")
    }

    @GlimmerInvokeHandler("setSpirit")
    @JvmStatic
    fun setSpirit(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val spirit = data.finiteDoubleArgument(1)?.takeIf { it >= 0.0 } ?: return
        glimmerFireAndForget("setSpirit") {
            Orryx.api().consumptionValueAPI.spiritInstance.setSpirit(player, spirit)
        }
    }

    @GlimmerInvokeHandler("giveSpirit")
    @JvmStatic
    fun giveSpirit(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val spirit = data.finiteDoubleArgument(1)?.takeIf { it >= 0.0 } ?: return
        glimmerFireAndForget("giveSpirit") {
            Orryx.api().consumptionValueAPI.spiritInstance.giveSpirit(player, spirit)
        }
    }

    @GlimmerInvokeHandler("takeSpirit")
    @JvmStatic
    fun takeSpirit(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val spirit = data.finiteDoubleArgument(1)?.takeIf { it >= 0.0 } ?: return
        glimmerFireAndForget("takeSpirit") {
            Orryx.api().consumptionValueAPI.spiritInstance.takeSpirit(player, spirit)
        }
    }

    @GlimmerInvokeHandler("castSkill")
    @JvmStatic
    fun castSkill(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val skill = data.nonBlankStringArgument(1) ?: return
        val level = data.nonNegativeIntArgument(2) ?: return
        if (Orryx.api().skillAPI.getSkill(skill) == null) return
        glimmerOperation("castSkill") {
            Orryx.api().skillAPI.castSkill(player, skill, level)
        }
    }

    @GlimmerInvokeHandler("isSuperBody")
    @JvmStatic
    fun isSuperBody(data: InvocationData): Boolean {
        val player = data.playerArgument() ?: return false
        return Orryx.api().profileAPI.isSuperBody(player)
    }

    @GlimmerInvokeHandler("isInvincible")
    @JvmStatic
    fun isInvincible(data: InvocationData): Boolean {
        val player = data.playerArgument() ?: return false
        return Orryx.api().profileAPI.isInvincible(player)
    }

    @GlimmerInvokeHandler("isSilence")
    @JvmStatic
    fun isSilence(data: InvocationData): Boolean {
        val player = data.playerArgument() ?: return false
        return Orryx.api().profileAPI.isSilence(player)
    }

    @GlimmerInvokeHandler("setSuperBody")
    @JvmStatic
    fun setSuperBody(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val timeout = data.nonNegativeLongArgument(1) ?: return
        glimmerOperation("setSuperBody") {
            Orryx.api().profileAPI.setSuperBody(player, timeout)
        }
    }

    @GlimmerInvokeHandler("setInvincible")
    @JvmStatic
    fun setInvincible(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val timeout = data.nonNegativeLongArgument(1) ?: return
        glimmerOperation("setInvincible") {
            Orryx.api().profileAPI.setInvincible(player, timeout)
        }
    }

    @GlimmerInvokeHandler("setSilence")
    @JvmStatic
    fun setSilence(data: InvocationData) {
        val player = data.playerArgument() ?: return
        val timeout = data.nonNegativeLongArgument(1) ?: return
        glimmerOperation("setSilence") {
            Orryx.api().profileAPI.setSilence(player, timeout)
        }
    }

    @GlimmerInvokeHandler("getSkillCooldown")
    @JvmStatic
    fun getSkillCooldown(data: InvocationData): Long {
        val player = data.playerArgument() ?: return 0L
        val skill = data.nonBlankStringArgument(1) ?: return 0L
        return Orryx.api().timerAPI.skillTimer.getCountdown(adaptPlayer(player), skill)
    }

    @GlimmerInvokeHandler("getPoint")
    @JvmStatic
    fun getPoint(data: InvocationData): Int {
        val player = data.playerArgument() ?: return 0
        return Orryx.api().profileAPI.modifyProfile(player) { it.point }
            .glimmerNow(null, "getPoint") ?: 0
    }

    @GlimmerInvokeHandler("getJob")
    @JvmStatic
    fun getJob(data: InvocationData): String {
        val player = data.playerArgument() ?: return ""
        return Orryx.api().profileAPI.modifyProfile(player) { it.job }
            .glimmerNow(null, "getJob") ?: ""
    }

    @GlimmerInvokeHandler("getJobLevel")
    @JvmStatic
    fun getJobLevel(data: InvocationData): Int {
        val player = data.playerArgument() ?: return 0
        return Orryx.api().jobAPI.modifyJob(player) { it.level }
            .glimmerNow(null, "getJobLevel") ?: 0
    }

    @GlimmerInvokeHandler("getJobExperience")
    @JvmStatic
    fun getJobExperience(data: InvocationData): Int {
        val player = data.playerArgument() ?: return 0
        return Orryx.api().jobAPI.modifyJob(player) { it.experienceOfLevel }
            .glimmerNow(null, "getJobExperience") ?: 0
    }

    @GlimmerInvokeHandler("getJobMaxExperience")
    @JvmStatic
    fun getJobMaxExperience(data: InvocationData): Int {
        val player = data.playerArgument() ?: return 0
        return Orryx.api().jobAPI.modifyJob(player) { it.maxExperienceOfLevel }
            .glimmerNow(null, "getJobMaxExperience") ?: 0
    }

    @GlimmerInvokeHandler("getSkillLevel")
    @JvmStatic
    fun getSkillLevel(data: InvocationData): Int {
        val player = data.playerArgument() ?: return 0
        val skill = data.nonBlankStringArgument(1) ?: return 0
        return Orryx.api().skillAPI.modifySkill(player, skill) { it.level }
            .glimmerNow(null, "getSkillLevel") ?: 0
    }

    @GlimmerInvokeHandler("isSkillLocked")
    @JvmStatic
    fun isSkillLocked(data: InvocationData): Boolean {
        val player = data.playerArgument() ?: return true
        val skill = data.nonBlankStringArgument(1) ?: return true
        return Orryx.api().skillAPI.modifySkill(player, skill) { it.locked }
            .glimmerNow(null, "isSkillLocked") ?: true
    }
}
