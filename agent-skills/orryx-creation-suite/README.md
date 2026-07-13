# Orryx Creation Suite

面向 Orryx Minecraft 技能插件的跨平台 Agent SKILL 套件。套件根据 Orryx 源码约束和生产配置模式生成、审查和组合技能、职业、经验、中转站、战斗状态机、选择器及 UI 配置。

## 组件

1. `orryx-config-validator-skill`
2. `orryx-kether-authoring-skill`
3. `orryx-ability-authoring-skill`
4. `orryx-progression-curve-skill`
5. `orryx-job-kit-skill`
6. `orryx-station-mechanic-skill`
7. `orryx-combat-state-controller-skill`
8. `orryx-workflow-orchestrator-skill`
9. `orryx-selector-library-skill`
10. `orryx-ui-adapter-skill`

## 依赖

- Python 3.10+
- PyYAML 6.x

```bash
py -3 -m pip install -r requirements.txt
```

Linux/macOS：

```bash
python3 -m pip install -r requirements.txt
```

## 安装

### Claude Code 用户级

```bash
./install.sh --platform claude-code
```

Windows PowerShell：

```powershell
./install.ps1 -Platform claude-code
```

### Codex / Universal

```bash
./install.sh --platform universal
```

### VS Code Copilot 项目级

```bash
./install.sh --platform copilot --project
```

### 自定义目录

```bash
./install.sh --path /path/to/skills
```

安装器默认拒绝覆盖已有目录。只有目标带有本套件安装标记时，才可以使用 `--force` 更新。

## 使用

打开新的 Agent 会话后调用：

```text
/orryx-ability-authoring-skill 创建一个 1-7 级的火焰冲刺技能
/orryx-progression-curve-skill 设计 1-60 级先快后慢的经验曲线
/orryx-job-kit-skill 给现有职业添加两个技能并检查排序
/orryx-workflow-orchestrator-skill 创建完整的雷系长枪职业
```

## CLI

只生成结果，不写文件：

```bash
py -3 scripts/run_pipeline.py --input request.json --output result.json
```

显式写入 staging（默认拒绝覆盖，输入合同的 `request.artifacts` 承接生成结果）：

```bash
py -3 scripts/materialize.py --input materialize-request.json --output materialize-result.json
```

验证套件、技能规范、源码生成 Action Schema 与安全边界：

```bash
py -3 scripts/build_action_schema.py --check
py -3 scripts/validate_suite.py
py -3 scripts/validate_skills.py
py -3 scripts/security_scan.py
```

执行单测：

```bash
py -3 -m unittest discover -s tests -p "test_*.py"
```

## 安全模型

- YAML 使用安全加载器。
- 默认禁止网络和覆盖。
- Artifact 只能写入显式指定根目录。
- 写入采用临时文件和原子替换。
- 不自动执行 Orryx reload。
- 不把生产配置中的 Token、密码或数据库凭据复制到模板。

## Eval

每个组件均自带 Eval 契约和三个 golden 输入。验证示例：

```bash
py -3 skills/orryx-ability-authoring-skill/scripts/run_evals.py --validate
py -3 skills/orryx-ability-authoring-skill/scripts/run_evals.py --rollout
```

套件级评测：

```bash
py -3 scripts/run_evals.py --validate
py -3 scripts/run_evals.py --rollout
```
