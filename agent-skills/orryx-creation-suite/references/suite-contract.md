# 套件统一契约

## 请求

所有组件接受 UTF-8 JSON：

```json
{
  "contractVersion": "1.0",
  "component": "ability",
  "operation": "generate",
  "workspace": {
    "root": "plugins/Orryx",
    "mode": "standalone"
  },
  "request": {},
  "policy": {
    "strict": true,
    "network": "deny",
    "overwrite": "deny",
    "minecraftVersion": "1.20.4",
    "plugins": []
  }
}
```

### operation

- `generate`：计算 artifact，不写入真实配置。
- `validate`：验证工作区或请求。
- `plan`：只规划并返回候选/诊断，不写盘。
- `materialize`：仅通过显式写盘边界执行。

### workspace.mode

- `standalone` 与 `project` 都会保留在合同中。
- 当前 Runtime 尚不根据 mode 改变必需引用严重度；两者缺失 required reference 都会返回 error。

## 结果

```json
{
  "contractVersion": "1.0",
  "suiteVersion": "1.0.0",
  "component": "ability",
  "operation": "generate",
  "status": "ok",
  "summary": {},
  "artifacts": [],
  "diagnostics": [],
  "checks": [],
  "references": [],
  "requirements": [],
  "nextSteps": [],
  "metadata": {},
  "provenance": {}
}
```

### artifact

每个 artifact 至少包含：

- `path`：相对 Orryx 数据目录的 POSIX 路径。
- `kind` 与 `mediaType`。
- `encoding`：固定 `utf-8`。
- `content`。
- `sha256`。
- `metadata`。

### diagnostics

诊断按严重度、错误码、路径和字段指针稳定排序：

```json
{
  "severity": "error",
  "code": "ORRYX-REF-SKILL-NOT-FOUND",
  "path": "jobs/剑修.yml",
  "pointer": "Options.Skills[2]",
  "message": "职业引用的技能不存在",
  "suggestion": "创建同名技能文件或修正技能 ID"
}
```

## 确定性

相同请求、相同 Schema 和相同套件版本必须得到相同 artifact 内容与 SHA-256。Pipeline 结果不得包含当前时间、随机 UUID、临时目录或主机绝对路径。

## 写入边界

Pipeline 与 materialize 分离：

1. Pipeline 只返回内容。
2. Materialize 校验目标位于指定 root 内。
3. 默认拒绝覆盖。
4. 使用临时文件后原子替换。
5. 不触发 Orryx reload。
