# Bloom 配置系统实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现服务端 Bloom 配置系统，支持 YAML 配置、玩家登录延迟同步、热重载推送。

**Architecture:** 新建 `BloomConfigManager` 管理配置加载和同步，扩展 `PluginMessageHandler` 添加数据包发送方法，监听 `PlayerJoinEvent` 延迟同步。

**Tech Stack:** Kotlin, TabooLib (Config, Awake, SubscribeEvent), Bukkit Plugin Message API

---

### Task 1: 添加 PacketType 定义

**Files:**
- Modify: `src/main/kotlin/org/gitee/orryx/core/message/PluginMessageHandler.kt:41-56`

**Step 1: 在 PacketType sealed class 中添加三个新类型**

在 `SectorShockwave` 后添加：

```kotlin
data object BloomConfigSync : PacketType(15)
data object BloomConfigUpdate : PacketType(16)
data object BloomConfigRemove : PacketType(17)
```

**Step 2: 验证**

检查代码无语法错误。

**Step 3: Commit**

```bash
git add src/main/kotlin/org/gitee/orryx/core/message/PluginMessageHandler.kt
git commit -m "feat(message): 添加 Bloom 配置数据包类型 (ID 15-17)"
```

---

### Task 2: 创建 BloomConfig 数据类

**Files:**
- Create: `src/main/kotlin/org/gitee/orryx/core/message/bloom/BloomConfig.kt`

**Step 1: 创建数据类文件**

```kotlin
package org.gitee.orryx.core.message.bloom

data class BloomConfig(
    val id: String,
    val name: String,
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int,
    val strength: Float,
    val radius: Float,
    val priority: Int
)
```

**Step 2: 验证**

检查代码无语法错误。

**Step 3: Commit**

```bash
git add src/main/kotlin/org/gitee/orryx/core/message/bloom/BloomConfig.kt
git commit -m "feat(bloom): 添加 BloomConfig 数据类"
```

---

### Task 3: 创建 BloomConfigManager 基础结构

**Files:**
- Create: `src/main/kotlin/org/gitee/orryx/core/message/bloom/BloomConfigManager.kt`

**Step 1: 创建管理器基础结构**

```kotlin
package org.gitee.orryx.core.message.bloom

import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import java.util.concurrent.ConcurrentHashMap

object BloomConfigManager {

    @Config("bloom.yml")
    lateinit var config: ConfigFile
        private set

    private var syncDelay = 40L
    private val configs = ConcurrentHashMap<String, BloomConfig>()

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        config.reload()
        syncDelay = config.getLong("sync-delay", 40L)
        configs.clear()
        config.getConfigurationSection("configs")?.getKeys(false)?.forEach { id ->
            val section = config.getConfigurationSection("configs.$id") ?: return@forEach
            val color = section.getIntegerList("color")
            if (color.size < 4) return@forEach
            configs[id] = BloomConfig(
                id = id,
                name = section.getString("name", "")!!,
                r = color[0],
                g = color[1],
                b = color[2],
                a = color[3],
                strength = section.getDouble("strength", 1.0).toFloat(),
                radius = section.getDouble("radius", 30.0).toFloat(),
                priority = section.getInt("priority", 0)
            )
        }
        consoleMessage("&e┣&7Bloom configs loaded &e${configs.size} &a√")
    }

    fun getConfigs(): Map<String, BloomConfig> = configs

    fun getSyncDelay(): Long = syncDelay
}
```

**Step 2: 验证**

检查代码无语法错误。

**Step 3: Commit**

```bash
git add src/main/kotlin/org/gitee/orryx/core/message/bloom/BloomConfigManager.kt
git commit -m "feat(bloom): 添加 BloomConfigManager 配置加载"
```

---

### Task 4: 添加数据包发送方法到 PluginMessageHandler

**Files:**
- Modify: `src/main/kotlin/org/gitee/orryx/core/message/PluginMessageHandler.kt`

**Step 1: 在 `sendSectorShockwave` 方法后添加 Bloom 相关方法**

