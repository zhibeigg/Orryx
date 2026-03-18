# 碰撞箱客户端同步渲染方案

## 需求

将服务端碰撞箱（Hitbox）信息通过 `orryxmod:main` 通道发送到客户端，让客户端同步渲染显示碰撞箱线框。

## 现状分析

- 碰撞体在 Kether 脚本中通过 `hitbox` 动作临时创建，没有全局注册表
- 碰撞体绑定到 `ICoordinateConverter`，跟随目标位置/旋转自动更新
- 现有 `selector show` 是服务端粒子渲染，性能差且无法渲染碰撞体
- OrryxMod 通道已成熟，下一个可用 PacketType ID 为 18

## 方案设计

### 新增 3 个 PacketType

| ID | 名称 | 用途 |
|---|---|---|
| 18 | ColliderShow | 创建/更新碰撞箱渲染 |
| 19 | ColliderUpdate | 更新已有碰撞箱的位置/旋转 |
| 20 | ColliderRemove | 移除碰撞箱渲染 |

### ColliderShow (ID: 18) — 创建碰撞箱

```
[18: Int] [id: String] [type: Int] [r: Int] [g: Int] [b: Int] [a: Int] [payload: ...]
```

- `id` — 碰撞箱唯一标识（由 Kether 脚本指定）
- `type` — 碰撞体类型枚举值（0=SPHERE, 1=AABB, 2=OBB, 3=CAPSULE, 4=RAY, 5=COMPOSITE）
- `r,g,b,a` — 渲染颜色 RGBA (0-255)

payload 按 type 不同：

**SPHERE (0):**
```
[cx: Double] [cy: Double] [cz: Double] [radius: Double]
```

**AABB (1):**
```
[cx: Double] [cy: Double] [cz: Double] [hx: Double] [hy: Double] [hz: Double]
```

**OBB (2):**
```
[cx: Double] [cy: Double] [cz: Double] [hx: Double] [hy: Double] [hz: Double] [qx: Double] [qy: Double] [qz: Double] [qw: Double]
```

**CAPSULE (3):**
```
[cx: Double] [cy: Double] [cz: Double] [radius: Double] [height: Double] [qx: Double] [qy: Double] [qz: Double] [qw: Double]
```

**RAY (4):**
```
[ox: Double] [oy: Double] [oz: Double] [dx: Double] [dy: Double] [dz: Double] [length: Double]
```

**COMPOSITE (5):**
```
[count: Int] [子碰撞体...]
```
每个子碰撞体递归写入 `[type: Int] [payload: ...]`（不含 id 和颜色，继承父级）。

### ColliderUpdate (ID: 19) — 更新位置/旋转

```
[19: Int] [id: String] [type: Int] [payload: ...]
```

payload 格式与 ColliderShow 相同（不含颜色），客户端根据 id 找到已有碰撞箱并更新几何数据。

### ColliderRemove (ID: 20) — 移除

```
[20: Int] [id: String]
```

## 代码改动

### 1. PluginMessageHandler.kt

新增 3 个 PacketType：
```kotlin
data object ColliderShow : PacketType(18)
data object ColliderUpdate : PacketType(19)
data object ColliderRemove : PacketType(20)
```

新增 3 个发送方法：
- `sendColliderShow(viewer, id, collider, r, g, b, a)` — 序列化碰撞体并发送
- `sendColliderUpdate(viewer, id, collider)` — 更新碰撞体几何数据
- `sendColliderRemove(viewer, id)` — 移除碰撞箱

抽取私有方法 `writeColliderPayload(output, collider)` 递归序列化碰撞体。

### 2. OrryxModActions.kt

新增 3 个 Kether 动作：

```
colliderShow <id> <hitbox> [color <r> <g> <b> <a>] [viewers <container>]
colliderUpdate <id> <hitbox> [viewers <container>]
colliderRemove <id> [viewers <container>]
```

### 3. docs/Plugin-Integration.md

补充 ColliderShow/ColliderUpdate/ColliderRemove 的协议文档。

## 文件清单

| 文件 | 改动 |
|---|---|
| `core/message/PluginMessageHandler.kt` | 新增 3 个 PacketType + 3 个发送方法 + 序列化工具方法 |
| `core/kether/actions/game/OrryxModActions.kt` | 新增 3 个 Kether 动作 |
| `docs/Plugin-Integration.md` | 补充碰撞箱协议文档 |
