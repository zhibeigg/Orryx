package org.gitee.orryx.compat.arcartx.glimmer

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import priv.seventeen.artist.arcartx.glimmer.annotations.GlimmerInvokeHandler
import priv.seventeen.artist.arcartx.glimmer.annotations.GlimmerObjectConstructor
import priv.seventeen.artist.arcartx.glimmer.callable.InvocationData
import priv.seventeen.artist.arcartx.glimmer.`object`.IGlimmerObject

class OrryxPlayerObject: IGlimmerObject {

    private val player: Player?

    @GlimmerObjectConstructor("OrryxPlayer")
    constructor(data: InvocationData) {
        player = if (data.size() >= 1) {
            Bukkit.getPlayerExact(data.get(0).stringValue())
        } else {
            null
        }
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
        return Orryx.api().consumptionValueAPI.manaInstance.getMana(p).join()
    }

    @GlimmerInvokeHandler("getMaxMana")
    fun getMaxMana(data: InvocationData): Double {
        val p = player ?: return 0.0
        return Orryx.api().consumptionValueAPI.manaInstance.getMaxMana(p).join()
    }

    @GlimmerInvokeHandler("getSpirit")
    fun getSpirit(data: InvocationData): Double {
        val p = player ?: return 0.0
        return Orryx.api().consumptionValueAPI.spiritInstance.getSpirit(p)
    }

    @GlimmerInvokeHandler("getMaxSpirit")
    fun getMaxSpirit(data: InvocationData): Double {
        val p = player ?: return 0.0
        return Orryx.api().consumptionValueAPI.spiritInstance.getMaxSpirit(p).join()
    }

    @GlimmerInvokeHandler("getJob")
    fun getJob(data: InvocationData): String {
        val p = player ?: return ""
        return Orryx.api().profileAPI.modifyProfile(p) { it.job }.join() ?: ""
    }

    @GlimmerInvokeHandler("getJobLevel")
    fun getJobLevel(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().jobAPI.modifyJob(p) { it.level }.join() ?: 0
    }

    @GlimmerInvokeHandler("getJobMaxLevel")
    fun getJobMaxLevel(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().jobAPI.modifyJob(p) { it.maxLevel }.join() ?: 0
    }

    @GlimmerInvokeHandler("getExperience")
    fun getExperience(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().jobAPI.modifyJob(p) { it.experienceOfLevel }.join() ?: 0
    }

    @GlimmerInvokeHandler("getMaxExperience")
    fun getMaxExperience(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().jobAPI.modifyJob(p) { it.maxExperienceOfLevel }.join() ?: 0
    }

    @GlimmerInvokeHandler("getPoint")
    fun getPoint(data: InvocationData): Int {
        val p = player ?: return 0
        return Orryx.api().profileAPI.modifyProfile(p) { it.point }.join() ?: 0
    }

    @GlimmerInvokeHandler("getSkillLevel")
    fun getSkillLevel(data: InvocationData): Int {
        val p = player ?: return 0
        val skill = data.get(0).stringValue()
        return Orryx.api().skillAPI.modifySkill(p, skill) { it.level }.join() ?: 0
    }

    @GlimmerInvokeHandler("isSkillLocked")
    fun isSkillLocked(data: InvocationData): Boolean {
        val p = player ?: return true
        val skill = data.get(0).stringValue()
        return Orryx.api().skillAPI.modifySkill(p, skill) { it.locked }.join() ?: true
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
        val skill = data.get(0).stringValue()
        return Orryx.api().timerAPI.skillTimer.getCountdown(
            taboolib.common.platform.function.adaptPlayer(p), skill
        )
    }

    @GlimmerInvokeHandler("castSkill")
    fun castSkill(data: InvocationData) {
        val p = player ?: return
        val skill = data.get(0).stringValue()
        val level = data.get(1).stringValue().toInt()
        Orryx.api().skillAPI.castSkill(p, skill, level)
    }

    @GlimmerInvokeHandler("getGroup")
    fun getGroup(data: InvocationData): String {
        val p = player ?: return ""
        return Orryx.api().jobAPI.modifyJob(p) { it.group }.join() ?: ""
    }

    @GlimmerInvokeHandler("getFlag")
    fun getFlag(data: InvocationData): String {
        val p = player ?: return ""
        val flagName = data.get(0).stringValue()
        return Orryx.api().profileAPI.modifyProfile(p) {
            it.getFlag(flagName)?.toString()
        }.join() ?: ""
    }
}