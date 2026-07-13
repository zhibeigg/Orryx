# Orryx Creation Suite 工作流

## 1. 通用生产流程

无论使用哪个组件，都应遵循以下边界：

1. **扫描**：只读检查目标 workspace、现有 basename、引用和敏感值。
2. **分配 ID**：把文件 basename 与展示名、图标、动画 ID、Controller ID 分开管理。
3. **计划**：使用原生 `operation=plan` 只计算候选与诊断，不写盘。
4. **生成**：获取候选 `artifacts`，不写真实配置。
5. **检查**：处理 diagnostics、checks、references 和 requirements。
6. **预览**：审核候选 path、content、sha256、外部资源和覆盖风险。
7. **staging materialize**：显式写入隔离 staging root，默认拒绝覆盖。
8. **磁盘验证**：让 validator 读取 staging；必要时在临时服务器执行 Orryx 加载预检。
9. **受控发布**：再次显式 materialize 到目标 root，准备外部备份或回滚方案。
10. **运行时动作**：reload/restart 不属于本套件，必须由运维流程单独决定。

当前 validator 读取磁盘文件，不能直接把内存 artifacts 当作完整工作区扫描。因此“完整 YAML 工作区验证”需要先写 staging，或由宿主提供额外的虚拟文件系统/内存 validator；后者当前未实现。

## 2. 跨组件依赖顺序

推荐执行顺序：

```text
validator
  → kether
  → ability
  → progression
  → job
  → station
  → combat
  → selector
  → ui
  → orchestrator 合并检查
  → 可选 materialize 安全门
```

理由：

- validator 先发现 ID 冲突和现有依赖。
- Kether 是多个领域的共同质量门；领域 runner 内也会重复执行轻量检查。
- progression 和 ability 提供 Job 常引用的 Experience/Skill。
- Station 可能引用被动技能。
- Combat 必须把 Status 与 Controller 作为一组。
- selector 常被技能脚本使用，但当前 Runtime 不解析 Kether 中的 selector ID，所以需要人工或外部引用图补充。
- Job 汇总技能与经验引用。
- UI 通常在技能 ID 稳定后生成；其技能引用当前标为 optional。

这是 Runtime 的固定依赖顺序。orchestrator 会先按该顺序重排 `request.steps`（同组件内保持输入相对顺序），再执行并对结果数组稳定排序。

## 3. 十个组件工作流

### 3.1 Validator 工作流

**输入**：`component=validator`、`operation=validate`、存在的 workspace root。

**步骤**：

1. 扫描 `skills/`、`jobs/`、`experiences/`、`stations/`、`status/`、`controllers/`、`ui/`、`selectors.yml`、`buffs.yml`。
2. 检查同一配置域重复 basename。
3. 检查 Job 的 Skill/Experience 引用和职业内 Skill Sort 冲突。
4. 检查 Status 的 Controller、部分动画声明和 `running` 状态目标。
5. 检查 Station Priority、`Async` 大小写/类型、Event 和 Actions。
6. 扫描已编码的敏感键、私钥标记和带凭据 URL。

**限制**：不验证所有 Orryx 配置域，不证明 Trigger、Kether action、第三方插件或客户端资产存在，也不启动服务器。

### 3.2 Kether 工作流

**输入**：`script/actions/scripts` 至少一个，建议提供 `actionsSchema` 或 `actionsSchemaPath`，并声明 context/variables。

**步骤**：

1. 加载请求内 schema；否则依次查找工作区 schema、构建产物和随 Runtime 打包的源码生成 `actions-schema.json`。
2. 检查括号和引号平衡。
3. 扫描候选 action token。
4. 根据 schema 提示未知 action。
5. 根据 context 提示未声明隐式变量。
6. 在异步 context 中提示主线程敏感 action。
7. 检查 Aim 与 Minecraft 版本。

**限制**：这是正则和结构驱动的静态检查，不是 Kether parser/compiler。没有 actions schema 时只能给出有限结论，必须保留 `KETHER_SCHEMA_MISSING`。

### 3.3 Ability 工作流

**输入**：安全 key，五种技能类型之一；非 PASSIVE 必须提供 actions。

**步骤**：

