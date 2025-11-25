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

</div>

---

## 核心特性

<table>
<tr>
<td width="50%">

### 技能系统
- 多触发方式（主动/被动/按键/事件）
- 技能冷却、等级、经验管理
- 技能组与职业绑定

</td>
<td width="50%">

### 脚本引擎
- Kether 表达式语言支持
- Kotlin 脚本 (KTS) 热重载
- 复杂逻辑轻松实现

</td>
</tr>
<tr>
<td width="50%">

### 资源管理
- 法力值 (Mana) 系统
- 精力值 (Spirit) 系统
- 完整经验与升级机制

</td>
<td width="50%">

### 多端 UI
- Bukkit 原生 UI
- GermPlugin 支持
- DragonCore 支持

</td>
</tr>
</table>

## 快速开始

### 安装

1. 下载 [最新版本](https://github.com/zhibeigg/Orryx/releases) 的 Orryx
2. 将 JAR 文件放入服务器 `plugins` 目录
3. 重启服务器
4. 编辑 `plugins/Orryx/config.yml` 进行配置

### 依赖

| 类型     | 依赖项                                                                 |
|--------|---------------------------------------------------------------------|
| **必需** | [TabooLib](https://github.com/TabooLib/taboolib) (已内置)              |
| **可选** | PlaceholderAPI, MythicMobs, DragonCore, GermPlugin, AttributePlus 等 |

## 项目结构

```
Orryx/
├── core/                # 核心模块
│   ├── skill/          # 技能系统
│   ├── station/        # 事件触发器
│   ├── kether/         # Kether 脚本引擎
│   ├── kts/            # Kotlin 脚本系统
│   ├── job/            # 职业系统
│   └── damage/         # 伤害计算
├── module/              # 功能模块
│   ├── mana/           # 法力值
│   ├── spirit/         # 精力值
│   ├── state/          # 状态管理
│   └── ui/             # UI 渲染
└── api/                 # 公开 API
```

## 构建

### 发行版本
构建可直接运行的插件（不含 TabooLib 本体）：
```bash
./gradlew build
```

### 开发版本
构建用于开发的 API 包：
```bash
./gradlew taboolibBuildApi -PDeleteCode
```
> `-PDeleteCode` 参数移除逻辑代码以减少体积

## API 使用

### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://www.mcwar.cn/nexus/repository/maven-public/")
}

dependencies {
    compileOnly("org.gitee.orryx:orryx:1.31.68:api")
}
```

### Gradle (Groovy)
```groovy
repositories {
    maven { url 'https://www.mcwar.cn/nexus/repository/maven-public/' }
}

dependencies {
    compileOnly 'org.gitee.orryx:orryx:1.31.68:api'
}
```

## 第三方集成

<details>
<summary><b>支持的插件列表</b></summary>

| 插件                  | 功能        |
|---------------------|-----------|
| **PlaceholderAPI**  | 变量占位符     |
| **MythicMobs**      | 怪物管理集成    |
| **DragonCore**      | 自定义 UI/物品 |
| **GermPlugin**      | 动画和 UI    |
| **Adyeshach**       | NPC 系统    |
| **AttributePlus**   | 属性系统      |
| **RedisChannel**    | 多服数据同步    |
| **ProtocolLib**     | 数据包处理     |
| **PacketEvents**    | 数据包处理     |
| **OriginAttribute** | 属性系统      |
| **SX-Attribute**    | 属性系统      |

</details>

## 数据存储

支持多种数据库后端：

- **SQLite** (默认) - 无需配置，开箱即用
- **MySQL** - 生产环境推荐
- **H2** - 轻量级嵌入式数据库
- **Redis** - 可选缓存层

## 文档资源

- [飞书 Wiki](https://o0vvjwgpeju.feishu.cn/wiki/Syzzw7aQwixJ4YkXoOAcyYkfnOg) - 完整使用文档
- [DeepWiki AI](https://deepwiki.com/zhibeigg/Orryx) - AI 问答助手
- [API 文档](https://github.com/zhibeigg/Orryx) - 开发者参考

## 统计

<div align="center">

[![bStats](https://bstats.org/signatures/bukkit/Orryx.svg)](https://bstats.org/plugin/bukkit/Orryx/24289/)

</div>

<div align="center">

*Powered by TabooLib*

</div>

---

<div align="center">

**Orryx** © 2024-2025

</div>
