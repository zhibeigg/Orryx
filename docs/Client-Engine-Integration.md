# Orryx 客户端引擎集成文档

Orryx 对 DragonCore（龙之核心）、GermPlugin（萌芽引擎）、ArcartX 三个 Minecraft 客户端引擎提供兼容支持，包括触发器、Kether 动作和扩展功能。

---

## 一、DragonCore（龙之核心）

### 触发器（7个）

| 触发器                             | 说明      | 参数                                              |
|---------------------------------|---------|-------------------------------------------------|
| `DragonKeyPressTrigger`         | 按键按下    | `key`（按键）, 特殊键 `Keys`                           |
| `DragonKeyReleaseTrigger`       | 按键释放    | `key`（按键）, 特殊键 `Keys`                           |
| `DragonPacketTrigger`           | 自定义数据包  | `identifier`（包名）, `data`（数据）, 特殊键 `Identifier`  |
| `DragonSlotTrigger`             | 槽位更新    | `identifier`（槽位名）, `item`（物品）, 特殊键 `Identifier` |
| `DragonEntityJoinWorldTrigger`  | 实体加入客户端 | `uuid`, `entity`                                |
| `DragonEntityLeaveWorldTrigger` | 实体离开客户端 | `uuid`, `entity`                                |
| `DragonCacheLoadTrigger`        | 龙核缓存加载  | —                                               |

### Kether 动作

#### 时装系统

- `dragoncore armourers send` — 设置临时时装
- `dragoncore armourers clear` — 清除临时时装
- `dragoncore armourers update` — 更新时装

#### 粒子特效

- `dragoncore effect send` — 发送暴雪粒子
- `dragoncore effect remove` — 移除暴雪粒子
- `dragoncore effect clear` — 清除暴雪粒子

#### 动画系统

- `dragoncore animation set player` — 设置玩家动作
- `dragoncore animation remove player` — 移除玩家动作
- `dragoncore animation set entity` — 设置实体动作
- `dragoncore animation remove entity` — 移除实体动作
- `dragoncore animation set item` — 设置手持物品动作
- `dragoncore animation set block` — 设置方块动作

#### 音效系统

- `dragoncore sound send` — 播放音乐
- `dragoncore sound stop` — 停止音乐

#### UI 系统

- `dragoncore gui` — 打开 GUI
- `dragoncore hud` — 打开 HUD
- `dragoncore function gui` — 运行 GUI 方法
- `dragoncore function animation` — 运行动作控制器方法
- `dragoncore function headtag` — 运行 headTag 方法

#### 模型系统

- `dragoncore model set` — 设置实体模型
- `dragoncore model remove` — 移除实体模型
- `dragoncore modelEffect create` — 创建实体模型特效
- `dragoncore modelEffect remove` — 移除实体模型特效

#### 其他

- `dragoncore papi send` — 发送同步 placeholder 数据
- `dragoncore papi delete` — 删除 placeholder 数据
- `dragoncore headtag set/remove` — 设置/移除 headTag
- `dragoncore view` — 设置视角（1/2/3人称）
- `dragoncore title` — 设置窗口标题
- `dragoncore bindEntity` — 虚拟绑定实体位置
- `dragoncore invisibleHand` — 隐藏玩家手持武器
- `dragoncore slot` — 获取槽位内物品

---

## 二、GermPlugin（萌芽引擎）

### 触发器（3个）

| 触发器                       | 说明      | 参数                                                                        |
|---------------------------|---------|---------------------------------------------------------------------------|
| `GermKeyDownTrigger`      | 按键按下    | `key`, `keyBinding`（含 `name`/`index`/`defaultKey`/`category`）, 特殊键 `Keys` |
| `GermKeyUpTrigger`        | 按键释放    | `key`, `keyBinding`, 特殊键 `Keys`                                           |
| `GermClientLinkedTrigger` | 客户端连接完成 | `ip`, `machineCode`, `modVersion`, `qq`                                   |

### Kether 动作

#### 时装系统

- `germplugin armourers send` — 设置临时基岩时装
- `germplugin armourers clear` — 清除临时基岩时装

#### 特效系统

- `germplugin effect send` — 发送 Effect 特效
- `germplugin effect remove` — 移除 Effect 特效
- `germplugin effect clear` — 清除 Effect 特效

#### 动画系统

- `germplugin animation set entity` — 设置实体动作（自动识别玩家/怪物）
- `germplugin animation stop entity` — 停止实体动作
- `germplugin animation remove entity` — 移除实体动作
- `germplugin animation set item` — 设置玩家物品动作
- `germplugin animation stop item` — 停止玩家物品动作
- `germplugin animation remove item` — 移除玩家物品动作
- `germplugin animation set block` — 设置方块动作
- `germplugin animation stop block` — 停止方块动作
- `germplugin animation remove block` — 移除方块动作

#### 音效系统

- `germplugin sound send` — 播放音乐
- `germplugin sound stop` — 停止音乐

#### UI 系统

- `germplugin gui` — 打开萌芽 GUI
- `germplugin hud` — 打开萌芽 HUD

#### 其他

- `germplugin view` — 设置视角（FIRST_PERSON / THIRD_PERSON / THIRD_PERSON_REVERSE / CURRENT_PERSON）
- `germplugin slot` — 获取槽位内物品

