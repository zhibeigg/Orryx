# Ability component contract

## Request

launcher 固定注入 `component: ability`。`request` 应包含技能 `id`、`type`、显示信息、等级范围、描述规格、变量/数值规格、行为、目标 Minecraft 版本和插件依赖。Passive 的事件效果应提供事件名与效果，以便生成 Station scaffold。

## Type rules

- Passive：只生成技能元数据；事件效果另建 Station。
- Direct：立即 Actions。
- DirectAim：1.12.2 客户端瞄准后 Actions。
- Pressing：Period、PressPeriodAction、MaxPressTickAction 与释放 Actions。
- PressingAim：Pressing 与 Aim 字段组合，仍限 1.12.2。

## Consistency

每个 Description 动态值必须映射到 Variables、等级/蓄力上下文或 Actions；Actions 引用必须有来源。生成 artifact 路径为 `skills/<id>.yml`，必要的 Station 为 `stations/<station-id>.yml`。结果列出插件、版本和跨文件 requirements。
