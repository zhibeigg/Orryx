# Job component contract

## Request

launcher 固定注入 `component: job`。`request` 应包含职业 `id`、Name/Icon、技能 ID 数组、属性、法力/精力/升级点动作和经验 ID。project 模式可通过请求文件清单或索引校验引用。

二转请求可包含 `advancementFrom`，但该字段只用于生成说明和 requirement，不写为 Orryx 原生配置键。

## Result

主 artifact 为 `jobs/<id>.yml`。缺失技能或经验引用进入 diagnostics。显示名与 ID 不同是合法情况，不能自动重命名引用。

若请求二转：

- 生成独立 `jobs/<id>.yml` scaffold；
- requirements 说明需要外部转职流程；
- 不包含 `ParentJob`；
- 不宣称自动继承等级、经验、技能或资源。
