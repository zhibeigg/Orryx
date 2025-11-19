/**
 * 示例脚本 4: 物品给予
 *
 * 演示物品操作和背包管理
 */

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

player?.let { p ->
    // 获取参数
    val itemType = context["itemType"] as? String ?: "DIAMOND"
    val amount = context["amount"] as? Int ?: 1
    val itemName = context["itemName"] as? String
    val lore = context["lore"] as? List<*>

    // 创建物品
    val material = try {
        Material.valueOf(itemType.uppercase())
    } catch (e: IllegalArgumentException) {
        p.sendMessage("§c无效的物品类型: $itemType")
        return@let "物品给予失败"
    }

    val item = ItemStack(material, amount)

    // 设置物品元数据
    val meta = item.itemMeta
    meta?.let { m ->
        itemName?.let { m.setDisplayName(it) }
        lore?.let { m.lore = it.map { it.toString() } }
        item.itemMeta = m
    }

    // 给予物品
    val notFit = p.inventory.addItem(item)

    if (notFit.isEmpty()) {
        p.sendMessage("§a已给予物品: ${itemName ?: material.name} x$amount")
        context["success"] = true
        "物品给予成功"
    } else {
        p.sendMessage("§c背包空间不足")
        context["success"] = false
        "背包空间不足"
    }
} ?: "此脚本需要玩家执行"
