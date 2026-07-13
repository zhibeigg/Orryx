# Source evidence

- `src/main/resources/experiences/default.yml`：Min 0、Max 20，`ExperienceOfLevel: calc "200*level"`。
- `example/Orryx/jobs/剑修.yml`：`RegainManaActions: 1`、`MaxManaActions: calc "100+10*level"`、`RegainSpiritActions: 5`、`MaxSpiritActions: 100`、`UpgradePointActions: 1`、`Experience: default`。
- `src/main/kotlin/org/gitee/orryx/module/experience/ExperienceLoader.kt`：Min 必须小于 Max；范围外返回 0；范围内把 `level` 注入 KetherShell 并转 Int。
- `src/main/kotlin/org/gitee/orryx/utils/Experience.kt`：累计经验按 `minLevel until level` 逐级求和；区间经验按 from until to 求和。
- `src/main/kotlin/org/gitee/orryx/core/job/JobLoader.kt`：职业分别保存 max/regain mana、max/regain spirit、upgrade point 与 experience ID。
- `src/main/kotlin/org/gitee/orryx/core/job/PlayerJob.kt`：当前级最大经验直接调用 Experience 的逐级计算。
- `agent-skills/orryx-creation-suite/references/suite-contract.md`：artifact 要求 path、mediaType、utf-8、content、sha256，并要求相同请求产生确定内容。

line-chart 是本套件的可审计 artifact 协议，用于表达上述逐级模拟结果；它不是 Orryx 运行时原生配置字段。
