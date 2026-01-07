# Bloom 配置系统设计

## 概述

为 Orryx 插件添加 Bloom（泛光）配置系统，支持服务端通过 YAML 配置文件管理客户端的泛光效果，玩家登录时自动同步配置。

## 数据包协议

| ID | 名称 | 方向 | 说明 |
|----|------|------|------|
| 15 | BloomConfigSync | 服务端 → 客户端 | 全量同步所有配置 |
| 16 | BloomConfigUpdate | 服务端 → 客户端 | 增量更新单个配置 |
| 17 | BloomConfigRemove | 服务端 → 客户端 | 删除指定配置 |

## 配置文件

**路径**: `plugins/Orryx/bloom.yml`

```yaml
# 延迟同步时间（tick，20tick = 1秒）
sync-delay: 40

# Bloom 配置列表
configs:
  bloom_fire:
    name: "fire"
    color: [255, 100, 0, 255]
    strength: 1.5
    radius: 30.0
    priority: 10

  bloom_ice:
    name: "ice"
    color: [100, 200, 255, 255]
    strength: 1.2
    radius: 25.0
    priority: 5
```

### 配置项说明

| 字段 | 类型 | 范围 | 说明 |
|------|------|------|------|
| name | String | - | 匹配关键词（实体名称包含即匹配，忽略大小写） |
| color | Int[4] | 0-255 | RGBA 颜色值 |
| strength | Float | 0-10 | 泛光强度 |
| radius | Float | 1-128 | 渲染距离（方块） |
| priority | Int | - | 优先级（数值越大越优先） |

## 数据结构

```kotlin
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

## 文件结构

```
src/main/kotlin/org/gitee/orryx/core/message/
├── PluginMessageHandler.kt  (修改)
└── bloom/
    ├── BloomConfig.kt       (新增)
    └── BloomConfigManager.kt (新增)
```

## 核心实现

### PacketType 扩展

```kotlin
// 在 PluginMessageHandler.PacketType 中添加
data object BloomConfigSync : PacketType(15)
data object BloomConfigUpdate : PacketType(16)
data object BloomConfigRemove : PacketType(17)
```

### BloomConfigManager

```kotlin
object BloomConfigManager {
    private var syncDelay = 40L
    private val configs = ConcurrentHashMap<String, BloomConfig>()

    // 加载配置
    @Awake(LifeCycle.ENABLE)
    fun load() { ... }

    // 热重载支持
    @Reload(1)
    fun reload() { ... }

    // 向单个玩家同步所有配置
    fun syncToPlayer(player: Player) { ... }

    // 向所有在线玩家同步
    fun syncToAll() { ... }
}
```

## 触发时机

1. **插件启用** → 加载 `bloom.yml` 配置
2. **玩家登录** → 延迟 `sync-delay` tick 后自动同步
3. **执行 `/orryx reload`** → 重新加载配置并向所有在线玩家推送

## 数据包格式

### BloomConfigSync (ID: 15)

```
[15: Int] [count: Int] [configs: ...]

configs 格式（重复 count 次）：
[id: String] [name: String] [r: Int] [g: Int] [b: Int] [a: Int]
[strength: Float] [radius: Float] [priority: Int]
```

### BloomConfigUpdate (ID: 16)

```
[16: Int] [id: String] [name: String] [r: Int] [g: Int] [b: Int] [a: Int]
[strength: Float] [radius: Float] [priority: Int]
```

### BloomConfigRemove (ID: 17)

```
[17: Int] [id: String]
```
