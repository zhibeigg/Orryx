---
name: orryx-config-validator-skill
description: >-
  Validate Orryx configuration packs without writing or reloading them. Use for basename-derived IDs, cross-file skill/job/experience/status/station references, skill Sort ordering, DirectAim and PressingAim Minecraft 1.12.2 compatibility, and reload-safety review. Produces deterministic diagnostics through the shared orryx_toolkit validator component.
license: MIT
activation:
  command: /orryx-config-validator-skill
  intents:
    - validate Orryx configuration
    - audit Orryx cross references
    - check Orryx reload safety
metadata:
  author: NarraFork
  version: 1.0.0
  created: 2025-07-13
  last_reviewed: 2025-07-13
  review_interval_days: 90
provenance:
  repository: Orryx
  evidence: references/source-evidence.md
  contract: references/contract.md
---
# /orryx-config-validator-skill

验证 Orryx 配置包，返回可机器消费的诊断，不修改配置、不触发 reload。

## 使用时机

- 检查 `skills/`、`jobs/`、`experiences/`、`status/`、`stations/` 的文件与引用。
- 发布或重载前审计错误、警告和排序冲突。
- 判断指向性技能是否符合服务器版本约束。

## 核心事实

1. 技能、职业、经验、Status 与 Station 的运行时 ID 来自文件 basename，不是显示名。
2. `Options.Name` 是显示名；引用必须使用文件 ID。
3. `Options.Sort` 默认 `0`，用于技能 UI 顺序；重复值应提示歧义。
4. `DirectAim` 与 `PressingAim` 通过客户端瞄准消息实现，当前实现仅支持 Minecraft `1.12.2`。
5. 多个 reload 管理器先 `clear()` 再逐文件加载，不具备跨文件事务；一次坏配置可能留下部分加载状态。
6. 项目模式中缺失引用是 error；独立模式中未随请求提供的外部引用可降为 warning。

## 工作流

1. 读取 `assets/request.example.json` 或同形请求。
2. 建立 basename ID 索引，保留相对路径。
3. 检查职业技能、经验曲线及 Status/Station 相关引用。
4. 检查技能类型、`Sort` 和版本兼容性。
5. 按 severity、code、path、pointer 稳定排序诊断。
6. 只输出 JSON 结果；即使配置无效也输出 `status: invalid` 并正常退出。

## 运行

```powershell
py -3 scripts/run_pipeline.py --input assets/request.example.json --output result.json
```

## 输出解释

- `status: ok`：没有阻止使用的错误。
- `status: invalid`：存在错误；查看 `diagnostics`。
- `artifacts` 通常为空，因为 validator 不生成配置文件。
- `provenance.inputDigest` 可用于确认同一输入的确定性结果。

## 安全边界

- 不写入真实工作区。
- 不尝试 reload 来“验证”配置。
- 不把显示名当作 ID 猜测修复。
- 不隐瞒 Aim 版本限制或清空后加载的非事务风险。