> 注意：GermPlugin 动画桥接不支持单独移除动画，使用基岩版皮肤（`GermSkinBedrock`）。

---

## 三、ArcartX

### 触发器（10个）

| 触发器                              | 说明      | 参数                              |
|----------------------------------|---------|---------------------------------|
| `ArcartXKeyPressTrigger`         | 按键按下    | `key`, 特殊键 `Keys`               |
| `ArcartXKeyReleaseTrigger`       | 按键释放    | `key`, 特殊键 `Keys`               |
| `ArcartXSimpleKeyPressTrigger`   | 简单按键按下  | `key`, 特殊键 `Keys`               |
| `ArcartXSimpleKeyReleaseTrigger` | 简单按键释放  | `key`, 特殊键 `Keys`               |
| `ArcartXKeyGroupPressTrigger`    | 按键组按下   | `group`（按键组ID）, 特殊键 `Groups`    |
| `ArcartXMouseClickTrigger`       | 鼠标点击    | `button`（鼠标按键）, `action`（动作类型）  |
| `ArcartXCustomPacketTrigger`     | 自定义数据包  | `id`, `data`, `args`, 特殊键 `Ids` |
| `ArcartXEntityJoinTrigger`       | 实体加入客户端 | `uuid`, `entity`                |
| `ArcartXEntityLeaveTrigger`      | 实体离开客户端 | `uuid`, `entity`                |
| `ArcartXClientChannelTrigger`    | 客户端通道连接 | —                               |

### Kether 动作

#### 动画系统

- `arcartx animation set` — 设置实体动画（支持速度、过渡时间、持续时间）
- `arcartx animation default` — 设置实体默认动画状态

#### 音效系统

- `arcartx sound send` — 播放音效
- `arcartx sound stop` — 停止音效

#### UI 系统

- `arcartx ui open` — 打开 UI
- `arcartx ui close` — 关闭 UI
- `arcartx ui run` — 运行 UI 脚本

#### 模型系统

- `arcartx model set` — 设置实体模型（支持缩放）

#### 变量系统

- `arcartx variable set` — 设置服务端变量
- `arcartx variable remove` — 移除服务端变量

#### 其他

- `arcartx packet` — 发送自定义数据包
- `arcartx shake` — 屏幕震动
- `arcartx title` — 设置窗口标题

### Glimmer 脚本引擎集成

ArcartX 独有的 Glimmer 脚本集成，命名空间为 `Orryx`。

#### 静态函数（`OrryxGlimmerFunctions`）

| 分类  | 函数                                                                                        |
|-----|-------------------------------------------------------------------------------------------|
| 法力值 | `getMana`, `getMaxMana`, `setMana`, `giveMana`, `takeMana`                                |
| 精力值 | `getSpirit`, `getMaxSpirit`, `setSpirit`, `giveSpirit`, `takeSpirit`                      |
| 技能  | `castSkill`, `getSkillLevel`, `isSkillLocked`, `getSkillCooldown`                         |
| 状态  | `isSuperBody`, `isInvincible`, `isSilence`, `setSuperBody`, `setInvincible`, `setSilence` |
| 职业  | `getJob`, `getJobLevel`, `getJobExperience`, `getJobMaxExperience`                        |
| 其他  | `getPoint`                                                                                |

#### 玩家对象（`OrryxPlayerObject`）

- 构造：`OrryxPlayer(playerName)`
- 包含上述所有方法的对象形式，额外支持 `getJobMaxLevel`, `getExperience`, `getMaxExperience`, `getGroup`, `getFlag`

---

## 四、功能对比

| 功能          |  DragonCore   | GermPlugin |     ArcartX      |
|-------------|:-------------:|:----------:|:----------------:|
| 触发器数量       |       7       |     3      |        10        |
| 按键事件        |    ✅ 按下/释放    |  ✅ 按下/释放   |  ✅ 按下/释放/简单/组合   |
| 鼠标事件        |       ❌       |     ❌      |        ✅         |
| 自定义数据包      |       ✅       |     ❌      |        ✅         |
| 实体加入/离开     |       ✅       |     ❌      |        ✅         |
| 槽位系统        |       ✅       |     ✅      |        ❌         |
| 动画系统        | ✅ 玩家/实体/物品/方块 | ✅ 实体/物品/方块 |       ✅ 实体       |
| 音效系统        |       ✅       |     ✅      |        ✅         |
| 粒子特效        |    ✅ 暴雪粒子     |  ✅ Effect  |        ❌         |
| UI 系统       |   ✅ GUI/HUD   | ✅ GUI/HUD  | ✅ open/close/run |
| 时装系统        |       ✅       |   ✅ 基岩版    |        ❌         |
| 模型系统        |     ✅ 完整      |     ❌      |       ✅ 基础       |
| 视角控制        |       ✅       |     ✅      |        ❌         |
| Placeholder |       ✅       |     ❌      |        ❌         |
| 脚本引擎        |       ❌       |     ❌      |    ✅ Glimmer     |
| 变量系统        |       ✅       |     ❌      |        ✅         |
| 屏幕震动        |       ✅       |     ❌      |        ✅         |
