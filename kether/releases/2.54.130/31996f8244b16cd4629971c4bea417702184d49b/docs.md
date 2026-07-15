# Orryx-2.54.130 脚本语句文档（自生成）

更多原生Kether语句请查看 https://kether.tabooproject.org/list.html

符号说明: `[*]` 代表可选 `<*>` 代表必选 `()` 代表默认值 前缀`*`代表先导词

# Kether Registry 类型系统

| 类型 ID | 父类型 | 可由 Kether expression 产生 | Raw 类型/提示 |
|---------|--------|----------------------------|---------------|
| any | 无 | 是 | java.lang.Object |
| boolean | scalar | 是 | java.lang.Boolean |
| byte | number | 是 | java.lang.Byte |
| collection | object | 是 | java.util.Collection |
| container | collection | 是 | org.gitee.orryx.core.container.Container |
| double | number | 是 | java.lang.Double |
| duration | scalar | 是 | java.time.Duration |
| effect | object | 是 | org.gitee.orryx.core.effect.Effect |
| effect-spawner | object | 是 | org.gitee.orryx.core.effect.EffectSpawner |
| entity | target | 是 | org.bukkit.entity.Entity |
| event | object | 否 | org.bukkit.event.Event（请使用 raw 原始值） |
| float | number | 是 | java.lang.Float |
| hitbox | object | 是 | org.gitee.orryx.api.collider.ICollider |
| int | number | 是 | java.lang.Integer |
| item-meta | object | 否 | org.bukkit.inventory.meta.ItemMeta（请使用 raw 原始值） |
| item-stack | object | 是 | org.bukkit.inventory.ItemStack |
| job | object | 否 | org.gitee.orryx.core.job.IJob（请使用 raw 原始值） |
| key-binding | object | 否 | com.germ.germplugin.api.bean.KeyBinding（请使用 raw 原始值） |
| keyword | scalar | 否 | kether.keyword（请使用 raw 原始值） |
| list | collection | 是 | java.lang.Iterable |
| living-entity | entity | 是 | org.bukkit.entity.LivingEntity |
| location | target | 是 | org.bukkit.Location |
| long | number | 是 | java.lang.Long |
| map | collection | 是 | java.util.Map |
| material | object | 否 | org.bukkit.Material（请使用 raw 原始值） |
| matrix | object | 是 | org.gitee.orryx.joml.Matrix3d |
| nbt | object | 否 | org.gitee.orryx.taboolib.module.nms.ItemTag（请使用 raw 原始值） |
| null | any | 是 | null |
| number | scalar | 是 | java.lang.Number |
| object | any | 是 | java.lang.Object |
| player | living-entity | 是 | org.bukkit.entity.Player |
| profile | object | 否 | org.gitee.orryx.api.profile.IProfile（请使用 raw 原始值） |
| quaternion | object | 是 | org.gitee.orryx.joml.Quaterniond |
| scalar | any | 是 | java.lang.Object |
| short | number | 是 | java.lang.Short |
| skill | object | 否 | org.gitee.orryx.core.skill.ISkill（请使用 raw 原始值） |
| skill-group | object | 否 | org.gitee.orryx.core.skill.SkillGroup（请使用 raw 原始值） |
| skill-parameter | object | 否 | org.gitee.orryx.core.skill.SkillParameter（请使用 raw 原始值） |
| state | object | 是 | org.gitee.orryx.module.state.IState |
| target | object | 是 | org.gitee.orryx.core.targets.ITarget |
| text | scalar | 是 | java.lang.String |
| typed-list | list | 是 | java.util.List |
| uuid | scalar | 是 | java.util.UUID |
| vector3 | object | 是 | org.gitee.orryx.api.adapters.vector.IVector |

# 技能YAML配置结构

## 通用字段（所有技能类型）

```yaml
Options:
  Type: "Direct"          # 技能类型: Direct(直接) | Direct Aim(指向性) | Pressing(蓄力) | Pressing Aim(蓄力指向性) | Passive(被动)
  Name: "技能名称"         # 显示名称（默认为文件名）
  Sort: 0                  # UI排序位置
  Icon: "图标"             # HUD图标（默认为Name）
  XMaterial: "BLAZE_ROD"   # 物品材质类型
  Description:             # 技能描述（*开头不预览下一级，{{}}内语句二级预览）
    - '&f技能等级&7: &e{{ &level }} &f级'
  IsLocked: false          # 是否需要解锁
  MinLevel: 1              # 最小等级
  MaxLevel: 5              # 最高等级
  UpgradePointAction: 1    # 升级消耗技能点（Kether表达式）
  UpLevelCheckAction: |-   # 升级检查（Kether脚本，返回Boolean）
    check orryx level >= calc "2+2*(to-1)"
  UpLevelSuccessAction: |- # 升级成功后执行（Kether脚本）
    tell "升级成功"
  IgnoreSilence: false     # 是否无视沉默
  Variables:               # 自定义变量（键自动转大写）
    Silence: 5             # 释放后沉默时间(tick)
    Mana: 5                # 法力消耗
    Cooldown: 5            # 冷却时间(tick)
```

## 可释放技能字段（Direct、Direct Aim、Pressing、Pressing Aim）

```yaml
Options:
  CastCheckAction: true    # 释放前检查（Kether脚本，返回Boolean）
Actions: |-                # 技能主逻辑（Kether脚本，#开头的行被过滤）
  damage 10 they "@range 5 !@self"
ExtendActions:             # 扩展动作（命名的Kether脚本）
  完成: |-
    tell "完成"
```

## 指向性技能字段（Direct Aim）

```yaml
Options:
  AimSizeAction: "5"       # 指示范围大小（Kether表达式）
  AimRadiusAction: "10"    # 指示原点最大半径（Kether表达式）
```

## 蓄力技能字段（Pressing、Pressing Aim）

```yaml
Options:
  Period: 10               # 蓄力周期（tick）
  MaxPressTickAction: "20" # 最大蓄力时间（Kether表达式）
  PressPeriodAction: |-    # 蓄力每周期执行（Kether脚本，可用&pressTick变量）
    tell &pressTick
  PressBrockTriggers:      # 蓄力打断触发器列表
    - "Player Damaged Post"
```

## 蓄力指向性技能字段（Pressing Aim）

```yaml
Options:
  AimMinAction: "5"        # 指示范围初始大小（Kether表达式）
  AimMaxAction: "10"       # 指示范围最大大小（Kether表达式）
  AimRadiusAction: "10"    # 指示原点最大半径（Kether表达式）
```

# AI智能

## aiChat

### 模拟Npc对话（公有语句）

> `aiChat <STRING> <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 模拟的npc |
| 无 | STRING | false | 对话信息 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 回复信息 |

> 模拟Npc对话，不同Npc人设请在npc.yml中配置，等待直到AI返回内容

# ArcartX附属语句

## arcartx

### 停止音效（公有语句）

> `arcartx sound stop <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| sound | SYMBOL | false | 音效标识符 |
| stop | SYMBOL | false | 停止标识符 |
| 无 | STRING | false | 音效名 |
| they | CONTAINER | true | 可听玩家 |

> 停止音效

### 关闭UI（公有语句）

> `arcartx ui close <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| ui | SYMBOL | false | ui标识符 |
| close | SYMBOL | false | 关闭标识符 |
| 无 | STRING | false | ui名字 |
| they | CONTAINER | true | 关闭UI的玩家 |

> 关闭ArcartX UI

### 发送自定义数据包（公有语句）

> `arcartx packet <STRING> <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| packet | SYMBOL | false | 数据包标识符 |
| 无 | STRING | false | 数据包ID |
| 无 | STRING | false | 数据内容(逗号分隔) |
| they | CONTAINER | true | 发送的玩家 |

> 发送自定义数据包

### 屏幕震动（公有语句）

> `arcartx shake <INT> <INT> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| shake | SYMBOL | false | 震动标识符 |
| 无 | INT | false | 震动强度 |
| 无 | INT | false | 震动时长tick |
| they | CONTAINER | true | 震动的玩家 |

> 屏幕震动

### 打开UI（公有语句）

> `arcartx ui open <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| ui | SYMBOL | false | ui标识符 |
| open | SYMBOL | false | 打开标识符 |
| 无 | STRING | false | ui名字 |
| they | CONTAINER | true | 打开UI的玩家 |

> 打开ArcartX UI

### 播放音效（公有语句）

> `arcartx sound send <STRING> *category [STRING(master)] *volume [FLOAT(1.0)] *pitch [FLOAT(1.0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| sound | SYMBOL | false | 音效标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 音效文件 |
| category | STRING | true | 音效类型 |
| volume | FLOAT | true | 音量 |
| pitch | FLOAT | true | 音调 |
| they | CONTAINER | true | 可听玩家 |

> 播放音效

### 移除服务端变量（公有语句）

> `arcartx variable/var remove <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| variable/var | SYMBOL | false | 变量标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 变量名 |
| they | CONTAINER | true | 移除的玩家 |

> 移除服务端变量

### 设置实体动画（公有语句）

> `arcartx animation/ani set/to <STRING> *speed [FLOAT(1.0)] *transition [INT(0)] *duration [LONG(-1)] *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动画标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| 无 | STRING | false | 动画名 |
| speed | FLOAT | true | 动画速度 |
| transition | INT | true | 过渡时间tick |
| duration | LONG | true | 持续时间tick |
| they | CONTAINER | false | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体动画

### 设置实体模型（公有语句）

> `arcartx model set/to <STRING> *scale [FLOAT(1.0)] *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| model | SYMBOL | false | 模型标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| 无 | STRING | false | 模型名 |
| scale | FLOAT | true | 模型缩放 |
| they | CONTAINER | false | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体模型

### 设置实体默认动画状态（公有语句）

> `arcartx animation/ani default <STRING> <STRING> *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动画标识符 |
| default | SYMBOL | false | 默认标识符 |
| 无 | STRING | false | 动画名 |
| 无 | STRING | false | 状态名 |
| they | CONTAINER | false | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体默认动画状态

### 设置服务端变量（公有语句）

> `arcartx variable/var set/to <STRING> <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| variable/var | SYMBOL | false | 变量标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| 无 | STRING | false | 变量名 |
| 无 | STRING | false | 变量值 |
| they | CONTAINER | true | 设置的玩家 |

> 设置服务端变量

### 设置窗口标题（公有语句）

> `arcartx title <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| title | SYMBOL | false | 标题标识符 |
| 无 | STRING | false | 标题内容 |
| they | CONTAINER | true | 设置的玩家 |

> 设置窗口标题

### 运行UI脚本（公有语句）

> `arcartx ui run <STRING> <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| ui | SYMBOL | false | ui标识符 |
| run | SYMBOL | false | 运行标识符 |
| 无 | STRING | false | ui名字 |
| 无 | STRING | false | 脚本内容 |
| they | CONTAINER | true | 执行的玩家 |

> 运行ArcartX UI脚本

# AstraXHero

## axhDamage

### AstraXHero攻击（公有语句）

