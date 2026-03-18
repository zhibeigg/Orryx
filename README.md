<div align="center">

# Orryx

![orryx.png](https://image.mcwar.cn/i/2026/02/11/698c2c2c82af5.png)

**跨时代技能插件，支持实现复杂逻辑，为稳定高效而生**

[![Version](https://img.shields.io/badge/version-2.38.96-blue?style=for-the-badge)](https://github.com/zhibeigg/Orryx/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12--1.21-green?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![TabooLib](https://img.shields.io/badge/TabooLib-6.2.4-orange?style=for-the-badge)](https://github.com/TabooLib/taboolib)

[![Wiki](https://img.shields.io/badge/Wiki-开始使用-darkred?style=for-the-badge&logo=gitbook)](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg)
[![Ask DeepWiki](https://img.shields.io/badge/DeepWiki-Ask_AI-00D4AA?style=for-the-badge)](https://deepwiki.com/zhibeigg/Orryx)
[![Ask ZRead](https://img.shields.io/badge/ZRead-Ask_AI-00b0aa?style=for-the-badge)](https://zread.ai/zhibeigg/Orryx)

</div>

---

## 特色功能

### 技能系统

| 技能类型 | 说明 |
|---------|------|
| **被动技能 (Passive)** | 自动触发，无需手动释放 |
| **直接释放 (Direct)** | 按键即释放 |
| **直接指向性 (Direct Aim)** | 带指示器的指向性技能 |
| **蓄力释放 (Pressing)** | 长按蓄力后释放 |
| **蓄力指向性 (Pressing Aim)** | 蓄力型指向性技能 |

- 技能等级与经验管理
- 技能点升级系统
- 技能冷却管理（重置/增加/减少/设置/查询）
- 技能升级前检测与成功执行钩子
- 按键绑定系统（支持多键组合如 `G+F`）

### 职业系统

- 职业绑定技能组
- 职业等级与经验
- 职业升级获取技能点
- 职业最大法力值/精力值配置
- 职业属性系统集成

### 资源管理

| 资源类型 | 功能 |
|---------|------|
| **法力值 (Mana)** | 给予/消耗/设置、充足检查、自然恢复 |
| **精力值 (Spirit)** | 给予/消耗/设置、充足检查、自然恢复 |
| **经验系统** | 自定义经验算法、升级配置 |

### 脚本引擎

基于 Kether 脚本引擎，内置 **74 个动作文件**：

- 基础：延迟、同步、条件判断、流程控制
- 技能：冷却管理、法力/精力操作、伤害计算
- 效果：粒子特效、动画、音效
- 数学：矩阵变换、四元数、向量运算
- 选择器：几何体范围选择、目标筛选
- 射线：光线追踪、碰撞检测
- 兼容：Nodens 属性、AstraXHero、AttributePlus、GDDTitle、MythicMobs 等

### 触发器系统 (104+)

<details>
<summary><b>Bukkit 原生事件 (55)</b></summary>

- 玩家加入/退出/踢出/死亡/重生
- 玩家伤害（前/后）
- 玩家移动/跳跃/切换飞行/切换疾跑/切换潜行
- 物品操作（消耗、掉落、损坏、拾取、合并）
- 交互事件（实体、方块）
- 方块破坏/放置
- 聊天/命令
- 等级/经验变化
- 弹射物命中
- 更多...

</details>

<details>
<summary><b>Orryx 内部事件 (36)</b></summary>

- 技能：释放、检查、冷却增加/减少/设置（13 个）
- 法力值：增加、减少、恢复、治疗（4 个）
- 精力值：增加、减少、恢复、治疗（4 个）
- 职业：更改（前/后）、清除、设置（4 个）
- 等级：升级、降级（2 个）
- 经验：增加、减少（2 个）
- 技能点：增加、减少（2 个）
- 标志：玩家标志变更、全局标志变更（4 个）
- 按键：按下、抬起、持续（3 个）
- 档案：保存（1 个）

</details>

<details>
<summary><b>第三方插件事件</b></summary>

- **DragonCore** (7)：按键按下/释放、缓存加载、实体加入/离开世界、数据包、槽位
- **ArcartX** (10)：按键按下/释放、简单按键按下/释放、组合键、鼠标点击、实体加入/离开、客户端通道、自定义数据包
- **GermPlugin** (3)：客户端连接、按键按下/抬起
- **MythicMobs** (3)：怪物死亡、怪物掉落、怪物生成
- **DungeonPlus** (4)：副本开始/结束/离开、事件代理

</details>

### 选择器系统

**几何体选择器 (16 种)**

| 几何体 | 说明 |
|-------|------|
| Range | 圆形范围 |
| Sector | 扇形范围 |
| Annular | 环形范围 |
| Cone | 锥形范围 |
| Cylinder | 圆柱范围 |
| Ring | 圆环范围 |
| Line | 线段范围 |
| Floor | 地面范围 |
| Frustum | 视锥体 |
| OBB | 有向包围盒 |
| RayHit | 射线击中 |
| Scatter | 散射范围 |
| Nearest | 最近目标 |
| LookAt | 朝向目标 |
| Location | 位置选择 |
| VectorLocation | 向量位置 |

**流式过滤器 (20 种)**

Self、Origin、Direct、Offset、Type、Teammate、Team、PVP、Amount、Server、World、Current、Joiner、Alive、Distance、Health、Random、Sight、Sort、Unique

**预设选择器**：支持自定义预设组合

### 碰撞系统

| 碰撞体 | 说明 |
|-------|------|
| Sphere | 球体碰撞 |
| Capsule | 胶囊体碰撞 |
| AABB | 轴对齐包围盒 |
| OBB | 有向包围盒 |
| Ray | 射线碰撞 |
| Composite | 复合碰撞体 |

所有碰撞体均支持本地坐标系变体（Local），附带坐标转换器。

### 状态管理

| 状态类型 | 说明 |
|---------|------|
| VertigoState | 眩晕状态 |
| BlockState | 格挡状态 |
| DodgeState | 闪避状态 |
| SkillState | 技能状态 |
| GeneralAttackState | 普通攻击状态 |
| PressGeneralAttackState | 蓄力普通攻击状态 |

- 状态前置条件检查
- 状态入场/退出钩子

### 伤害系统

| 伤害类型 | 说明 |
|---------|------|
| PHYSICS | 物理伤害 |
| MAGIC | 魔法伤害 |
| FIRE | 火焰伤害 |
| REAL | 真实伤害 |
| SELF | 自身伤害 |
| CONSOLE | 控制台伤害 |
| CUSTOM | 自定义伤害 |

### 多端 UI

| 端 | 功能 |
|---|------|
| **Bukkit** | 原生 UI 界面 |
| **GermPlugin** | HUD、动画、UI |
| **DragonCore** | 自定义 UI、物品 |
| **ArcartX** | UI、脚本、变量 |

### 其他模块

- **AI 集成**：OpenAI 接入
- **Wiki 生成**：自动生成飞书文档、Markdown 文档、Actions Schema JSON
- **在线编辑器**：WebSocket 连接中心服务器，支持浏览器远程编辑配置文件、重载模块、查看日志
- **Buff 系统**：Buff 配置与管理
- **NPC 系统**：基于 Adyeshach 的虚拟实体
- **Bloom 泛光**：泛光特效配置

---

## 第三方插件集成

### 客户端引擎

<details>
<summary><b>DragonCore</b></summary>

- 动画桥接：通过自定义数据包控制玩家/实体动画（播放、移除、清空）
- 按键注册：合并 DragonCore 配置键与 Orryx 扩展键同步到客户端
- UI 系统：推送 YAML GUI 配置到客户端，处理自定义数据包实现技能 UI 交互
- Kether 动作 `dragoncore`：时装管理（DragonArmourers 皮肤刷新）、暴雪粒子特效、动画控制（玩家/实体/物品/方块）、音乐播放、GUI/HUD 打开、PlaceholderAPI 数据同步、headTag 管理、实体模型管理、视角切换、窗口标题、虚拟实体绑定、模型特效（通过 Adyeshach 创建临时实体）、隐藏手持武器、槽位物品读取
- 触发器 (7)：按键按下/释放、缓存加载、实体加入/离开世界、数据包、槽位

</details>

<details>
<summary><b>GermPlugin</b></summary>

- 动画桥接：通过 GermPacketAPI 播放/清空动画（不支持移除单个动画）
- 按键注册：逐个注册按键，自动映射 `MOUSE_LEFT`/`MOUSE_RIGHT` 为 `MLEFT`/`MRIGHT`
- UI 系统：监听客户端连接自动打开 HUD，支持热重载配置
- Kether 动作 `germplugin`：基岩时装管理、特效管理、动画控制（实体/物品/方块）、音乐播放、GUI/HUD 打开、视角切换、槽位物品读取
- 触发器 (3)：客户端连接、按键按下/抬起

</details>

<details>
<summary><b>ArcartX</b></summary>

- 动画桥接：通过 ArcartX Handler 播放/移除/清空动画（优先级最高）
- 按键注册：通过 NetworkMessageSender 同步按键注册
- UI 系统：通过 ArcartX UI Registry 注册 UI，处理自定义数据包实现技能交互
- Kether 动作 `arcartx`：动画控制（支持速度/过渡/持续时间）、音效播放、UI 打开/关闭/脚本运行、实体模型设置（支持缩放）、服务端变量管理、自定义数据包发送、屏幕震动、窗口标题
- Glimmer 脚本集成：注册 `Orryx` 命名空间，提供 22 个静态函数（法力值/精力值/技能/状态/档案/职业操作）和 `OrryxPlayer` 对象类型（20 个实例方法）
- 触发器 (10)：按键按下/释放、简单按键按下/释放、组合键、鼠标点击、实体加入/离开、客户端通道、自定义数据包

</details>

<details>
<summary><b>CloudPick</b></summary>

- Kether 动作 `cloudpick`：时装管理（FashionAPI）、暴雪粒子特效、动画控制（玩家/实体/物品/方块）、音乐播放、GUI/HUD 打开、PlaceholderAPI 数据同步、headTag 管理、实体模型管理、虚拟实体绑定、模型特效（通过 Adyeshach 创建临时实体）、隐藏手持武器、槽位物品读取

</details>

### 属性系统

<details>
<summary><b>AttributePlus</b></summary>

- 属性桥接：添加/移除临时属性（支持超时自动移除）、强制刷新属性
- 伤害计算：通过 `AttributeAPI.runAttributeAttackEntity()` 执行带属性的攻击
- 高级攻防：完整走一遍 `AttributeHandle` 攻防计算流程（含反伤处理），支持重置属性数据和叠加额外属性源
- Kether 动作 `attribute`（通用）、`apAttack`（AttributePlus 专用）

</details>

<details>
<summary><b>Nodens</b></summary>

- 属性桥接：添加/移除临时属性（原生支持超时）、强制刷新属性
- 伤害计算：创建 `DamageProcessor`，映射 Orryx 伤害类型到 Nodens 伤害类型（Magic/Physics/Real），走完整伤害流程
- Kether 动作 `damageProcessor`（伤害处理器）、`regainProcessor`（治疗处理器）

</details>

<details>
<summary><b>AstraXHero</b></summary>

- 属性桥接：添加/移除属性源（支持超时自动移除）、强制刷新属性
- 伤害计算：创建 `FightData` 注入 Orryx 变量，调用 `FightAPI.runFight()` 执行战斗流程
- Kether 动作 `axhDamage`（支持自定义战斗变量）

</details>

### 怪物与副本

<details>
<summary><b>MythicMobs</b></summary>

- 条件 (12)：`O-FLAG`、`O-JOB`、`O-LEVEL`、`O-MANA`、`O-SPIRIT`、`O-POINT`、`O-EXPERIENCE`、`O-SKILLLEVEL`、`O-SUPERBODY`、`O-INVINCIBLE`、`O-SILENCE`、`O-SUPERFOOT`
- 技能 (8)：`O-CAST`（释放 Orryx 技能）、`O-GIVEMANA`/`O-TAKEMANA`、`O-GIVESPIRIT`/`O-TAKESPIRIT`、`O-SUPERBODY`/`O-INVINCIBLE`/`O-SILENCE`（设置状态+持续时间）
- 目标选择器 (7)：`O-SELECTORL`/`O-SELECTORE`（Orryx 选择器语法）、`O-RANGE`（球形）、`O-SECTOR`（扇形）、`O-OBB`（有向包围盒）、`O-ANNULAR`（环形）、`O-FRUSTUM`（视锥）
- Kether 动作 `mm`：嘲讽、仇恨值管理、信号发送、释放 MM 技能
- 触发器 (3)：怪物死亡、怪物掉落、怪物生成

</details>

<details>
<summary><b>DungeonPlus</b></summary>

- 地牢进入条件 (4)：`o-flag-condition`（Flag 值检查）、`o-job-condition`（职业检查）、`o-level-condition`（等级范围检查）、`o-mana-condition`（法力值检查）
- 触发器 (4)：副本开始/结束/离开、事件代理

</details>

### 虚拟实体

<details>
<summary><b>Adyeshach</b></summary>

- 实体适配：将 Adyeshach `EntityInstance` 适配为 Orryx 的 `IEntity`/`ITargetEntity`/`ITargetLocation` 接口
- Kether 动作 `entity create ady`：通过 Adyeshach 创建临时虚拟实体（支持私有/公共管理器）
- 模型特效载体：DragonCore/CloudPick 的 `modelEffect create` 通过 Adyeshach 创建临时虚拟实体
- 容器系统：自动识别 `EntityInstance` 类型并转换
- Flag 序列化：支持 Adyeshach 实体类型的编解码
- 触发器联动：客户端引擎的实体进出视野事件中查找对应虚拟实体

</details>

### 数据包与协议

| 插件 | 功能 |
|-----|------|
| **packetevents** | 拦截 `PLAYER_ABILITIES` 数据包，将 FOV modifier 强制设为 0，消除速度变化导致的 FOV 抖动（配置项 `OffSpeedFovChange` 控制，默认开启） |
| **ProtocolLib** | 功能同上，仅在 packetevents 未启用时生效（互斥，packetevents 优先） |

### 其他

| 插件 | 功能 |
|-----|------|
| **PlaceholderAPI** | 注册 `%orryx_xxx%` 占位符，从 `placeholders/` 目录加载 YAML 配置，每个键对应一段 Kether 脚本，请求时同步执行返回结果，支持热重载 |
| **GDDTitle** | 在语言文件中注册 `gddtitle_action` 和 `gddtitle_title` 类型，让语言消息以龙核 HUD Title/Action 形式展示；Kether 动作 `gddtitle`/`gddaction` 发送 HUD 文本（支持淡入/停留/淡出时间） |
| **RedisChannel** | 跨服数据同步缓存，支持单节点和集群模式；缓存玩家档案、职业、技能、按键设置数据，缓存未命中时回源 Storage，过期时间 6-12 小时 |
| **DragonArmourers** | DragonCore 时装子插件，状态切换时触发 `DragonAPI.updatePlayerSkin()` 刷新玩家皮肤 |

---

## 快速开始

### 安装

1. 下载 [最新版本](https://github.com/zhibeigg/Orryx/releases)
2. 放入服务器 `plugins` 目录
3. 重启服务器
4. 编辑 `plugins/Orryx/config.yml`

### 依赖

| 类型 | 依赖项 |
|-----|--------|
| **必需** | TabooLib (已内置) |
| **可选** | 见上方第三方插件列表 |

### 数据存储

| 类型 | 说明 |
|-----|------|
| **SQLite** | 默认，开箱即用 |
| **MySQL** | 生产环境推荐 |
| **H2** | 轻量级嵌入式 |
| **Redis** | 可选缓存层 |

---

## 项目结构

```
Orryx/
├── api/                    # 公开 API (事件、接口、碰撞系统)
│   ├── events/            # 事件系统 (伤害、全局、玩家)
│   ├── interfaces/        # API 接口 (12 个)
│   └── collider/          # 碰撞系统 (6 种碰撞体 + 本地坐标系变体)
├── core/                   # 核心模块
│   ├── skill/             # 技能系统 (5 种技能类型)
│   ├── job/               # 职业系统
│   ├── station/           # 触发器系统 (104+ 触发器)
│   ├── kether/            # Kether 脚本引擎 (74 个动作文件)
│   ├── selector/          # 选择器系统 (16 几何体 + 20 过滤器)
│   ├── damage/            # 伤害系统
│   └── common/timer/      # 冷却时间系统
├── module/                 # 功能模块
│   ├── ai/                # OpenAI 集成
│   ├── experience/        # 经验系统
│   ├── lang/              # 语言/国际化
│   ├── mana/              # 法力值
│   ├── spirit/            # 精力值
│   ├── state/             # 状态管理 (6 种状态)
│   ├── ui/                # UI 渲染 (Bukkit/Germ/Dragon/ArcartX)
│   ├── wiki/              # Wiki 文档生成 (飞书/Markdown/Actions Schema)
│   └── editor/            # 在线编辑器 (WebSocket 客户端)
├── compat/                 # 第三方插件兼容 (14 个插件)
├── dao/                    # 数据层 (缓存、存储、序列化)
├── command/                # 命令系统
└── utils/                  # 工具类
```

### 配置文件

```
plugins/Orryx/
├── config.yml             # 主配置 (数据库、UI端、缓存等)
├── keys.yml               # 按键配置
├── bloom.yml              # Bloom 泛光配置
├── buffs.yml              # Buff 配置
├── npc.yml                # NPC 配置
├── state.yml              # 状态机配置
├── selectors.yml          # 选择器预设
├── skills/                # 技能定义
├── jobs/                  # 职业定义
├── stations/              # 中转站定义
├── controllers/           # 控制器定义
├── experiences/           # 经验算法
├── status/                # 状态定义
├── ui/                    # UI 配置
├── lang/                  # 语言文件
└── placeholders/          # 占位符配置
```

---

## API 使用

### 入口

```kotlin
val api = Orryx.api()  // 获取 IOrryxAPI 实例
api.skillAPI.castSkill(player, "fireball", 5)
```

### 主要接口

| 接口 | 说明 |
|-----|------|
| `IOrryxAPI` | 总 API 入口 |
| `ISkillAPI` | 技能操作 |
| `IJobAPI` | 职业操作 |
| `IProfileAPI` | 玩家档案 |
| `IKeyAPI` | 按键管理 |
| `ITaskAPI` | 任务管理 |
| `ITimerAPI` | 计时器管理 |
| `IReloadAPI` | 重载管理 |
| `IConsumptionValueAPI` | 消耗值 (法力/精力) |
| `IMiscAPI` | 杂项功能 |

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://maven.mcwar.cn/releases")
}

dependencies {
    compileOnly("org.gitee.orryx:orryx:2.38.96:api")
}
```

### Gradle (Groovy)

```groovy
repositories {
    maven { url 'https://maven.mcwar.cn/releases' }
}

dependencies {
    compileOnly 'org.gitee.orryx:orryx:2.38.96:api'
}
```

---

## 构建

```bash
# 构建发行版本
./gradlew build

# 生成 API 包（开发用）
./gradlew taboolibBuildApi -PDeleteCode

# 生成 API 文档
./gradlew dokkaHtml
```

---

## 版本号规则

版本格式：`A.B.C`

| 位 | 含义 | 递增时机 |
|---|------|---------|
| `A` | API 变动 | 公开接口发生不兼容变更 |
| `B` | 功能更新 | 新增功能 |
| `C` | 修复 | Bug 修复、兼容性修复 |

---

## 文档资源

- [飞书 Wiki](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg) — 完整使用文档
- [DeepWiki AI](https://deepwiki.com/zhibeigg/Orryx) — AI 问答助手
- [ZRead AI](https://zread.ai/zhibeigg/Orryx) — AI 问答助手
- [API 文档](docs/API.md) — 开发者 API 参考
- [客户端协议文档](docs/Plugin-Integration.md) — OrryxMod 客户端协议
- [客户端引擎集成](docs/Client-Engine-Integration.md) — DragonCore/GermPlugin/ArcartX 集成
- [实体字段文档](docs/EntityField.md) — 实体字段参考

---

## 统计

<div align="center">

[![bStats](https://bstats.org/signatures/bukkit/Orryx.svg)](https://bstats.org/plugin/bukkit/Orryx/24289/)

*Powered by TabooLib*

</div>

---

<div align="center">

**Orryx** © 2024-2026

</div>