1. 区分文件 ID、`Options.Name`、`Icon` 和 Sort。
2. 生成 PASSIVE、DIRECT、DIRECT AIM、PRESSING 或 PRESSING AIM 字段。
3. 对 Actions/ExtendActions 调用 Kether 检查。
4. 对 Aim 请求检查 Minecraft 版本；当前只支持 `1.12.2`。
5. 对 PASSIVE 检查 Station 意图；可由 `request.station` 一并生成 Station 候选。
6. 审核 `skills/<key>.yml` 候选。

**限制**：PASSIVE 元数据不等于机制完成；PRESSING AIM 的部分 Press 字段当前 Caster 不消费；伤害、声音、模型、动画等脚本语义需外部 schema/资产证明。

### 3.4 Progression 工作流

**输入**：key、`minLevel < maxLevel`、exponential/polynomial/piecewise/table 曲线。

**步骤**：

1. 使用 `Decimal` 逐级计算经验。
2. 按 floor/ceil/round 取整。
3. 检查非正值、单级或累计 Int 溢出。
4. 尝试静态模拟法力、精力和技能点公式。
5. 生成 `experiences/<key>.yml`。
6. 生成 `reports/progression/<key>.json` 的数据和图表描述。

**限制**：资源公式只支持数字或可安全解析的有限 `level` 算术；无法求值时给 warning。二转不是 Experience 原生语义，只能形成 Job/Station requirement。

### 3.5 Job 工作流

**输入**：职业 key、Skill ID 列表、Experience ID，以及资源公式。

**步骤**：

1. 生成 `jobs/<key>.yml`。
2. 为每个技能建立 required reference。
3. 为 Experience 建立 required reference。
4. 检查五类资源公式的 Kether 结构。
5. 若声明 advancement，生成 `plans/jobs/<key>-advancement.json`。
6. 在完整 bundle 中确认所有引用目标存在。

**限制**：Orryx JobLoader 没有原生 ParentJob、继承或绑定迁移字段；advancement artifact 只是实施计划。单独预览 Job 时可临时关闭引用检查，但生产发布前必须在 bundle/workspace 中重新开启。

### 3.6 Station 工作流

**输入**：key、非空 event、actions、合法 Priority，明确 async。

**步骤**：

1. 生成 `stations/<key>.yml`。
2. 对 Priority 和 Async 类型做结构检查。
3. 对 Actions/BaffleAction 做 Kether 静态检查。
4. 若声明 skill/passiveSkill，建立 required reference。
5. async=true 时返回线程安全 requirement。

**限制**：Runtime 不查询 Trigger 注册表，所以 event 非空不表示事件真实存在。异步 Station 中的 Bukkit、世界、实体、玩家背包和客户端插件 API 仍需同步边界；数据库/Redis I/O 则应由外部实现异步化。

### 3.7 Combat 工作流

**输入**：`request.status` 与 `request.controller` 两个 object。

**步骤**：

1. 为 Status 与 Controller 分配独立 key。
2. 检查 state Type。
3. 检查 Connection/Check/Invincible tick 区间。
4. 生成 `status/<key>.yml` 和 `controllers/<key>.yml`。
5. 建立 Status→Controller required reference。
6. 收集 Condition、Action、BlockAction 等 Kether 脚本进行检查。
7. 非 bukkit backend 返回外部插件/客户端 requirement。

**限制**：自动派生的 Controller 只是最小层结构；Runtime 不证明 DragonCore/GermPlugin/其他客户端动画和 Trigger 真正存在。Status 与 Controller 必须一起评审和发布。

### 3.8 Selector 工作流

**输入**：非空 selectors map，值为脚本字符串或含 Actions 的 object。

**步骤**：

1. 验证 selector key 不含路径分隔符。
2. 生成完整 `selectors.yml` 候选。
3. 为 `v0...vN` 声明有限的隐式变量。
4. 对每个 Actions 执行 Kether 静态检查。
5. 返回调用方必须按顺序提供参数的 requirement。

**限制**：该组件输出整份单文件；多个 selector step 会产生重复 artifact 冲突。对已有 `selectors.yml` 的合并不是 Runtime 能力，调用方必须先读取并显式合并请求。几何语义和参数单位需由调用约定确认。

