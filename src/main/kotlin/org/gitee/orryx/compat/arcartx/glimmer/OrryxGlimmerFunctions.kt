package org.gitee.orryx.compat.arcartx.glimmer

import org.bukkit.Bukkit
import org.gitee.orryx.api.Orryx
import priv.seventeen.artist.arcartx.glimmer.annotations.GlimmerInvokeHandler
import priv.seventeen.artist.arcartx.glimmer.annotations.GlimmerNamespace
import priv.seventeen.artist.arcartx.glimmer.callable.InvocationData

@GlimmerNamespace("Orryx")
object OrryxGlimmerFunctions {

    @GlimmerInvokeHandler("getMana")
    @JvmStatic
    fun getMana(data: InvocationData): Double {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0.0
        return Orryx.api().consumptionValueAPI.manaInstance.getMana(player).join()
    }

    @GlimmerInvokeHandler("getMaxMana")
    @JvmStatic
    fun getMaxMana(data: InvocationData): Double {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0.0
        return Orryx.api().consumptionValueAPI.manaInstance.getMaxMana(player).join()
    }

    @GlimmerInvokeHandler("setMana")
    @JvmStatic
    fun setMana(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val mana = data.get(1).stringValue().toDouble()
        Orryx.api().consumptionValueAPI.manaInstance.setMana(player, mana)
    }

    @GlimmerInvokeHandler("giveMana")
    @JvmStatic
    fun giveMana(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val mana = data.get(1).stringValue().toDouble()
        Orryx.api().consumptionValueAPI.manaInstance.giveMana(player, mana)
    }

    @GlimmerInvokeHandler("takeMana")
    @JvmStatic
    fun takeMana(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val mana = data.get(1).stringValue().toDouble()
        Orryx.api().consumptionValueAPI.manaInstance.takeMana(player, mana)
    }

    @GlimmerInvokeHandler("getSpirit")
    @JvmStatic
    fun getSpirit(data: InvocationData): Double {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0.0
        return Orryx.api().consumptionValueAPI.spiritInstance.getSpirit(player)
    }

    @GlimmerInvokeHandler("getMaxSpirit")
    @JvmStatic
    fun getMaxSpirit(data: InvocationData): Double {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0.0
        return Orryx.api().consumptionValueAPI.spiritInstance.getMaxSpirit(player).join()
    }

    @GlimmerInvokeHandler("setSpirit")
    @JvmStatic
    fun setSpirit(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val spirit = data.get(1).stringValue().toDouble()
        Orryx.api().consumptionValueAPI.spiritInstance.setSpirit(player, spirit)
    }

    @GlimmerInvokeHandler("giveSpirit")
    @JvmStatic
    fun giveSpirit(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val spirit = data.get(1).stringValue().toDouble()
        Orryx.api().consumptionValueAPI.spiritInstance.giveSpirit(player, spirit)
    }

    @GlimmerInvokeHandler("takeSpirit")
    @JvmStatic
    fun takeSpirit(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val spirit = data.get(1).stringValue().toDouble()
        Orryx.api().consumptionValueAPI.spiritInstance.takeSpirit(player, spirit)
    }

    @GlimmerInvokeHandler("castSkill")
    @JvmStatic
    fun castSkill(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val skill = data.get(1).stringValue()
        val level = data.get(2).stringValue().toInt()
        Orryx.api().skillAPI.castSkill(player, skill, level)
    }

    @GlimmerInvokeHandler("isSuperBody")
    @JvmStatic
    fun isSuperBody(data: InvocationData): Boolean {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return false
        return Orryx.api().profileAPI.isSuperBody(player)
    }

    @GlimmerInvokeHandler("isInvincible")
    @JvmStatic
    fun isInvincible(data: InvocationData): Boolean {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return false
        return Orryx.api().profileAPI.isInvincible(player)
    }

    @GlimmerInvokeHandler("isSilence")
    @JvmStatic
    fun isSilence(data: InvocationData): Boolean {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return false
        return Orryx.api().profileAPI.isSilence(player)
    }

    @GlimmerInvokeHandler("setSuperBody")
    @JvmStatic
    fun setSuperBody(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val timeout = data.get(1).stringValue().toLong()
        Orryx.api().profileAPI.setSuperBody(player, timeout)
    }

    @GlimmerInvokeHandler("setInvincible")
    @JvmStatic
    fun setInvincible(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val timeout = data.get(1).stringValue().toLong()
        Orryx.api().profileAPI.setInvincible(player, timeout)
    }

    @GlimmerInvokeHandler("setSilence")
    @JvmStatic
    fun setSilence(data: InvocationData) {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return
        val timeout = data.get(1).stringValue().toLong()
        Orryx.api().profileAPI.setSilence(player, timeout)
    }

    @GlimmerInvokeHandler("getSkillCooldown")
    @JvmStatic
    fun getSkillCooldown(data: InvocationData): Long {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0L
        val skill = data.get(1).stringValue()
        return Orryx.api().timerAPI.skillTimer.getCountdown(
            taboolib.common.platform.function.adaptPlayer(player), skill
        )
    }

    @GlimmerInvokeHandler("getPoint")
    @JvmStatic
    fun getPoint(data: InvocationData): Int {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0
        return Orryx.api().profileAPI.modifyProfile(player) { it.point }.join() ?: 0
    }

    @GlimmerInvokeHandler("getJob")
    @JvmStatic
    fun getJob(data: InvocationData): String {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return ""
        return Orryx.api().profileAPI.modifyProfile(player) { it.job }.join() ?: ""
    }

    @GlimmerInvokeHandler("getJobLevel")
    @JvmStatic
    fun getJobLevel(data: InvocationData): Int {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0
        return Orryx.api().jobAPI.modifyJob(player) { it.level }.join() ?: 0
    }

    @GlimmerInvokeHandler("getJobExperience")
    @JvmStatic
    fun getJobExperience(data: InvocationData): Int {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0
        return Orryx.api().jobAPI.modifyJob(player) { it.experienceOfLevel }.join() ?: 0
    }

    @GlimmerInvokeHandler("getJobMaxExperience")
    @JvmStatic
    fun getJobMaxExperience(data: InvocationData): Int {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0
        return Orryx.api().jobAPI.modifyJob(player) { it.maxExperienceOfLevel }.join() ?: 0
    }

    @GlimmerInvokeHandler("getSkillLevel")
    @JvmStatic
    fun getSkillLevel(data: InvocationData): Int {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return 0
        val skill = data.get(1).stringValue()
        return Orryx.api().skillAPI.modifySkill(player, skill) { it.level }.join() ?: 0
    }

    @GlimmerInvokeHandler("isSkillLocked")
    @JvmStatic
    fun isSkillLocked(data: InvocationData): Boolean {
        val player = Bukkit.getPlayerExact(data.get(0).stringValue()) ?: return true
        val skill = data.get(1).stringValue()
        return Orryx.api().skillAPI.modifySkill(player, skill) { it.locked }.join() ?: true
    }
}
