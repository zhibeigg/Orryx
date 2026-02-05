package org.gitee.orryx.core.kether.actions.game

import org.bukkit.inventory.ItemStack
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Property
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.OpenResult
import taboolib.module.kether.*
import taboolib.module.nms.ItemTag
import taboolib.module.nms.getItemTag
import taboolib.platform.util.isAir
import java.util.concurrent.CompletableFuture

object ItemActions {

    init {
        registerProperty(
            itemTagProperty(),
            Property.new("Game原版游戏", "ItemTag", "orryx.player.itemtag.operator")
                .description("物品NBT标签对象，支持读写任意NBT键值")
                .addEntry("<任意键>", Type.ANY, "读取/写入指定键的NBT数据", true),
            ItemTag::class.java
        )
    }

    @KetherParser(["itemInMainHand"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionItemInMainHand() = combinationParser(
        Action.new("Game原版游戏", "获取玩家主手物品", "itemInMainHand", true)
            .description("获取玩家主手物品")
            .addContainerEntry(optional = true, default = "@self")
            .result("物品", Type.ITEM_STACK)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstance<PlayerTarget>()
                CompletableFuture.completedFuture(player.getSource().inventory.itemInMainHand)
            }
        }
    }

    @KetherParser(["itemInOffHand"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionItemInOffHand() = combinationParser(
        Action.new("Game原版游戏", "获取玩家副手物品", "itemInOffHand", true)
            .description("获取玩家副手物品")
            .addContainerEntry(optional = true, default = "@self")
            .result("物品", Type.ITEM_STACK)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstance<PlayerTarget>()
                CompletableFuture.completedFuture(player.getSource().inventory.itemInOffHand)
            }
        }
    }

    @KetherParser(["itemOnCursor"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionItemOnCursor() = combinationParser(
        Action.new("Game原版游戏", "获取玩家光标物品", "itemOnCursor", true)
            .description("获取玩家光标物品")
            .addContainerEntry(optional = true, default = "@self")
            .result("物品", Type.ITEM_STACK)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstance<PlayerTarget>()
                CompletableFuture.completedFuture(player.getSource().itemOnCursor)
            }
        }
    }

    @KetherParser(["helmet"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionHelmet() = combinationParser(
        Action.new("Game原版游戏", "获取玩家头上物品", "helmet", true)
            .description("获取玩家光标物品")
            .addContainerEntry(optional = true, default = "@self")
            .result("物品", Type.ITEM_STACK)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstance<PlayerTarget>()
                CompletableFuture.completedFuture(player.getSource().inventory.helmet)
            }
        }
    }

    @KetherParser(["chestplate"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionChestplate() = combinationParser(
        Action.new("Game原版游戏", "获取玩家胸上物品", "chestplate", true)
            .description("获取玩家胸上物品")
            .addContainerEntry(optional = true, default = "@self")
            .result("物品", Type.ITEM_STACK)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstance<PlayerTarget>()
                CompletableFuture.completedFuture(player.getSource().inventory.chestplate)
            }
        }
    }

    @KetherParser(["leggings"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionLeggings() = combinationParser(
        Action.new("Game原版游戏", "获取玩家腿上物品", "leggings", true)
            .description("获取玩家腿上物品")
            .addContainerEntry(optional = true, default = "@self")
            .result("物品", Type.ITEM_STACK)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstance<PlayerTarget>()
                CompletableFuture.completedFuture(player.getSource().inventory.leggings)
            }
        }
    }

    @KetherParser(["boots"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionBoots() = combinationParser(
        Action.new("Game原版游戏", "获取玩家脚上物品", "boots", true)
            .description("获取玩家脚上物品")
            .addContainerEntry(optional = true, default = "@self")
            .result("物品", Type.ITEM_STACK)
    ) {
        it.group(
            theyContainer(true)
        ).apply(it) { container ->
            future {
                val player = container.orElse(self()).firstInstance<PlayerTarget>()
                CompletableFuture.completedFuture(player.getSource().inventory.boots)
            }
        }
    }

    @KetherParser(["nbt"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun nbt() = scriptParser(
        Action.new("Game原版游戏", "获取物品 nbt", "nbt", true)
            .description("获取物品 nbt")
            .addEntry("物品", Type.ITEM_STACK)
            .result("物品 nbt", Type.NBT)
            .example("set a to nbt itemInMainHand")
            .example("tell &a[key]")
            .example("set &a[key] to 极品")
    ) {
        val itemStack = it.nextParsedAction()
        actionFuture { f ->
            run(itemStack).thenAccept { itemStack ->
                val itemStack = itemStack as? ItemStack
                f.complete(itemStack.takeUnless { item -> item.isAir }?.getItemTag())
            }
        }
    }


    /**
     * ItemTag 的 ScriptProperty。
     *
     * 支持读写任意 NBT 键值：
     * - 读取: 返回指定键的 NBT 数据
     * - 写入: 设置指定键的 NBT 数据（主线程同步执行）
     */
    private fun itemTagProperty() = object : ScriptProperty<ItemTag>("orryx.player.itemtag.operator") {

        override fun read(instance: ItemTag, key: String): OpenResult {
            return OpenResult.successful(instance[key]?.unsafeData())
        }

        override fun write(instance: ItemTag, key: String, value: Any?): OpenResult {
            ensureSync { instance[key] = value }
            return OpenResult.successful()
        }
    }
}