#!/usr/bin/env python3
"""离线验证十个 SKILL 的目录结构、frontmatter、示例和 Eval 资产。"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

import yaml

SUITE_ROOT = Path(__file__).resolve().parents[1]
SKILLS_ROOT = SUITE_ROOT / "skills"
EXPECTED = {
    "orryx-config-validator-skill": "validator",
    "orryx-kether-authoring-skill": "kether",
    "orryx-ability-authoring-skill": "ability",
    "orryx-progression-curve-skill": "progression",
    "orryx-job-kit-skill": "job",
    "orryx-station-mechanic-skill": "station",
    "orryx-combat-state-controller-skill": "combat",
    "orryx-selector-library-skill": "selector",
    "orryx-ui-adapter-skill": "ui",
    "orryx-workflow-orchestrator-skill": "orchestrator",
}
FRONTMATTER = re.compile(r"\A---\s*\n(.*?)\n---\s*\n", re.DOTALL)
JSON_BLOCK = re.compile(r"```json\s*\n(.*?)\n```", re.DOTALL)
PLACEHOLDER = re.compile(r"\b(?:TODO|FIXME|TKTK)\b", re.I)


def issue(code: str, path: Path, message: str) -> dict[str, str]:
    return {"severity": "error", "code": code, "path": path.relative_to(SUITE_ROOT).as_posix(), "message": message}


def check_skill(skill_dir: Path, component: str) -> tuple[list[dict[str, str]], list[dict[str, str]]]:
    diagnostics: list[dict[str, str]] = []
    checks: list[dict[str, str]] = []
    required = (
        skill_dir / "SKILL.md",
        skill_dir / "scripts" / "run_pipeline.py",
        skill_dir / "scripts" / "run_evals.py",
        skill_dir / "references",
        skill_dir / "assets",
        skill_dir / "evals",
    )
    for path in required:
        if path.exists():
            checks.append({"code": "SKILL_PATH_PRESENT", "path": path.relative_to(SUITE_ROOT).as_posix(), "status": "pass"})
        else:
            diagnostics.append(issue("SKILL_PATH_MISSING", path, "缺少必需技能资产"))
    skill_md = skill_dir / "SKILL.md"
    if not skill_md.is_file():
        return diagnostics, checks
    try:
        text = skill_md.read_text(encoding="utf-8")
    except UnicodeError as exc:
        diagnostics.append(issue("SKILL_UTF8_INVALID", skill_md, str(exc)))
        return diagnostics, checks
    match = FRONTMATTER.match(text)
    if not match:
        diagnostics.append(issue("SKILL_FRONTMATTER_MISSING", skill_md, "SKILL.md 必须以 YAML frontmatter 开头"))
    else:
        try:
            frontmatter = yaml.safe_load(match.group(1))
        except yaml.YAMLError as exc:
            diagnostics.append(issue("SKILL_FRONTMATTER_INVALID", skill_md, str(exc)))
            frontmatter = {}
        if not isinstance(frontmatter, dict):
            diagnostics.append(issue("SKILL_FRONTMATTER_TYPE", skill_md, "frontmatter 必须是 object"))
            frontmatter = {}
        if frontmatter.get("name") != skill_dir.name:
            diagnostics.append(issue("SKILL_NAME_MISMATCH", skill_md, "frontmatter.name 必须与目录名一致"))
        description = frontmatter.get("description")
        if not isinstance(description, str) or not description.strip():
            diagnostics.append(issue("SKILL_DESCRIPTION_MISSING", skill_md, "description 必须是非空字符串"))
        elif len(description) > 1024:
            diagnostics.append(issue("SKILL_DESCRIPTION_TOO_LONG", skill_md, "description 超过 1024 字符"))
    if len(text.splitlines()) > 500:
        diagnostics.append(issue("SKILL_TOO_LONG", skill_md, "SKILL.md 超过 500 行，应拆分到 references"))
    if PLACEHOLDER.search(text):
        diagnostics.append(issue("SKILL_PLACEHOLDER_FOUND", skill_md, "SKILL.md 含未完成占位词"))

    pipeline = skill_dir / "scripts" / "run_pipeline.py"
    if pipeline.is_file():
        pipeline_text = pipeline.read_text(encoding="utf-8")
        component_match = re.search(r'^COMPONENT\s*=\s*["\']([^"\']+)["\']', pipeline_text, re.MULTILINE)
        if not component_match or component_match.group(1) != component:
            diagnostics.append(issue("SKILL_COMPONENT_MISMATCH", pipeline, f"launcher 必须固定 COMPONENT={component}"))

    specs = sorted((skill_dir / "evals").glob("*.eval.md")) if (skill_dir / "evals").is_dir() else []
    if len(specs) != 1:
        diagnostics.append(issue("SKILL_EVAL_SPEC_COUNT", skill_dir / "evals", "每个技能必须恰有一个 *.eval.md"))
    else:
        eval_text = specs[0].read_text(encoding="utf-8")
        block = JSON_BLOCK.search(eval_text)
        if not block:
            diagnostics.append(issue("SKILL_EVAL_JSON_MISSING", specs[0], "eval spec 缺少 fenced JSON"))
        else:
            try:
                spec: Any = json.loads(block.group(1))
            except json.JSONDecodeError as exc:
                diagnostics.append(issue("SKILL_EVAL_JSON_INVALID", specs[0], str(exc)))
                spec = {}
            golden = spec.get("golden", []) if isinstance(spec, dict) else []
            if not isinstance(golden, list) or len(golden) < 3:
                diagnostics.append(issue("SKILL_GOLDEN_TOO_FEW", specs[0], "每个技能至少需要 3 个 golden case"))
            for case in golden if isinstance(golden, list) else []:
                relative = case.get("input") if isinstance(case, dict) else None
                target = skill_dir / "evals" / str(relative or "")
                if not relative or not target.is_file():
                    diagnostics.append(issue("SKILL_GOLDEN_INPUT_MISSING", specs[0], f"缺少 golden input: {relative}"))

    examples = sorted((skill_dir / "assets").rglob("*.json")) if (skill_dir / "assets").is_dir() else []
    if not examples:
        diagnostics.append(issue("SKILL_ASSET_EXAMPLE_MISSING", skill_dir / "assets", "每个技能至少需要一个可复用 JSON 请求模板；三组正负场景由 golden cases 提供"))
    for path in examples:
        try:
            value = json.loads(path.read_text(encoding="utf-8"))
            if not isinstance(value, dict):
                raise ValueError("根节点不是 object")
        except (OSError, ValueError, json.JSONDecodeError) as exc:
            diagnostics.append(issue("SKILL_EXAMPLE_INVALID", path, str(exc)))
    return diagnostics, checks


def main() -> int:
    diagnostics: list[dict[str, str]] = []
    checks: list[dict[str, str]] = []
    actual = {path.name for path in SKILLS_ROOT.iterdir() if path.is_dir()} if SKILLS_ROOT.is_dir() else set()
    missing = sorted(set(EXPECTED) - actual)
    extra = sorted(actual - set(EXPECTED))
    for name in missing:
        diagnostics.append(issue("SKILL_DIRECTORY_MISSING", SKILLS_ROOT / name, "缺少预期技能目录"))
    for name in extra:
        diagnostics.append(issue("SKILL_DIRECTORY_EXTRA", SKILLS_ROOT / name, "存在未登记技能目录"))
    for name, component in EXPECTED.items():
        skill_dir = SKILLS_ROOT / name
        if skill_dir.is_dir():
            found, passed = check_skill(skill_dir, component)
            diagnostics.extend(found)
            checks.extend(passed)
    diagnostics.sort(key=lambda item: (item["path"], item["code"], item["message"]))
    checks.sort(key=lambda item: (item["path"], item["code"]))
    result = {
        "status": "invalid" if diagnostics else "ok",
        "skillCount": len(actual & set(EXPECTED)),
        "diagnostics": diagnostics,
        "checks": checks,
    }
    sys.stdout.write(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n")
    return 1 if diagnostics else 0


if __name__ == "__main__":
    raise SystemExit(main())