> `axhDamage *doDamage [BOOLEAN(true)] *variable <STRING> *they <CONTAINER> *source [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| doDamage | BOOLEAN | true | 是否造成伤害 |
| variable | STRING | false | 战斗变量(格式: var1=1;var2=2) |
| they | CONTAINER | false | 防御者defender |
| source | CONTAINER | true | 攻击来源 |

> AstraXHero攻击

# AttributePlus

## apAttack

### ap3攻击（公有语句）

> `apAttack <BOOLEAN> *attributes [STRING] *they <CONTAINER> *source [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | BOOLEAN | false | 重置属性 |
| attributes | STRING | true | 属性(用,分割) |
| they | CONTAINER | false | 防御者defender |
| source | CONTAINER | true | 攻击来源 |

> ap3攻击

# Attribute属性

## attribute

### 更新实体属性（公有语句）

> `attribute *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 更新目标 |

> 更新实体属性

### 添加临时属性（公有语句）

> `attribute <STRING> <STRING> <LONG> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 识别key |
| 无 | STRING | false | 属性类似：(物理攻击: +10, 生命上限: +10) |
| 无 | LONG | false | 时长(-1为永久) |
| they | CONTAINER | true | 添加目标 |

> 添加临时属性

### 移除临时属性（公有语句）

> `attribute <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 识别key |
| they | CONTAINER | true | 添加目标 |

> 移除临时属性

# CloudPick附属语句

## cloudpick

### 停止播放音乐（公有语句）

> `cloudpick <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 音乐唯一ID |
| they | CONTAINER | true | 可听玩家 |

> 停止播放音乐

### 删除papi数据（公有语句）

> `cloudpick placeholders/papi delete/remove <STRING> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| placeholders/papi | SYMBOL | false | placeholder标识符 |
| delete/remove | SYMBOL | false | 删除标识符 |
| 无 | STRING | false | 删除的键 |
| 无 | BOOLEAN | false | 是否检测startWith键 |
| they | CONTAINER | true | 删除数据的玩家 |

> 删除客户端placeholder数据

```yaml
cp papi delete a,b,c false they "@self"
```

### 发送同步papi数据（公有语句）

> `cloudpick placeholders/papi send <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| placeholders/papi | SYMBOL | false | placeholder标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 存储了的数据的键，用逗号隔开 |
| they | CONTAINER | true | 发送数据的玩家 |

> 发送同步placeholder数据

```yaml
cp papi send a,b,c they "@self"
```

### 发送暴雪粒子（公有语句）

> `cloudpick effect/particle send <STRING> <STRING> *rotation <STRING(0,0,0)> *translate <VECTOR(0,0,0)> *timeout <INT(100)> *viewers [CONTAINER(@server)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect/particle | SYMBOL | false | 粒子标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 粒子ID |
| 无 | STRING | false | 粒子文件名 |
| rotation | STRING | false | x,y,z旋转角度 |
| translate | VECTOR | false | x,y,z平移位置 |
| timeout | INT | false | 存活时长tick |
| viewers | CONTAINER | true | 可视玩家 |
| they | CONTAINER | true | 生成位置或者绑定实体 |

> 发送暴雪粒子

### 实体模型特效移除（公有语句）

> `cloudpick modelEffect remove <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| modelEffect | SYMBOL | false | 实体模型标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 实体唯一ID |
| they | CONTAINER | true | 绑定的实体 |

> 实体模型特效移除

### 实体模型特效绑定（公有语句）

> `cloudpick modelEffect create <STRING> <STRING> <LONG> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| modelEffect | SYMBOL | false | 实体模型标识符 |
| create | SYMBOL | false | 创建标识符 |
| 无 | STRING | false | 实体唯一ID |
| 无 | STRING | false | 模型匹配名 |
| 无 | LONG | false | 延迟消失时间 |
| they | CONTAINER | true | 绑定实体 |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 绑定的实体容器 |

> 实体模型特效绑定，脚本运行时间必须大于延迟消失时间，若提前停止将会直接回收实体

### 打开GUI（公有语句）

> `cloudpick gui <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| gui | SYMBOL | false | gui标识符 |
| 无 | STRING | false | gui名字 |
| they | CONTAINER | true | 打开GUI的玩家 |

> 打开云拾Gui

### 打开HUD（公有语句）

> `cloudpick hud <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| hud | SYMBOL | false | hud标识符 |
| 无 | STRING | false | hud名字 |
| they | CONTAINER | true | 打开HUD的玩家 |

> 打开云拾HUD

### 播放音乐（公有语句）

> `cloudpick sound send <STRING> <STRING> <STRING> *loc [VECTOR(可听玩家眼睛位置)] *loop [BOOLEAN(false)] *by/with [FLOAT(1.0)] [FLOAT(1.0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| sound | SYMBOL | false | 音乐标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 音乐唯一ID |
| 无 | STRING | false | 音乐文件位置 |
| 无 | STRING | false | 播放类型 |
| loc | VECTOR | true | 播放世界位置向量 |
| loop | BOOLEAN | true | 是否循环 |
| by/with | FLOAT | true | 声音大小 |
| 无 | FLOAT | true | 声音音调 |
| they | CONTAINER | true | 可听玩家 |

> 播放音乐

### 更新时装（公有语句）

> `cloudpick armourers update *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| armourers | SYMBOL | false | armourers标识符 |
| update | SYMBOL | false | 更新标识符 |
| they | CONTAINER | true | 目标容器 |

> 更新时装

### 清除临时时装（公有语句）

> `cloudpick armourers clear [STRING(ALL)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| armourers | SYMBOL | false | armourers标识符 |
| clear | SYMBOL | false | 清除标识符 |
| 无 | STRING | true | 时装名 |
| they | CONTAINER | true | 目标容器 |

> 清除临时时装

### 清除暴雪粒子（公有语句）

> `cloudpick effect/particle clear *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect/particle | SYMBOL | false | 粒子标识符 |
| clear | SYMBOL | false | 清除标识符 |
| they | CONTAINER | true | 可视玩家 |

> 清除暴雪粒子

### 移除headTag（公有语句）

> `cloudpick headtag/tag remove <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| headtag/tag | SYMBOL | false | headTag标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 移除的实体uuid |
| viewers | CONTAINER | true | 可视玩家 |

> 移除实体的headTag

### 移除实体动作（公有语句）

> `cloudpick animation/ani remove entity <STRING> <INT> *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| remove | SYMBOL | false | 移除标识符 |
| entity | SYMBOL | false | 实体标识符 |
| 无 | STRING | false | 动作名 |
| 无 | INT | false | 过渡时间 |
| they | CONTAINER | false | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 移除实体动作

### 移除实体模型（公有语句）

> `cloudpick model remove <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| model | SYMBOL | false | 模型标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 移除的实体uuid |
| viewers | CONTAINER | true | 可视玩家 |

> 移除实体模型

### 移除暴雪粒子（公有语句）

> `cloudpick effect/particle remove <STRING> *they [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect/particle | SYMBOL | false | 粒子标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 粒子ID |
| they | CONTAINER | true | 可视玩家 |

> 移除暴雪粒子

### 移除玩家动作（公有语句）

> `cloudpick animation/ani remove player *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| remove | SYMBOL | false | 移除标识符 |
| player | SYMBOL | false | 玩家标识符 |
| they | CONTAINER | true | 移除玩家 |

> 移除玩家动作

### 获取槽位内物品（公有语句）

> `cloudpick slot <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| slot | SYMBOL | false | 槽位标识符 |
| 无 | STRING | false | 槽位名 |
| they | CONTAINER | true | 获取的玩家 |

> 获取槽位内物品

### 虚拟绑定实体位置（公有语句）

> `cloudpick bindEntity/bind <STRING> <STRING> <VECTOR> <BOOLEAN> <BOOLEAN> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| bindEntity/bind | SYMBOL | false | 绑定标识符 |
| 无 | STRING | false | 被绑定的实体UUID |
| 无 | STRING | false | 绑定到的实体UUID |
| 无 | VECTOR | false | 偏移向量 |
| 无 | BOOLEAN | false | 是否绑定yaw角 |
| 无 | BOOLEAN | false | 是否绑定pitch角 |
| viewers | CONTAINER | true | 可视玩家 |

> 虚拟绑定实体位置

### 设置headTag（公有语句）

> `cloudpick headtag/tag set/to <STRING> <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| headtag/tag | SYMBOL | false | headTag标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| 无 | STRING | false | 设置的实体uuid |
| 无 | STRING | false | 匹配名 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体的headTag

### 设置临时时装（公有语句）

> `cloudpick armourers send <STRING> *timeout [LONG(100)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| armourers | SYMBOL | false | armourers标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 时装名 |
| timeout | LONG | true | 临时时长 |
| they | CONTAINER | true | 目标容器 |

> 设置临时时装

### 设置实体动作（公有语句）

> `cloudpick animation/ani set/to entity <STRING> <INT> <FLOAT> *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| entity | SYMBOL | false | 实体标识符 |
| 无 | STRING | false | 动作名 |
| 无 | INT | false | 过渡时间 |
| 无 | FLOAT | false | 动作速度 |
| they | CONTAINER | false | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体动作

### 设置实体模型（公有语句）

> `cloudpick model set/to <STRING> <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| model | SYMBOL | false | 模型标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| 无 | STRING | false | 设置的实体uuid |
| 无 | STRING | false | 匹配名 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体模型

### 设置方块动作（公有语句）

> `cloudpick animation/ani set block <STRING> <VECTOR> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set | SYMBOL | false | 设置标识符 |
| block | SYMBOL | false | 方块标识符 |
| 无 | STRING | false | 动作名 |
| 无 | VECTOR | false | xyz位置 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置方块动作

### 设置玩家动作（公有语句）

> `cloudpick animation/ani set/to player <STRING> <FLOAT> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| player | SYMBOL | false | 玩家标识符 |
| 无 | STRING | false | 动作名 |
| 无 | FLOAT | false | 动作速度 |
| they | CONTAINER | true | 设置玩家 |

> 设置玩家动作

### 设置玩家手持物品动作（公有语句）

> `cloudpick animation/ani set item <STRING> <FLOAT> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set | SYMBOL | false | 设置标识符 |
| item | SYMBOL | false | 物品标识符 |
| 无 | STRING | false | 动作名 |
| 无 | FLOAT | false | 动作速度 |
| they | CONTAINER | true | 设置实体 |

> 设置玩家手持物品动作

### 运行云拾GUI方法（公有语句）

> `cloudpick function/func gui <STRING> <STRING> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| function/func | SYMBOL | false | 方法标识符 |
| gui | SYMBOL | false | gui标识符 |
| 无 | STRING | false | gui名字 |
| 无 | STRING | false | 方法语句 |
| 无 | BOOLEAN | false | 是否异步执行 |
| they | CONTAINER | true | 客户端参与执行的玩家 |

> 运行云拾GUI方法

### 运行云拾headTag方法（公有语句）

> `cloudpick function/func headtag/tag <STRING> <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| function/func | SYMBOL | false | 方法标识符 |
| headtag/tag | SYMBOL | false | tag标识符 |
| 无 | STRING | false | 执行实体UUID |
| 无 | STRING | false | 方法语句 |
| they | CONTAINER | true | 客户端参与执行的玩家 |

> 运行云拾headTag方法

### 运行云拾动作控制器方法（公有语句）

> `cloudpick function/func animation/ani <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| function/func | SYMBOL | false | 方法标识符 |
| animation/ani | SYMBOL | false | 动作标识符 |
| 无 | STRING | false | 方法语句 |
| they | CONTAINER | true | 执行的玩家 |

> 运行云拾动作控制器方法

### 隐藏玩家手持武器（公有语句）

> `cloudpick invisibleHand <LONG> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| invisibleHand | SYMBOL | false | 隐藏手持标识符 |
| 无 | LONG | false | 隐藏时间 0 为取消 |
| they | CONTAINER | true | 取消的玩家 |

> 隐藏玩家手持武器

# Container容器

## contain

### 检测包含（公有语句）

> `contain <CONTAINER> *they <CONTAINER>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | CONTAINER | false | 实体或容器 |
| they | CONTAINER | false | 被检测的容器 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否包含 |

> 检测Container容器中是否包含另一个容器中的目标，或是否包含实体

## container

### 目标容器（公有语句）

> `container *they [CONTAINER]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 被复制的容器 |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 创建的容器 |

> 创建/复制 一个Container容器，用于储存各类Target

## merge

### 合并目标容器（公有语句）

> `merge <CONTAINER> *they <CONTAINER>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | CONTAINER | false | 合并到的Container容器 |
| they | CONTAINER | false | 被合并的Container容器 |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 合并后的容器 |

> 合并两个Container容器

## mergeIf

### 合并如果目标容器（公有语句）

> `mergeIf <CONTAINER> *they <CONTAINER> <BOOLEAN>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | CONTAINER | false | 合并到的Container容器 |
| they | CONTAINER | false | 被合并的Container容器 |
| 无 | BOOLEAN | false | 方法体返回布尔类型 |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 合并后的容器 |

> 合并两个Container容器，剔除被合并容器中返回false的目标，可读取@Target参数

## removeIf

### 删除如果目标容器（公有语句）

> `removeIf *they <CONTAINER> <BOOLEAN>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | false | 被检测的Container容器 |
| 无 | BOOLEAN | false | 方法体返回布尔类型 |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 被删除后的容器 |

> 检测Container容器，删除返回值为true的Target，可读取@Target参数

## stream

### 流过流式选择器（公有语句）

> `stream <STRING> *they <CONTAINER>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 选择器文本 |
| they | CONTAINER | false | 被检测的Container容器 |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 经过流后的容器 |

> 将指定容器流过流式选择器

# Cooldown冷却

## cooldown

### 减少冷却（私有语句）

> `cooldown take <LONG> *key [STRING(当前)] *type [STRING(当前)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| take | SYMBOL | false | 减少标识符 |
| 无 | LONG | false | 冷却值 |
| key | STRING | true | 技能/中转站 |
| type | STRING | true | 类型 skill/station |

> 减少玩家技能/中转站冷却

### 检测冷却（私有语句）

> `cooldown has *key [STRING(当前)] *type [STRING(当前)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| has | SYMBOL | false | 检测标识符 |
| key | STRING | true | 检测的技能/中转站 |
| type | STRING | true | 类型 skill/station |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否在冷却中 |

> 检测技能/中转站是否在冷却中

### 给予冷却（私有语句）

> `cooldown add <LONG> *key [STRING(当前)] *type [STRING(当前)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| add | SYMBOL | false | 给予标识符 |
| 无 | LONG | false | 冷却值 |
| key | STRING | true | 技能/中转站 |
| type | STRING | true | 类型 skill/station |

> 给予玩家技能/中转站冷却

### 获取倒计时（私有语句）

> `cooldown get/countdown *key [STRING(当前)] *type [STRING(当前)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| get/countdown | SYMBOL | true | 获取标识符 |
| key | STRING | true | 技能/中转站 |
| type | STRING | true | 类型 skill/station |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 倒计时 |

> 获取玩家技能/中转站冷却倒计时

### 设置冷却（私有语句）

> `cooldown set/to <LONG> *key [STRING(当前)] *type [STRING(当前)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| set/to | SYMBOL | false | 设置标识符 |
| 无 | LONG | false | 冷却值 |
| key | STRING | true | 技能/中转站 |
| type | STRING | true | 类型 skill/station |

> 设置玩家技能/中转站冷却

### 重置冷却（私有语句）

> `cooldown reset`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| reset | SYMBOL | false | 重置标识符 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 新的冷却值 |

> 重置玩家技能/中转站冷却

# Coroutine协程

## inMain

### 检测线程（公有语句）

> `inMain`

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否在主线程 |

> 检测当前位置是否在主线程运行

# DragonCore附属语句

## dragoncore

### 停止播放音乐（公有语句）

> `dragoncore <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 音乐唯一ID |
| they | CONTAINER | true | 可听玩家 |

> 停止播放音乐

### 删除papi数据（公有语句）

> `dragoncore placeholders/papi delete/remove <STRING> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| placeholders/papi | SYMBOL | false | placeholder标识符 |
| delete/remove | SYMBOL | false | 删除标识符 |
| 无 | STRING | false | 删除的键 |
| 无 | BOOLEAN | false | 是否检测startWith键 |
| they | CONTAINER | true | 删除数据的玩家 |

> 删除客户端placeholder数据

```yaml
dragon papi delete a,b,c false they "@self"
```

### 发送同步papi数据（公有语句）

> `dragoncore placeholders/papi send <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| placeholders/papi | SYMBOL | false | placeholder标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 存储了的数据的键，用逗号隔开 |
| they | CONTAINER | true | 发送数据的玩家 |

> 发送同步placeholder数据

```yaml
dragon papi send a,b,c they "@self"
```

### 发送暴雪粒子（公有语句）

> `dragoncore effect/particle send <STRING> <STRING> *rotation <STRING(0,0,0)> *translate <VECTOR(0,0,0)> *timeout <INT(100)> *viewers [CONTAINER(@server)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect/particle | SYMBOL | false | 粒子标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 粒子ID |
| 无 | STRING | false | 粒子文件名 |
| rotation | STRING | false | x,y,z旋转角度 |
| translate | VECTOR | false | x,y,z平移位置 |
| timeout | INT | false | 存活时长tick |
| viewers | CONTAINER | true | 可视玩家 |
| they | CONTAINER | true | 生成位置或者绑定实体 |

> 发送暴雪粒子

### 实体模型特效移除（公有语句）

> `dragoncore modelEffect remove <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| modelEffect | SYMBOL | false | 实体模型标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 实体唯一ID |
| they | CONTAINER | true | 绑定的实体 |

> 实体模型特效移除

### 实体模型特效绑定（公有语句）

> `dragoncore modelEffect create <STRING> <STRING> <LONG> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| modelEffect | SYMBOL | false | 实体模型标识符 |
| create | SYMBOL | false | 创建标识符 |
| 无 | STRING | false | 实体唯一ID |
| 无 | STRING | false | 模型匹配名 |
| 无 | LONG | false | 延迟消失时间 |
| they | CONTAINER | true | 绑定实体 |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 绑定的实体容器 |

> 实体模型特效绑定，脚本运行时间必须大于延迟消失时间，若提前停止将会直接回收实体

### 打开GUI（公有语句）

> `dragoncore gui <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| gui | SYMBOL | false | gui标识符 |
| 无 | STRING | false | gui名字 |
| they | CONTAINER | true | 打开GUI的玩家 |

> 打开龙核Gui

### 打开HUD（公有语句）

> `dragoncore hud <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| hud | SYMBOL | false | hud标识符 |
| 无 | STRING | false | hud名字 |
| they | CONTAINER | true | 打开HUD的玩家 |

> 打开龙核HUD

### 播放音乐（公有语句）

> `dragoncore sound send <STRING> <STRING> <STRING> *loc [VECTOR(可听玩家眼睛位置)] *loop [BOOLEAN(false)] *by/with [FLOAT(1.0)] [FLOAT(1.0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| sound | SYMBOL | false | 音乐标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 音乐唯一ID |
| 无 | STRING | false | 音乐文件位置 |
| 无 | STRING | false | 播放类型 |
| loc | VECTOR | true | 播放世界位置向量 |
| loop | BOOLEAN | true | 是否循环 |
| by/with | FLOAT | true | 声音大小 |
| 无 | FLOAT | true | 声音音调 |
| they | CONTAINER | true | 可听玩家 |

> 播放音乐

### 更新时装（公有语句）

> `dragoncore armourers update *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| armourers | SYMBOL | false | armourers标识符 |
| update | SYMBOL | false | 更新标识符 |
| they | CONTAINER | true | 目标容器 |

> 更新时装

### 清除临时时装（公有语句）

> `dragoncore armourers clear [STRING(ALL)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| armourers | SYMBOL | false | armourers标识符 |
| clear | SYMBOL | false | 清除标识符 |
| 无 | STRING | true | 时装名 |
| they | CONTAINER | true | 目标容器 |

> 清除临时时装

### 清除暴雪粒子（公有语句）

> `dragoncore effect/particle clear *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect/particle | SYMBOL | false | 粒子标识符 |
| clear | SYMBOL | false | 清除标识符 |
| they | CONTAINER | true | 可视玩家 |

> 清除暴雪粒子

### 移除headTag（公有语句）

> `dragoncore headtag/tag remove <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| headtag/tag | SYMBOL | false | headTag标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 移除的实体uuid |
| viewers | CONTAINER | true | 可视玩家 |

> 移除实体的headTag

### 移除实体动作（公有语句）

> `dragoncore animation/ani remove entity <STRING> <INT> *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| remove | SYMBOL | false | 移除标识符 |
| entity | SYMBOL | false | 实体标识符 |
| 无 | STRING | false | 动作名 |
| 无 | INT | false | 过渡时间 |
| they | CONTAINER | false | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 移除实体动作

### 移除实体模型（公有语句）

> `dragoncore model remove <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| model | SYMBOL | false | 模型标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 移除的实体uuid |
| viewers | CONTAINER | true | 可视玩家 |

> 移除实体模型

### 移除暴雪粒子（公有语句）

> `dragoncore effect/particle remove <STRING> *they [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect/particle | SYMBOL | false | 粒子标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | 粒子ID |
| they | CONTAINER | true | 可视玩家 |

> 移除暴雪粒子

### 移除玩家动作（公有语句）

> `dragoncore animation/ani remove player *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| remove | SYMBOL | false | 移除标识符 |
| player | SYMBOL | false | 玩家标识符 |
| they | CONTAINER | true | 移除玩家 |

> 移除玩家动作

### 获取槽位内物品（公有语句）

> `dragoncore slot <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| slot | SYMBOL | false | 槽位标识符 |
| 无 | STRING | false | 槽位名 |
| they | CONTAINER | true | 获取的玩家 |

> 获取槽位内物品

### 虚拟绑定实体位置（公有语句）

> `dragoncore bindEntity/bind <STRING> <STRING> <VECTOR> <BOOLEAN> <BOOLEAN> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| bindEntity/bind | SYMBOL | false | 绑定标识符 |
| 无 | STRING | false | 被绑定的实体UUID |
| 无 | STRING | false | 绑定到的实体UUID |
| 无 | VECTOR | false | 偏移向量 |
| 无 | BOOLEAN | false | 是否绑定yaw角 |
| 无 | BOOLEAN | false | 是否绑定pitch角 |
| viewers | CONTAINER | true | 可视玩家 |

> 虚拟绑定实体位置

### 设置headTag（公有语句）

> `dragoncore headtag/tag set/to <STRING> <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| headtag/tag | SYMBOL | false | headTag标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| 无 | STRING | false | 设置的实体uuid |
| 无 | STRING | false | 匹配名 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体的headTag

### 设置windows窗口标题（公有语句）

> `dragoncore title <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| title | SYMBOL | false | 窗口title标识符 |
| 无 | STRING | false | 标题 |
| they | CONTAINER | true | 设置标题的玩家 |

> 设置windows窗口标题

### 设置临时时装（公有语句）

> `dragoncore armourers send <STRING> *timeout [LONG(100)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| armourers | SYMBOL | false | armourers标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 时装名 |
| timeout | LONG | true | 临时时长 |
| they | CONTAINER | true | 目标容器 |

> 设置临时时装

### 设置实体动作（公有语句）

> `dragoncore animation/ani set/to entity <STRING> <INT> <FLOAT> *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| entity | SYMBOL | false | 实体标识符 |
| 无 | STRING | false | 动作名 |
| 无 | INT | false | 过渡时间 |
| 无 | FLOAT | false | 动作速度 |
| they | CONTAINER | false | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体动作

### 设置实体模型（公有语句）

> `dragoncore model set/to <STRING> <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| model | SYMBOL | false | 模型标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| 无 | STRING | false | 设置的实体uuid |
| 无 | STRING | false | 匹配名 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体模型

### 设置方块动作（公有语句）

> `dragoncore animation/ani set block <STRING> <VECTOR> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set | SYMBOL | false | 设置标识符 |
| block | SYMBOL | false | 方块标识符 |
| 无 | STRING | false | 动作名 |
| 无 | VECTOR | false | xyz位置 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置方块动作

### 设置玩家动作（公有语句）

> `dragoncore animation/ani set/to player <STRING> <FLOAT> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| player | SYMBOL | false | 玩家标识符 |
| 无 | STRING | false | 动作名 |
| 无 | FLOAT | false | 动作速度 |
| they | CONTAINER | true | 设置玩家 |

> 设置玩家动作

### 设置玩家手持物品动作（公有语句）

> `dragoncore animation/ani set item <STRING> <FLOAT> *they [CONTAINER(@self)] *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set | SYMBOL | false | 设置标识符 |
| item | SYMBOL | false | 物品标识符 |
| 无 | STRING | false | 动作名 |
| 无 | FLOAT | false | 动作速度 |
| they | CONTAINER | true | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置玩家手持物品动作

### 设置视角（公有语句）

> `dragoncore view <INT> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| view | SYMBOL | false | 视角标识符 |
| 无 | INT | false | 视角(1,2,3) |
| they | CONTAINER | true | 设置人称的玩家 |

> 设置第几人称视角

### 运行龙核GUI方法（公有语句）

> `dragoncore function/func gui <STRING> <STRING> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| function/func | SYMBOL | false | 方法标识符 |
| gui | SYMBOL | false | gui标识符 |
| 无 | STRING | false | gui名字 |
| 无 | STRING | false | 方法语句 |
| 无 | BOOLEAN | false | 是否异步执行 |
| they | CONTAINER | true | 客户端参与执行的玩家 |

> 运行龙核GUI方法

### 运行龙核headTag方法（公有语句）

> `dragoncore function/func headtag/tag <STRING> <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| function/func | SYMBOL | false | 方法标识符 |
| headtag/tag | SYMBOL | false | tag标识符 |
| 无 | STRING | false | 执行实体UUID |
| 无 | STRING | false | 方法语句 |
| they | CONTAINER | true | 客户端参与执行的玩家 |

> 运行龙核headTag方法

### 运行龙核动作控制器方法（公有语句）

> `dragoncore function/func animation/ani <STRING> <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| function/func | SYMBOL | false | 方法标识符 |
| animation/ani | SYMBOL | false | 动作标识符 |
| 无 | STRING | false | 执行实体UUID |
| 无 | STRING | false | 方法语句 |
| they | CONTAINER | true | 客户端参与执行的玩家 |

> 运行龙核动作控制器方法

### 隐藏玩家手持武器（公有语句）

> `dragoncore invisibleHand <LONG> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| invisibleHand | SYMBOL | false | 隐藏手持标识符 |
| 无 | LONG | false | 隐藏时间 0 为取消 |
| they | CONTAINER | true | 取消的玩家 |

> 隐藏玩家手持武器

# Effect粒子效果

## draw

### 设置BlockData（公有语句）

> `draw blockData <STRING> <INT>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| blockData | SYMBOL | false | 占位符 |
| 无 | STRING | false | 材质名 |
| 无 | INT | false | data |

> 设置BlockData

### 设置DustTransitionData（公有语句）

> `draw dustTransitionData <STRING> <STRING> <FLOAT>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| dustTransitionData | SYMBOL | false | 占位符 |
| 无 | STRING | false | 粒子颜色color "255 255 255" |
| 无 | STRING | false | 粒子颜色toColor "255 255 255" |
| 无 | FLOAT | false | 粒子大小size |

> 设置DustTransitionData

### 设置ItemData（公有语句）

> `draw itemData <STRING> <INT> <STRING> <STRING> <INT>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| itemData | SYMBOL | false | 占位符 |
| 无 | STRING | false | 材质名 |
| 无 | INT | false | data |
| 无 | STRING | false | 物品名字 |
| 无 | STRING | false | 物品描述 |
| 无 | INT | false | customModelData |

> 设置ItemData

### 设置VibrationData（公有语句）

> `draw vibrationData <CONTAINER> <INT> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| vibrationData | SYMBOL | false | 占位符 |
| 无 | CONTAINER | false | origin |
| 无 | INT | false | 到达时间 |
| they | CONTAINER | true | 目标 |

> 设置VibrationData

### 设置位移向量（公有语句）

> `draw translate <VECTOR>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| translate | SYMBOL | false | 占位符 |
| 无 | VECTOR | false | vector |

> 设置位移向量

### 设置偏移向量（公有语句）

> `draw offset <VECTOR>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| offset | SYMBOL | false | 占位符 |
| 无 | VECTOR | false | vector |

> 设置偏移向量

### 设置变换矩阵（公有语句）

> `draw transform <MATRIX>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| transform | SYMBOL | false | 矩阵变换占位符 |
| 无 | MATRIX | false | 矩阵 |

> 设置变换矩阵

### 设置粒子参数（公有语句）

> `draw particle <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| particle | SYMBOL | false | 粒子占位符 |
| 无 | STRING | false | 粒子基础参数 |

> 设置粒子基础参数

### 设置红石粒子数据（公有语句）

> `draw dustData <STRING> <FLOAT>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| dustData | SYMBOL | false | 红石数据占位符 |
| 无 | STRING | false | 粒子颜色color "255 255 255" |
| 无 | FLOAT | false | 粒子大小size |

> 设置红石粒子数据

### 设置贝塞尔曲线途经点（公有语句）

> `draw locations *they <CONTAINER>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| locations | SYMBOL | false | 途经点占位符 |
| they | CONTAINER | false | 贝塞尔曲线途经点 |

> 设置贝塞尔曲线途经点

## effect

### 停止显示粒子（公有语句）

> `effect stop <EFFECT_SPAWNER>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| stop | SYMBOL | false | 停止显示占位符 |
| 无 | EFFECT_SPAWNER | false | 粒子生成器 |

| 返回值类型 | 描述 |
|-----------|------|
| EFFECT_SPAWNER | 粒子生成器 |

> 停止显示粒子

### 创建临时粒子效果构建器（公有语句）

> `effect temp <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| temp | SYMBOL | false | 临时占位符 |
| 无 | ANY | false | 画板语句 |

| 返回值类型 | 描述 |
|-----------|------|
| EFFECT | 粒子效果构建器 |

> 创建临时粒子效果构建器

### 创建指定名粒子效果构建器（公有语句）

> `effect create/new <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| create/new | SYMBOL | false | 创建占位符 |
| 无 | ANY | false | 画板语句 |

| 返回值类型 | 描述 |
|-----------|------|
| EFFECT | 粒子效果构建器 |

> 创建指定名粒子效果构建器，并存储到键名中

### 微调粒子效果构建器（公有语句）

> `effect trim <EFFECT> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| trim | SYMBOL | false | 微调占位符 |
| 无 | EFFECT | false | 特效构建器 |
| 无 | ANY | false | 画板语句 |

| 返回值类型 | 描述 |
|-----------|------|
| EFFECT | 粒子效果构建器 |

> 微调粒子效果构建器

### 显示粒子（公有语句）

> `effect show <EFFECT> *duration [LONG(1)] *period [LONG(1)] *they [CONTAINER(@self)] *viewer [CONTAINER(@world)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| show | SYMBOL | false | 显示占位符 |
| 无 | EFFECT | false | 粒子效果构建器 |
| duration | LONG | true | 粒子显示时长，默认单次 |
| period | LONG | true | 粒子显示周期 |
| they | CONTAINER | true | 粒子显示位置 |
| viewer | CONTAINER | true | 粒子可视者 |

| 返回值类型 | 描述 |
|-----------|------|
| EFFECT_SPAWNER | 粒子生成器 |

> 创建粒子生成器并显示粒子

# Entity实体操作

## entity

### 生成Ady实体（私有语句）

> `entity ady <STRING> <STRING> *vector [VECTOR(0,0,0)] *gravity [BOOLEAN(true)] *timeout [LONG(0)] *they [CONTAINER(@self)] *viewer [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| ady | SYMBOL | false | ady标识符 |
| 无 | STRING | false | 实体名 |
| 无 | STRING | false | 实体类型 |
| vector | VECTOR | true | 实体冲量 |
| gravity | BOOLEAN | true | 实体是否具有重力 |
| timeout | LONG | true | 实体存在时间(0为永久) |
| they | CONTAINER | true | 实体生成位置 |
| viewer | CONTAINER | true | 能看到实体的玩家（默认私有） |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 生成的实体列表 |

> 生成Ady实体，脚本运行时间必须大于实体存在时间，若提前停止将会直接回收实体

### 生成原版实体（私有语句）

> `entity spawn <STRING> <STRING> *health [DOUBLE(0.0)] *vector [VECTOR(0,0,0)] *gravity [BOOLEAN(true)] *timeout [LONG(0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| spawn | SYMBOL | false | 生成标识符 |
| 无 | STRING | false | 实体名 |
| 无 | STRING | false | 实体类型 |
| health | DOUBLE | true | 实体血量 |
| vector | VECTOR | true | 实体冲量 |
| gravity | BOOLEAN | true | 实体是否具有重力 |
| timeout | LONG | true | 实体存在时间(0为永久) |
| they | CONTAINER | true | 实体生成位置 |

| 返回值类型 | 描述 |
|-----------|------|
| CONTAINER | 生成的实体列表 |

> 生成原版实体，脚本运行时间必须大于实体存在时间，若提前停止将会直接回收实体

### 移除实体（私有语句）

> `entity remove/destroy *they <CONTAINER>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| remove/destroy | SYMBOL | false | 移除标识符 |
| they | CONTAINER | false | 实体 |

> 移除实体

### 获取实体参数（私有语句）

> `entity <STRING(uuid)> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 实体参数key |
| they | CONTAINER | true | 被获取的实体 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 指定参数 |

> 获取实体参数

# Flag数据标签

## flag

### 创建数据（公有语句）

> `flag <STRING> set/to <ANY> *pst [BOOLEAN(false)] *timeout [LONG(0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| set/to | SYMBOL | false | 创建占位符 |
| 无 | ANY | false | 数据 |
| pst | BOOLEAN | true | 是否持久化，默认false |
| timeout | LONG | true | 存活时长，默认永久 |
| they | CONTAINER | true | 创建的玩家 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 数据 |

> 创建一个存储任意类型数据的标签，可持久化存储向量,矩阵,Bukkit实体,Ady实体,时间,和所有基础类型

### 删除数据标签（公有语句）

> `flag <STRING> remove/delete *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| remove/delete | SYMBOL | false | 删除占位符 |
| they | CONTAINER | true | 删除的玩家 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 数据 |

> 删除数据标签

### 清除所有数据标签（公有语句）

> `flag clear *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| clear | SYMBOL | false | 清除占位符 |
| they | CONTAINER | true | 清除的玩家 |

> 清除所有数据标签

### 获取数据剩余存活时间（公有语句）

> `flag <STRING> countdown *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| countdown | SYMBOL | false | 剩余存活占位符 |
| they | CONTAINER | true | 获取的玩家 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 数据剩余存活时间(Tick) |

> 获取数据剩余存活时间

### 获取数据存活时间（公有语句）

> `flag <STRING> survival *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| survival | SYMBOL | false | 存活占位符 |
| they | CONTAINER | true | 获取的玩家 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 数据存活时间(Tick) |

> 获取数据存活时间

### 获取数据标签（公有语句）

> `flag <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| they | CONTAINER | true | 获取的玩家 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 数据 |

> 获取数据标签

# Game原版游戏

## boots

### 获取玩家脚上物品（公有语句）

> `boots *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| ITEM_STACK | 物品 |

> 获取玩家脚上物品

## chestplate

### 获取玩家胸上物品（公有语句）

> `chestplate *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| ITEM_STACK | 物品 |

> 获取玩家胸上物品

## direct

### 给予视角冲量（参考pitch）（公有语句）

> `direct <DOUBLE> <DOUBLE> <DOUBLE> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 视角前方冲量大小 |
| 无 | DOUBLE | false | 视角上方冲量大小 |
| 无 | DOUBLE | false | 视角右方冲量大小 |
| 无 | BOOLEAN | false | 是否叠加原冲量 |
| they | CONTAINER | true | 目标容器 |

> 给予视角冲量

## drag

### 向原点聚拢（私有语句）

> `drag <DOUBLE> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 冲量大小系数 |
| 无 | BOOLEAN | false | 是否叠加原冲量 |
| they | CONTAINER | true | 目标容器 |

> 给予向原点聚拢的冲量

## flash

### 给予视角瞬移量（公有语句）

> `flash <DOUBLE> <DOUBLE> <DOUBLE> <VECTOR> *offset/os <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 视角前方移动距离 |
| 无 | DOUBLE | false | 视角上方移动距离 |
| 无 | DOUBLE | false | 视角右方移动距离 |
| 无 | VECTOR | false | 视角向量 |
| offset/os | DOUBLE | false | 偏移yaw和pitch(os 90.0 90.0) |
| they | CONTAINER | true | 目标容器 |

> 给予视角瞬移量

## flyHeight

### 获得玩家离地面高度（公有语句）

> `flyHeight *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 目标容器 |

> 获得玩家离地面高度

## helmet

### 获取玩家头上物品（公有语句）

> `helmet *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| ITEM_STACK | 物品 |

> 获取玩家光标物品

## itemInMainHand

### 获取玩家主手物品（公有语句）

> `itemInMainHand *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| ITEM_STACK | 物品 |

> 获取玩家主手物品

## itemInOffHand

### 获取玩家副手物品（公有语句）

> `itemInOffHand *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| ITEM_STACK | 物品 |

> 获取玩家副手物品

## itemOnCursor

### 获取玩家光标物品（公有语句）

> `itemOnCursor *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| ITEM_STACK | 物品 |

> 获取玩家光标物品

## launch

### 给予视角冲量（不参考pitch）（公有语句）

> `launch <DOUBLE> <DOUBLE> <DOUBLE> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 视角前方冲量大小 |
| 无 | DOUBLE | false | 视角上方冲量大小 |
| 无 | DOUBLE | false | 视角右方冲量大小 |
| 无 | BOOLEAN | false | 是否叠加原冲量 |
| they | CONTAINER | true | 目标容器 |

> 给予视角冲量

## leggings

### 获取玩家腿上物品（公有语句）

> `leggings *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| ITEM_STACK | 物品 |

> 获取玩家腿上物品

## lookAt

### 让实体看向某处（公有语句）

> `lookAt <VECTOR> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | VECTOR | false | 目标处的世界原点向量 |
| they | CONTAINER | true | 目标容器 |

> 让实体看向某处

## nbt

### 获取物品 nbt（公有语句）

> `nbt <ITEM_STACK>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITEM_STACK | false | 物品 |

| 返回值类型 | 描述 |
|-----------|------|
| NBT | 物品 nbt |

> 获取物品 nbt

```yaml
set a to nbt itemInMainHand
tell &a[key]
set &a[key] to 极品
```

## potion

### 删除药水效果（公有语句）

> `potion remove/delete <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| remove/delete | SYMBOL | false | 删除标识符 |
| 无 | STRING | false | 效果 |
| they | CONTAINER | true | 实体 |

> 删除药水效果

### 清除所有药水效果（公有语句）

> `potion clear *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| clear | SYMBOL | false | 清除标识符 |
| they | CONTAINER | true | 实体 |

> 清除所有药水效果

### 设置药水效果（公有语句）

> `potion set <STRING> <INT> *level [INT(1)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| set | SYMBOL | false | 设置标识符 |
| 无 | STRING | false | 效果 |
| 无 | INT | false | 持续时间 |
| level | INT | true | 等级 |
| they | CONTAINER | true | 玩家 |

> 设置药水效果

## specialTarget

### 设置旁观者模式下的附着视角（公有语句）

> `specialTarget <CONTAINER> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | CONTAINER | false | 目标实体 |
| they | CONTAINER | true | 玩家 |

> 设置旁观者模式下的附着视角

## sprint

### 设置跑步状态（公有语句）

> `sprint <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | BOOLEAN | false | 是否跑步 |
| they | CONTAINER | true | 目标容器 |

> 设置跑步状态

## teleport

### 传送到指向向量点（公有语句）

> `teleport <VECTOR> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | VECTOR | false | 指向向量 |
| 无 | BOOLEAN | false | 是否保留原朝向 |
| they | CONTAINER | true | 目标容器 |

> 传送到指向向量点

## velocity

### 改变目标速度（公有语句）

> `velocity <VECTOR> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | VECTOR | false | 矢量 |
| they | CONTAINER | true | 目标容器 |

> 改变目标速度

# GddTitle

## gddaction

### 发送龙核Hud Action（公有语句）

> `gddaction <STRING> *by/with [INT(0 20 0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | action（@sender和@player会替换） |
| by/with | INT | true | 淡入 停留 淡出 |
| they | CONTAINER | true | 目标玩家 |

> 发送龙核Hud Action

## gddtitle

### 发送龙核Hud Title（公有语句）

> `gddtitle <STRING> *by/with [INT(0 20 0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | title（@sender和@player会替换） |
| by/with | INT | true | 淡入 停留 淡出 |
| they | CONTAINER | true | 目标玩家 |

> 发送龙核Hud Title

# Global全局数据标签

## global

### 创建数据（公有语句）

> `global <STRING> set/to <ANY> *pst [BOOLEAN(false)] *timeout [LONG(0)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| set/to | SYMBOL | false | 创建占位符 |
| 无 | ANY | false | 数据 |
| pst | BOOLEAN | true | 是否持久化，默认false |
| timeout | LONG | true | 存活时长，默认永久 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 数据 |

> 创建一个存储任意类型数据的标签，可持久化存储向量,矩阵,Bukkit实体,Ady实体,时间,和所有基础类型

### 删除数据标签（公有语句）

> `global <STRING> remove/delete`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| remove/delete | SYMBOL | false | 删除占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 数据 |

> 删除数据标签

### 清除所有数据标签（公有语句）

> `global clear`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| clear | SYMBOL | false | 清除占位符 |

> 清除所有数据标签

### 获取数据剩余存活时间（公有语句）

> `global <STRING> countdown`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| countdown | SYMBOL | false | 剩余存活占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 数据剩余存活时间(Tick) |

> 获取数据剩余存活时间

### 获取数据存活时间（公有语句）

> `global <STRING> survival`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |
| survival | SYMBOL | false | 存活占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 数据存活时间(Tick) |

> 获取数据存活时间

### 获取数据标签（公有语句）

> `global <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 键名 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 数据 |

> 获取数据标签

# Hitbox碰撞箱

## hitbox

### 创建AABB碰撞箱（公有语句）

> `hitbox aabb <DOUBLE> <DOUBLE> *they [CONTAINER(@self)] *offset [VECTOR(null)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| aabb | SYMBOL | false | aabb标识符 |
| 无 | DOUBLE | false | 宽度 |
| 无 | DOUBLE | false | 高度 |
| they | CONTAINER | true | 绑定的目标 |
| offset | VECTOR | true | 偏移 |

| 返回值类型 | 描述 |
|-----------|------|
| HITBOX | 创建的碰撞箱 |

> 创建AABB碰撞箱

### 创建OBB碰撞箱（公有语句）

> `hitbox obb <DOUBLE> <DOUBLE> <DOUBLE> *they [CONTAINER(@self)] *offset [VECTOR(null)] *rotate [QUATERNION(null)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| obb | SYMBOL | false | obb标识符 |
| 无 | DOUBLE | false | 宽度 |
| 无 | DOUBLE | false | 长度 |
| 无 | DOUBLE | false | 高度 |
| they | CONTAINER | true | 绑定的目标 |
| offset | VECTOR | true | 偏移 |
| rotate | QUATERNION | true | 旋转 |

| 返回值类型 | 描述 |
|-----------|------|
| HITBOX | 创建的碰撞箱 |

> 创建OBB碰撞箱

### 创建复合体碰撞箱（公有语句）

> `hitbox composite *they [CONTAINER(@self)] *offset [VECTOR(null)] *rotate [QUATERNION(null)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| composite | SYMBOL | false | 复合体标识符 |
| they | CONTAINER | true | 绑定的目标 |
| offset | VECTOR | true | 偏移 |
| rotate | QUATERNION | true | 旋转 |

| 返回值类型 | 描述 |
|-----------|------|
| HITBOX | 创建的碰撞箱 |

> 创建复合体碰撞箱

### 创建射线碰撞箱（公有语句）

> `hitbox ray <DOUBLE> <VECTOR> *they [CONTAINER(@self)] *offset [VECTOR(null)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| ray | SYMBOL | false | 射线标识符 |
| 无 | DOUBLE | false | 长度 |
| 无 | VECTOR | false | 方向向量 |
| they | CONTAINER | true | 绑定的目标 |
| offset | VECTOR | true | 偏移 |

| 返回值类型 | 描述 |
|-----------|------|
| HITBOX | 创建的碰撞箱 |

> 创建射线碰撞箱

### 创建球体碰撞箱（公有语句）

> `hitbox sphere <DOUBLE> *they [CONTAINER(@self)] *offset [VECTOR(null)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| sphere | SYMBOL | false | 球体标识符 |
| 无 | DOUBLE | false | 半径 |
| they | CONTAINER | true | 绑定的目标 |
| offset | VECTOR | true | 偏移 |

| 返回值类型 | 描述 |
|-----------|------|
| HITBOX | 创建的碰撞箱 |

> 创建球体碰撞箱

### 创建胶囊体碰撞箱（公有语句）

> `hitbox capsule <DOUBLE> <DOUBLE> *they [CONTAINER(@self)] *offset [VECTOR(null)] *rotate [QUATERNION(null)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| capsule | SYMBOL | false | capsule标识符 |
| 无 | DOUBLE | false | 半径 |
| 无 | DOUBLE | false | 高度 |
| they | CONTAINER | true | 绑定的目标 |
| offset | VECTOR | true | 偏移 |
| rotate | QUATERNION | true | 旋转 |

| 返回值类型 | 描述 |
|-----------|------|
| HITBOX | 创建的碰撞箱 |

> 创建胶囊体碰撞箱

### 更新碰撞箱（公有语句）

> `hitbox update <HITBOX>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| update | SYMBOL | false | 更新标识符 |
| 无 | HITBOX | false | 更新的碰撞箱 |

> 更新碰撞箱

### 添加复合体中的碰撞箱（公有语句）

> `hitbox add <HITBOX> <HITBOX>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| add | SYMBOL | false | 添加标识符 |
| 无 | HITBOX | false | 复合体碰撞箱 |
| 无 | HITBOX | false | 创建碰撞箱 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 添加的碰撞箱索引 |

> 添加复合体中的碰撞箱

### 移除复合体中的碰撞箱（公有语句）

> `hitbox remove <HITBOX> <INT>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| remove | SYMBOL | false | 移除标识符 |
| 无 | HITBOX | false | 复合体碰撞箱 |
| 无 | INT | false | 移除位置的索引 |

> 移除复合体中的碰撞箱

# Kether原生-变量

## get

### 获取变量（私有语句）

> `get <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 变量名 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 变量值 |

> 获取变量值，也支持 get property <name> from <action> 获取属性

```yaml
get test
get property "name" from &player
```

## null

### 空值（私有语句）

> `null`

> 返回 null 值

## pass

### 空字符串（私有语句）

> `pass`

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 空字符串 |

> 返回空字符串

## set

### 设置变量（私有语句）

> `set <STRING> to <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 变量名 |
| to | SYMBOL | false | to标识 |
| 无 | ANY | false | 变量值 |

> 设置变量值，也支持 set property <name> from <action> to <value> 和 set <name>[<key>] to <value>

```yaml
set test to 100
set property "name" from &player to "Steve"
```

## vars

### 变量列表（私有语句）

> `vars`

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 变量名列表 |

> 返回当前所有变量名列表，别名 variables

# Kether原生-延迟

## wait

### 延迟执行（私有语句）

> `wait <LONG>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | LONG | false | 延迟时间(Duration) |

> 延迟执行后续语句，支持 Duration 格式（20/1s/500ms），别名 delay/sleep

```yaml
wait 20
wait 1s
wait 500ms
```

# Kether原生-循环

## break

### 跳出循环（私有语句）

> `break`

> 跳出当前 for/while 循环

## for

### 遍历循环（私有语句）

> `for <STRING> in <ITERABLE> then <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 变量名 |
| in | SYMBOL | false | in标识 |
| 无 | ITERABLE | false | 可迭代对象 |
| then | SYMBOL | false | then标识 |
| 无 | ANY | false | 循环体 |

> 遍历集合/数组/Map

```yaml
for i in range 1 to 10 then tell &i
```

## join

### 拼接字符串（私有语句）

> `join <ITERABLE> *by [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 动作列表 |
| by | ANY | true | 分隔符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 拼接结果 |

> 将列表拼接为字符串，by/with 后接分隔符

```yaml
join [ "a" "b" "c" ] by ","
```

## map

### 映射转换（私有语句）

> `map <STRING> in <ITERABLE> with <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 变量名 |
| in | SYMBOL | false | in标识 |
| 无 | ITERABLE | false | 可迭代对象 |
| with | SYMBOL | false | with标识 |
| 无 | ANY | false | 转换动作 |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 转换后的列表 |

> 对集合每个元素执行转换，返回新列表

## while

### 条件循环（私有语句）

> `while <BOOLEAN> then <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | BOOLEAN | false | 条件表达式 |
| then | SYMBOL | false | then标识 |
| 无 | ANY | false | 循环体 |

> 当条件为真时重复执行

# Kether原生-控制流

## all

### 全部为真（私有语句）

> `all <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 布尔动作列表 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否全部为真 |

> 短路与，所有动作结果为 true 时返回 true

## any

### 任一为真（私有语句）

> `any <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 布尔动作列表 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否有任一为真 |

> 短路或，任一动作结果为 true 时返回 true

## async

### 异步执行（私有语句）

> `async <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 执行的动作 |

> 在异步线程中执行动作

## await

### 等待异步（私有语句）

> `await <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 异步动作 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 异步结果 |

> 等待异步 Future 完成并返回结果

## await_all

### 等待全部（私有语句）

> `await_all <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 动作列表 |

> 等待所有异步 Future 完成

## await_any

### 等待任一（私有语句）

> `await_any <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 动作列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 最先完成的结果 |

> 等待任一异步 Future 完成

## call

### 调用Block（私有语句）

> `call <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | Block名称 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | Block返回值 |

> 调用指定的 block 并返回结果

## exit

### 终止脚本（私有语句）

> `exit`

> 终止脚本执行，别名 stop/terminate

```yaml
exit
```

## goto

### 跳转Block（私有语句）

> `goto <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | Block名称 |

> 跳转到指定的 block 执行

## if

### 条件分支（私有语句）

> `if <BOOLEAN> then <ANY> *else [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | BOOLEAN | false | 条件表达式 |
| then | SYMBOL | false | then标识 |
| 无 | ANY | false | 条件为真时执行 |
| else | ANY | true | 条件为假时执行 |

> 条件判断，根据条件执行不同分支

```yaml
if check &health > 50 then tell "健康" else tell "虚弱"
```

## import

### 导入命名空间（私有语句）

> `import <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 命名空间名称 |

> 导入指定命名空间的语句

```yaml
import Orryx
```

## not

### 布尔取反（私有语句）

> `not <BOOLEAN>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | BOOLEAN | false | 布尔值 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 取反结果 |

> 对布尔值取反

## pause

### 暂停脚本（私有语句）

> `pause`

> 暂停脚本执行（永不完成）

## release

### 释放命名空间（私有语句）

> `release <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 命名空间名称 |

> 释放已导入的命名空间

## repeat

### 重复执行（私有语句）

> `repeat <INT> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | INT | false | 重复次数 |
| 无 | ANY | false | 执行的动作 |

> 重复执行指定动作 N 次

```yaml
repeat 3 tell "hello"
```

## seq

### 顺序执行（公有语句）

> `seq <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 动作列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 最后一个动作的结果 |

> 顺序执行动作列表，返回最后一个结果

# Kether原生-数学

## calc

### 表达式计算（私有语句）

> `calc <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | JEXL3表达式 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 计算结果 |

> 使用 JEXL3 引擎计算表达式，别名 calculate，支持 calc dynamic <action>

```yaml
calc "1+2*3"
calc "&damage * 1.5"
```

## check

### 比较判断（私有语句）

> `check <ANY> <SYMBOL> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 左值 |
| 无 | SYMBOL | false | 比较符 |
| 无 | ANY | false | 右值 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 比较结果 |

> 比较两个值，支持 ==/is/!=/not/>/gt/>=/gte/</lt/<=/lte/in/contains/has 等

```yaml
check &health > 50
check &name == "Steve"
```

## math

### 数学运算（私有语句）

> `math <SYMBOL> <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | SYMBOL | false | 运算符(+/-/*/÷) |
| 无 | ITERABLE | false | 数值列表 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 运算结果 |

> 数学四则运算，支持列表模式和链式模式

```yaml
math + [ 1 2 3 ]
math &a + &b - &c
```

## random

### 随机数（私有语句）

> `random <ANY> *to [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 值或起始值 |
| to | ANY | true | 结束值 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 随机结果 |

> 生成随机数（random <from> to <to>）或从集合中随机取元素（random <collection>），别名 random2

```yaml
random 1 to 10
random &list
```

## range

### 数字范围（私有语句）

> `range <DOUBLE> to <DOUBLE> *step [DOUBLE]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 起始值 |
| to | SYMBOL | false | to标识 |
| 无 | DOUBLE | false | 结束值 |
| step | DOUBLE | true | 步长 |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 数字列表 |

> 生成数字范围列表

```yaml
range 1 to 10
range 0 to 100 step 5
```

## round

### 四舍五入（私有语句）

> `round <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 数值 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 整数结果 |

> 四舍五入取整

## scale

### 格式化数字（私有语句）

> `scale <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 数值 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 格式化结果 |

> 格式化数字（保留小数位），别名 scaled

# Kether原生-数组

## arr-add

### 添加元素（私有语句）

> `arr-add <ANY> *to <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 元素 |
| to | ITERABLE | false | 列表 |

> 添加元素到列表末尾

## arr-add-first

### 头部添加（私有语句）

> `arr-add-first <ANY> *to <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 元素 |
| to | ITERABLE | false | 列表 |

> 添加元素到列表开头，别名 arr-push

## arr-find

### 查找索引（私有语句）

> `arr-find <ANY> *in <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 元素 |
| in | ITERABLE | false | 列表 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 索引（-1表示未找到） |

> 查找元素在列表中的索引

## arr-get

### 获取元素（私有语句）

> `arr-get <INT> *in <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | INT | false | 索引 |
| in | ITERABLE | false | 列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 元素 |

> 获取列表指定索引的元素，别名 element/elem

```yaml
arr-get 0 in &list
```

## arr-remove

### 按值移除（私有语句）

> `arr-remove <ANY> *in <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 元素 |
| in | ITERABLE | false | 列表 |

> 从列表中按值移除元素

## arr-remove-at

### 按索引移除（私有语句）

> `arr-remove-at <INT> *in <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | INT | false | 索引 |
| in | ITERABLE | false | 列表 |

> 从列表中按索引移除元素

## arr-remove-first

### 移除首元素（私有语句）

> `arr-remove-first <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 被移除的元素 |

> 移除并返回列表首个元素，别名 arr-take

## arr-remove-last

### 移除末元素（私有语句）

> `arr-remove-last <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 被移除的元素 |

> 移除并返回列表末尾元素，别名 arr-drop

## array

### 构建列表（私有语句）

> `array <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 元素列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 列表 |

> 构建一个列表，别名 arr

```yaml
array [ 1 2 3 ]
```

## mutable

### 转可变列表（私有语句）

> `mutable <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 可变列表 |

> 将列表转为可变列表

## reverse

### 反转列表（私有语句）

> `reverse <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 反转后的列表 |

> 反转列表顺序

## shuffle

### 打乱列表（私有语句）

> `shuffle <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 列表 |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 打乱后的列表 |

> 随机打乱列表顺序

## size

### 获取长度（私有语句）

> `size <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 集合/数组/字符串 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 长度 |

> 获取集合、数组或字符串的长度，别名 length

# Kether原生-时间

## day

### 天数（私有语句）

> `day *of <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| of | STRING | false | of标识(year/month/week) |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 天数 |

> 获取当前天数（年中第几天/月中第几天/周中第几天），别名 days，支持 of/in

## hour

### 当前小时（私有语句）

> `hour`

| 返回值类型 | 描述 |
|-----------|------|
| INT | 小时 |

> 获取当前小时，别名 hours

## minute

### 当前分钟（私有语句）

> `minute`

| 返回值类型 | 描述 |
|-----------|------|
| INT | 分钟 |

> 获取当前分钟，别名 minutes

## month

### 当前月份（私有语句）

> `month`

| 返回值类型 | 描述 |
|-----------|------|
| INT | 月份 |

> 获取当前月份，别名 months

## second

### 当前秒（私有语句）

> `second`

| 返回值类型 | 描述 |
|-----------|------|
| INT | 秒 |

> 获取当前秒，别名 seconds

## time

### 当前时间戳（私有语句）

> `time *as [STRING]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| as | STRING | true | 格式模板 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 时间戳或格式化字符串 |

> 获取当前时间戳（毫秒），可格式化输出，别名 date

```yaml
time
time as "yyyy-MM-dd"
```

## year

### 当前年份（私有语句）

> `year`

| 返回值类型 | 描述 |
|-----------|------|
| INT | 年份 |

> 获取当前年份，别名 years

# Kether原生-游戏

## color

### 颜色转换（私有语句）

> `color <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 文本 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 着色文本 |

> 将 & 颜色代码转换为实际颜色，别名 colored

## command

### 执行命令（私有语句）

> `command <STRING> *as [STRING]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 命令内容 |
| as | STRING | true | 执行方式(player/op/operator/console/server) |

> 执行命令，支持 by/with/as 关键字指定执行方式

```yaml
command "say hello" as console
```

## itemstack

### 创建物品（私有语句）

> `itemstack <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 材质名 |

| 返回值类型 | 描述 |
|-----------|------|
| ITEM_STACK | ItemStack |

> 创建 ItemStack 对象

## loc

### 构建Location（私有语句）

> `loc <STRING> <DOUBLE> <DOUBLE> <DOUBLE> *and [FLOAT] [FLOAT]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 世界名 |
| 无 | DOUBLE | false | X坐标 |
| 无 | DOUBLE | false | Y坐标 |
| 无 | DOUBLE | false | Z坐标 |
| and | FLOAT | true | Yaw |
| 无 | FLOAT | true | Pitch |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | Location |

> 构建 Location 对象，别名 location

```yaml
loc "world" 0 64 0
loc "world" 0 64 0 and 90 0
```

## material

### 获取材质（私有语句）

> `material <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 材质名 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | Material |

> 获取 Material 枚举

## papi

### PAPI变量（私有语句）

> `papi <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 变量文本 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 解析结果 |

> 解析 PlaceholderAPI 变量，别名 placeholder

```yaml
papi "%player_name%"
```

## perm

### 权限检查（私有语句）

> `perm <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 权限节点 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否有权限 |

> 检查执行者是否拥有指定权限，别名 permission

## player

### 玩家属性（私有语句）

> `player <STRING> [STRING] [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 属性名 |
| 无 | STRING | true | 操作(to/=/add/+/sub/-) |
| 无 | ANY | true | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 属性值 |

> 读写玩家属性（57个属性），支持 to/=/add/increase/+/sub/decrease/- 操作

```yaml
player health
player health to 20
player food_level add 5
```

## players

### 在线玩家列表（私有语句）

> `players`

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 玩家名列表 |

> 获取所有在线玩家名列表

## scoreboard

### 计分板（私有语句）

> `scoreboard <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 内容列表或null |

> 向执行者发送计分板

## sender

### 执行者名称（私有语句）

> `sender`

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 执行者名称 |

> 获取当前脚本执行者的名称

## sound

### 播放音效（私有语句）

> `sound <STRING> *by [FLOAT] [FLOAT]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 音效名称 |
| by | FLOAT | true | 音量 |
| 无 | FLOAT | true | 音调 |

> 向执行者播放音效，by/with 后接音量和音调（float）

```yaml
sound ENTITY_EXPERIENCE_ORB_PICKUP by 1.0 1.0
```

## stopsound

### 停止音效（私有语句）

> `stopsound <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 音效名称 |

> 停止播放指定音效

## switch

### 切换执行者（私有语句）

> `switch <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 玩家名或console |

> 切换脚本执行者为指定玩家或控制台

## uncolor

### 去除颜色（私有语句）

> `uncolor <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 文本 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 纯文本 |

> 去除文本中的颜色代码，别名 uncolored

# Kether原生-类型转换

## case

### 模式匹配（私有语句）

> `case <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 输入值 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 匹配分支的结果 |

> 模式匹配（when/switch），根据输入值匹配不同分支

```yaml
case &type [ when == "fire" then tell "火" when == "ice" then tell "冰" else tell "未知" ]
```

## format

### 格式化时间（私有语句）

> `format <LONG> *by [STRING]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | LONG | false | 时间戳(毫秒) |
| by | STRING | true | 格式模板 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 格式化结果 |

> 将时间戳格式化为字符串，by/with 后接格式模板

```yaml
format time by "yyyy-MM-dd HH:mm:ss"
```

## inline

### 内联函数（私有语句）

> `inline <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | Kether函数字符串 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 执行结果 |

> 内联执行 Kether 函数字符串，别名 function

## match

### 正则匹配（私有语句）

> `match <STRING> *by <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 文本 |
| by | STRING | false | 正则表达式 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | Matcher对象 |

> 正则表达式匹配，返回 Matcher 对象（可通过 matcher.operator 访问捕获组）

## optional

### 空值处理（私有语句）

> `optional <ANY> *else [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 值 |
| else | ANY | true | 默认值 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 非空结果 |

> 如果值为 null 则返回默认值

## printed

### 打字机效果（私有语句）

> `printed <STRING> *by [STRING]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 文本 |
| by | STRING | true | 光标字符 |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 文本列表 |

> 生成逐字展开的文本列表（打字机效果），by/with 后接光标字符

## split

### 拆分字符串（私有语句）

> `split <STRING> *by [STRING]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 文本 |
| by | STRING | true | 分隔符 |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 拆分后的列表 |

> 按分隔符拆分字符串，by/with 后接分隔符

```yaml
split "a,b,c" by ","
```

## type

### 类型转换（私有语句）

> `type <STRING> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 目标类型(int/long/float/double/boolean) |
| 无 | ANY | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 转换后的值 |

> 将值转换为指定类型，也可用于字面量类型推断

```yaml
type int "123"
```

# Kether原生-脚本

## invoke

### JEXL3脚本（私有语句）

> `invoke <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 脚本内容 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 脚本返回值 |

> 执行 JEXL3 脚本，支持 invoke dynamic <action>

## js

### JavaScript脚本（私有语句）

> `js <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 脚本内容 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 脚本返回值 |

> 执行 JavaScript 脚本，别名 $/javascript

# Kether原生-输出

## actionbar

### ActionBar消息（私有语句）

> `actionbar <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 消息内容 |

> 向执行者发送 ActionBar 消息

## broadcast

### 全服广播（私有语句）

> `broadcast <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 广播内容 |

> 向全服玩家广播消息，别名 bc

## error

### 控制台错误（私有语句）

> `error <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 错误内容 |

> 控制台输出 severe 级别日志，别名 severe

## log

### 控制台日志（私有语句）

> `log <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 日志内容 |

> 控制台输出 info 级别日志，别名 print/info

```yaml
log "hello world"
```

## subtitle

### 发送Subtitle（私有语句）

> `subtitle <STRING> *by [INT] [INT] [INT]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 副标题内容 |
| by | INT | true | 淡入时间 |
| 无 | INT | true | 停留时间 |
| 无 | INT | true | 淡出时间 |

> 向执行者发送 Subtitle，by/with 后接三个整数

## tell

### 发送消息（私有语句）

> `tell <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 消息内容 |

> 向执行者发送消息，别名 send/message

```yaml
tell "你好"
```

## title

### 发送Title（私有语句）

> `title <STRING> *subtitle [STRING] *by [INT] [INT] [INT]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 标题内容 |
| subtitle | STRING | true | 副标题 |
| by | INT | true | 淡入时间 |
| 无 | INT | true | 停留时间 |
| 无 | INT | true | 淡出时间 |

> 向执行者发送 Title，by/with 后接三个整数

```yaml
title "主标题" subtitle "副标题" by 10 40 10
```

## warn

### 控制台警告（私有语句）

> `warn <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 警告内容 |

> 控制台输出 warn 级别日志，别名 warning

# KeySetting按键设置

## keySetting

### 指向取消按键（公有语句）

> `keySetting aimCancel`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| aimCancel | SYMBOL | false | 指向取消键占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 按键名 |

> 指向取消按键

### 获取技能绑定按键（公有语句）

> `keySetting bind <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| bind | SYMBOL | false | 技能绑定占位符 |
| 无 | STRING | false | 技能绑定键名 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 按键名 |

> 获取技能绑定按键

### 获取拓展按键（公有语句）

> `keySetting extend <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| extend | SYMBOL | false | 拓展占位符 |
| 无 | STRING | false | 拓展ID |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 按键名 |

> 获取拓展按键

### 获取指向确认按键（公有语句）

> `keySetting aimConfirm`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| aimConfirm | SYMBOL | false | 指向确认键占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 按键名 |

> 获取指向确认按键

### 获取普通攻击按键（公有语句）

> `keySetting generalAttack`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| generalAttack | SYMBOL | false | 普攻键占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 按键名 |

> 获取普通攻击按键

### 获取格挡按键（公有语句）

> `keySetting block`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| block | SYMBOL | false | 格挡占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 按键名 |

> 获取格挡按键

### 获取闪避按键（公有语句）

> `keySetting dodge`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| dodge | SYMBOL | false | 闪避占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 按键名 |

> 获取闪避按键

# Mana法力

## mana

### 减少法力（公有语句）

> `mana take <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| take | SYMBOL | false | take |
| 无 | DOUBLE | false | 法力值 |
| they | CONTAINER | true | 减少法力的目标 |

> 获得法力值

### 检测法力（公有语句）

> `mana has <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| has | SYMBOL | false | has |
| 无 | DOUBLE | false | 检测法力值 |
| they | CONTAINER | true | 检测的目标 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否有足够法力值 |

> 检测是否有足够法力值

### 给予法力（公有语句）

> `mana give <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| give | SYMBOL | false | give |
| 无 | DOUBLE | false | 法力值 |
| they | CONTAINER | true | 给予法力的目标 |

> 玩家获得法力值

### 获取最大法力值（公有语句）

> `mana max *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| max | SYMBOL | true | max |
| they | CONTAINER | true | 获取的目标 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 法力值 |

> 获取玩家拥有的最大法力值

### 获取法力值（公有语句）

> `mana now *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| now | SYMBOL | true | now |
| they | CONTAINER | true | 获取的目标 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 法力值 |

> 获取玩家拥有的法力值

# Math数学运算

## abs

### 取绝对值（公有语句）

> `abs <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 绝对值 |

> 取绝对值

## acos

### 求acos值（公有语句）

> `acos <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 角度值 |

> 求acos值

## asin

### 求asin值（公有语句）

> `asin <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 角度值 |

> 求asin值

## atan

### 求atan值（公有语句）

> `atan <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 角度值 |

> 求atan值

## ceil

### 向上取整（公有语句）

> `ceil <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 值 |

> 向上取整

## cos

### 求cos值（公有语句）

> `cos <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 角度值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | cos值 |

> 求cos值

## degrees

### 将弧度值转化为角度值（公有语句）

> `degrees <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 弧度值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 角度值 |

> 将弧度值转化为角度值

## floor

### 向下取整（公有语句）

> `floor <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 值 |

> 向下取整

## matrix

### rotateX矩阵绕X轴旋转（公有语句）

> `matrix rotateX <MATRIX> <DOUBLE> *dest [MATRIX]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotateX | SYMBOL | false | X轴旋转标识符 |
| 无 | MATRIX | false | 被旋转的矩阵 |
| 无 | DOUBLE | false | 旋转角度 |
| dest | MATRIX | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| MATRIX | 旋转矩阵 |

> 矩阵绕X轴旋转

### rotateY矩阵绕Y轴旋转（公有语句）

> `matrix rotateY <MATRIX> <DOUBLE> *dest [MATRIX]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotateY | SYMBOL | false | Y轴旋转标识符 |
| 无 | MATRIX | false | 被旋转的矩阵 |
| 无 | DOUBLE | false | 旋转角度 |
| dest | MATRIX | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| MATRIX | 旋转矩阵 |

> 矩阵绕Y轴旋

### rotateZ矩阵绕Z轴旋转（公有语句）

> `matrix rotateZ <MATRIX> <DOUBLE> *dest [MATRIX]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotateZ | SYMBOL | false | Z轴旋转标识符 |
| 无 | MATRIX | false | 被旋转的矩阵 |
| 无 | DOUBLE | false | 旋转角度 |
| dest | MATRIX | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| MATRIX | 旋转矩阵 |

> 矩阵绕Z轴旋转

### rotate矩阵绕向量轴旋转（公有语句）

> `matrix rotate <MATRIX> <VECTOR> <DOUBLE> *dest [MATRIX]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotate | SYMBOL | false | 给定轴旋转标识符 |
| 无 | MATRIX | false | 被旋转的矩阵 |
| 无 | VECTOR | false | 旋转的轴向量 |
| 无 | DOUBLE | false | 旋转角度 |
| dest | MATRIX | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| MATRIX | 旋转矩阵 |

> 矩阵绕给定轴旋转

### rotate矩阵统一缩放（公有语句）

> `matrix scale <MATRIX> <DOUBLE> *dest [MATRIX]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| scale | SYMBOL | false | 统一缩放标识符 |
| 无 | MATRIX | false | 被缩放的矩阵 |
| 无 | DOUBLE | false | 缩放系数 |
| dest | MATRIX | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| MATRIX | 缩放矩阵 |

> 矩阵按照系数统一缩放

### rotate矩阵缩放（公有语句）

> `matrix scaleXYZ <MATRIX> <DOUBLE> <DOUBLE> <DOUBLE> *dest [MATRIX]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| scaleXYZ | SYMBOL | false | 缩放标识符 |
| 无 | MATRIX | false | 被缩放的矩阵 |
| 无 | DOUBLE | false | 缩放系数X |
| 无 | DOUBLE | false | 缩放系数Y |
| 无 | DOUBLE | false | 缩放系数Z |
| dest | MATRIX | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| MATRIX | 缩放矩阵 |

> 矩阵分别按照XYZ系数缩放

### transform应用矩阵变换向量（公有语句）

> `matrix transform <MATRIX> <VECTOR> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| transform | SYMBOL | false | 应用矩阵标识符 |
| 无 | MATRIX | false | 应用的矩阵 |
| 无 | VECTOR | false | 向量 |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 变换后的向量 |

> 应用矩阵变换向量

### 创建单位矩阵（公有语句）

> `matrix identity`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| identity | SYMBOL | true | 创建单位矩阵标识符 |

| 返回值类型 | 描述 |
|-----------|------|
| MATRIX | 创建的单位矩阵 |

> 创建单位矩阵

### 创建矩阵（公有语句）

> `matrix create <DOUBLE> <DOUBLE> <DOUBLE> <DOUBLE> <DOUBLE> <DOUBLE> <DOUBLE> <DOUBLE> <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| create | SYMBOL | false | 创建标识符 |
| 无 | DOUBLE | false | 0行0列 |
| 无 | DOUBLE | false | 0行1列 |
| 无 | DOUBLE | false | 0行2列 |
| 无 | DOUBLE | false | 1行0列 |
| 无 | DOUBLE | false | 1行1列 |
| 无 | DOUBLE | false | 1行2列 |
| 无 | DOUBLE | false | 2行0列 |
| 无 | DOUBLE | false | 2行1列 |
| 无 | DOUBLE | false | 2行2列 |

| 返回值类型 | 描述 |
|-----------|------|
| MATRIX | 创建的矩阵 |

> 创建矩阵

## natural

### 自然对数函数的底数（公有语句）

> `natural`

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 自然对数函数的底数 |

> 自然对数函数的底数

## pi

### 圆周率（公有语句）

> `pi`

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 圆周率 |

> 圆周率

## pow

### 计算幂函数（公有语句）

> `pow <DOUBLE> <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 底 |
| 无 | DOUBLE | false | 幂 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 幂函数值 |

> 计算幂函数

## quaternion

### rotateX四元数绕X轴旋转（公有语句）

> `quaternion rotateX <QUATERNION> <DOUBLE> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotateX | SYMBOL | false | X轴旋转标识符 |
| 无 | QUATERNION | false | 被旋转的四元数 |
| 无 | DOUBLE | false | 旋转角度 |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 旋转四元数 |

> 四元数绕X轴旋转

### rotateY四元数绕Y轴旋转（公有语句）

> `quaternion rotateY <QUATERNION> <DOUBLE> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotateY | SYMBOL | false | Y轴旋转标识符 |
| 无 | QUATERNION | false | 被旋转的四元数 |
| 无 | DOUBLE | false | 旋转角度 |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 旋转四元数 |

> 四元数绕Y轴旋转

### rotateZ四元数绕Z轴旋转（公有语句）

> `quaternion rotateZ <QUATERNION> <DOUBLE> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotateZ | SYMBOL | false | Z轴旋转标识符 |
| 无 | QUATERNION | false | 被旋转的四元数 |
| 无 | DOUBLE | false | 旋转角度 |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 旋转四元数 |

> 四元数绕Z轴旋转

### rotate四元数绕指定轴旋转（公有语句）

> `quaternion rotate <QUATERNION> <DOUBLE> <VECTOR>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotate | SYMBOL | false | 指定轴旋转标识符 |
| 无 | QUATERNION | false | 被旋转的四元数 |
| 无 | DOUBLE | false | 旋转角度 |
| 无 | VECTOR | false | 转轴 |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 旋转四元数 |

> 四元数绕指定轴旋转

### 创建单位四元数（公有语句）

> `quaternion identity`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| identity | SYMBOL | true | 创建单位四元数标识符 |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 创建的单位四元数 |

> 创建单位四元数

### 创建四元数（公有语句）

> `quaternion create <DOUBLE> <DOUBLE> <DOUBLE> <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| create | SYMBOL | false | 创建标识符 |
| 无 | DOUBLE | false | x |
| 无 | DOUBLE | false | y |
| 无 | DOUBLE | false | z |
| 无 | DOUBLE | false | w |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 创建的矩阵 |

> 创建四元数

### 应用四元数到指定向量（公有语句）

> `quaternion transform <QUATERNION> <VECTOR> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| transform | SYMBOL | false | 应用标识符 |
| 无 | QUATERNION | false | 四元数 |
| 无 | VECTOR | false | 向量 |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 应用后的向量 |

> 应用四元数到指定向量

### 缩放四元数（公有语句）

> `quaternion scale <QUATERNION> <DOUBLE> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| scale | SYMBOL | false | 缩放标识符 |
| 无 | QUATERNION | false | 四元数 |
| 无 | DOUBLE | false | 缩放值 |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 缩放后的四元数 |

> 缩放四元数

### 获取共轭的四元数（公有语句）

> `quaternion conjugate <QUATERNION> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| conjugate | SYMBOL | false | 共轭标识符 |
| 无 | QUATERNION | false | 四元数 |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 旋转四元数 |

> 获取共轭的四元数

### 获得两向量之间的旋转四元数（公有语句）

> `quaternion rotateTo <VECTOR> <VECTOR> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| rotateTo | SYMBOL | false | 标识符 |
| 无 | VECTOR | false | 开始向量 |
| 无 | VECTOR | false | 结束向量 |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 应用后的四元数 |

> 获得两向量之间的旋转四元数

### 通过插值获取两四元数中过渡态（公有语句）

> `quaternion nlerp <QUATERNION> <QUATERNION> <DOUBLE> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| nlerp | SYMBOL | false | 插值标识符 |
| 无 | QUATERNION | false | 开始四元数 |
| 无 | QUATERNION | false | 结束四元数 |
| 无 | DOUBLE | false | 插值（0-1） |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 旋转四元数 |

> 通过插值获取两四元数中过渡态

### 通过插值获取两四元数中过渡态（公有语句）

> `quaternion slerp <QUATERNION> <QUATERNION> <DOUBLE> *dest [QUATERNION]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| slerp | SYMBOL | false | 插值标识符 |
| 无 | QUATERNION | false | 开始四元数 |
| 无 | QUATERNION | false | 结束四元数 |
| 无 | DOUBLE | false | 插值（0-1） |
| dest | QUATERNION | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| QUATERNION | 旋转四元数 |

> 通过插值获取两四元数中过渡态

## radians

### 将角度值转化为弧度值（公有语句）

> `radians <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 角度值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 弧度值 |

> 将角度值转化为弧度值

## sin

### 求sin值（公有语句）

> `sin <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 角度值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | sin值 |

> 求sin值

## sqrt

### 计算平方（公有语句）

> `sqrt <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 平方值 |

> 计算平方

## tan

### 求tan值（公有语句）

> `tan <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 角度值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | tan值 |

> 求tan值

## truncate

### 向0取整（公有语句）

> `truncate <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 值 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 值 |

> 向0取整

## vector

### 世界原点向量(0向量)（公有语句）

> `vector center`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| center | SYMBOL | false | 0向量标识符 |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 0向量 |

> 世界原点向量(0向量)

### 创建向量（公有语句）

> `vector create/new <DOUBLE> <DOUBLE> <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| create/new | SYMBOL | true | 创建标识符 |
| 无 | DOUBLE | false | X |
| 无 | DOUBLE | false | Y |
| 无 | DOUBLE | false | Z |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 创建的向量 |

> 创建向量

### 原点向量（公有语句）

> `vector origin`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| origin | SYMBOL | false | 原点标识符 |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | origin原点向量 |

> 原点向量

### 原点的视角向量（公有语句）

> `vector eye *offset/os <DOUBLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| eye | SYMBOL | false | 原点视角标识符 |
| offset/os | DOUBLE | false | 偏移yaw和pitch(os 90.0 90.0) |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | origin原点的视角向量 |

> 原点的视角向量

### 叉乘向量（公有语句）

> `vector cross <VECTOR> <VECTOR> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| cross | SYMBOL | false | 叉乘标识符 |
| 无 | VECTOR | false | 叉乘号前的向量A |
| 无 | VECTOR | false | 叉乘号后的向量B |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 叉乘结果的向量 |

> 叉乘向量

### 反转向量（公有语句）

> `vector negate <VECTOR> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| negate | SYMBOL | false | 反转标识符 |
| 无 | VECTOR | false | 向量 |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 反转后向量 |

> 反转向量

### 向量夹角（公有语句）

> `vector angle <VECTOR> <VECTOR>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| angle | SYMBOL | false | 夹角标识符 |
| 无 | VECTOR | false | 向量A |
| 无 | VECTOR | false | 向量B |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 向量夹角角度值 |

> 向量A与向量B的夹角

### 向量相减（公有语句）

> `vector sub <VECTOR> <VECTOR> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| sub | SYMBOL | false | 相减标识符 |
| 无 | VECTOR | false | 减号前的向量A |
| 无 | VECTOR | false | 减号后的向量B |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 相减结果的向量 |

> 向量相减

### 向量相加（公有语句）

> `vector add <VECTOR> <VECTOR> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| add | SYMBOL | false | 相加标识符 |
| 无 | VECTOR | false | 加号前的向量A |
| 无 | VECTOR | false | 加号后的向量B |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 相加结果的向量 |

> 向量相加

### 向量距离（公有语句）

> `vector distance <VECTOR> <VECTOR>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| distance | SYMBOL | false | 距离标识符 |
| 无 | VECTOR | false | 向量A |
| 无 | VECTOR | false | 向量B |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 向量距离 |

> 向量A与向量B的距离

### 向量远离另一向量（公有语句）

> `vector further <VECTOR> <VECTOR> <DOUBLE> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| further | SYMBOL | false | 远离占位符 |
| 无 | VECTOR | false | 移动向量 |
| 无 | VECTOR | false | 目标向量 |
| 无 | DOUBLE | false | 距离 |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 移动后的向量 |

> 使向量远离另一向量

### 向量长度（公有语句）

> `vector length <VECTOR>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| length | SYMBOL | false | 长度标识符 |
| 无 | VECTOR | false | 向量 |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 标准化的向量 |

> 向量长度

### 向量靠近另一向量（公有语句）

> `vector closer <VECTOR> <VECTOR> <DOUBLE> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| closer | SYMBOL | false | 靠近占位符 |
| 无 | VECTOR | false | 移动向量 |
| 无 | VECTOR | false | 目标向量 |
| 无 | DOUBLE | false | 距离 |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 移动后的向量 |

> 使向量靠近另一向量

### 归一化（公有语句）

> `vector normalize <VECTOR> *length [DOUBLE(1.0)] *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| normalize | SYMBOL | false | 标准化标识符 |
| 无 | VECTOR | false | 需要标准化的向量 |
| length | DOUBLE | true | 标准化的长度 |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 标准化的向量 |

> 标准化向量

### 数乘向量（公有语句）

> `vector mul <VECTOR> <DOUBLE> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| mul | SYMBOL | false | 数乘标识符 |
| 无 | VECTOR | false | 向量 |
| 无 | DOUBLE | false | 数字 |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 数乘结果 |

> 数乘向量

### 根据法线反射向量（公有语句）

> `vector reflect <VECTOR> <VECTOR> *dest [VECTOR]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| reflect | SYMBOL | false | 反射占位符 |
| 无 | VECTOR | false | 向量 |
| 无 | VECTOR | false | 法线向量 |
| dest | VECTOR | true | 结果存储Key |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 反射后向量 |

> 根据法线反射向量

### 点乘向量（公有语句）

> `vector dot <VECTOR> <VECTOR>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| dot | SYMBOL | false | 点乘标识符 |
| 无 | VECTOR | false | 点乘号前的向量A |
| 无 | VECTOR | false | 点乘号后的向量B |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 点乘结果 |

> 点乘向量

# Money财富

## money

### 减少财富（私有语句）

> `money take/withdraw <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| take/withdraw | SYMBOL | false | take/withdraw |
| 无 | DOUBLE | false | 财富值 |
| they | CONTAINER | true | 减少财富的目标 |

> 获得财富值

### 检测财富（私有语句）

> `money has <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| has | SYMBOL | false | has |
| 无 | DOUBLE | false | 检测财富值 |
| they | CONTAINER | true | 检测的目标们 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否有足够财富值 |

> 检测是否有足够财富值

### 给予财富（私有语句）

> `money add/deposit <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| add/deposit | SYMBOL | false | add/deposit |
| 无 | DOUBLE | false | 财富值 |
| they | CONTAINER | true | 给予财富的目标 |

> 玩家获得财富值

### 获取财富值（私有语句）

> `money get/look *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| get/look | SYMBOL | true | get |
| they | CONTAINER | true | 获取的目标 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 财富值 |

> 获取玩家拥有的财富值

# MythicMobs附属语句

## mythicmobs

### 减少仇恨值（公有语句）

> `mythicmobs threat take <DOUBLE> <CONTAINER> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| threat | SYMBOL | false | 仇恨表标识符 |
| take | SYMBOL | false | 减少标识符 |
| 无 | DOUBLE | false | 仇恨值 |
| 无 | CONTAINER | false | 怪物 |
| they | CONTAINER | true | 仇恨目标 |

> 减少仇恨值

### 发送怪物信号（公有语句）

> `mythicmobs signal <STRING> <CONTAINER(@self)> *trigger [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| signal | SYMBOL | false | 信号标识符 |
| 无 | STRING | false | 信号名 |
| 无 | CONTAINER | false | 怪物 |
| trigger | CONTAINER | true | 触发者 |

> 发送MythicMobs怪物信号

### 嘲讽怪物（公有语句）

> `mythicmobs taunt <CONTAINER> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| taunt | SYMBOL | false | 嘲讽标识符 |
| 无 | CONTAINER | false | 被嘲讽的怪物 |
| they | CONTAINER | true | 嘲讽者 |

> 嘲讽怪物

### 添加仇恨值（公有语句）

> `mythicmobs threat add <DOUBLE> <CONTAINER> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| threat | SYMBOL | false | 仇恨表标识符 |
| add | SYMBOL | false | 添加标识符 |
| 无 | DOUBLE | false | 仇恨值 |
| 无 | CONTAINER | false | 怪物 |
| they | CONTAINER | true | 仇恨目标 |

> 添加仇恨值

### 设置仇恨值（公有语句）

> `mythicmobs threat set <DOUBLE> <CONTAINER> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| threat | SYMBOL | false | 仇恨表标识符 |
| set | SYMBOL | false | 设置标识符 |
| 无 | DOUBLE | false | 仇恨值 |
| 无 | CONTAINER | false | 怪物 |
| they | CONTAINER | true | 仇恨目标 |

> 设置仇恨值

### 读取怪物架势（公有语句）

> `mythicmobs stance *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| stance | SYMBOL | false | 架势标识符 |
| they | CONTAINER | true | 怪物 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 架势名 |

> 读取MythicMobs怪物当前架势(stance)，目标非MM怪物时返回null

### 释放MM技能（公有语句）

> `mythicmobs cast <STRING> <FLOAT> *they [CONTAINER(@self)] *trigger [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| cast | SYMBOL | false | 释放标识符 |
| 无 | STRING | false | 技能名 |
| 无 | FLOAT | false | 技能强度 |
| they | CONTAINER | true | 释放者 |
| trigger | CONTAINER | true | 触发者 |

> 释放MythicMobs技能

# Nodens

## damageProcessor

### 创建伤害处理器DamageProcessor（公有语句）

> `damageProcessor <STRING> *they <CONTAINER>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 伤害类型 |
| they | CONTAINER | false | 防御者defender |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 伤害处理器列表 |

> 创建伤害处理器DamageProcessor

## regainProcessor

### 创建治疗处理器RegainProcessor（公有语句）

> `regainProcessor <STRING> *they <CONTAINER>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 治疗原因 |
| they | CONTAINER | false | 受疗者passive |

| 返回值类型 | 描述 |
|-----------|------|
| ITERABLE | 治疗处理器列表 |

> 创建治疗处理器RegainProcessor

# Orryx Mod额外功能

## circleShockwave

### 发送一个圆形地震波效果（公有语句）

> `circleShockwave <DOUBLE> *they [CONTAINER(@self)] *viewer [CONTAINER(@world)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 半径 |
| they | CONTAINER | true | 位置 |
| viewer | CONTAINER | true | 可视玩家 |

> 发送一个圆形地震波效果

## colliderRemove

### 移除客户端碰撞箱渲染（公有语句）

> `colliderRemove <STRING> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 碰撞箱唯一标识 |
| viewers | CONTAINER | true | 可视玩家 |

> 移除客户端已有的碰撞箱渲染并停止实时同步

## colliderShow

### 发送碰撞箱显示到客户端（公有语句）

> `colliderShow <STRING> <HITBOX> *color [STRING(255,255,255,255)] *realtime [BOOLEAN(true)] *interval [INT(1)] *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 碰撞箱唯一标识 |
| 无 | HITBOX | false | 碰撞箱 |
| color | STRING | true | 颜色 |
| realtime | BOOLEAN | true | 是否实时同步 |
| interval | INT | true | 同步间隔 Tick |
| viewers | CONTAINER | true | 可视玩家 |

> 将碰撞箱渲染信息发送到客户端，并可自动同步后续位置与旋转变化

## colliderUpdate

### 更新客户端碰撞箱渲染（公有语句）

> `colliderUpdate <STRING> <HITBOX> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 碰撞箱唯一标识 |
| 无 | HITBOX | false | 碰撞箱 |
| viewers | CONTAINER | true | 可视玩家 |

> 更新已有碰撞箱的位置/旋转等几何数据

## entityShow

### 投影实体模型（公有语句）

> `entityShow <STRING> <LONG> *rotate [STRING(0,0,0)] *scale [FLOAT(1.0)] *alpha [FLOAT(1.0)] *fadeout [BOOLEAN(false)] <CONTAINER> *viewer [CONTAINER(@world)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 组名 |
| 无 | LONG | false | 持续时间 |
| rotate | STRING | true | 旋转 |
| scale | FLOAT | true | 缩放 |
| alpha | FLOAT | true | 透明度 |
| fadeout | BOOLEAN | true | 是否渐隐 |
| 无 | CONTAINER | false | 被投影的实体 |
| viewer | CONTAINER | true | 目标容器 |
| they | CONTAINER | true | 目标容器 |

> 投影一个实体模型到指定位置

## flicker

### 滞留一道闪影（公有语句）

> `flicker <LONG> [FLOAT(1)] [LONG(-1)] [FLOAT(1.0)] *they [CONTAINER(@self)] *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | LONG | false | 时长 |
| 无 | FLOAT | true | 透明度 |
| 无 | LONG | true | 透明度淡化时间(-1为不淡化) |
| 无 | FLOAT | true | 缩放 |
| they | CONTAINER | true | 目标容器 |
| viewers | CONTAINER | true | 可视玩家 |

> 滞留一道闪影

## ghost

### 设置鬼影状态（公有语句）

> `ghost <LONG> <INT> [INT(0)] *they [CONTAINER(@self)] *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | LONG | false | 时长 |
| 无 | INT | false | 密度 |
| 无 | INT | true | 间隔 |
| they | CONTAINER | true | 目标容器 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置鬼影状态

## mouse

### 设置是否呼出鼠标（公有语句）

> `mouse <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | BOOLEAN | false | 是否呼出 |
| they | CONTAINER | true | 目标容器 |

> 设置是否呼出鼠标

## navigation

### 停止自动寻路导航（公有语句）

> `navigation stop`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| stop | SYMBOL | false | 停止占位符 |

> 停止自动寻路导航

### 开始一个自动寻路导航（公有语句）

> `navigation start <INT> <INT> <INT> <INT>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| start | SYMBOL | false | 开始占位符 |
| 无 | INT | false | x |
| 无 | INT | false | y |
| 无 | INT | false | z |
| 无 | INT | false | range |

> 开始一个自动寻路导航

## removeShow

### 移除投影实体模型（公有语句）

> `removeShow <STRING> <CONTAINER> *viewer [CONTAINER(@world)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 组名 |
| 无 | CONTAINER | false | 被投影的实体 |
| viewer | CONTAINER | true | 目标容器 |

> 移除一个投影模型组

## sectorShockwave

### 发送一个扇形地震波效果（公有语句）

> `sectorShockwave <DOUBLE> <DOUBLE> *they [CONTAINER(@self)] *viewer [CONTAINER(@world)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 半径 |
| 无 | DOUBLE | false | 开合角度 |
| they | CONTAINER | true | 位置 |
| viewer | CONTAINER | true | 可视玩家 |

> 发送一个扇形地震波效果

## squareShockwave

### 发送一个方形地震波效果（公有语句）

> `squareShockwave <DOUBLE> <DOUBLE> *they [CONTAINER(@self)] *viewer [CONTAINER(@world)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 宽度 |
| 无 | DOUBLE | false | 长度 |
| they | CONTAINER | true | 位置 |
| viewer | CONTAINER | true | 可视玩家 |

> 发送一个方形地震波效果

# Orryx Profile玩家信息

## buff

### 是否有状态效果（公有语句）

> `buff has <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| has | SYMBOL | false | 检测标识符 |
| 无 | STRING | false | buff名 |
| they | CONTAINER | true | 目标容器 |

> 检测玩家是否有状态效果

### 清除状态效果（公有语句）

> `buff clear [STRING(ALL)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| clear | SYMBOL | false | 清除标识符 |
| 无 | STRING | true | buff名 |
| they | CONTAINER | true | 目标容器 |

> 清除玩家状态效果

### 设置状态效果（公有语句）

> `buff send <STRING> [LONG] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | buff名 |
| 无 | LONG | true | 持续时长 |
| they | CONTAINER | true | 目标容器 |

> 设置玩家状态效果

## invincible

### 获取无敌状态倒计时（公有语句）

> `invincible count *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| count | SYMBOL | false | 倒计时占位符 |
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 倒计时Tick |

> 获取无敌状态倒计时

### 设置无敌状态（公有语句）

> `invincible set/to/=,add/+,reduce/-,cancel/stop [LONG(0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| set/to/=,add/+,reduce/-,cancel/stop | SYMBOL | false | 设置方法 |
| 无 | LONG | true | 状态时长 |
| they | CONTAINER | true | 目标容器 |

> 设置、增加、减少或取消无敌状态

## silence

### 获取沉默状态倒计时（公有语句）

> `silence count *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| count | SYMBOL | false | 倒计时占位符 |
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 倒计时Tick |

> 获取沉默状态倒计时

### 设置沉默状态（公有语句）

> `silence set/to/=,add/+,reduce/-,cancel/stop [LONG(0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| set/to/=,add/+,reduce/-,cancel/stop | SYMBOL | false | 设置方法 |
| 无 | LONG | true | 状态时长 |
| they | CONTAINER | true | 目标容器 |

> 设置、增加、减少或取消沉默状态

## superBody

### 获取霸体状态倒计时（公有语句）

> `superBody count *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| count | SYMBOL | false | 倒计时占位符 |
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 倒计时Tick |

> 获取霸体状态倒计时

### 设置霸体状态（公有语句）

> `superBody set/to/=,add/+,reduce/-,cancel/stop [LONG(0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| set/to/=,add/+,reduce/-,cancel/stop | SYMBOL | false | 设置方法 |
| 无 | LONG | true | 状态时长 |
| they | CONTAINER | true | 目标容器 |

> 设置、增加、减少或取消霸体状态

## superFoot

### 获取免疫摔落状态倒计时（公有语句）

> `superFoot count *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| count | SYMBOL | false | 倒计时占位符 |
| they | CONTAINER | true | 目标容器 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 倒计时Tick |

> 获取免疫摔落状态倒计时

### 设置免疫摔落状态（公有语句）

> `superFoot set/to/=,add/+,reduce/-,cancel/stop [LONG(0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| set/to/=,add/+,reduce/-,cancel/stop | SYMBOL | false | 设置方法 |
| 无 | LONG | true | 状态时长 |
| they | CONTAINER | true | 目标容器 |

> 设置、增加、减少或取消免疫摔落状态

# Orryx信息获取

## orryx

### 获取技能最低等级（公有语句）

> `orryx skill minLevel/min <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| minLevel/min | SYMBOL | false | 最低等级标识符 |
| 无 | STRING | false | 技能 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 技能最低等级 |

> 获取技能最低等级

### 获取技能最高等级（公有语句）

> `orryx skill maxLevel/max <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| maxLevel/max | SYMBOL | false | 最高等级标识符 |
| 无 | STRING | false | 技能 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 技能最高等级 |

> 获取技能最高等级

### 获取技能点（公有语句）

> `orryx point`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| point | SYMBOL | false | 获取玩家技能点 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 技能点 |

> 获取玩家技能点

### 获取玩家当前等级最大经验（公有语句）

> `orryx maxExperienceOfLevel/maxExpOfLevel *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| maxExperienceOfLevel/maxExpOfLevel | SYMBOL | false | 获取玩家当前等级最大经验 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 经验 |

> 获取玩家当前等级最大经验

### 获取玩家当前等级经验（公有语句）

> `orryx experienceOfLevel/expOfLevel *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| experienceOfLevel/expOfLevel | SYMBOL | false | 获取玩家当前等级经验 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 经验 |

> 获取玩家当前等级经验

### 获取玩家所有经验（公有语句）

> `orryx experience/exp *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| experience/exp | SYMBOL | false | 获取玩家所有经验 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 经验 |

> 获取玩家所有经验

### 获取玩家技能冷却（公有语句）

> `orryx skill cooldown <STRING> *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| cooldown | SYMBOL | false | 冷却标识符 |
| 无 | STRING | false | 技能 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 技能冷却 |

> 获取玩家技能冷却

### 获取玩家技能冷却倒计时（公有语句）

> `orryx skill countdown <STRING> *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| countdown | SYMBOL | false | 倒计时标识符 |
| 无 | STRING | false | 技能 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| LONG | 技能冷却倒计时 |

> 获取玩家技能冷却倒计时

### 获取玩家技能前置变量（公有语句）

> `orryx skill variables/var <STRING> <STRING> *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| variables/var | SYMBOL | false | 变量标识符 |
| 无 | STRING | false | 技能 |
| 无 | STRING | false | 变量名 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 技能释放沉默时间 |

> 获取玩家技能前置变量

### 获取玩家技能是否锁定（公有语句）

> `orryx skill locked <STRING> *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| locked | SYMBOL | false | 等级标识符 |
| 无 | STRING | false | 技能 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 技能是否锁定 |

> 获取玩家技能是否锁定

### 获取玩家技能沉默时间（公有语句）

> `orryx skill silence <STRING> *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| silence | SYMBOL | false | 沉默标识符 |
| 无 | STRING | false | 技能 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 技能释放沉默时间 |

> 获取玩家技能沉默时间

### 获取玩家技能消耗法力值（公有语句）

> `orryx skill mana <STRING> *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| mana | SYMBOL | false | 法力值标识符 |
| 无 | STRING | false | 技能 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 技能释放消耗法力值 |

> 获取玩家技能消耗法力值

### 获取玩家技能等级（公有语句）

> `orryx skill level <STRING> *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| skill | SYMBOL | false | 技能标识符 |
| level | SYMBOL | false | 等级标识符 |
| 无 | STRING | false | 技能 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 技能等级 |

> 获取玩家技能等级

### 获取玩家技能组（公有语句）

> `orryx group *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| group | SYMBOL | false | 获取玩家技能组 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 技能组 |

> 获取玩家技能组

### 获取玩家等级（公有语句）

> `orryx level *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| level | SYMBOL | false | 获取玩家等级 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| INT | 等级 |

> 获取玩家等级

### 获取玩家绑定技能（公有语句）

> `orryx bindSkill *job [STRING(当前职业)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| bindSkill | SYMBOL | false | 获取玩家绑定技能 |
| job | STRING | true | 获取的玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 绑定技能 |

> 获取玩家绑定技能

### 获取玩家职业（公有语句）

> `orryx job`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| job | SYMBOL | false | 获取玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 职业 |

> 获取玩家职业

### 获取玩家职业实例（公有语句）

> `orryx jobInstance`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| jobInstance | SYMBOL | false | 获取玩家职业 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 职业实例 |

> 获取玩家职业实例

# Pipe管式任务

## pipe

### 管式任务（公有语句）

> `pipe <STRING> <LONG> *trigger [STRING] *onComplete [ANY] *onBrock [ANY] *onPeriod [ANY] *period [LONG]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | UUID，唯一ID名 |
| 无 | LONG | false | 多少Ticks后完成 |
| trigger | STRING | true | 中断触发器使用 , 分割Key |
| onComplete | ANY | true | 完成时运行脚本 { action } |
| onBrock | ANY | true | 中断时运行脚本 { action } |
| onPeriod | ANY | true | 每周期 tick 时运行脚本 { action } |
| period | LONG | true | 周期Tick |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 完成或中断运行脚本返回值 |

> 阻塞kether语句直到执行完毕

# PressSkill蓄力技能

## press

### 发送蓄力进度条（公有语句）

> `press send <LONG> *progress [ITERABLE] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| send | SYMBOL | false | 发送占位符 |
| 无 | LONG | false | 最大蓄力时间 |
| progress | ITERABLE | true | 蓄力阶段时间 |
| they | CONTAINER | true | 目标 |

> 发送蓄力进度条

```yaml
press bar send 60 progress 10,20,40 they @self
```

### 清除蓄力进度条（公有语句）

> `press clear *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| clear | SYMBOL | false | 清除占位符 |
| they | CONTAINER | true | 目标 |

> 清除蓄力进度条

```yaml
press bar clear they @self
```

### 获取玩家正在蓄力的技能（公有语句）

> `press get *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| get | SYMBOL | false | 获取占位符 |
| they | CONTAINER | true | 目标 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 玩家正在蓄力的技能 |

> 获取玩家正在蓄力的技能

# Projectile抛射物

## projectile

### 生成一个无实体抛射物（公有语句）

> `projectile none <VECTOR> <HITBOX> *onHit [ANY] *onPeriod [ANY] *period/p [LONG(1)] *timeout/t [LONG(0)] *hitEntity/he [BOOLEAN(true)] *hitBlock/hb [BOOLEAN(false)] *through/th [BOOLEAN(false)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| none | SYMBOL | false | 抛射物类型 |
| 无 | VECTOR | false | 下一Tick前进的方向 |
| 无 | HITBOX | false | 碰撞箱 |
| onHit | ANY | true | 碰撞时执行 |
| onPeriod | ANY | true | 每周期执行 |
| period/p | LONG | true | 周期 |
| timeout/t | LONG | true | 存活时间 |
| hitEntity/he | BOOLEAN | true | 是否与实体碰撞 |
| hitBlock/hb | BOOLEAN | true | 是否与方块碰撞 |
| through/th | BOOLEAN | true | 是否可以穿透方块 |
| they | CONTAINER | true | 生成位置 |

| 返回值类型 | 描述 |
|-----------|------|
| TARGET | 抛射物 |

> 生成一个无实体抛射物

# RayTrace光线追踪

## ray

### 追踪hitBlock（私有语句）

> `ray <VECTOR> <DOUBLE> <STRING> <BOOLEAN> <BOOLEAN>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | VECTOR | false | 射线向量 |
| 无 | DOUBLE | false | 碰撞范围(1.12.2及以下无效) |
| 无 | STRING | false | 流体处理{NONE[忽略流体], SOURCE_ONLY[仅与源流体块碰撞], ALWAYS[与所有流体碰撞(低于1.13无效)]} |
| 无 | BOOLEAN | false | 是否比对轴对称包围盒 |
| 无 | BOOLEAN | false | 即使光线未命中任何可碰撞方块，也会返回光线路径中最后的落点 |

| 返回值类型 | 描述 |
|-----------|------|
| VECTOR | 击中的位置 |

> 根据光线追踪击中的Block位置(此语句运行前请设置原点，默认以玩家为原点)

# Selector选择器

## selector

### 显示选区（公有语句）

> `selector show <LONG> *they <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| show | SYMBOL | false | 显示占位符 |
| 无 | LONG | false | 显示时长 |
| they | STRING | false | 显示的选择器（只支持几何选择器） |

> 用粒子显示包含的几何Selector的选区

### 载入预设（公有语句）

> `selector preset <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| preset | SYMBOL | true | 预设占位符 |
| 无 | STRING | false | 预设键名 |

> 载入配置文件中的预设选择器

# Spirit精力

## spirit

### 减少精力（公有语句）

> `spirit take <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| take | SYMBOL | false | take |
| 无 | DOUBLE | false | 精力值 |
| they | CONTAINER | true | 减少精力的目标 |

> 获得精力值

### 检测精力（公有语句）

> `spirit has <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| has | SYMBOL | false | has |
| 无 | DOUBLE | false | 检测精力值 |
| they | CONTAINER | true | 检测的目标 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否有足够精力值 |

> 检测是否有足够精力值

### 给予精力（公有语句）

> `spirit give <DOUBLE> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| give | SYMBOL | false | give |
| 无 | DOUBLE | false | 精力值 |
| they | CONTAINER | true | 给予精力的目标 |

> 玩家获得精力值

### 获取最大精力值（公有语句）

> `spirit max *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| max | SYMBOL | true | max |
| they | CONTAINER | true | 获取的目标 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 精力值 |

> 获取玩家拥有的最大精力值

### 获取精力值（公有语句）

> `spirit now *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| now | SYMBOL | true | now |
| they | CONTAINER | true | 获取的目标 |

| 返回值类型 | 描述 |
|-----------|------|
| DOUBLE | 精力值 |

> 获取玩家拥有的精力值

# State状态机

## running

### 获取可运行state（公有语句）

> `running <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 状态名 |

| 返回值类型 | 描述 |
|-----------|------|
| STATE | 运动状态 |

> 获取可运行state

## state

### 强制停止当前状态（公有语句）

> `state stop`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| stop | SYMBOL | false | 停止位符 |

> 强制停止当前状态

### 强制执行下一状态（公有语句）

> `state next <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| next | SYMBOL | false | 下一占位符 |
| 无 | STRING | false | 状态名 |

| 返回值类型 | 描述 |
|-----------|------|
| STATE | 运动状态 |

> 强制执行指定下一状态

### 自动检测Status条件更新（公有语句）

> `state update`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| update | SYMBOL | false | 更新占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 适配的Status名 |

> 自动检测Status条件并更新Status

### 获取当前移动方向（公有语句）

> `state move`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| move | SYMBOL | false | 移动占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 移动方向 |

> 获取当前移动方向

### 获取当前运行的状态名（公有语句）

> `state now`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| now | SYMBOL | false | 当前占位符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | 状态名 |

> 获取当前运行的状态名

# Station专属语句

## event

### 设置事件是否取消（私有语句）

> `event cancelled <BOOLEAN>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| cancelled | SYMBOL | false | 取消标识符 |
| 无 | BOOLEAN | false | 是否取消 |

> 设置当前Station监听到的事件是否取消

# UUID唯一标识符

## uuid

### 随机生成UUID（公有语句）

> `uuid random`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| random | SYMBOL | false | 随机生成标识符 |

| 返回值类型 | 描述 |
|-----------|------|
| STRING | UUID |

> 随机生成一个唯一UUID

# Util工具类

## coalesce

### 取第一个非空（公有语句）

> `coalesce <ANY> *or [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 候选值 1 |
| or | ANY | true | 候选值 N |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 第一个非空值 |

> 从左到右依次求值，返回第一个不为 null 的结果；可以使用 `or` 连接多个候选值

## contains

### 是否包含（公有语句）

> `contains <ANY> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | Iterable 或 String |
| 无 | ANY | false | value |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否包含 |

> Iterable 或 String 是否包含 value

## ifBlank

### 空白默认（公有语句）

> `ifBlank <ANY> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |
| 无 | ANY | false | 如果为空白则返回 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 参数 |

> 如果 参数1 为空白（null/空字符串/全空格），则返回 参数2，否则返回 参数1（参数2 为惰性求值）

## ifEmpty

### 空集合默认（公有语句）

> `ifEmpty <ANY> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |
| 无 | ANY | false | 如果为空则返回 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 参数 |

> 如果 参数1 为空（null/空字符串/空集合等），则返回 参数2，否则返回 参数1（参数2 为惰性求值）

## ifNull

### 空值默认（公有语句）

> `ifNull <ANY> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |
| 无 | ANY | false | 如果为 null 则返回 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 参数 |

> 如果 参数1 为 null，则返回 参数2，否则返回 参数1（参数2 为惰性求值）

## isNull

### 是否为空（公有语句）

> `isNull <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否为空 |

> 检测指定值是否为 null

## isUnlimited

### 是否无限模式（公有语句）

> `isUnlimited`

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否处于无限模式 |

> 检测当前玩家是否处于无限模式（unlimit），无限模式下施放技能不消耗资源

## let

### 带值执行（公有语句）

> `let <ANY> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 原始值 |
| 无 | ANY | false | 执行语句 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 执行结果 |

> 先求值参数1，写入变量 @value，执行参数2，并返回参数2的结果

## notNull

### 是否非空（公有语句）

> `notNull <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否非空 |

> 检测指定值是否不为 null

## require

### 断言为真（公有语句）

> `require <BOOLEAN> <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | BOOLEAN | false | 条件 |
| 无 | STRING | false | 失败信息 |

| 返回值类型 | 描述 |
|-----------|------|
| BOOLEAN | 是否通过 |

> 若条件为 false，则抛出异常中断脚本

## requireNotNull

### 断言非空（公有语句）

> `requireNotNull <ANY> <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |
| 无 | STRING | false | 失败信息 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 参数 |

> 若参数为 null，则抛出异常中断脚本，否则返回该参数

## tap

### 旁路执行（公有语句）

> `tap <ANY> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 原始值 |
| 无 | ANY | false | 旁路语句 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 原始值 |

> 先求值参数1，写入变量 @value，执行参数2（忽略其返回值），最后返回参数1

## unlessBlank

### 非空白执行（公有语句）

> `unlessBlank <ANY> <ANY> *else [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |
| 无 | ANY | false | 非空白时执行的语句 |
| else | ANY | true | 空白时执行的语句 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 执行结果 |

> 参数1 非空白时，将其写入变量 @value 并执行 参数2；否则执行 else 后的语句（若未提供则返回 null）

## unlessEmpty

### 非空集合执行（公有语句）

> `unlessEmpty <ANY> <ANY> *else [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |
| 无 | ANY | false | 非空时执行的语句 |
| else | ANY | true | 空时执行的语句 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 执行结果 |

> 参数1 非空时，将其写入变量 @value 并执行 参数2；否则执行 else 后的语句（若未提供则返回 null）

## unlessNull

### 非空执行（公有语句）

> `unlessNull <ANY> <ANY> *else [ANY]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | 要检测的对象 |
| 无 | ANY | false | 非空时执行的语句 |
| else | ANY | true | 空值时执行的语句 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 执行结果 |

> 参数1 不为 null 时，将其写入变量 @value 并执行 参数2；否则执行 else 后的语句（若未提供则返回 null）

# Variable懒变量

## lazy

### 懒变量（私有语句）

> `lazy <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 加载的变量名(大小写不敏感) |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 变量值 |

> 以懒加载方式加载变量(第一次加载后保存数据，后续直接调用)

## reinit

### 重置懒变量（私有语句）

> `reinit <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 加载的变量名(大小写不敏感) |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 变量值 |

> 重新初始化变量，返回实时值

# germplugin附属语句

## germplugin

### 停止实体动作（公有语句）

> `germplugin animation/ani stop entity <STRING> *they [CONTAINER(@self)] *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| stop | SYMBOL | false | 停止标识符 |
| entity | SYMBOL | false | 实体标识符 |
| 无 | STRING | false | 动作名 |
| they | CONTAINER | true | 设置实体(自动识别玩家还是怪物) |
| viewers | CONTAINER | true | 可视玩家 |

> 停止实体动作

### 停止播放音乐（公有语句）

> `germplugin <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 音乐名 |
| they | CONTAINER | true | 可听玩家 |

> 停止播放音乐

### 停止方块动作（公有语句）

> `germplugin animation/ani stop block <STRING> <VECTOR> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| stop | SYMBOL | false | 停止标识符 |
| block | SYMBOL | false | 方块标识符 |
| 无 | STRING | false | 动作名 |
| 无 | VECTOR | false | xyz位置 |
| viewers | CONTAINER | true | 可视玩家 |

> 停止方块动作

### 停止玩家物品动作（公有语句）

> `germplugin animation/ani stop item <STRING> <STRING> *they [CONTAINER(@self)] *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| stop | SYMBOL | false | 停止标识符 |
| item | SYMBOL | false | 物品标识符 |
| 无 | STRING | false | 槽位名 |
| 无 | STRING | false | 动作名 |
| they | CONTAINER | true | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 停止玩家物品动作

### 发送effect特效（公有语句）

> `germplugin effect send <STRING> <STRING> *rotation <STRING(0,0,0)> *translate <VECTOR(0,0,0)> *timeout <INT(100)> *viewers [CONTAINER(@server)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect | SYMBOL | false | 特效标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | Index |
| 无 | STRING | false | 特效名 |
| rotation | STRING | false | x,y,z旋转角度 |
| translate | VECTOR | false | x,y,z平移位置 |
| timeout | INT | false | 存活时长tick |
| viewers | CONTAINER | true | 可视玩家 |
| they | CONTAINER | true | 生成位置或者绑定实体 |

> 发送effect特效

### 打开GUI（公有语句）

> `germplugin gui <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| gui | SYMBOL | false | gui标识符 |
| 无 | STRING | false | gui名字 |
| they | CONTAINER | true | 打开GUI的玩家 |

> 打开萌芽Gui

### 打开HUD（公有语句）

> `germplugin hud <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| hud | SYMBOL | false | hud标识符 |
| 无 | STRING | false | hud名字 |
| they | CONTAINER | true | 打开HUD的玩家 |

> 打开萌芽HUD

### 播放音乐（公有语句）

> `germplugin sound send <STRING> <STRING> *loc [VECTOR(可听玩家眼睛位置)] *loop [BOOLEAN(false)] *by/with [FLOAT(1.0)] [FLOAT(1.0)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| sound | SYMBOL | false | 音乐标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 音乐名 |
| 无 | STRING | false | 播放类型 |
| loc | VECTOR | true | 播放世界位置向量 |
| loop | BOOLEAN | true | 是否循环 |
| by/with | FLOAT | true | 声音大小 |
| 无 | FLOAT | true | 声音音调 |
| they | CONTAINER | true | 可听玩家 |

> 播放音乐

### 清除effect特效（公有语句）

> `germplugin effect clear *they [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect | SYMBOL | false | 特效标识符 |
| clear | SYMBOL | false | 清除标识符 |
| they | CONTAINER | true | 可视玩家 |

> 清除effect特效

### 清除临时时装（公有语句）

> `germplugin armourers clear [STRING(ALL)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| armourers | SYMBOL | false | armourers标识符 |
| clear | SYMBOL | false | 清除标识符 |
| 无 | STRING | true | 时装名 |
| they | CONTAINER | true | 目标容器 |

> 清除临时基岩时装

### 移除effect特效（公有语句）

> `germplugin effect remove <STRING> *they [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| effect | SYMBOL | false | 特效标识符 |
| remove | SYMBOL | false | 移除标识符 |
| 无 | STRING | false | Index |
| they | CONTAINER | true | 可视玩家 |

> 移除effect特效

### 移除实体动作（公有语句）

> `germplugin animation/ani remove entity *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| remove | SYMBOL | false | 移除标识符 |
| entity | SYMBOL | false | 实体标识符 |
| they | CONTAINER | false | 设置实体(自动识别玩家还是怪物) |
| viewers | CONTAINER | true | 可视玩家 |

> 移除实体动作

### 移除方块动作（公有语句）

> `germplugin animation/ani remove block <VECTOR> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| remove | SYMBOL | false | 移除标识符 |
| block | SYMBOL | false | 方块标识符 |
| 无 | VECTOR | false | xyz位置 |
| viewers | CONTAINER | true | 可视玩家 |

> 移除方块动作

### 移除玩家物品动作（公有语句）

> `germplugin animation/ani remove item <STRING> *they [CONTAINER(@self)] *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| remove | SYMBOL | false | 移除标识符 |
| item | SYMBOL | false | 物品标识符 |
| 无 | STRING | false | 槽位名 |
| they | CONTAINER | true | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 移除玩家物品动作

### 获取槽位内物品（公有语句）

> `germplugin slot <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| slot | SYMBOL | false | 槽位标识符 |
| 无 | STRING | false | 槽位名 |
| they | CONTAINER | true | 获取的玩家 |

> 获取槽位内物品

### 设置临时时装（公有语句）

> `germplugin armourers send <STRING> *timeout [LONG(100)] *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| armourers | SYMBOL | false | armourers标识符 |
| send | SYMBOL | false | 发送标识符 |
| 无 | STRING | false | 时装名 |
| timeout | LONG | true | 临时时长 |
| they | CONTAINER | true | 目标容器 |

> 设置临时基岩时装

### 设置实体动作（公有语句）

> `germplugin animation/ani set/to entity <STRING> <FLOAT> <BOOLEAN> *they <CONTAINER> *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set/to | SYMBOL | false | 设置标识符 |
| entity | SYMBOL | false | 实体标识符 |
| 无 | STRING | false | 动作名 |
| 无 | FLOAT | false | 播放速度 |
| 无 | BOOLEAN | false | 是否倒放 |
| they | CONTAINER | false | 设置实体(自动识别玩家还是怪物) |
| viewers | CONTAINER | true | 可视玩家 |

> 设置实体动作

### 设置方块动作（公有语句）

> `germplugin animation/ani set block <STRING> <FLOAT> <BOOLEAN> <VECTOR> *viewers [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set | SYMBOL | false | 设置标识符 |
| block | SYMBOL | false | 方块标识符 |
| 无 | STRING | false | 动作名 |
| 无 | FLOAT | false | 播放速度 |
| 无 | BOOLEAN | false | 是否倒放 |
| 无 | VECTOR | false | xyz位置 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置方块动作

### 设置玩家物品动作（公有语句）

> `germplugin animation/ani set item <STRING> <STRING> <FLOAT> <BOOLEAN> *they [CONTAINER(@self)] *viewers [CONTAINER(@server)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| animation/ani | SYMBOL | false | 动作标识符 |
| set | SYMBOL | false | 设置标识符 |
| item | SYMBOL | false | 物品标识符 |
| 无 | STRING | false | 槽位名 |
| 无 | STRING | false | 动作名 |
| 无 | FLOAT | false | 播放速度 |
| 无 | BOOLEAN | false | 是否倒放 |
| they | CONTAINER | true | 设置实体 |
| viewers | CONTAINER | true | 可视玩家 |

> 设置玩家物品动作

### 设置视角（公有语句）

> `germplugin view <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| view | SYMBOL | false | 视角标识符 |
| 无 | STRING | false | 视角(FIRST_PERSON, THIRD_PERSON_REVERSE, CURRENT_PERSON, THIRD_PERSON) |
| they | CONTAINER | true | 设置人称的玩家 |

> 设置第几人称视角

# 上下文

## parameter/parm

### 减parameter参数（私有语句）

> `parameter <STRING> sub/decrease/- <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 目标参数 |
| sub/decrease/- | SYMBOL | false | 减标识符 |
| 无 | ANY | false | 目标参数 |

> 减指定parameter参数

### 加parameter参数（私有语句）

> `parameter <STRING> add/increase/+ <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 目标参数 |
| add/increase/+ | SYMBOL | false | 加标识符 |
| 无 | ANY | false | 目标参数 |

> 加指定parameter参数

### 设置parameter参数（私有语句）

> `parameter <STRING> set/to/= <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 目标参数 |
| set/to/= | SYMBOL | false | 设置标识符 |
| 无 | ANY | false | 目标参数 |

> 设置指定parameter参数

### 读取parameter参数（私有语句）

> `parameter <STRING>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 目标参数 |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 获取的参数 |

> 获取指定parameter参数

## senderSpace

### sender空间（公有语句）

> `senderSpace *they <CONTAINER> <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| they | CONTAINER | false | 需要参与的Senders |
| 无 | ANY | false | 执行语句 { action } |

> 循环以指定sender执行内部语句

# 属性系统

## damage

### 攻击目标（公有语句）

> `damage <DOUBLE> <BOOLEAN> *they <CONTAINER> *source [CONTAINER(@self)] *type [STRING(PHYSICS)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | DOUBLE | false | 攻击数值 |
| 无 | BOOLEAN | false | 攻击是否接入属性系统 |
| they | CONTAINER | false | 攻击目标 |
| source | CONTAINER | true | 攻击来源 |
| type | STRING | true | 攻击类型 |

> 攻击目标，支持接入属性系统

# 普通语句

## directCast

### 强制释放技能（私有语句）

> `directCast <STRING> <INT> <BOOLEAN> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 技能名 |
| 无 | INT | false | 等级 |
| 无 | BOOLEAN | false | 是否消耗(蓝,冷却,沉默) |
| they | CONTAINER | true | 释放者 |

> 强制释放技能，蓄力类技能进入蓄力

## directRelease

### 强制释放蓄力技能（私有语句）

> `directRelease <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 技能名 |
| they | CONTAINER | true | 释放者 |

> 强制释放蓄力技能，使蓄力类技能退出蓄力状态，并释放效果

## randomAction

### 随机运行一段Action（公有语句）

> `randomAction <ITERABLE>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ITERABLE | false | 语句列表[ { tell me }, { tell her } ] |

| 返回值类型 | 描述 |
|-----------|------|
| ANY | 运行的Action返回值 |

> 随机运行一段Action

## runExtend

### 运行拓展子Action（私有语句）

> `runExtend <STRING> *origin [TARGET(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 拓展名 |
| origin | TARGET | true | 私有原点 |

> 运行拓展子Action，继承母环境上下文，但是私有上下文，返回运行结果(只能在技能环境中使用)

## sync

### 同步Sync（私有语句）

> `sync <ANY>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | ANY | false | actions |

> 将语句在主线程运行并等待返回

## tryCast

### 尝试释放技能（私有语句）

> `tryCast <STRING> *they [CONTAINER(@self)]`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | STRING | false | 技能名 |
| they | CONTAINER | true | 释放者 |

> 尝试释放技能，通过条件检测才会释放

## wait/delay/sleep

### 延迟delay（私有语句）

> `wait <LONG>`

| 先导词 | 类型 | 可选 | 描述 |
|--------|------|------|------|
| 无 | LONG | false | tick |

> 延迟多少Tick

# Selector选择器列表

## 流式选择器

### 存活过滤

> `@alive `

> 过滤存活/死亡实体，@alive只保留存活实体，!@alive只保留死亡实体

> @alive
> !@alive

### 数量过滤

> `@amount [SYMBOL(take)]`

| 类型 | 描述 |
|------|------|
| SYMBOL | drop丢弃前方take丢弃后方 |

> 丢弃超出指定容器大小范围的目标，可选择丢弃方式

> @amount 1 drop
> @amount 1 take

### Sender当前位置

> `@current [SYMBOL(l)]`

| 类型 | 描述 |
|------|------|
| SYMBOL | 位置模式，l代表脚底，e代表眼睛 |

> Sender的当前位置

> @current e
> @current l

### 改变视角向量

> `@direct [DOUBLE(0.0)] [DOUBLE(0.0)] [DOUBLE(0.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | x |
| DOUBLE | y |
| DOUBLE | z |

> 将所有目标的视角向量修改

> @direct x y z
> !@direct x y z

### 距离过滤

> `@distance/dist [DOUBLE(0.0)] [DOUBLE(32.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 最小距离 |
| DOUBLE | 最大距离 |

> 按距离原点的范围过滤目标，只保留距离在指定范围内的目标

> @distance 5 10

### 血量过滤

> `@health/hp [DOUBLE(0.0)] [DOUBLE(1.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 最小血量百分比 |
| DOUBLE | 最大血量百分比 |

> 按血量百分比过滤实体，适合斩杀类技能

> @health 0 0.5

### 进入sender客户端的实体

> `@joiner `

> 进入sender客户端的实体

> @joiner
> !@joiner

### 改变坐标

> `@offset [DOUBLE(0.0)] [DOUBLE(0.0)] [DOUBLE(0.0)] [BOOLEAN(false)] [BOOLEAN(false)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | x |
| DOUBLE | y |
| DOUBLE | z |
| BOOLEAN | 是否随YAW改变 |
| BOOLEAN | 是否随PITCH改变 |

> 将所有目标转换成location并位移

> @offset 2 0 0 true false
> !@offset x y z yaw pitch

### 原点位置

> `@origin [SYMBOL(l)]`

| 类型 | 描述 |
|------|------|
| SYMBOL | 位置模式，l代表脚底，e代表眼睛 |

> 原点位置

> @origin e
> @origin l

### pvp筛选

> `@pvp `

> 筛选是否为pvp

> @pvp
> !@pvp

### 随机抽取

> `@random/rand [INT(1)]`

| 类型 | 描述 |
|------|------|
| INT | 数量 |

> 从容器中随机抽取N个目标

> @random 3

### sender玩家

> `@self `

> Sender转化为玩家

> @self
> !@self

### 全服玩家

> `@server/all/players/online `

> 获取全服在线玩家

> @server
> @all
> @players
> @online

### 视线可见过滤

> `@sight/visible `

> 过滤视线可达的实体，@sight只保留可见实体，!@sight只保留被遮挡实体

> @sight
> !@sight

### 排序

> `@sort [STRING(near)]`

| 类型 | 描述 |
|------|------|
| STRING | 排序模式: near=按距离从近到远, far=从远到近, random=随机打乱, health=按血量从低到高 |

> 对容器中的目标进行排序

> @sort near
> @sort health

### dp队伍过滤

> `@team `

> 只保留队内人员,或只保留队外人员

> @team
> !@team

### dp队员

> `@teammate `

> dp队员

> @teammate

### 实体/目标类型过滤

> `@type [STRING(player)]`

| 类型 | 描述 |
|------|------|
| STRING | 指定实体类型，location表示坐标类型目标，用英文逗号分割 |

> 实体/目标类型过滤

> @type player,pig
> !@type location

### 去重

> `@unique/distinct `

> 移除容器中重复的目标（按UUID或坐标去重）

> @unique

### 添加/剔除世界目标

> `@world [STRING(sender)]`

| 类型 | 描述 |
|------|------|
| STRING | 指定世界或者sender世界 |

> 添加/剔除世界成员

> @world sender/指定世界
> !@world sender/指定世界

## 几何选择器

### 环状选取

> `@annular [DOUBLE(0.0)] [DOUBLE(0.0)] [DOUBLE(0.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 最小半径 |
| DOUBLE | 最大半径 |
| DOUBLE | 高度 |

> 选中根据原点来定义的环状实体

> @annular 2 3 5

### 圆锥体范围

> `@cone [DOUBLE(5.0)] [DOUBLE(10.0)] [DOUBLE(0.0)] [DOUBLE(0.0)] [BOOLEAN(false)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 底部半径 |
| DOUBLE | 长度 |
| DOUBLE | 偏航角 |
| DOUBLE | y轴偏移 |
| BOOLEAN | 跟随pitch |

> 以玩家眼睛为顶点、朝向为轴的圆锥体范围选取

> @cone 5 10

### 圆柱体范围

> `@cylinder/cyl [DOUBLE(5.0)] [DOUBLE(10.0)] [DOUBLE(0.0)] [DOUBLE(0.0)] [BOOLEAN(false)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 半径 |
| DOUBLE | 高度 |
| DOUBLE | 前方偏移 |
| DOUBLE | y轴偏移 |
| BOOLEAN | 跟随pitch |

> 以玩家朝向为轴心的圆柱体范围选取

> @cylinder 5 10

### 脚下地面位置

> `@floor [DOUBLE(50.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 最低限度 |

> 选中脚下的第一个碰撞方块(最低支持1.12.2版本)

> @floor 10

### 圆台形范围

> `@frustum [DOUBLE(1.0)] [DOUBLE(10.0)] [DOUBLE(10.0)] [DOUBLE(0)] [DOUBLE(0.0)] [BOOLEAN(false)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 上半径 |
| DOUBLE | 下半径 |
| DOUBLE | 仰角 |
| DOUBLE | 偏航角 |
| DOUBLE | y轴偏移 |
| BOOLEAN | 跟随pitch |

> 前方扇形范围的实体

> @frustum 1 5 10 0 1 false

### 线段范围

> `@line [DOUBLE(10.0)] [DOUBLE(1.0)] [DOUBLE(2.0)] [BOOLEAN(false)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 长度 |
| DOUBLE | 宽度 |
| DOUBLE | 高度 |
| BOOLEAN | 跟随pitch |

> 沿朝向方向的窄长方体，前方偏移固定为长度/2，适合剑气、直线冲击波类技能

> @line 10 1 2

### 具体坐标点

> `@location [DOUBLE(0.0)] [DOUBLE(0.0)] [DOUBLE(0.0)] [FLOAT(0.0)] [FLOAT(0.0)] [STRING(原点世界)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | x |
| DOUBLE | y |
| DOUBLE | z |
| FLOAT | yaw |
| FLOAT | pitch |
| STRING | world |

> 具体坐标点

> @location 2 3 5 1 2

### 视线目标

> `@lookat/look [DOUBLE(32.0)] [DOUBLE(5.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 最大距离 |
| DOUBLE | 角度容差 |

> 选取玩家准星正对的实体（视线方向夹角最小的实体）

> @lookat 32 5

### 最近实体

> `@nearest [INT(1)] [DOUBLE(32.0)]`

| 类型 | 描述 |
|------|------|
| INT | 数量 |
| DOUBLE | 搜索半径 |

> 选取距离原点最近的N个实体

> @nearest 3 32

### 有向包围盒

> `@obb [DOUBLE(0.0)] [DOUBLE(0.0)] [DOUBLE(0.0)] [DOUBLE(0.0)] [DOUBLE(0.0)] [BOOLEAN(false)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 长度 |
| DOUBLE | 宽度 |
| DOUBLE | 高度 |
| DOUBLE | 前方偏移 |
| DOUBLE | 上方偏移 |
| BOOLEAN | 是否随俯仰角改变 |

> 选中视角方向的给定长宽高碰撞箱接触的实体

> @obb 2 2 2 2 1 true

### 球形范围

> `@range [DOUBLE(10.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 半径 |

> 球形范围内的所有实体

> @range 10

### 射线实体

> `@rayhit [STRING(a)] [DOUBLE(0.0)]`

| 类型 | 描述 |
|------|------|
| STRING | 存储方向向量的键名 |
| DOUBLE | 长度 |

> 选中从原点沿向量方向、给定有限长度内穿过的所有实体

> @rayhit a 10

### 环形多点

> `@ring [DOUBLE(5.0)] [INT(8)] [DOUBLE(0.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 半径 |
| INT | 数量 |
| DOUBLE | y轴偏移 |

> 在原点周围等间距生成N个位置点（圆周均匀分布），适合环形法阵、召唤阵等技能

> @ring 5 8

### 散射多点

> `@scatter [INT(5)] [DOUBLE(10.0)] [DOUBLE(0.0)]`

| 类型 | 描述 |
|------|------|
| INT | 数量 |
| DOUBLE | 半径 |
| DOUBLE | 前方偏移 |

> 在指定范围内随机生成N个位置点，适合陨石雨、随机落雷等AOE技能

> @scatter 5 10

### 扇形范围

> `@sec/sector [DOUBLE(10.0)] [DOUBLE(120.0)] [DOUBLE(2)] [DOUBLE(0.0)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 半径 |
| DOUBLE | 角度 |
| DOUBLE | 高度 |
| DOUBLE | y轴偏移 |

> 前方扇形范围的实体

> @sector 10 120 2 0 false
> @sec 10 120 2 true

### 原点偏移

> `@vector/direction [DOUBLE(0.0)] [DOUBLE(0.0)] [DOUBLE(0.0)] [BOOLEAN(false)] [BOOLEAN(false)]`

| 类型 | 描述 |
|------|------|
| DOUBLE | 前方 |
| DOUBLE | 上方 |
| DOUBLE | 右方 |
| BOOLEAN | 是否随YAW改变 |
| BOOLEAN | 是否随PITCH改变 |

> 根据原点实体朝向来确认点的位置

> @vector 2 3 5 true false
> @direction x y z yaw pitch

# Trigger触发器列表

## Bukkit原版

### Async Player Chat

- 事件类：`org.bukkit.event.player.AsyncPlayerChatEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | message | 无 | true | false | false | java.lang.String | true | 消息 |
| STRING | format | 无 | true | false | false | java.lang.String | true | 消息格式 |
| CONTAINER | recipients | 无 | true | false | false | org.gitee.orryx.core.container.Container | true | 能看到这条消息的玩家 |

> 异步玩家聊天事件触发器

### Async Player Chat Preview

- 事件类：`org.bukkit.event.player.AsyncPlayerChatPreviewEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | message | 无 | true | false | false | java.lang.String | true | 消息 |
| STRING | format | 无 | true | false | false | java.lang.String | true | 消息格式 |
| CONTAINER | recipients | 无 | true | false | false | org.gitee.orryx.core.container.Container | true | 能看到这条消息的玩家 |

> 异步玩家格式化聊天预览

### Block Break

- 事件类：`org.bukkit.event.block.BlockBreakEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | block | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 被破坏的方块位置 |
| INT | expToDrop | 无 | true | false | false | java.lang.Integer | true | 掉落的经验值 |

> 当玩家破坏方块时触发

### Block Place

- 事件类：`org.bukkit.event.block.BlockPlaceEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | block | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 放置的方块位置 |
| TARGET | blockAgainst | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 被放置方块依附的方块位置 |
| STRING | hand | 无 | true | false | false | java.lang.String | true | 使用的手 |
| ITEM_STACK | itemInHand | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 手中的物品 |

> 当玩家放置方块时触发

### Entity Regain Health

- 事件类：`org.bukkit.event.entity.EntityRegainHealthEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | amount | 无 | true | false | false | java.lang.Double | true | 恢复的生命值 |
| STRING | reason | 无 | true | false | false | java.lang.String | true | 恢复原因 |

> 当玩家恢复生命值时触发

### Entity Shoot Bow

- 事件类：`org.bukkit.event.entity.EntityShootBowEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITEM_STACK | bow | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 使用的弓 |
| TARGET | projectile | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 发射的投射物 |
| FLOAT | force | 无 | true | false | false | java.lang.Float | true | 弓的拉力 |
| STRING | hand | 无 | true | false | false | java.lang.String | true | 使用的手 |

> 当实体射箭时触发（仅玩家）

### Player Advancement Done

- 事件类：`org.bukkit.event.player.PlayerAdvancementDoneEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | advancement | 无 | true | false | false | java.lang.String | true | 成就 |

> 玩家成就完成

### Player Animation

- 事件类：`org.bukkit.event.player.PlayerAnimationEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | animation | 无 | true | false | false | java.lang.String | true | 动作名 |

> 玩家动作

### Player ArmorStand Manipulate

- 事件类：`org.bukkit.event.player.PlayerArmorStandManipulateEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITEM_STACK | armorStandItem | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 盔甲架的物品 |
| ITEM_STACK | playerItem | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 玩家的物品 |
| STRING | slot | 无 | true | false | false | java.lang.String | true | 玩家的EquipmentSlot：BODY/CHEST/FEET/HAND/HEAD/LEGS/OFF_HAND |
| TARGET | rightClicked | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 玩家右键的实体 |
| STRING | hand | 无 | true | false | false | java.lang.String | true | 玩家使用的手的EquipmentSlot：HAND/OFF_HAND |

> 当玩家与装甲架交互并且进行交换, 取回或放置物品时触发

### Player Bed Enter

- 事件类：`org.bukkit.event.player.PlayerBedEnterEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | bedEnterResult | 无 | true | false | false | java.lang.String | true | 玩家进入床的结果：NOT_POSSIBLE_HERE/NOT_POSSIBLE_NOW/NOT_SAFE/OK/OTHER_PROBLEM/TOO_FAR_AWAY |
| TARGET | bed | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 床的位置 |
| STRING | useBed | 无 | true | false | false | java.lang.String | true | 玩家使用床的结果：ALLOW/DEFAULT/DENY |

> 玩家准备躺到床上时触发

### Player Bed Leave

- 事件类：`org.bukkit.event.player.PlayerBedLeaveEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | bed | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 床的位置 |
| BOOLEAN | shouldSetSpawn | 无 | true | false | false | java.lang.Boolean | true | 是否需要设置出生点 |

> 玩家离开床时触发

### Player Bucket Empty

- 事件类：`org.bukkit.event.player.PlayerBucketEmptyEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | blockClicked | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 点击的方块位置 |
| TARGET | block | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 水或者岩浆的位置 |
| STRING | bucket | 无 | true | false | false | java.lang.String | true | 返回玩家手里的桶的类型 |

> 玩家用完一只桶后触发

### Player Bucket Entity

- 事件类：`org.bukkit.event.player.PlayerBucketEntityEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITEM_STACK | originalBucket | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 捕获前的桶 |
| ITEM_STACK | entityBucket | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 捕获后的桶 |
| TARGET | entity | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 获取将要放入桶中的实体 |

> 玩家捕获存储桶中的实体时触发

### Player Bucket Fill

- 事件类：`org.bukkit.event.player.PlayerBucketFillEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | blockClicked | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 点击的方块位置 |
| TARGET | block | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 水或者岩浆的位置 |
| STRING | bucket | 无 | true | false | false | java.lang.String | true | 返回玩家手里的桶的类型 |

> 桶装满时触发

### Player Changed MainHand

- 事件类：`org.bukkit.event.player.PlayerChangedMainHandEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | mainHand | 无 | true | false | false | java.lang.String | true | 改变后的主手：LEFT/RIGHT |

> 当玩家在客户端设置改变主手时触发

### Player Changed World

- 事件类：`org.bukkit.event.player.PlayerChangedWorldEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | from | 无 | true | false | false | java.lang.String | true | 从哪个世界来 |
| STRING | to | 无 | true | false | false | java.lang.String | true | 到哪个世界去 |

> 当玩家切换到另一个世界时触发

### Player Chat Tab Complete

- 事件类：`org.bukkit.event.player.PlayerChatTabCompleteEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | lastToken | 无 | true | false | false | java.lang.String | true | 获取被补全消息的最后一个'标记' |
| STRING | chatMessage | 无 | true | false | false | java.lang.String | true | 获取将被补全的聊天消息 |
| ITERABLE | tabCompletions | 无 | true | false | false | java.lang.Iterable | true | 获取所有补全项集合 |

> 当玩家尝试补全聊天消息时触发

### Player Command Preprocess

- 事件类：`org.bukkit.event.player.PlayerCommandPreprocessEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | message | 无 | true | false | false | java.lang.String | true | 命令消息 |

> 当玩家执行命令前触发

### Player Command Send

- 事件类：`org.bukkit.event.player.PlayerCommandSendEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITERABLE | commands | 无 | true | false | false | java.lang.Iterable | true | 返回将发送给客户端的所有顶级命令的可变集合 |

> 当服务器可用命令列表发送给玩家时触发

### Player Damage Post

- 事件类：`org.gitee.orryx.api.events.damage.OrryxDamageEvents$Post`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | damage | 无 | true | false | false | java.lang.Double | true | 伤害 |
| TARGET | attacker | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 攻击者 |
| TARGET | defender | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 防御者 |
| STRING | type | 无 | true | false | false | java.lang.String | true | 攻击类型：PHYSICS/MAGIC/FIRE/REAL/SELF/CONSOLE/CUSTOM |

> 当玩家攻击时发生，如果攻击来自于Or技能，那将会继承技能环境中的参数

### Player Damage Pre

- 事件类：`org.gitee.orryx.api.events.damage.OrryxDamageEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | damage | 无 | true | true | false | java.lang.Double | true | 伤害 |
| TARGET | attacker | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 攻击者 |
| TARGET | defender | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 防御者 |
| STRING | type | 无 | true | false | false | java.lang.String | true | 攻击类型：PHYSICS/MAGIC/FIRE/REAL/SELF/CONSOLE/CUSTOM |
| BOOLEAN | crit | 无 | true | true | false | java.lang.Boolean | true | 是否暴击 |
| BOOLEAN | no | 无 | true | false | false | java.lang.Boolean | true | 是否为原版伤害事件 |
| BOOLEAN | ap | 无 | true | false | false | java.lang.Boolean | true | 是否为 AttributePlus 伤害事件 |
| BOOLEAN | axh | 无 | true | false | false | java.lang.Boolean | true | 是否为 AstraXHero 伤害事件 |

> 当玩家攻击时发生，如果攻击来自于Or技能，那将会继承技能环境中的参数

### Player Damaged Post

- 事件类：`org.gitee.orryx.api.events.damage.OrryxDamageEvents$Post`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | damage | 无 | true | false | false | java.lang.Double | true | 伤害 |
| TARGET | attacker | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 攻击者 |
| TARGET | defender | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 防御者 |
| STRING | type | 无 | true | false | false | java.lang.String | true | 攻击类型：PHYSICS/MAGIC/FIRE/REAL/SELF/CONSOLE/CUSTOM |

> 当玩家受到攻击时发生，如果攻击来自于Or技能，那将会继承技能环境中的参数

### Player Damaged Pre

- 事件类：`org.gitee.orryx.api.events.damage.OrryxDamageEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | damage | 无 | true | false | false | java.lang.Double | true | 伤害 |
| TARGET | attacker | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 攻击者 |
| TARGET | defender | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 防御者 |
| STRING | type | 无 | true | false | false | java.lang.String | true | 攻击类型：PHYSICS/MAGIC/FIRE/REAL/SELF/CONSOLE/CUSTOM |

> 当玩家受到攻击时发生，如果攻击来自于Or技能，那将会继承技能环境中的参数

### Player Death

- 事件类：`org.bukkit.event.entity.PlayerDeathEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | deathMessage | 无 | true | false | false | java.lang.String | true | 死亡消息 |
| BOOLEAN | keepInventory | 无 | true | false | false | java.lang.Boolean | true | 是否保留背包 |
| BOOLEAN | keepLevel | 无 | true | false | false | java.lang.Boolean | true | 是否保留等级 |
| INT | newLevel | 无 | true | false | false | java.lang.Integer | true | 重生后的等级 |
| INT | newTotalExp | 无 | true | false | false | java.lang.Integer | true | 重生后的总经验 |
| INT | newExp | 无 | true | false | false | java.lang.Integer | true | 重生后的经验 |
| INT | droppedExp | 无 | true | false | false | java.lang.Integer | true | 掉落的经验 |

> 当玩家死亡时触发

### Player Drop Item

- 事件类：`org.bukkit.event.player.PlayerDropItemEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | itemDrop | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 获得此玩家丢出的物品实体 |
| ITEM_STACK | itemStackDrop | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 获得此玩家丢出的物品 |

> 玩家丢出物品时触发

### Player Edit Book

- 事件类：`org.bukkit.event.player.PlayerEditBookEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | newBookMeta | 无 | true | false | false | java.lang.Object | true | 新书的数据 |
| ANY | previousBookMeta | 无 | true | false | false | java.lang.Object | true | 旧书的数据 |
| BOOLEAN | isSigning | 无 | true | false | false | java.lang.Boolean | true | 检测书本是否正在被签名 |

> 当玩家编辑或签名书与笔时触发。如果事件中断取消，书与笔的元数据不会改变。

### Player Egg Throw

- 事件类：`org.bukkit.event.player.PlayerEggThrowEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | egg | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 鸡蛋实体 |
| BOOLEAN | isHatching | 无 | true | false | false | java.lang.Boolean | true | 检测鸡蛋是否将被孵化 |
| BYTE | numHatches | 无 | true | false | false | java.lang.Byte | true | 检测将被孵化生物的数量 |
| STRING | hatchingType | 无 | true | false | false | java.lang.String | true | 获取将被孵化的生物类型(默认为EntityType.CHICKEN) |

> 玩家抛出鸡蛋时触发本事件，鸡蛋可能孵化。

### Player Exp Change

- 事件类：`org.bukkit.event.player.PlayerExpChangeEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| INT | amount | 无 | true | false | false | java.lang.Integer | true | 经验 |

> 当玩家经验值发生变化时触发

### Player Exp Cooldown Change

- 事件类：`org.bukkit.event.player.PlayerExpCooldownChangeEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| INT | newCooldown | 无 | true | false | false | java.lang.Integer | true | 新的冷却时间 |
| INT | reason | 无 | true | false | false | java.lang.Integer | true | 原因：PICKUP_ORB/PLUGIN |

> 当玩家经验冷却时间发生变化时触发

### Player Fish

- 事件类：`org.bukkit.event.player.PlayerFishEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | status | 无 | true | false | false | java.lang.String | true | 钓鱼状态：BITE/CAUGHT_ENTITY/CAUGHT_FISH/FAILED_ATTEMPT/FISHING/IN_GROUND/REEL_IN |
| STRING | hand | 无 | true | false | false | java.lang.String | true | 获取此事件中使用的手：OFF_HAND/HAND |
| TARGET | caught | 无 | true | false | true | org.gitee.orryx.core.targets.ITarget | true | 玩家捕获的实体 |
| INT | expToDrop | 无 | true | true | false | java.lang.Integer | true | 掉落的经验 |
| STRING | hookState | 无 | true | false | false | java.lang.String | true | 获取此鱼钩的当前状态：BOBBING/HOOKED_ENTITY/UNHOOKED |
| TARGET | hookedEntity | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 被钩中的实体 |
| BOOLEAN | applyLure | 无 | true | true | false | java.lang.Boolean | true | 获取是否应应用诱饵附魔来减少等待时间 |
| BOOLEAN | isInOpenWater | 无 | true | false | false | java.lang.Boolean | true | 检查这个鱼钩是否在开阔的水域中 |
| BOOLEAN | isRainInfluenced | 无 | true | true | false | java.lang.Boolean | true | 等待和诱饵的时间是否会受到雨水的影响 |
| BOOLEAN | isSkyInfluenced | 无 | true | true | false | java.lang.Boolean | true | 等待和诱饵的时间是否会受到直达天空的影响 |
| FLOAT | maxLureAngle | 无 | true | true | false | java.lang.Float | true | 获取等待时间之后鱼出现的最大角度 |
| FLOAT | minLureAngle | 无 | true | true | false | java.lang.Float | true | 获取等待时间之后鱼出现的最小角度 |
| INT | maxLureTime | 无 | true | true | false | java.lang.Integer | true | 获得鱼出现后等待鱼上钩所需等待的最大Tick数 |
| INT | minLureTime | 无 | true | true | false | java.lang.Integer | true | 获得鱼出现后等待鱼上钩所需等待的最小Tick数 |
| INT | maxWaitTime | 无 | true | true | false | java.lang.Integer | true | 获取等待鱼出现的最大Tick数 |
| INT | minWaitTime | 无 | true | true | false | java.lang.Integer | true | 获取等待鱼出现的最小Tick数 |
| DOUBLE | biteChance | 无 | true | true | false | java.lang.Double | true | 咬钩几率 |

> 当玩家钓鱼时触发

### Player GameMode Change

- 事件类：`org.bukkit.event.player.PlayerGameModeChangeEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | newGameMode | 无 | true | false | false | java.lang.String | true | 新的游戏模式：ADVENTURE/CREATIVE/SPECTATOR/SURVIVAL |

> 当玩家游戏模式发生变化时触发

### Player Harvest Block

- 事件类：`org.bukkit.event.player.PlayerHarvestBlockEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | harvestedBlock | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 获取被收获的方块位置 |
| ITERABLE | itemsHarvested | 无 | true | false | false | java.lang.Iterable | true | 获取从此方块收获的物品列表 |
| STRING | hand | 无 | true | false | false | java.lang.String | true | 获取用于收获方块的手：OFF_HAND/HAND |

> 当玩家收获作物方块时触发

### Player Hide Entity

- 事件类：`org.bukkit.event.player.PlayerHideEntityEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | entity | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 实体 |

> 当可见实体对玩家隐藏时触发

### Player Interact

- 事件类：`org.bukkit.event.player.PlayerInteractEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| VECTOR | clickedPosition | 无 | true | false | false | org.gitee.orryx.api.adapters.vector.IVector | true | 点击的方向向量 |
| ITEM_STACK | item | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 点击物品 |
| STRING | action | 无 | true | false | false | java.lang.String | true | 点击动作类型：LEFT_CLICK_AIR/LEFT_CLICK_BLOCK/PHYSICAL/RIGHT_CLICK_AIR/RIGHT_CLICK_BLOCK |
| TARGET | clickedBlock | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 被点击的方块位置 |

> 当玩家对一个对象或空气进行交互时触发

### Player Interact At Entity

- 事件类：`org.bukkit.event.player.PlayerInteractAtEntityEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| VECTOR | clickedPosition | 无 | true | false | false | org.gitee.orryx.api.adapters.vector.IVector | true | 点击的方向向量 |
| TARGET | rightClicked | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 被点击的实体 |

> 当玩家在实体上点击某实体上的某位置时触发

### Player Interact Entity

- 事件类：`org.bukkit.event.player.PlayerInteractEntityEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | rightClicked | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 被点击的实体 |

> 当玩家点击一个实体时调用

### Player Item Break

- 事件类：`org.bukkit.event.player.PlayerItemBreakEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITEM_STACK | brokenItem | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 损坏的物品 |

> 某玩家工具耐久消耗完毕时触发(比如铲子，打火石，铁制工具)

### Player Item Consume

- 事件类：`org.bukkit.event.player.PlayerItemConsumeEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITEM_STACK | item | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 消耗完的物品 |
| STRING | hand | 无 | true | false | false | java.lang.String | true | 获取此事件中使用的手：OFF_HAND/HAND |

> 某玩家工具耐久消耗完毕时触发(比如铲子，打火石，铁制工具)

### Player Item Damage

- 事件类：`org.bukkit.event.player.PlayerItemDamageEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITEM_STACK | item | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 损伤的物品 |
| INT | damage | 无 | true | false | false | java.lang.Integer | true | 损伤的耐久 |

> 当玩家使用的物品因使用而受到耐久性损坏时触发

### Player Item Held

- 事件类：`org.bukkit.event.player.PlayerItemHeldEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| INT | newSlot | 无 | true | false | false | java.lang.Integer | true | 新格子 |
| INT | previousSlot | 无 | true | false | false | java.lang.Integer | true | 旧格子 |
| ITEM_STACK | newItemStack | 无 | true | false | true | org.bukkit.inventory.ItemStack | true | 新格子中物品 |
| ITEM_STACK | previousItemStack | 无 | true | false | true | org.bukkit.inventory.ItemStack | true | 旧格子中物品 |

> 玩家改变手持某物品时触发

### Player Item Mend

- 事件类：`org.bukkit.event.player.PlayerItemMendEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITEM_STACK | item | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 物品 |
| STRING | slot | 无 | true | false | false | java.lang.String | true | 装备位置 |
| INT | repairAmount | 无 | true | false | false | java.lang.Integer | true | 修复值 |
| INT | experience | 无 | true | false | false | java.lang.Integer | true | 经验 |

> 当玩家通过装备上的经验修补附魔修复装备耐久时触发

### Player Join

- 事件类：`org.bukkit.event.player.PlayerJoinEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | joinMessage | 无 | true | false | false | java.lang.String | true | 进入信息 |

> 玩家进入服务器时触发

### Player Jump

- 事件类：`org.gitee.orryx.taboolib.platform.event.PlayerJumpEvent`；可取消：`false`

> 玩家跳跃

### Player Kick

- 事件类：`org.bukkit.event.player.PlayerKickEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | leaveMessage | 无 | true | false | false | java.lang.String | true | 退出信息 |
| STRING | reason | 无 | true | false | false | java.lang.String | true | 原因 |

> 玩家被踢出服务器时触发

### Player Level Change

- 事件类：`org.bukkit.event.player.PlayerLevelChangeEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| INT | newLevel | 无 | true | false | false | java.lang.Integer | true | 新等级 |
| INT | oldLevel | 无 | true | false | false | java.lang.Integer | true | 老等级 |

> 玩家等级改变时触发

### Player Move

- 事件类：`org.bukkit.event.player.PlayerMoveEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | to | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 到达的位置 |
| TARGET | from | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 来的位置 |

> 玩家移动触发（高频）

### Player Pickup Arrow

- 事件类：`org.bukkit.event.player.PlayerPickupArrowEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | arrow | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 箭矢实体 |
| TARGET | remaining | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 剩余掉落箭矢数量 |

> 当玩家从地上捡起箭时触发

### Player Pickup Item

- 事件类：`org.bukkit.event.entity.EntityPickupItemEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | item | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 掉落物实体 |
| ITEM_STACK | itemStack | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 掉落物 |
| INT | remaining | 无 | true | false | false | java.lang.Integer | true | 获得地面剩余掉落物品数量 |

> 当玩家从地上捡起掉落物时触发

### Player Portal

- 事件类：`org.bukkit.event.player.PlayerPortalEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| BOOLEAN | canCreatePortal | 无 | true | false | false | java.lang.Boolean | true | 返回服务器是否尝试创建目标传送门 |
| INT | searchRadius | 无 | true | false | false | java.lang.Integer | true | 获取查找可用门户的搜索半径值 |
| INT | creationRadius | 无 | true | false | false | java.lang.Integer | true | 获取从给定位置搜索世界可用空间的最大半径 |
| TARGET | from | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 来的位置 |
| TARGET | to | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 到的位置 |

> 玩家将要被传送门传送触发, 传送过程中会生成一个退出传送门

### Player Quit

- 事件类：`org.bukkit.event.player.PlayerQuitEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | quitMessage | 无 | true | false | false | java.lang.String | true | 退出信息 |

> 玩家退出服务器时触发

### Player Respawn

- 事件类：`org.bukkit.event.player.PlayerRespawnEvent`；可取消：`false`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | respawnLocation | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 重生位置 |
| BOOLEAN | isBedSpawn | 无 | true | false | false | java.lang.Boolean | true | 是否为床重生 |

> 当玩家重生时触发

### Player Swap Hand Items

- 事件类：`org.bukkit.event.player.PlayerSwapHandItemsEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ITEM_STACK | mainHandItem | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 主手物品 |
| ITEM_STACK | offHandItem | 无 | true | false | false | org.bukkit.inventory.ItemStack | true | 副手物品 |

> 当玩家切换主副手物品时触发

### Player Teleport

- 事件类：`org.bukkit.event.player.PlayerTeleportEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | to | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 目标位置 |
| TARGET | from | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 来源位置 |
| STRING | cause | 无 | true | false | false | java.lang.String | true | 传送原因 |

> 当玩家传送时触发

### Player Toggle Sneak

- 事件类：`org.bukkit.event.player.PlayerToggleSneakEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| BOOLEAN | isSneaking | 无 | true | false | false | java.lang.Boolean | true | 是否正在潜行 |

> 当玩家切换潜行状态时触发

### Player Toggle Sprint

- 事件类：`org.bukkit.event.player.PlayerToggleSprintEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| BOOLEAN | isSprinting | 无 | true | false | false | java.lang.Boolean | true | 是否正在疾跑 |

> 当玩家切换疾跑状态时触发

### Projectile Hit

- 事件类：`org.bukkit.event.entity.ProjectileHitEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | projectile | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 投射物实体 |
| TARGET | hitEntity | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 命中的实体 |
| TARGET | hitBlock | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 命中的方块位置 |

> 当投射物命中时触发（仅玩家发射）

### Projectile Launch

- 事件类：`org.bukkit.event.entity.ProjectileLaunchEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| TARGET | projectile | 无 | true | false | false | org.gitee.orryx.core.targets.ITarget | true | 投射物实体 |
| STRING | entityType | 无 | true | false | false | java.lang.String | true | 投射物类型 |

> 当玩家发射投射物时触发

## Orryx自身

### Orryx Clear All Skill Level

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxClearAllSkillLevelAndBackPointEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | job | 无 | true | false | false | java.lang.Object | true | 玩家职业 |

> 清除所有技能等级并返还点数事件

### Orryx Clear Skill Level

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxClearSkillLevelAndBackPointEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |

> 清除技能等级并返还点数事件

### Orryx Global Flag Change Post

- 事件类：`org.gitee.orryx.api.events.OrryxGlobalFlagChangeEvents$Post`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | key | flagName | true | false | false | java.lang.String | true | flag的键 |
| ANY | oldFlag | 无 | true | false | false | java.lang.Object | true | 旧的flag |
| ANY | newFlag | 无 | true | false | false | java.lang.Object | true | 新的flag |

> 全局Flag变化事件

### Orryx Global Flag Change Pre

- 事件类：`org.gitee.orryx.api.events.OrryxGlobalFlagChangeEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | key | flagName | true | false | false | java.lang.String | true | flag的键 |
| ANY | oldFlag | 无 | true | false | false | java.lang.Object | true | 旧的flag |
| ANY | newFlag | 无 | true | false | false | java.lang.Object | true | 新的flag |

> 全局Flag变化事件

### Orryx Player Change Group

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerChangeGroupEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | job | 无 | true | false | false | java.lang.Object | true | 玩家职业 |
| ANY | group | 无 | true | false | false | java.lang.Object | true | 技能组 |

> 玩家切换技能组事件

### Orryx Player Exp Down

- 事件类：`org.gitee.orryx.api.events.player.job.OrryxPlayerJobExperienceEvents$Down$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | exp | experience | true | false | false | java.lang.Double | true | 变化经验 |

> 玩家经验下降事件

### Orryx Player Exp Up

- 事件类：`org.gitee.orryx.api.events.player.job.OrryxPlayerJobExperienceEvents$Up$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | exp | experience | true | false | false | java.lang.Double | true | 变化经验 |

> 玩家经验上升事件

### Orryx Player Flag Change Post

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerFlagChangeEvents$Post`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | key | flagName | true | false | false | java.lang.String | true | flag的键 |
| ANY | oldFlag | 无 | true | false | false | java.lang.Object | true | 旧的flag |
| ANY | newFlag | 无 | true | false | false | java.lang.Object | true | 新的flag |

> 玩家Flag变化事件

### Orryx Player Flag Change Pre

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerFlagChangeEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | key | flagName | true | false | false | java.lang.String | true | flag的键 |
| ANY | oldFlag | 无 | true | false | false | java.lang.Object | true | 旧的flag |
| ANY | newFlag | 无 | true | false | false | java.lang.Object | true | 新的flag |

> 玩家Flag变化事件

### Orryx Player Job Change Post

- 事件类：`org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents$Post`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | from | old | true | false | false | java.lang.String | true | 老职业 |
| STRING | to | new | true | false | false | java.lang.String | true | 新职业 |

> 玩家职业改变后事件

### Orryx Player Job Change Pre

- 事件类：`org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | from | old | true | false | false | java.lang.String | true | 老职业 |
| STRING | to | new | true | false | false | java.lang.String | true | 新职业 |

> 玩家职业改变前事件

### Orryx Player Job Clear

- 事件类：`org.gitee.orryx.api.events.player.job.OrryxPlayerJobClearEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | job | 无 | true | false | false | java.lang.Object | true | 玩家职业 |

> 玩家职业清除事件

### Orryx Player Level Down

- 事件类：`org.gitee.orryx.api.events.player.job.OrryxPlayerJobLevelEvents$Down$Post`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | level | 无 | true | false | false | java.lang.Double | true | 变化等级 |

> 玩家等级下降事件

### Orryx Player Level Up

- 事件类：`org.gitee.orryx.api.events.player.job.OrryxPlayerJobLevelEvents$Up$Post`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | level | 无 | true | false | false | java.lang.Double | true | 变化等级 |

> 玩家等级上升事件

### Orryx Player Mana Down

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerManaEvents$Down$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | mana | 无 | true | true | false | java.lang.Double | true | 变化蓝量 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家蓝量下降事件

### Orryx Player Mana Heal

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerManaEvents$Heal$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | mana | 无 | true | true | false | java.lang.Double | true | 变化蓝量 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家蓝量指令回满事件

### Orryx Player Mana Regain

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerManaEvents$Regain$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | mana | 无 | true | true | false | java.lang.Double | true | 变化蓝量 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家蓝量自然恢复事件

### Orryx Player Mana Up

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerManaEvents$Up$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | mana | 无 | true | true | false | java.lang.Double | true | 变化蓝量 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家蓝量上升事件

### Orryx Player Point Down

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerPointEvents$Down$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | point | 无 | true | true | false | java.lang.Double | true | 变化技能点 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家技能点下降事件

### Orryx Player Point Up

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerPointEvents$Up$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | point | 无 | true | true | false | java.lang.Double | true | 变化技能点 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家技能点上升事件

### Orryx Player Press Start

- 事件类：`org.gitee.orryx.api.events.player.press.OrryxPlayerPressStartEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | skill | 无 | true | false | false | java.lang.String | true | 技能（空为普攻） |
| LONG | tick | 无 | true | false | false | java.lang.Long | true | 蓄力最长时间 |

> 玩家开始蓄力事件

### Orryx Player Press Stop

- 事件类：`org.gitee.orryx.api.events.player.press.OrryxPlayerPressStopEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | skill | 无 | true | false | false | java.lang.String | true | 技能（空为普攻） |
| LONG | tick | 无 | true | false | false | java.lang.Long | true | 已经蓄力时间 |
| LONG | maxTick | 无 | true | false | false | java.lang.Long | true | 蓄力最长时间 |

> 玩家停止蓄力事件

### Orryx Player Press Tick

- 事件类：`org.gitee.orryx.api.events.player.press.OrryxPlayerPressTickEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | skill | 无 | true | false | false | java.lang.String | true | 技能（空为普攻） |
| LONG | tick | 无 | true | false | false | java.lang.Long | true | 已经蓄力时间 |
| LONG | maxTick | 无 | true | false | false | java.lang.Long | true | 蓄力最长时间 |
| LONG | period | 无 | true | false | false | java.lang.Long | true | 周期时间 |

> 玩家开始蓄力后的周期性事件

### Orryx Player Profile Save

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerProfileSaveEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | profile | 无 | true | false | false | java.lang.Object | true | 玩家存档 |
| BOOLEAN | async | 无 | true | false | false | java.lang.Boolean | true | 是否异步 |
| BOOLEAN | remove | 无 | true | false | false | java.lang.Boolean | true | 是否移除 |

> 玩家存档保存事件

### Orryx Player Skill Bind Key

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillBindKeyEvent$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |
| ANY | group | 无 | true | false | false | java.lang.Object | true | 按键组 |
| ANY | bindKey | 无 | true | false | false | java.lang.Object | true | 绑定按键 |

> 玩家技能绑定按键事件

### Orryx Player Skill Cast

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCastEvents$Cast`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| SKILL | skill | 无 | true | false | false | org.gitee.orryx.core.skill.ISkill | false | 玩家技能 |
| SKILL_PARAMETER | skillParameter | 无 | true | false | false | org.gitee.orryx.core.skill.SkillParameter | false | 技能参数上下文 |

> 玩家技能释放事件

### Orryx Player Skill Check

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCastEvents$Check`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| SKILL | skill | 无 | true | false | false | org.gitee.orryx.core.skill.ISkill | false | 玩家技能 |
| SKILL_PARAMETER | skillParameter | 无 | true | false | false | org.gitee.orryx.core.skill.SkillParameter | false | 技能参数上下文 |

> 玩家技能释放前检查事件

### Orryx Player Skill Clear

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillClearEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |

> 玩家技能清除事件

### Orryx Player Skill Cooldown Increase

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCooldownEvents$Increase$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |
| LONG | amount | 无 | true | false | false | java.lang.Long | true | 增加的!毫秒!数值 |

> 玩家技能冷却增加事件

### Orryx Player Skill Cooldown Reduce

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCooldownEvents$Reduce$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |
| LONG | amount | 无 | true | false | false | java.lang.Long | true | 缩减的!毫秒!数值 |

> 玩家技能冷却缩减事件

### Orryx Player Skill Cooldown Set

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCooldownEvents$Set$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |
| LONG | amount | 无 | true | false | false | java.lang.Long | true | 设置的!毫秒!数值 |

> 玩家技能冷却设置事件

### Orryx Player Skill Level Down

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillLevelEvents$Down$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |
| INT | level | 无 | true | false | false | java.lang.Integer | true | 降级等级数 |

> 玩家技能降级事件

### Orryx Player Skill Level Up

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillLevelEvents$Up$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |
| INT | level | 无 | true | false | false | java.lang.Integer | true | 升级等级数 |

> 玩家技能升级事件

### Orryx Player Skill UnBind Key

- 事件类：`org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillUnBindKeyEvent$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| ANY | skill | 无 | true | false | false | java.lang.Object | true | 玩家技能 |
| ANY | group | 无 | true | false | false | java.lang.Object | true | 按键组 |

> 玩家技能解绑按键事件

### Orryx Player Spirit Down

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents$Down$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | spirit | 无 | true | true | false | java.lang.Double | true | 变化精力值 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家精力值下降事件

### Orryx Player Spirit Heal

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents$Heal$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | spirit | 无 | true | true | false | java.lang.Double | true | 变化精力值 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家精力值上升事件

### Orryx Player Spirit Regain

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents$Regain$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | spirit | 无 | true | true | false | java.lang.Double | true | 变化精力值 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家精力值上升事件

### Orryx Player Spirit Up

- 事件类：`org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents$Up$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| DOUBLE | spirit | 无 | true | true | false | java.lang.Double | true | 变化精力值 |
| PROFILE | profile | 无 | true | false | false | org.gitee.orryx.api.profile.IProfile | false | 玩家档案 |

> 玩家精力值上升事件

### Orryx Player State Skill

- 事件类：`org.gitee.orryx.api.events.player.state.OrryxPlayerStateSkillEvents$Pre`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| SKILL_PARAMETER | skillParameter | 无 | true | false | false | org.gitee.orryx.core.skill.SkillParameter | false | 技能参数上下文 |
| LONG | silence | 无 | true | true | false | java.lang.Long | true | 沉默时长 |
| STATE | state | 无 | true | false | false | org.gitee.orryx.module.state.IState | true | 技能状态 |

> 玩家状态技能触发事件

## DungeonPlus地牢

### Dungeon End

- 事件类：`org.gitee.orryx.api.events.compat.PlayerDungeonEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | dungeonName | 无 | true | false | false | java.lang.String | true | 副本名 |
| STRING | dungeonUUID | 无 | true | false | false | java.lang.String | true | 副本UUID |
| ITERABLE | params | 无 | true | false | false | java.lang.Iterable | true | 副本参数 |

> 副本结束时触发

### Dungeon Leave

- 事件类：`org.gitee.orryx.api.events.compat.PlayerDungeonEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | dungeonName | 无 | true | false | false | java.lang.String | true | 副本名 |
| STRING | dungeonUUID | 无 | true | false | false | java.lang.String | true | 副本UUID |
| ITERABLE | params | 无 | true | false | false | java.lang.Iterable | true | 副本参数 |

> 副本结束时触发

### Dungeon Start

- 事件类：`org.gitee.orryx.api.events.compat.PlayerDungeonEvent`；可取消：`true`

| 类型 | Key | Aliases | 可读 | 可写 | 可空 | Raw 类型 | Kether 可填充 | Value |
|------|-----|---------|------|------|------|----------|---------------|-------|
| STRING | dungeonName | 无 | true | false | false | java.lang.String | true | 副本名 |
| STRING | dungeonUUID | 无 | true | false | false | java.lang.String | true | 副本UUID |
| ITERABLE | params | 无 | true | false | false | java.lang.Iterable | true | 副本参数 |

> 副本启动时触发

# Property属性列表

## Container容器

### IContainer

> 用法: `&变量名[key]` | ID: `orryx.container.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| list | ANY | 否 | 目标列表 |
| length | INT | 否 | 容器中目标数量 |
| size | INT | 否 | 容器中目标数量（同length） |

> 目标容器对象，用于储存各类Target

## Cooldown冷却

### CooldownEntry

> 用法: `&变量名[key]` | ID: `orryx.cooldown.entry.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| tag | STRING | 否 | 冷却标签 |
| countdown | LONG | 否 | 剩余冷却时间(ms) |
| overStamp | LONG | 否 | 冷却结束时间戳 |
| isReady | BOOLEAN | 否 | 是否已就绪 |

> 冷却条目对象，包含冷却相关信息

## Game原版游戏

### Block

> 用法: `&变量名[key]` | ID: `orryx.block.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| type | STRING | 否 | 方块类型名 |
| x | INT | 否 | X 坐标 |
| y | INT | 否 | Y 坐标 |
| z | INT | 否 | Z 坐标 |
| world | STRING | 否 | 世界名 |
| location | ANY | 否 | 方块位置 (Location) |
| lightLevel | INT | 否 | 光照等级 |
| temperature | DOUBLE | 否 | 温度 |
| humidity | DOUBLE | 否 | 湿度 |
| isLiquid | BOOLEAN | 否 | 是否液体 |
| isSolid | BOOLEAN | 否 | 是否固体 |
| isEmpty | BOOLEAN | 否 | 是否为空 |
| isPassable | BOOLEAN | 否 | 是否可通过 |

> Bukkit Block 对象，包含方块的基础信息

### IEntity

> 用法: `&变量名[key]` | ID: `orryx.entity.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| uniqueId | STRING | 否 | 实体 UUID |
| entityId | INT | 否 | 实体数值 ID |
| name | STRING | 否 | 实体名称 |
| customName | STRING | 是 | 自定义名称 |
| type | STRING | 否 | 实体类型 |
| isDead | BOOLEAN | 否 | 是否死亡 |
| isValid | BOOLEAN | 否 | 是否有效 |
| world | STRING | 否 | 所在世界名 |
| width | DOUBLE | 否 | 实体宽度 |
| height | DOUBLE | 否 | 实体高度 |
| location | ANY | 否 | 实体位置 (Location) |
| eyeLocation | ANY | 否 | 视线位置 (Location) |
| velocity | VECTOR | 是 | 速度向量 |
| gravity | BOOLEAN | 否 | 是否受重力 |
| moveSpeed | DOUBLE | 否 | 移动速度 |
| isOnGround | BOOLEAN | 否 | 是否在地面 |
| isFrozen | BOOLEAN | 否 | 是否冻结 |
| isFired | BOOLEAN | 否 | 是否着火 |
| isInsideVehicle | BOOLEAN | 否 | 是否在载具内 |
| isSilent | BOOLEAN | 否 | 是否静默 |
| isCustomNameVisible | BOOLEAN | 否 | 自定义名称是否可见 |
| isGlowing | BOOLEAN | 否 | 是否发光 |
| isInWater | BOOLEAN | 否 | 是否在水中 |
| isInvulnerable | BOOLEAN | 否 | 是否无敌 |

> 实体适配器对象，包含实体的基础信息

### ItemTag

> 用法: `&变量名[key]` | ID: `orryx.player.itemtag.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| <任意键> | ANY | 是 | 读取/写入指定键的NBT数据 |

> 物品NBT标签对象，支持读写任意NBT键值

### Location

> 用法: `&变量名[key]` | ID: `orryx.location.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| x | DOUBLE | 是 | X 坐标 |
| y | DOUBLE | 是 | Y 坐标 |
| z | DOUBLE | 是 | Z 坐标 |
| yaw | FLOAT | 是 | 偏航角 |
| pitch | FLOAT | 是 | 俯仰角 |
| world | STRING | 否 | 世界名称 |
| blockX | INT | 否 | 方块 X 坐标 |
| blockY | INT | 否 | 方块 Y 坐标 |
| blockZ | INT | 否 | 方块 Z 坐标 |
| direction | VECTOR | 否 | 方向向量 |
| length | DOUBLE | 否 | 到原点距离 |

> Bukkit Location 对象，包含坐标、朝向和世界信息

## Hitbox碰撞箱

### IAABB

> 用法: `&变量名[key]` | ID: `orryx.aabb.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| halfExtents | VECTOR | 是 | 轴半长 |
| center | VECTOR | 是 | 中心点 |
| min | VECTOR | 否 | 最小点 |
| max | VECTOR | 否 | 最大点 |

> AABB 碰撞箱

### ICapsule

> 用法: `&变量名[key]` | ID: `orryx.capsule.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| height | DOUBLE | 是 | 高度 |
| radius | DOUBLE | 是 | 半径 |
| center | VECTOR | 是 | 中心点 |
| rotation | QUATERNION | 是 | 旋转四元数 |
| direction | VECTOR | 否 | 方向向量 |

> 胶囊体碰撞箱

### ICollider

> 用法: `&变量名[key]` | ID: `orryx.collider.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| type | STRING | 否 | 碰撞箱类型 |
| disable | BOOLEAN | 是 | 是否禁用 |

> 碰撞箱基础接口

### IComposite

> 用法: `&变量名[key]` | ID: `orryx.composite.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| count | INT | 否 | 子碰撞箱数量 |

> 复合碰撞箱

### IOBB

> 用法: `&变量名[key]` | ID: `orryx.obb.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| halfExtents | VECTOR | 是 | 轴半长 |
| center | VECTOR | 是 | 中心点 |
| rotation | QUATERNION | 是 | 旋转四元数 |

> OBB 碰撞箱

### IRay

> 用法: `&变量名[key]` | ID: `orryx.ray.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| length | DOUBLE | 是 | 长度 |
| direction | VECTOR | 是 | 方向 |
| origin | VECTOR | 否 | 起点 |
| end | VECTOR | 否 | 终点 |

> 射线碰撞箱

### ISphere

> 用法: `&变量名[key]` | ID: `orryx.sphere.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| radius | DOUBLE | 是 | 半径 |
| center | VECTOR | 是 | 中心点 |

> 球体碰撞箱

## Kether原生-属性

### Array

> 用法: `&变量名[key]` | ID: `array.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| <索引> | ANY | 是 | 按索引读写元素 |
| length | INT | 否 | 数组长度，别名 size |

> 数组属性操作，支持按数字索引读写元素

### ItemMeta

> 用法: `&变量名[key]` | ID: `itemMeta.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| name | STRING | 是 | 显示名称 |
| lore | ITERABLE | 是 | Lore 列表 |

> 物品Meta属性操作

### ItemStack

> 用法: `&变量名[key]` | ID: `item.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| type | STRING | 是 | 材质类型，别名 material |
| meta | ANY | 是 | ItemMeta 对象，别名 itemmeta |
| data | INT | 是 | 耐久/数据值，别名 damage/durability |
| name | STRING | 是 | 显示名称 |
| lore | ITERABLE | 是 | Lore 列表 |

> 物品堆属性操作

### List

> 用法: `&变量名[key]` | ID: `list.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| <索引> | ANY | 是 | 按索引读写元素 |
| length | INT | 否 | 列表长度，别名 size |

> 列表属性操作，支持按数字索引读写元素

### Map

> 用法: `&变量名[key]` | ID: `map.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| @<key> | ANY | 是 | 按键名读写值，写入 null 时删除该键 |
| length | INT | 否 | Map大小，别名 size |
| keys | ITERABLE | 否 | 所有键的集合 |
| values | ITERABLE | 否 | 所有值的集合 |

> Map属性操作，支持按 @key 读写键值对

### Matcher

> 用法: `&变量名[key]` | ID: `matcher.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| <数字> | STRING | 否 | 按索引获取捕获组 |
| <名称> | STRING | 否 | 按名称获取命名捕获组 |

> 正则匹配结果属性操作

### String

> 用法: `&变量名[key]` | ID: `string.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| upper | STRING | 否 | 转大写，别名 uppercase |
| lower | STRING | 否 | 转小写，别名 lowercase |
| length | INT | 否 | 字符串长度，别名 size |
| trim | STRING | 否 | 去除首尾空白 |

> 字符串属性操作

## KeySetting按键设置

### IBindKey

> 用法: `&变量名[key]` | ID: `bindKey.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| key | STRING | 否 | 按键绑定的键名 |
| name | STRING | 否 | 同 key |
| sort | INT | 否 | 排序权重 |

> 按键绑定对象

## Math数学运算

### IVector

> 用法: `&变量名[key]` | ID: `vector.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| x | DOUBLE | 是 | X 分量 |
| y | DOUBLE | 是 | Y 分量 |
| z | DOUBLE | 是 | Z 分量 |

> 可变向量对象

### Quaterniond

> 用法: `&变量名[key]` | ID: `orryx.quaternion.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| x | DOUBLE | 是 | X 分量 |
| y | DOUBLE | 是 | Y 分量 |
| z | DOUBLE | 是 | Z 分量 |
| w | DOUBLE | 是 | W 分量 |

> JOML 四元数对象

### Vector3dc

> 用法: `&变量名[key]` | ID: `Vector3dc.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| x | DOUBLE | 否 | X 分量 |
| y | DOUBLE | 否 | Y 分量 |
| z | DOUBLE | 否 | Z 分量 |
| size | DOUBLE | 否 | 向量长度 |
| length | DOUBLE | 否 | 向量长度（同size） |

> JOML 只读向量对象

## Orryx核心

### IExperience

> 用法: `&变量名[key]` | ID: `orryx.experience.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| key | STRING | 否 | 经验计算器键名 |
| minLevel | INT | 否 | 最低等级 |
| maxLevel | INT | 否 | 最高等级 |
| experienceEquation | STRING | 否 | 经验算法表达式 |

> 经验计算器对象，包含经验配置信息

### IJob

> 用法: `&变量名[key]` | ID: `orryx.job.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| key | STRING | 否 | 职业键名 |
| name | STRING | 否 | 职业显示名 |
| experience | ANY | 否 | 经验配置 |
| attributes | ANY | 否 | 属性配置 |
| skills | ANY | 否 | 技能列表 |

> 职业配置对象，包含职业的配置信息

### IPlayerJob

> 用法: `&变量名[key]` | ID: `orryx.player.job.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| key | STRING | 否 | 职业键名 |
| name | STRING | 否 | 职业显示名 |
| config | ANY | 否 | 职业配置对象 (IJob) |
| player | PLAYER | 否 | 玩家对象 |
| level | INT | 否 | 当前等级 |
| maxLevel | INT | 否 | 最大等级 |
| experienceOfLevel | LONG | 否 | 当前等级经验 |
| maxExperienceOfLevel | LONG | 否 | 当前等级最大经验 |
| experience | LONG | 否 | 总经验 |
| binds | ANY | 否 | 按键绑定映射 |
| maxMana | DOUBLE | 否 | 最大法力值 |
| regainMana | DOUBLE | 否 | 法力恢复值 |
| maxSpirit | DOUBLE | 否 | 最大精力值 |
| regainSpirit | DOUBLE | 否 | 精力恢复值 |
| attributes | ANY | 否 | 属性列表 |
| spirit | DOUBLE | 否 | 当前精力值 |

> 玩家职业对象，包含职业相关的所有信息

### IPlayerProfile

> 用法: `&变量名[key]` | ID: `orryx.player.profile.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| point | INT | 否 | 技能点数 |
| job | STRING | 否 | 当前职业键名 |
| player | PLAYER | 否 | 玩家对象 |
| flags | ANY | 否 | 标记映射 |

> 玩家档案对象，包含玩家的基础信息

### IPlayerSkill

> 用法: `&变量名[key]` | ID: `orryx.player.skill.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| key | STRING | 否 | 技能键名 |
| job | STRING | 否 | 所属职业键名 |
| player | PLAYER | 否 | 玩家对象 |
| level | INT | 否 | 技能等级 |
| config | ANY | 否 | 技能配置对象 (ISkill) |
| locked | BOOLEAN | 否 | 是否锁定 |

> 玩家技能对象，包含技能相关的所有信息

### ISkill

> 用法: `&变量名[key]` | ID: `orryx.skill.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| key | STRING | 否 | 技能键名 |
| name | STRING | 否 | 技能显示名 |
| type | STRING | 否 | 技能类型 |
| minLevel | INT | 否 | 最小等级 |
| maxLevel | INT | 否 | 最大等级 |
| icon | STRING | 否 | 图标 |
| locked | BOOLEAN | 否 | 是否需要解锁 |
| sort | INT | 否 | 排序权重 |
| material | STRING | 否 | 材质名 |

> 技能配置对象，包含技能的配置信息

## State状态机

### IRunningState

> 用法: `&变量名[key]` | ID: `orryx.running.state.operator`

| 属性Key | 类型 | 可写 | 描述 |
|---------|------|------|------|
| key | STRING | 否 | 状态键名 |
| stop | BOOLEAN | 否 | 是否已停止 |

> 运行中的状态对象

