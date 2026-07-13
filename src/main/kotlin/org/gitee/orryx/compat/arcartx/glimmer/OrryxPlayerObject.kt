package org.gitee.orryx.compat.arcartx.glimmer

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import priv.seventeen.artist.arcartx.glimmer.annotations.GlimmerInvokeHandler
import priv.seventeen.artist.arcartx.glimmer.annotations.GlimmerObjectConstructor
import priv.seventeen.artist.arcartx.glimmer.callable.InvocationData
import priv.seventeen.artist.arcartx.glimmer.`object`.IGlimmerObject
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.adaptPlayer

@Ghost
class OrryxPlayerObject : IGlimmerObject {

    private val player: Player?

    @GlimmerObjectConstructor("OrryxPlayer")
    constructor(data: InvocationData) {
        player = data.playerArgument()
    }

    override fun stringValue(): String {
        return player?.name ?: "null"
    }

    override fun getTypeName(): String {
        return "OrryxPlayer"
    }

    @GlimmerInvokeHandler("getMana")
    fun getMana(data: InvocationData): Double {
        val p = player ?: return 0.0
        return Orryx.api().consumptionValueAPI.manaInstance.getMana(p)
            .glimmerNow(0.0, "OrryxPlayer.getMana")
    }

    @GlimmerInvokeHandler("getMaxMana")
    fun getMaxMana(data: InvocationData): Double {
        val p = player ?: return 0.0
        return Orryx.api().consumptionValueAPI.manaInstance.getMaxMana(p)
            .glimmerNow(0.0, "OrryxPlayer.getMaxMana")
    }

    @GlimmerInvokeHandler("getSpirit")
    fun getSpirit(data: InvocationData): Double {
        val p = player ?: return 0.0
        return Orryx.api().consumptionValueAPI.spiritInstance.getSpirit(p)
    }

    @GlimmerInvokeHandler("getMaxSpirit")
    fun getMaxSpirit(data: InvocationData): Double {
        val p = player ?: return 0.0
        return Orryx.api().consumptionValueAPI.spiritInstance.getMaxSpirit(p)
            .glimmerNow(0.0, "OrryxPlayer.getMaxSpirit")
    }

    @GlimmerInvokeHandler("getJob")
    fun getJob(data: InvocationData): String {
        val p = player ?: return ""
        return Orryx.api().profileAPI.modifyProfile(p) { it.job }
            .glimmerNow(null, "OrryxPlayer.getJob") ?: ""
    }

    @GlimmerInvokeHandler("getJobLevel")
    fun getJobLevel(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().jobAPI.modifyJob(p) { it.level }
            .glimmerNow(null, "OrryxPlayer.getJobLevel") ?: 0
    }

    @GlimmerInvokeHandler("getJobMaxLevel")
    fun getJobMaxLevel(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().jobAPI.modifyJob(p) { it.maxLevel }
            .glimmerNow(null, "OrryxPlayer.getJobMaxLevel") ?: 0
    }

    @GlimmerInvokeHandler("getExperience")
    fun getExperience(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().jobAPI.modifyJob(p) { it.experienceOfLevel }
            .glimmerNow(null, "OrryxPlayer.getExperience") ?: 0
    }

    @GlimmerInvokeHandler("getMaxExperience")
    fun getMaxExperience(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().jobAPI.modifyJob(p) { it.maxExperienceOfLevel }
            .glimmerNow(null, "OrryxPlayer.getMaxExperience") ?: 0
    }

    @GlimmerInvokeHandler("getPoint")
    fun getPoint(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().profileAPI.modifyProfile(p) { it.point }
            .glimmerNow(null, "OrryxPlayer.getPoint") ?: 0
    }

    @GlimmerInvokeHandler("getSkillLevel")
    fun getSkillLevel(data: InvocationData): Int {
        val p = player ?: return 0
        val skill = data.nonBlankStringArgument(0) ?: return 0
        return Orryx.api().skillAPI.modifySkill(p, skill) { it.level }
            .glimmerNow(null, "OrryxPlayer.getSkillLevel") ?: 0
    }

    @GlimmerInvokeHandler("isSkillLocked")
    fun isSkillLocked(data: InvocationData): Boolean {
        val p = player ?: return true
        val skill = data.nonBlankStringArgument(0) ?: return true
        return Orryx.api().skillAPI.modifySkill(p, skill) { it.locked }
            .glimmerNow(null, "OrryxPlayer.isSkillLocked") ?: true
    }

    @GlimmerInvokeHandler("isSuperBody")
    fun isSuperBody(data: InvocationData): Boolean {
        val p = player ?: return false
        return Orryx.api().profileAPI.isSuperBody(p)
    }

    @GlimmerInvokeHandler("isInvincible")
    fun isInvincible(data: InvocationData): Boolean {
        val p = player ?: return false
        return Orryx.api().profileAPI.isInvincible(p)
    }

    @GlimmerInvokeHandler("isSilence")
    fun isSilence(data: InvocationData): Boolean {
        val p = player ?: return false
        return Orryx.api().profileAPI.isSilence(p)
    }

    @GlimmerInvokeHandler("getSkillCooldown")
    fun getSkillCooldown(data: InvocationData): Long {
        val p = player ?: return 0L
        val skill = data.nonBlankStringArgument(0) ?: return 0L
        return Orryx.api().timerAPI.skillTimer.getCountdown(adaptPlayer(p), skill)
    }

    @GlimmerInvokeHandler("castSkill")
    fun castSkill(data: InvocationData) {
        val p = player ?: return
        val skill = data.nonBlankStringArgument(0) ?: return
        val level = data.nonNegativeIntArgument(1) ?: return
        if (Orryx.api().skillAPI.getSkill(skill) == null) return
        glimmerOperation("OrryxPlayer.castSkill") {
            Orryx.api().skillAPI.castSkill(p, skill, level)
        }
    }

    @GlimmerInvokeHandler("getGroup")
    fun getGroup(data: InvocationData): String {
        val p = player ?: return ""
        return Orryx.api().jobAPI.modifyJob(p) { it.group }
            .glimmerNow(null, "OrryxPlayer.getGroup") ?: ""
    }

    @GlimmerInvokeHandler("getFlag")
    fun getFlag(data: InvocationData): String {
        val p = player ?: return ""
        val flagName = data.nonBlankStringArgument(0) ?: return ""
        return Orryx.api().profileAPI.modifyProfile(p) {
            it.getFlag(flagName)?.toString()
        }.glimmerNow(null, "OrryxPlayer.getFlag") ?: ""
    }
}
