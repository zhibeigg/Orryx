# 碰撞箱客户端实时同步渲染方案

## 目标

将 Orryx 的自定义 Hitbox 通过 `orryxmod:main` 同步给 OrryxMod，并在客户端平滑渲染。覆盖 Sphere、AABB、OBB、Capsule、Ray、Composite，同时保留一次性静态显示能力。

## 协议

继续使用现有包 ID：

| ID | 名称 | 用途 |
|---|---|---|
| 18 | ColliderShow | 创建或覆盖一个渲染项，包含顶层颜色 |
| 19 | ColliderUpdate | 更新已有渲染项的几何快照 |
| 20 | ColliderRemove | 移除渲染项 |

Shape 类型保持 0-5 兼容，并增加 6：

| 类型 | 编号 | 关键格式 |
|---|---:|---|
| Sphere | 0 | 中心与半径均为 Double |
| AABB | 1 | 中心与三轴半长均为 Double |
| OBB | 2 | 中心/半长为 Double，四元数为 Float |
| Capsule | 3 | 竖直胶囊，使用 radius + halfHeight |
| Ray | 4 | 起点、单位方向与长度均为 Double |
| Composite | 5 | 子 ID、类型、RGBA、递归 payload |
| Oriented Capsule | 6 | 类型 3 的字段加 4 个 Float 四元数 |

详细字节布局见 [`docs/Plugin-Integration.md`](../Plugin-Integration.md)。

## 服务端实现

- `ColliderWireCodec` 将可变 `ICollider` 转换为不可变、可比较的 wire snapshot。
- Local Collider 在 Bukkit 主线程调用 `update()` 后取值。
- Composite 子 ID 使用稳定索引路径，子节点继承顶层颜色。
- 数值、ID、深度、节点数和包体大小与客户端限制保持一致。
- `ColliderSyncManager` 以 `(viewer UUID, id)` 管理显示项：
  - Show 立即发送 ID 18；
  - 实时条目按配置间隔检查；
  - 仅在快照变化时发送 ID 19；
  - Remove、禁用、离线、换世界、重载和关闭会清理条目；
  - 使用公平游标及每 Tick 检查/发包预算，避免主线程尖峰。

默认配置位于 `config.yml` 的 `OrryxMod.ColliderSync`。

## Kether 动作

```text
colliderShow <id> <hitbox> [color "r,g,b,a"] [realtime <boolean>] [interval <ticks>] [viewers <container>]
colliderUpdate <id> <hitbox> [viewers <container>]
colliderRemove <id> [viewers <container>]
```

- `realtime` 默认 `true`。
- `realtime false` 只发送一次 Show，可继续手动 Update。
- `colliderUpdate` 会替换已注册条目的 Hitbox 引用并强制同步。
- `colliderRemove` 会停止跟踪并移除客户端线框。

## 客户端实现

- Update 到达时保留当前显示形状和目标形状，按客户端 Tick 与 `partialTicks` 插值。
- 位置/尺寸线性插值，Ray 方向归一化，OBB/定向 Capsule 使用最短路径四元数插值。
- Composite 结构稳定时递归插值，结构变化时直接切换新快照。
- 插值期间使用动态绘制；稳定后继续复用静态 VBO 缓存。
- 世界切换、断线、资源重载和功能清理时同时释放状态与 GPU 缓存。

## 限制

- 单查看者最多 200 个顶层 Collider。
- Composite 每层最多 50 个子节点、最多 3 层、总节点最多 200。
- Bukkit Plugin Message 最大 32766 字节。
- 静止 Hitbox 不产生持续更新包；移动 Hitbox 的发送频率由 `IntervalTicks` 和 Tick 预算共同限制。
