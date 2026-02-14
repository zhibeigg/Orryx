# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建命令

```bash
# 构建发行版本
./gradlew build

# 生成 API 包（开发用）
./gradlew taboolibBuildApi -PDeleteCode

# 生成 API 文档
./gradlew dokkaHtml
```

运行插件需在 Minecraft 服务器端加载生成的 jar（`build/libs/`）。

## 架构概览

Orryx 是一个 Minecraft 技能插件，支持 1.12-1.21 版本，基于 Kotlin 2.1.20 和 TabooLib 6.2.4 框架。

### 分层架构

```
src/main/kotlin/org/gitee/orryx/
├── api/        # 公开 API 层 - 对外暴露的接口、事件、碰撞系统
├── core/       # 核心层 - 技能、职业、触发器、Kether脚本、选择器
├── module/     # 功能模块 - 法力值、精力值、状态机、UI、经验
├── compat/     # 兼容层 - 第三方插件集成
├── dao/        # 数据层 - 缓存、存储、序列化
├── command/    # 命令系统
└── utils/      # 工具类
```

### 兼容插件

DragonCore、GermPlugin、ArcartX、MythicMobs、AstraXHero、AttributePlus、DungeonPlus、Nodens、PacketEvents、PlaceholderAPI、ProtocolLib

### 核心系统

- **技能系统** (`core/skill/`): 5种技能类型（被动、直接、指向性、蓄力、蓄力指向性）
- **触发器系统** (`core/station/`): 100+ 触发器，处理 Bukkit 事件和第三方插件事件
- **Kether 脚本** (`core/kether/`): 74 个动作文件，用于技能逻辑编写
- **选择器系统** (`core/selector/`): 几何体范围选择（圆形、扇形、环形、OBB、射线、视锥等）
- **碰撞系统** (`api/collider/`): 球体、胶囊体、AABB、OBB、射线、复合体碰撞检测

### API 入口

```kotlin
val api = Orryx.api()  // 获取 API 实例
api.skillAPI.castSkill(player, "fireball", 5)
```

主要 API 接口：`ISkillAPI`、`IJobAPI`、`IProfileAPI`、`IKeyAPI`、`ITaskAPI`、`ITimerAPI`、`IReloadAPI`、`IConsumptionValueAPI`、`IMiscAPI`

### 协程作用域

项目使用三个协程作用域：
- `ioScope` - IO 操作
- `effectScope` - 效果渲染
- `pluginScope` - 插件逻辑

## 编码规范

- Kotlin 4 空格缩进，`val` 优先于 `var`
- 类名 `PascalCase`，函数/变量 `camelCase`
- 包名以 `org.gitee.orryx` 开头
- 提交信息格式：`type(scope): 摘要` 或 `type（scope）：摘要`
  - 类型：`feat`、`fix`、`refactor`、`chore`、`docs`

## 配置文件

运行时配置位于 `plugins/Orryx/`，模板在 `src/main/resources/`：
- `config.yml` - 主配置（数据库、UI端、缓存等）
- `keys.yml` - 按键配置
- `bloom.yml` - Bloom 泛光配置
- `buffs.yml` - Buff 配置
- `npc.yml` - NPC 配置
- `selectors.yml` - 选择器配置
- `state.yml` - 状态配置
- `skills/` - 技能定义
- `jobs/` - 职业定义
- `stations/` - 中转站定义
- `controllers/` - 控制器定义
- `experiences/` - 经验配置
- `status/` - 状态定义
- `ui/` - UI 配置
- `lang/` - 语言文件
- `placeholders/` - 占位符配置

## 文档资源

- [飞书 Wiki](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg)
- [API 文档](docs/API.md)
- [OrryxMod 客户端协议文档](docs/Plugin-Integration.md)
- [客户端引擎集成文档](docs/Client-Engine-Integration.md)
- [实体字段文档](docs/EntityField.md)
