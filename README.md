<div align="center">

# Orryx

<img src="https://s21.ax1x.com/2025/01/16/pEFaJDs.png" alt="Orryx Logo" width="400">

**跨时代技能插件，支持实现复杂逻辑，为稳定高效而生**

[![Version](https://img.shields.io/badge/version-1.31.68-blue?style=for-the-badge)](https://github.com/zhibeigg/Orryx/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12--1.21-green?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![TabooLib](https://img.shields.io/badge/TabooLib-6.2-orange?style=for-the-badge)](https://github.com/TabooLib/taboolib)

[![Wiki](https://img.shields.io/badge/Wiki-开始使用-darkred?style=for-the-badge&logo=gitbook)](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg)
[![Ask DeepWiki](https://img.shields.io/badge/DeepWiki-Ask_AI-00D4AA?style=for-the-badge)](https://deepwiki.com/zhibeigg/Orryx)
[![Ask ZRead](https://img.shields.io/badge/ZRead-Ask_AI-00b0aa?style=for-the-badge)](https://zread.ai/zhibeigg/Orryx)

</div>

---

## 特色功能

### 技能系统

| 技能类型                     | 说明          |
|--------------------------|-------------|
| **被动技能 (Passive)**       | 自动触发，无需手动释放 |
| **直接释放 (Direct)**        | 按键即释放       |
| **直接指向性 (Direct Aim)**   | 带指示器的指向性技能  |
| **蓄力释放 (Pressing)**      | 长按蓄力后释放     |
| **蓄力指向性 (Pressing Aim)** | 蓄力型指向性技能    |

- 技能等级与经验管理
- 技能点升级系统
- 技能冷却管理
- 技能升级前检测与成功执行钩子
- 按键绑定系统（支持多键组合如 `G+F`）

### 冷却时间系统

| 操作        | 说明                                    |
|-----------|---------------------------------------|
| **重置冷却**  | `cooldown reset`                      |
| **增加冷却**  | `cooldown add <tick>` - 延长倒计时         |
| **减少冷却**  | `cooldown take <tick>` - 缩短倒计时        |
| **设置冷却**  | `cooldown set <tick>` - 重设倒计时         |
| **获取倒计时** | `cooldown get` / `cooldown countdown` |
| **检测冷却**  | `cooldown has` - 是否在冷却中               |

### 职业系统

- 职业绑定技能组
- 职业等级与经验
- 职业升级获取技能点
- 职业最大法力值/精力值配置
- 职业属性系统集成

### 资源管理

| 资源类型             | 功能                 |
|------------------|--------------------|
| **法力值 (Mana)**   | 给予/消耗/设置、充足检查、自然恢复 |
| **精力值 (Spirit)** | 给予/消耗/设置、充足检查、自然恢复 |
| **经验系统**         | 自定义经验算法、升级配置       |

### 脚本引擎

**Kether 脚本** - 40+ 内置动作
- 基础：延迟、同步、条件判断、流程控制
- 技能：冷却管理、法力/精力操作、伤害计算
- 效果：粒子特效、动画、音效
- 数学：矩阵变换、四元数、向量运算
- 选择器：几何体范围选择、目标筛选
- 射线：光线追踪、碰撞检测

**Kotlin 脚本 (KTS)** - 热重载支持
- 脚本编译缓存
- 文件监视自动重载
- 完整 Kotlin 语法支持

### 触发器系统 (80+)

<details>
<summary><b>Bukkit 原生事件 (40+)</b></summary>

- 玩家加入/退出/踢出
- 玩家伤害（前/后）
- 玩家移动/跳跃
- 物品操作（消耗、掉落、损坏、拾取）
- 交互事件（实体、方块）
- 聊天/命令
- 等级/经验变化
- 更多...

</details>

<details>
<summary><b>Orryx 事件 (15+)</b></summary>

- 技能：释放、检查、冷却增加/减少/设置
- 法力值：增加、减少、恢复、治疗、上升、下降
- 精力值：增加、减少、恢复、治疗、上升、下降
- 职业：更改（前/后）、清除
- 等级：升级、降级
- 经验：增加、减少
- 技能点：增加、减少
- 标志：玩家标志变更、全局标志变更
- 按键：按下、抬起、持续

</details>

<details>
<summary><b>第三方插件事件</b></summary>

- **DragonCore**：DragonEntity 加入/离开、缓存加载、按键、数据包、槽位
- **GermPlugin**：客户端连接、按键事件
- **DungeonPlus**：副本开始/结束/离开

</details>

### 选择器系统

| 几何体 | 说明 |
|-------|------|
| Range | 圆形范围 |
| Sector | 扇形范围 |
| Annular | 环形范围 |
| AABB | 轴对齐包围盒 |
| OBB | 有向包围盒 |
| RayHit | 射线击中 |
| Frustum | 视锥体 |
| Floor | 地板范围 |

**目标流筛选**：Self、Origin、Direct、Offset、Type、Teammate、Team、PVP、Amount、Server、World

### 碰撞系统

- 球体碰撞 (Sphere)
- 胶囊体 (Capsule)
- 轴对齐包围盒 (AABB)
- 有向包围盒 (OBB)
- 射线 (Ray)
- 复合体 (Composite)

### 状态管理

- 玩家状态机系统
- 状态类型：眩晕、格挡、躲避、技能状态、普通攻击、蓄力攻击
- 状态前置条件检查
- 状态入场/退出钩子

### 多端 UI

| 端              | 功能        |
|----------------|-----------|
| **Bukkit**     | 原生 UI 界面  |
| **GermPlugin** | HUD、动画、UI |
| **DragonCore** | 自定义 UI、物品 |

### 伤害系统

伤害类型：物理、魔法、火焰、真伤、自伤、控制台、自定义

---

## 第三方插件集成

| 插件                  | 功能               |
|---------------------|------------------|
| **Nodens**          | 自定义系统扩展          |
| **DungeonPlus**     | 副本系统集成           |
| **AttributePlus**   | 属性系统             |
| **AstraXHero**      | 英雄系统             |
| **MythicMobs**      | 怪物管理、机制、条件、目标选择器 |
| **DragonArmourers** | 模型系统             |
| **DragonCore**      | UI、自定义物品、动画、数据包  |
| **GermPlugin**      | UI、HUD、动画、按键     |
| **CloudPick**       | 镐子系统             |
| **OriginAttribute** | 属性系统             |
| **packetevents**    | 数据包处理            |
| **ProtocolLib**     | 数据包处理            |
| **PlaceholderAPI**  | 变量占位符            |
| **GlowAPI**         | 发光效果             |
| **Adyeshach**       | NPC 系统           |

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

---

## 项目结构

```
Orryx/
├── api/                    # 公开 API (30+ 事件和接口)
│   ├── events/            # 事件系统
│   ├── interfaces/        # API 接口
│   └── collider/          # 碰撞系统
├── core/                   # 核心模块
│   ├── skill/             # 技能系统
│   ├── job/               # 职业系统
│   ├── station/           # 触发器系统 (80+)
│   ├── kether/            # Kether 脚本引擎 (40+ 动作)
│   ├── kts/               # Kotlin 脚本系统
│   ├── selector/          # 选择器系统
│   ├── damage/            # 伤害系统
│   └── common/timer/      # 冷却时间系统
├── module/                 # 功能模块
│   ├── mana/              # 法力值
│   ├── spirit/            # 精力值
│   ├── state/             # 状态管理
│   └── ui/                # UI 渲染 (Bukkit/Germ/Dragon)
└── compat/                 # 第三方插件兼容
```

### 配置文件

```
plugins/Orryx/
├── config.yml             # 主配置
├── skills/                # 技能定义
├── jobs/                  # 职业定义
├── stations/              # 中转站定义
├── keys.yml               # 按键配置
├── state.yml              # 状态机配置
├── selectors.yml          # 选择器预设
├── experiences/           # 经验算法
├── ui/                    # UI 配置
├── lang/                  # 语言文件
└── kts/                   # Kotlin 脚本
```

---

## 构建

### 发行版本
```bash
./gradlew build
```

### 开发版本 (API 包)
```bash
./gradlew taboolibBuildApi -PDeleteCode
```

---

## API 使用

### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://jfrog.mcwar.cn/artifactory/maven-releases")
}

dependencies {
    compileOnly("org.gitee.orryx:orryx:1.31.68:api")
}
```

### Gradle (Groovy)
```groovy
repositories {
    maven { url 'https://jfrog.mcwar.cn/artifactory/maven-releases' }
}

dependencies {
    compileOnly 'org.gitee.orryx:orryx:1.31.68:api'
}
```

---

## 数据存储

| 类型         | 说明      |
|------------|---------|
| **SQLite** | 默认，开箱即用 |
| **MySQL**  | 生产环境推荐  |
| **H2**     | 轻量级嵌入式  |
| **Redis**  | 可选缓存层   |

---

## 文档资源

- [飞书 Wiki](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg) - 完整使用文档
- [DeepWiki AI](https://deepwiki.com/zhibeigg/Orryx) - AI 问答助手
- [ZRead AI](https://zread.ai/zhibeigg/Orryx) - AI 问答助手

---

## 统计

<div align="center">

[![bStats](https://bstats.org/signatures/bukkit/Orryx.svg)](https://bstats.org/plugin/bukkit/Orryx/24289/)

*Powered by TabooLib*

</div>

---

<div align="center">

**Orryx** © 2024-2025

</div>
