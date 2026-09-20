# Orryx 功能与集成参考

[返回项目首页](../README.md)

本页汇总技能、职业、脚本和第三方集成能力。动作语法与运行时注册信息以 [Kether 文档](https://zhibeigg.github.io/Orryx/) 为准；插件开发参见 [API 文档](API.md)。

## 技能与职业

### 技能类型

| 类型 | 说明 |
| --- | --- |
| `PASSIVE` | 被动技能，自动触发 |
| `DIRECT` | 直接释放 |
| `DIRECT AIM` | 带指示器的直接指向性释放 |
| `PRESSING` | 长按蓄力后释放 |
| `PRESSING AIM` | 蓄力型指向性释放 |

技能支持等级、技能点升级、冷却管理，以及升级前检测和成功执行钩子。冷却可重置、增加、减少、设置和查询；按键可绑定多键组合，如 `G+F`。

职业可绑定技能组，管理职业等级与经验、升级技能点、最大法力值与精力值，并接入属性系统。

### 资源管理

| 资源 | 功能 |
| --- | --- |
| 法力值 Mana | 给予、消耗、设置、充足检查、自然恢复 |
| 精力值 Spirit | 给予、消耗、设置、充足检查、自然恢复 |
| 经验 | 自定义经验算法与升级配置 |

## Kether 脚本

技能逻辑基于 Kether 编排，包含以下动作类别：

- 基础流程：延迟、同步、条件判断与流程控制。
- 技能逻辑：冷却、法力、精力与伤害计算。
- 效果表现：粒子特效、动画与音效。
- 数学运算：矩阵、四元数与向量。
- 空间判断：几何体选择、目标筛选、射线与碰撞。
- 插件联动：Nodens、AstraXHero、AttributePlus、GDDTitle、MythicMobs 等。

### 注释规则

技能、中转站、状态、占位符、临时脚本和表达式入口使用同一套注释预处理：

```yaml
Actions: |-
  # 纯注释行会保留为空行，便于错误行号定位
  tell "damage#fire" # 引号外的井号开始行尾注释
  tell '#tag'        # 单双引号内的井号都会保留
```

- 仅引号外且未转义的 `#` 开始注释；单双引号、转义引号和转义 `#` 均会保留。
- 支持 LF 与 CRLF，纯注释行对应的换行不会被删除。
- 移除注释后，若首个非空内容是完整 `def` 词元，则按完整脚本加载；否则自动包装 `def main`。

### 自动生成的文档

Orryx 从运行时完整注册表生成版本化文档。正式 Tag 更新 `channels/stable.json`，`master` 更新 `channels/snapshot.json`；每个不可变发布目录包含 Registry、兼容 Schema、Markdown、结构化差异与 SHA-256。

Registry v4 提供类型图、assignability、raw/Kether 可填充提示、结构化 aliases、grammar/variants、线程与上下文约束、Trigger 事件字段，以及实体类型、药水效果、声音、材质等有限值的可搜索 `options` 目录。`manifest.json`、v3 `actions-schema.json` 与 `latest.md` 继续作为兼容入口。

发布协议、机器数据合同与本地验证命令见 [Kether 文档发布说明](Kether-Docs-Publishing.md)。

## 事件与中转站

### Bukkit 事件

覆盖玩家加入、退出、踢出、死亡、重生、伤害前后、移动、跳跃、切换飞行/疾跑/潜行，以及物品消耗、掉落、损坏、拾取、合并、实体/方块交互、方块破坏/放置、聊天、命令、等级、经验和弹射物命中等事件。

### Orryx 事件

覆盖技能释放、检查与冷却变更，法力值和精力值的增减、恢复与治疗，职业切换/清除/设置，等级、经验、技能点、玩家与全局标志变更，按键按下/抬起/持续，以及档案保存。

### 第三方事件

| 来源 | 事件 |
| --- | --- |
| DragonCore | 按键按下/释放、缓存加载、实体加入/离开世界、数据包、槽位 |
| ArcartX | 按键按下/释放、简单按键按下/释放、组合键、鼠标点击、实体加入/离开、客户端通道、自定义数据包 |
| GermPlugin | 客户端连接、按键按下/抬起 |
| MythicMobs | 怪物死亡、掉落、生成 |
| DungeonPlus | 副本开始、结束、离开、事件代理 |

## 选择器与碰撞

### 几何体选择器

| 选择器 | 说明 | 选择器 | 说明 |
| --- | --- | --- | --- |
| `Range` | 圆形范围 | `Sector` | 扇形范围 |
| `Annular` | 环形范围 | `Cone` | 锥形范围 |
| `Cylinder` | 圆柱范围 | `Ring` | 圆环范围 |
| `Line` | 线段范围 | `Floor` | 地面范围 |
| `Frustum` | 视锥体 | `OBB` | 有向包围盒 |
| `RayHit` | 射线击中 | `Scatter` | 散射范围 |
| `Nearest` | 最近目标 | `LookAt` | 朝向目标 |
| `Location` | 位置选择 | `VectorLocation` | 向量位置 |

流式过滤器支持 `Self`、`Origin`、`Direct`、`Offset`、`Type`、`Teammate`、`Team`、`PVP`、`Amount`、`Server`、`World`、`Current`、`Joiner`、`Alive`、`Distance`、`Health`、`Random`、`Sight`、`Sort`、`Unique`，也可定义可复用的选择器预设。

`Team` 与 `Teammate` 过滤器依赖 DungeonPlus，只有启用该插件时才会注册。

### 碰撞体

| 类型 | 说明 |
| --- | --- |
| `Sphere` | 球体 |
| `Capsule` | 胶囊体 |
| `AABB` | 轴对齐包围盒 |
| `OBB` | 有向包围盒 |
| `Ray` | 射线 |
| `Composite` | 复合碰撞体 |

碰撞体提供本地坐标系变体与坐标转换器。配合 OrryxMod，可用 `colliderShow` 显示调试线框；服务端同步变化的几何快照，客户端对移动和旋转插值，`realtime false` 可保留一次性静态显示。协议、同步限额与示例见 [OrryxMod 对接文档](Plugin-Integration.md)。

## 战斗状态与伤害

| 状态 | 含义 |
| --- | --- |
| `VertigoState` | 眩晕 |
| `BlockState` | 格挡 |
| `DodgeState` | 闪避 |
| `SkillState` | 技能 |
| `GeneralAttackState` | 普通攻击 |
| `PressGeneralAttackState` | 蓄力普通攻击 |

状态支持前置条件检查与入场、退出钩子。伤害类型包括 `PHYSICS`（物理）、`MAGIC`（魔法）、`FIRE`（火焰）、`REAL`（真实）、`MONSTER`（怪物）、`SELF`（自身）、`CONSOLE`（控制台）与 `CUSTOM`（自定义）。

## UI 与客户端引擎

| UI | 功能 |
| --- | --- |
| Bukkit | 默认原生界面 |
| GermPlugin | HUD、动画与自定义 UI |
| DragonCore | 自定义 UI 与物品 |
| ArcartX | UI、脚本与变量 |

通过 `config.yml` 的 `UI.use` 选择 UI；切换后需重启。选择的客户端引擎插件未启用时，Orryx 会回退到 Bukkit UI。客户端引擎的详细动作和功能对照见 [客户端引擎集成](Client-Engine-Integration.md)。

### DragonCore

- 动画桥接：自定义数据包控制玩家/实体动画的播放、移除与清空。
- 按键注册：合并 DragonCore 配置键与 Orryx 扩展键后同步至客户端。
- UI：推送 YAML GUI 配置，使用自定义数据包完成技能 UI 交互。
- `dragoncore` 动作：时装、DragonArmourers 皮肤刷新、暴雪粒子、玩家/实体/物品/方块动画、音乐、GUI/HUD、PlaceholderAPI 同步、headTag、实体模型、视角、窗口标题、虚拟实体绑定、Adyeshach 模型特效、隐藏手持武器与槽位物品读取。

### GermPlugin

- 动画桥接：通过 GermPacketAPI 播放或清空动画，不支持移除单个动画。
- 按键注册：逐个注册，并将 `MOUSE_LEFT`/`MOUSE_RIGHT` 映射为 `MLEFT`/`MRIGHT`。
- UI：客户端连接时自动打开 HUD，支持配置热重载。
- `germplugin` 动作：基岩时装、特效、实体/物品/方块动画、音乐、GUI/HUD、视角与槽位物品读取。

### ArcartX

- 动画桥接：通过 ArcartX Handler 播放、移除、清空动画；在动画桥接选择中优先级最高。
- 按键注册：通过 NetworkMessageSender 同步。
- UI：通过 UI Registry 注册，以自定义数据包完成技能交互。
- `arcartx` 动作：动画速度/过渡/持续时间、音效、UI 打开/关闭/脚本、实体模型与缩放、服务端变量、自定义数据包、屏幕震动与窗口标题。
- Glimmer：注册 `Orryx` 命名空间与 `OrryxPlayer` 对象类型，提供法力值、精力值、技能、状态、档案与职业操作。

### CloudPick

`cloudpick` 动作支持 FashionAPI 时装、暴雪粒子、玩家/实体/物品/方块动画、音乐、GUI/HUD、PlaceholderAPI 同步、headTag、实体模型、虚拟实体绑定、Adyeshach 模型特效、隐藏手持武器与槽位物品读取。

### OrryxMod

OrryxMod 是 Minecraft 1.12.2 客户端模组，提供瞄准、视觉效果与碰撞体调试等协议能力。瞄准协议保持 1.12.2 legacy 字段顺序，使用一次性 wire token、确认状态、超时和最终施法状态复核限制跨会话重放。完整字节布局与使用示例见 [OrryxMod 对接文档](Plugin-Integration.md)。

## 属性系统

| 插件 | 属性桥接与伤害流程 | Kether 动作 |
| --- | --- | --- |
| AttributePlus | 添加/移除临时属性，超时自动移除，强制刷新；通过 `AttributeAPI.runAttributeAttackEntity()` 执行属性攻击；可走完整 `AttributeHandle` 攻防流程，含反伤、属性数据重置与额外属性源 | `attribute`、`apAttack` |
| Nodens | 添加/移除临时属性，原生超时与强制刷新；通过 `DamageProcessor` 映射 Magic/Physics/Real 并执行伤害流程 | `damageProcessor`、`regainProcessor` |
| AstraXHero | 添加/移除属性源，超时自动移除与强制刷新；创建 `FightData` 注入 Orryx 变量，通过 `FightAPI.runFight()` 执行战斗 | `axhDamage`，支持自定义战斗变量 |

## 怪物、副本与虚拟实体

### MythicMobs

- 条件：`O-FLAG`、`O-JOB`、`O-LEVEL`、`O-MANA`、`O-SPIRIT`、`O-POINT`、`O-EXPERIENCE`、`O-SKILLLEVEL`、`O-SUPERBODY`、`O-INVINCIBLE`、`O-SILENCE`、`O-SUPERFOOT`。
- 技能：`O-CAST` 释放 Orryx 技能；`O-GIVEMANA`/`O-TAKEMANA`、`O-GIVESPIRIT`/`O-TAKESPIRIT` 修改资源；`O-SUPERBODY`、`O-INVINCIBLE`、`O-SILENCE` 设置状态与持续时间。
- 目标选择器：`O-SELECTORL`/`O-SELECTORE` 使用 Orryx 选择器语法；`O-RANGE`、`O-SECTOR`、`O-OBB`、`O-ANNULAR`、`O-FRUSTUM` 提供对应几何选择。
- `mm` 动作：嘲讽、仇恨值、信号与 MythicMobs 技能释放。

### DungeonPlus

支持 `o-flag-condition`（Flag 值）、`o-job-condition`（职业）、`o-level-condition`（等级范围）和 `o-mana-condition`（法力值）副本进入条件，并对接副本事件。

### Adyeshach

- 将 `EntityInstance` 适配为 Orryx 的 `IEntity`、`ITargetEntity`、`ITargetLocation` 接口。
- `entity create ady` 创建临时虚拟实体，支持私有与公共管理器。
- 为 DragonCore/CloudPick 的 `modelEffect create` 提供临时实体载体。
- 容器自动识别与转换 `EntityInstance`，Flag 支持 Adyeshach 实体编解码。
- 客户端引擎的实体进出视野事件可关联虚拟实体。

## 数据包与其他集成

第三方插件缺失或出现二进制不兼容时，仅对相应桥接一次性降级。PacketEvents 与 ProtocolLib 监听器在停用时卸载。

| 插件 | 功能 |
| --- | --- |
| packetevents | 拦截 `PLAYER_ABILITIES`，将 FOV modifier 设为 0，消除速度变化引起的 FOV 抖动；由 `OffSpeedFovChange` 控制，默认开启 |
| ProtocolLib | 提供相同 FOV 控制，仅在 packetevents 未启用时生效 |
| PlaceholderAPI | 注册 `%orryx_xxx%`；从 `placeholders/` 加载 YAML，每个键对应一段 Kether，请求时同步执行并返回结果，支持热重载 |
| GDDTitle | 在语言文件中注册 `gddtitle_action`、`gddtitle_title`；`gddtitle`/`gddaction` 可发送 HUD 文本并设置淡入、停留、淡出 |
| DragonArmourers | DragonCore 时装子插件；状态切换时调用 `DragonAPI.updatePlayerSkin()` 刷新皮肤 |

## 存储与缓存

| 后端 | 用途 |
| --- | --- |
| SQLite | 默认持久存储 |
| MySQL | 可配置的持久存储 |
| H2 | 嵌入式持久存储 |
| Redis | 可选跨服缓存 |

Redis 缓存要求 RedisChannel 2.15.14+ API v2，支持单节点和集群模式，缓存玩家档案、职业、技能与按键设置。缓存未命中时异步回源 Storage，过期时间为 6 至 12 小时。玩家写入按 UUID 串行、同键合并，并按数据库、Redis、本地缓存的顺序发布。

`CacheManager` 默认为 `disable`。本服内存缓存的容量和访问过期时间通过 `MemoryCache` 配置，支持热重载。数据库与跨服缓存模式切换需要重启，配置见 [默认 config.yml](../src/main/resources/config.yml)。

## 在线编辑器

```yaml
Editor:
  License: ""
```

`Editor.License` 是中心用于识别并校验服务器身份的凭据。配置后插件自动连接固定中心 `wss://orryx.mcwar.cn/ws/server`，浏览器入口为 `https://orryx.mcwar.cn/connect#token=...`。License 到期不影响实时编辑器，但 License 不存在、被禁用或绑定 IP 不匹配时，中心仍会拒绝连接。旧配置中的 `Editor.Enable` 与 `Editor.PublicUrl` 会被忽略；匿名或空 License 无法使用。

拥有 `Orryx.Command.Editor` 权限的玩家执行 `/orryx edit` 后获得 5 分钟内有效、仅能使用一次的点击入口。服务器控制台执行 `or edit` 时会输出完整的一次性网址。Token 只放在 URL fragment 中，不会以完整链接显示在游戏聊天中；WebSocket 注册确认后才签发访问 Token。

文件请求采用有界顺序队列和 revision 冲突控制；文件沙箱拒绝路径穿越、符号链接及 NTFS 重解析路径，并限制消息、文件和目录遍历配额。访问条件与 Token 语义见 [编辑器说明](Editor.md)。

## 其他模块

- **AI 会话**：OpenAI 会话按玩家 UUID 与 NPC 配置键隔离并串行执行；总超时覆盖排队与 HTTP，取消、重载、退服、停服均清理在途调用与资源。
- **Wiki 生成**：生成飞书文档、Markdown 与 Actions Schema JSON。
- **Creation Suite**：基于配置和源码约束生成、审查技能、职业、经验、中转站、状态机、选择器与多端 UI，见 [Agent SKILL 套件](../agent-skills/orryx-creation-suite/README.md)。
- **Buff**：Buff 配置与管理。
- **NPC**：基于 Adyeshach 的虚拟实体。
- **Bloom**：泛光特效配置。

Orryx 显式加载并重定位与 Kotlin 2.1.20 匹配的 kotlinx-coroutines 1.10.1，启动不依赖其他插件预先提供协程类。

---

[返回项目首页](../README.md) · [使用 Wiki](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg) · [Kether 文档](https://zhibeigg.github.io/Orryx/)