### 3.9 UI 工作流

**输入**：backend 为 bukkit/dragoncore/germplugin/arcartx；提供 config 或 files。

**步骤**：

1. 规范化 files 的相对路径并限制不能逃逸。
2. 生成 `ui/<backend>/*.yml` 候选。
3. 对 request.skills 建立 optional reference。
4. 非 bukkit backend 返回插件与客户端资源 requirement。

**限制**：`request.assets` 当前不会生成图片、模型或客户端文件，也不会自动成为 artifact。占位符、图标、布局字段和客户端协议必须以实际兼容插件版本为前置条件。

### 3.10 Orchestrator 工作流

**输入**：非空 `request.steps`，每步使用九个领域 component；受控发布时可追加 `materialize`。

**步骤**：

1. Runtime 验证 step 结构、组件和 operation。
2. 按 `validator → kether → ability → progression → job → station → combat → selector → ui → materialize` 固定顺序重排。
3. 合并五类共享数组。
4. 检查重复 artifact path。
5. 检查 required reference 是否由 bundle artifact 或 workspace 文件满足。
6. 根据 error diagnostics 计算最终 status。
7. 默认返回候选结果；不含 materialize step 时绝不写盘。
8. materialize step 会先预检累计候选；仅在顶层 `operation=materialize` 且前序零 error 时执行。step 未传 artifacts 时自动承接累计候选，否则跳过写盘。

**限制**：不递归、不提供跨文件事务，也不把后续 artifact 注入早先 validator 的磁盘扫描。需要完整 staging 验证时，必须执行独立的“写 staging → validator”阶段。

## 4. Golden 与 Eval 机制

每个组件技能当前包含：

- 一个 `evals/<skill>.eval.md`；
- 至少三个 `evals/golden/case-*/input.json`；
- 通用 `scripts/run_evals.py`。

Eval runner 读取 eval Markdown 中第一个 fenced `json` block。核心字段是：

- `skill`：技能名；
- `run`：可选命令模板，至少包含 `{output}`，通常也包含 `{input}`；
- `criteria`：`command` 或 `llm-judge`；
- `golden`：至少三个 case，指向 input 和可选 expected。

### 4.1 模式

- `--validate`：只检查 eval spec 形状、case 数量和文件是否存在，不运行组件。
- 默认模式：把每个 case 的 expected 文件绑定到 command criterion 的 `{output}` 后执行检查。
- `--output ... --case ...`：用外部实际输出执行 command criteria。
- `--rollout`：在临时目录执行 `run` 命令，为每个 golden input 生成真实输出，再执行 command criteria。
- `--promote`：仅在 pending-first-green case 的 rollout 和 command criteria 通过后，把输出复制为 conventional expected 文件。
- `llm-judge`：runner 只列出，不自动评分。

### 4.2 当前限制

- 现有组件 case 普遍使用 `expected=null` 与 `pending-first-green`，所以默认基线模式中需要 `{output}` 的检查会被跳过。
- runner 不自动做 expected 与 produced 的字节或语义 diff；是否比较内容完全取决于 criterion 命令。
- `--promote` 会写 expected 文件，但不会自动修改 eval spec 的 `expected` 字段。
- 当所有 case 都是 pending-first-green 时，第一次 rollout 主要证明“能运行且满足命令断言”，不是已经建立稳定 snapshot。
- 套件根目录当前没有可被 runner 发现的 `evals/*.eval.md`，因此顶层 `scripts/run_evals.py` 目前没有套件级 spec；组件级 runner 才是现有可执行入口。

### 4.3 建议生产门禁

1. Python/JSON Schema 验证输入、manifest 和输出形状。
2. 对每个组件执行 `--validate`。
3. 对固定 fixture 执行 `--rollout`。
4. 为关键 artifact 增加显式 SHA-256、路径、diagnostic code 和内容语义断言。
5. 建立 expected 后，在 eval spec 中显式引用并增加真正的 snapshot/diff criterion。
6. 把 `llm-judge` 只用于难以机械判定的文档质量、玩法一致性和需求覆盖，不代替结构与安全检查。
7. materialize 测试只写临时 staging root，不触碰生产目录，不执行 reload。
