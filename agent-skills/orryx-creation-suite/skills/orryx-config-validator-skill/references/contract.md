# Validator component contract

## Request

统一请求字段见 suite `references/suite-contract.md`。launcher 无条件注入：

```json
{"component": "validator"}
```

推荐 `operation: validate`；`plan` 仍保持只读。当前 validator 从 `workspace.root` 扫描磁盘 YAML，不消费内存 `request.files`。`workspace.mode` 为 `standalone` 或 `project`，但暂不改变诊断严重度。

## Checks

- 同一配置域的重复 basename。
- 职业 `Options.Skills`、`Options.Experience` 引用和职业内 Skill Sort 冲突。
- Status→Controller、Controller 动画声明与 `running` 状态目标。
- Station Priority、`Async` 键大小写/类型、Event、Actions 与异步线程风险。
- 明文秘密键、私钥标记和带凭据 URL。

## Result

结果遵循 suite envelope。validator 通常不返回 artifacts。配置错误使用 `status: invalid` 和 diagnostics 表达；成功执行的无效配置不是 launcher 失败。诊断按 severity、code、path、pointer 稳定排序。
