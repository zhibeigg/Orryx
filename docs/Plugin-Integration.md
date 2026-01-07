# OrryxMod 插件对接文档

## 概述

OrryxMod 是一个 Minecraft 1.12.2 客户端模组，提供多种增强功能和视觉效果。服务端插件可以通过 Forge 网络通道向客户端发送数据包来控制这些功能。

## 网络通道

- **通道名称**: `orryxmod:main`
- **协议**: 自定义二进制协议
- **方向**: 服务端 → 客户端

---

## 数据包格式

所有数据包遵循以下格式：

```
[packetId: Int] [payload: ...]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| packetId | Int (4 bytes) | 数据包类型标识 |
| payload | ... | 数据包内容，根据类型不同而变化 |

---

## Bloom 配置系统

### 概述

Bloom（泛光）配置系统允许服务端动态配置客户端的泛光效果。支持为不同名称的实体配置不同的泛光颜色、强度和渲染距离。

### 配置项字段

| 字段 | 类型 | 范围 | 说明 |
|------|------|------|------|
| name | String | - | 匹配关键词（实体名称包含此字符串即匹配，忽略大小写） |
| color | Int[4] | 0-255 | RGBA 颜色值 |
| strength | Float | 0-10 | 泛光强度 |
| radius | Float | 1-128 | 渲染距离（方块） |
| priority | Int | - | 优先级（数值越大越优先，多个配置匹配时取最高优先级） |

### 数据包类型

#### BloomConfigSync (ID: 15) - 全量同步

玩家登录时发送，同步所有配置。

**格式：**
```
[15: Int] [count: Int] [configs: ...]
```

**configs 格式（重复 count 次）：**
```
[id: String] [name: String] [r: Int] [g: Int] [b: Int] [a: Int] [strength: Float] [radius: Float] [priority: Int]
```

**示例（Java/Kotlin）：**
```java
ByteArrayDataOutput out = ByteStreams.newDataOutput();
out.writeInt(15);  // packetId
out.writeInt(2);   // 配置数量

// 配置 1: fire
out.writeUTF("bloom_fire");     // id
out.writeUTF("fire");           // name
out.writeInt(255);              // r
out.writeInt(100);              // g
out.writeInt(0);                // b
out.writeInt(255);              // a
out.writeFloat(1.5f);           // strength
out.writeFloat(30.0f);          // radius
out.writeInt(10);               // priority

// 配置 2: ice
out.writeUTF("bloom_ice");      // id
out.writeUTF("ice");            // name
out.writeInt(100);              // r
out.writeInt(200);              // g
out.writeInt(255);              // b
out.writeInt(255);              // a
out.writeFloat(1.2f);           // strength
out.writeFloat(25.0f);          // radius
out.writeInt(5);                // priority

// 发送数据包
sendPluginMessage(player, "orryxmod:main", out.toByteArray());
```

#### BloomConfigUpdate (ID: 16) - 增量更新

运行时添加或更新单个配置。

**格式：**
```
[16: Int] [id: String] [name: String] [r: Int] [g: Int] [b: Int] [a: Int] [strength: Float] [radius: Float] [priority: Int]
```

**示例：**
```java
ByteArrayDataOutput out = ByteStreams.newDataOutput();
out.writeInt(16);               // packetId
out.writeUTF("bloom_fire");     // id
out.writeUTF("fire");           // name
out.writeInt(255);              // r
out.writeInt(100);              // g
out.writeInt(0);                // b
out.writeInt(255);              // a
out.writeFloat(2.0f);           // strength (更新为 2.0)
out.writeFloat(30.0f);          // radius
out.writeInt(10);               // priority

sendPluginMessage(player, "orryxmod:main", out.toByteArray());
```

#### BloomConfigRemove (ID: 17) - 删除配置

删除指定的配置。

**格式：**
```
[17: Int] [id: String]
```

**示例：**
```java
ByteArrayDataOutput out = ByteStreams.newDataOutput();
out.writeInt(17);               // packetId
out.writeUTF("bloom_fire");     // id

