"""离线验证共享 Runtime、合同、模板与确定性冒烟结果。"""
from __future__ import annotations

import ast
import importlib
import json
import sys
from pathlib import Path

SUITE_ROOT = Path(__file__).resolve().parents[1]
SHARED_ROOT = SUITE_ROOT / "shared"
if str(SHARED_ROOT) not in sys.path:
    sys.path.insert(0, str(SHARED_ROOT))

MODULES = (
    "contracts", "yaml_io", "workspace", "validation", "kether", "ability",
    "progression", "job", "station", "combat", "selector", "ui", "orchestrator",
    "materialize", "service_runner", "cli", "__init__", "__main__",
)
REQUIRED = (
    SUITE_ROOT / "requirements.txt",
    SUITE_ROOT / "scripts" / "run_pipeline.py",
    SUITE_ROOT / "scripts" / "materialize.py",
    SUITE_ROOT / "scripts" / "run_evals.py",
    SUITE_ROOT / "scripts" / "validate_skills.py",
    SUITE_ROOT / "scripts" / "security_scan.py",
    SUITE_ROOT / "scripts" / "build_action_schema.py",
    SUITE_ROOT / "tests" / "test_toolkit.py",
    SUITE_ROOT / "assets" / "contracts" / "component-input.schema.json",
    SUITE_ROOT / "assets" / "contracts" / "component-output.schema.json",
    SUITE_ROOT / "assets" / "contracts" / "service-runner-envelope.schema.json",
    SUITE_ROOT / "assets" / "contracts" / "orchestrator-manifest.schema.json",
    SUITE_ROOT / "assets" / "contracts" / "orchestrator-manifest.json",
    SUITE_ROOT / "assets" / "contracts" / "actions-schema.json",
    SUITE_ROOT / "shared" / "orryx_toolkit" / "actions-schema.json",
    SUITE_ROOT / "docs" / "architecture.md",
    SUITE_ROOT / "docs" / "contract.md",
    SUITE_ROOT / "docs" / "workflows.md",
    *(SUITE_ROOT / "assets" / "templates" / f"{name}.input.json" for name in (
        "ability", "job", "progression", "kether", "validator", "station", "combat", "selector", "ui", "orchestrator"
    )),
)


def validate() -> dict[str, object]:
    diagnostics: list[dict[str, str]] = []
    checks: list[dict[str, str]] = []
    for path in REQUIRED:
        if path.is_file():
            checks.append({"code": "FILE_PRESENT", "status": "pass", "path": path.relative_to(SUITE_ROOT).as_posix()})
        else:
            diagnostics.append({"severity": "error", "code": "FILE_MISSING", "path": path.relative_to(SUITE_ROOT).as_posix(), "message": "缺少必需文件"})
    for name in MODULES:
        path = SHARED_ROOT / "orryx_toolkit" / f"{name}.py"
        if not path.is_file():
            diagnostics.append({"severity": "error", "code": "MODULE_MISSING", "path": path.relative_to(SUITE_ROOT).as_posix(), "message": "缺少 Runtime 模块"})
            continue
        try:
            ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
            if name not in {"__main__"}:
                importlib.import_module(f"orryx_toolkit.{name}")
            checks.append({"code": "MODULE_IMPORTABLE", "status": "pass", "path": path.relative_to(SUITE_ROOT).as_posix()})
        except (SyntaxError, ImportError, ValueError) as exc:
            diagnostics.append({"severity": "error", "code": "MODULE_INVALID", "path": path.relative_to(SUITE_ROOT).as_posix(), "message": str(exc)})
    for path in sorted((SUITE_ROOT / "assets").rglob("*.json"), key=lambda item: item.as_posix().casefold()):
        try:
            json.loads(path.read_text(encoding="utf-8-sig"))
            checks.append({"code": "JSON_PARSEABLE", "status": "pass", "path": path.relative_to(SUITE_ROOT).as_posix()})
        except (OSError, json.JSONDecodeError) as exc:
            diagnostics.append({"severity": "error", "code": "JSON_INVALID", "path": path.relative_to(SUITE_ROOT).as_posix(), "message": str(exc)})
    try:
        from build_action_schema import build_schema
        expected_schema = build_schema(SUITE_ROOT.parents[1])
        expected_content = json.dumps(expected_schema, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
        action_outputs = (
            SUITE_ROOT / "assets" / "contracts" / "actions-schema.json",
            SUITE_ROOT / "shared" / "orryx_toolkit" / "actions-schema.json",
        )
        stale = [path for path in action_outputs if not path.is_file() or path.read_text(encoding="utf-8") != expected_content]
        if stale:
            for path in stale:
                diagnostics.append({"severity": "error", "code": "ACTION_SCHEMA_STALE", "path": path.relative_to(SUITE_ROOT).as_posix(), "message": "Kether Action Schema 与 Kotlin @KetherParser 注册点不一致"})
        else:
            checks.append({"code": "ACTION_SCHEMA_CURRENT", "status": "pass", "path": "assets/contracts/actions-schema.json"})
    except (OSError, ValueError, ImportError) as exc:
        diagnostics.append({"severity": "error", "code": "ACTION_SCHEMA_BUILD_FAILED", "path": "scripts/build_action_schema.py", "message": str(exc)})
    requirements = (SUITE_ROOT / "requirements.txt").read_text(encoding="utf-8").strip() if (SUITE_ROOT / "requirements.txt").is_file() else ""
    if requirements != "PyYAML>=6.0,<7":
        diagnostics.append({"severity": "error", "code": "REQUIREMENTS_INVALID", "path": "requirements.txt", "message": "必须固定声明 PyYAML>=6.0,<7"})
    from orryx_toolkit import run_contract
    sample = {
        "contractVersion": "1.0",
        "component": "progression",
        "operation": "generate",
        "workspace": {"root": ".", "mode": "standalone"},
        "request": {"id": "validate-suite", "minLevel": 1, "maxLevel": 2, "curve": {"type": "table", "startLevel": 1, "values": [10, 20]}},
        "policy": {"strict": True, "network": "deny", "overwrite": "deny", "minecraftVersion": "1.20.4", "plugins": [], "validateReferences": False},
    }
    if run_contract(sample) != run_contract(sample):
        diagnostics.append({"severity": "error", "code": "OUTPUT_UNSTABLE", "path": "shared/orryx_toolkit", "message": "相同输入输出不稳定"})
    else:
        checks.append({"code": "OUTPUT_STABLE", "status": "pass", "path": "shared/orryx_toolkit"})
    diagnostics.sort(key=lambda item: (item.get("path", ""), item.get("code", ""), item.get("message", "")))
    checks.sort(key=lambda item: (item.get("path", ""), item.get("code", "")))
    return {"status": "invalid" if diagnostics else "ok", "diagnostics": diagnostics, "checks": checks}


if __name__ == "__main__":
    result = validate()
    sys.stdout.write(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n")
    raise SystemExit(1 if result["status"] == "invalid" else 0)