```kotlin
/**
 * 同步所有 Bloom 配置到玩家
 * @param player 目标玩家
 * @param configs 配置映射
 */
fun sendBloomConfigSync(player: Player, configs: Map<String, BloomConfig>) {
    sendDataPacket(player, PacketType.BloomConfigSync) {
        writeInt(configs.size)
        configs.forEach { (id, config) ->
            writeUTF(id)
            writeUTF(config.name)
            writeInt(config.r)
            writeInt(config.g)
            writeInt(config.b)
            writeInt(config.a)
            writeFloat(config.strength)
            writeFloat(config.radius)
            writeInt(config.priority)
        }
    }
}

/**
 * 更新单个 Bloom 配置
 * @param player 目标玩家
 * @param config 配置
 */
fun sendBloomConfigUpdate(player: Player, config: BloomConfig) {
    sendDataPacket(player, PacketType.BloomConfigUpdate) {
        writeUTF(config.id)
        writeUTF(config.name)
        writeInt(config.r)
        writeInt(config.g)
        writeInt(config.b)
        writeInt(config.a)
        writeFloat(config.strength)
        writeFloat(config.radius)
        writeInt(config.priority)
    }
}

/**
 * 删除 Bloom 配置
 * @param player 目标玩家
 * @param id 配置ID
 */
fun sendBloomConfigRemove(player: Player, id: String) {
    sendDataPacket(player, PacketType.BloomConfigRemove) {
        writeUTF(id)
    }
}
```

**Step 2: 添加 import**

在文件顶部添加：
```kotlin
import org.gitee.orryx.core.message.bloom.BloomConfig
```

**Step 3: 验证**

检查代码无语法错误。

**Step 4: Commit**

```bash
git add src/main/kotlin/org/gitee/orryx/core/message/PluginMessageHandler.kt
git commit -m "feat(message): 添加 Bloom 配置数据包发送方法"
```

---

### Task 5: 添加玩家登录同步和热重载推送

**Files:**
- Modify: `src/main/kotlin/org/gitee/orryx/core/message/bloom/BloomConfigManager.kt`

**Step 1: 添加 import**

```kotlin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.gitee.orryx.core.message.PluginMessageHandler
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
```

**Step 2: 在 `load()` 方法末尾添加热重载推送**

在 `consoleMessage` 之后添加：
```kotlin
// 热重载时向所有在线玩家推送
Bukkit.getOnlinePlayers().forEach { syncToPlayer(it) }
```

**Step 3: 添加同步方法和事件监听**

在 `getSyncDelay()` 方法后添加：

```kotlin
fun syncToPlayer(player: Player) {
    PluginMessageHandler.sendBloomConfigSync(player, configs)
}

@SubscribeEvent
private fun onPlayerJoin(e: PlayerJoinEvent) {
    if (configs.isEmpty()) return
    submit(delay = syncDelay) {
        if (e.player.isOnline) {
            syncToPlayer(e.player)
        }
    }
}
```

**Step 4: 验证**

检查代码无语法错误。

**Step 5: Commit**

```bash
git add src/main/kotlin/org/gitee/orryx/core/message/bloom/BloomConfigManager.kt
git commit -m "feat(bloom): 添加玩家登录延迟同步和热重载推送"
```

---

### Task 6: 创建默认配置文件

**Files:**
- Create: `src/main/resources/bloom.yml`

**Step 1: 创建配置文件**

```yaml
# Bloom 配置系统
# 用于控制客户端 OrryxMod 的泛光效果

# 延迟同步时间（tick，20tick = 1秒）
# 玩家登录后等待此时间再发送配置，确保客户端 mod 已加载
sync-delay: 40

# Bloom 配置列表
# 每个配置项的 key 作为唯一标识符
configs:
  # 示例配置（取消注释启用）
  # bloom_fire:
  #   name: "fire"              # 匹配关键词（实体名称包含即匹配，忽略大小写）
  #   color: [255, 100, 0, 255] # RGBA 颜色值 (0-255)
  #   strength: 1.5             # 泛光强度 (0-10)
  #   radius: 30.0              # 渲染距离 (1-128 方块)
  #   priority: 10              # 优先级（数值越大越优先）
  #
  # bloom_ice:
  #   name: "ice"
  #   color: [100, 200, 255, 255]
  #   strength: 1.2
  #   radius: 25.0
  #   priority: 5
```

**Step 2: Commit**

```bash
git add src/main/resources/bloom.yml
git commit -m "feat(bloom): 添加默认配置文件 bloom.yml"
```

---

### Task 7: 最终验证和提交

**Step 1: 构建项目**

```bash
./gradlew build
```

预期：构建成功，无编译错误。

**Step 2: 最终提交**

```bash
git add -A
git commit -m "feat(bloom): 完成 Bloom 配置系统实现"
```