sendPluginMessage(player, "orryxmod:main", out.toByteArray());
```

---

## 其他数据包

### 鼠标控制 (ID: 7)

控制客户端鼠标指针显示。

**格式：**
```
[7: Int] [show: Boolean]
```

### 导航系统

#### NavigationStart (ID: 10)

启动导航到指定坐标。

**格式：**
```
[10: Int] [x: Int] [y: Int] [z: Int] [range: Int]
```

| 字段 | 范围 | 说明 |
|------|------|------|
| x, y, z | - | 目标坐标 |
| range | 0-100 | 到达判定范围 |

#### NavigationStop (ID: 11)

停止导航。

**格式：**
```
[11: Int]
```

### 冲击波效果

#### SquareShockwave (ID: 12)

矩形冲击波。

**格式：**
```
[12: Int] [x: Double] [y: Double] [z: Double] [length: Double] [width: Double] [yaw: Double]
```

#### CircleShockwave (ID: 13)

圆形冲击波。

**格式：**
```
[13: Int] [x: Double] [y: Double] [z: Double] [radius: Double]
```

#### SectorShockwave (ID: 14)

扇形冲击波。

**格式：**
```
[14: Int] [x: Double] [y: Double] [z: Double] [radius: Double] [angle: Double] [yaw: Double]
```

### 实体效果

#### GhostEffect (ID: 3)

幽灵残影效果。

**格式：**
```
[3: Int] [uuid: String] [timeout: Long] [density: Int] [gap: Int]
```

| 字段 | 范围 | 说明 |
|------|------|------|
| uuid | - | 实体 UUID |
| timeout | 0-60000 | 持续时间（毫秒） |
| density | 1-50 | 残影密度 |
| gap | 0-20 | 残影间隔 |

#### FlickerEffect (ID: 5)

闪烁效果。

**格式：**
```
[5: Int] [uuid: String] [timeout: Long] [alpha: Float]
```

| 字段 | 范围 | 说明 |
|------|------|------|
| uuid | - | 实体 UUID |
| timeout | 0-60000 | 持续时间（毫秒） |
| alpha | 0-1 | 透明度 |

#### EntityShowAdd (ID: 8)

添加实体展示。

**格式：**
```
[8: Int] [uuid: String] [group: String] [x: Double] [y: Double] [z: Double] [timeout: Long] [rotateX: Float] [rotateY: Float] [rotateZ: Float] [scale: Float] [alpha: Float] [fadeOut: Boolean]
```

#### EntityShowRemove (ID: 9)

移除实体展示。

**格式：**
```
[9: Int] [uuid: String] [group: String]
```

---

## Bukkit/Spigot 插件示例

```java
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class OrryxModIntegration {

    private static final String CHANNEL = "orryxmod:main";

    public static void registerChannel(JavaPlugin plugin) {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    /**
     * 同步所有 Bloom 配置
     */
    public static void syncBloomConfigs(Player player, Map<String, BloomConfig> configs) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(15);  // BloomConfigSync
        out.writeInt(configs.size());

        for (Map.Entry<String, BloomConfig> entry : configs.entrySet()) {
            BloomConfig config = entry.getValue();
            out.writeUTF(entry.getKey());
            out.writeUTF(config.name);
            out.writeInt(config.r);
            out.writeInt(config.g);
            out.writeInt(config.b);
            out.writeInt(config.a);
            out.writeFloat(config.strength);
            out.writeFloat(config.radius);
            out.writeInt(config.priority);
        }

        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    /**
     * 更新单个 Bloom 配置
     */
    public static void updateBloomConfig(Player player, String id, BloomConfig config) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(16);  // BloomConfigUpdate
        out.writeUTF(id);
        out.writeUTF(config.name);
        out.writeInt(config.r);
        out.writeInt(config.g);
        out.writeInt(config.b);
        out.writeInt(config.a);
        out.writeFloat(config.strength);
        out.writeFloat(config.radius);
        out.writeInt(config.priority);

        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    /**
     * 删除 Bloom 配置
     */
    public static void removeBloomConfig(Player player, String id) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeInt(17);  // BloomConfigRemove
        out.writeUTF(id);

        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    public static class BloomConfig {
        public String name;
        public int r, g, b, a;
        public float strength;
        public float radius;
        public int priority;
    }
}
```

---

## YAML 配置示例

服务端插件可以使用 YAML 文件管理 Bloom 配置：

```yaml
bloom:
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

  bloom_holy:
    name: "holy"
    color: [255, 255, 200, 255]
    strength: 2.0
    radius: 40.0
    priority: 15
```

---

## 注意事项

1. **通道注册**: 确保在插件启用时注册 `orryxmod:main` 通道
2. **玩家登录**: 建议在玩家登录后发送 `BloomConfigSync` 全量同步配置
3. **数值范围**: 客户端会对数值进行范围限制，超出范围的值会被截断
4. **优先级**: 当实体名称匹配多个配置时，使用优先级最高的配置
5. **断开连接**: 客户端断开连接时会自动清空所有配置
