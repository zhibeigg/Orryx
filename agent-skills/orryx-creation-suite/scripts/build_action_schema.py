#!/usr/bin/env python3
"""从 Orryx Kotlin @KetherParser 注册点生成确定性 Action Schema。"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

ANNOTATION = re.compile(r"@KetherParser\((.*?)\)", re.DOTALL)
QUOTED = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')
FUNCTION = re.compile(r"(?:private\s+|public\s+|internal\s+)?fun\s+([A-Za-z_][A-Za-z0-9_]*)\s*\([^)]*\)\s*=\s*([A-Za-z_][A-Za-z0-9_]*)")
VARIABLE = re.compile(r'(?:set|getVariable)\(\s*"([@A-Za-z_][@A-Za-z0-9_-]*)"')
BUKKIT_EVIDENCE = re.compile(
    r"(?:import\s+org\.bukkit\.|\.teleport\(|\.velocity\s*=|\.damage\(|\.addPotionEffect\(|"
    r"\.inventory\b|\.world\b|spawnParticle\(|playSound\(|dispatchCommand\()"
)
SELECTOR_EVIDENCE = re.compile(r"(?:\bselector\b|StringParser\(they\)|showAFrame\(|ITarget|TargetLocation)", re.I)
DANGEROUS_EVIDENCE = re.compile(
    r"(?:\.damage\(|\.teleport\(|\.velocity\s*=|dispatchCommand\(|setHealth\(|remove\(|spawn\w*\()"
)


def _namespace(annotation: str) -> str:
    match = re.search(r"\bnamespace\s*=\s*([A-Za-z_][A-Za-z0-9_]*|\"[^\"]+\")", annotation)
    return match.group(1).strip('"') if match else "DEFAULT"


def _shared(annotation: str) -> bool:
    match = re.search(r"\bshared\s*=\s*(true|false)", annotation, re.I)
    return bool(match and match.group(1).casefold() == "true")


def _line(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def _source_digest(files: list[Path], root: Path) -> str:
    digest = hashlib.sha256()
    for path in files:
        relative = path.relative_to(root).as_posix()
        digest.update(relative.encode("utf-8"))
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def build_schema(repository_root: Path) -> dict[str, Any]:
    source_root = repository_root / "src/main/kotlin/org/gitee/orryx/core/kether"
    actions_root = source_root / "actions"
    files = sorted(actions_root.rglob("*.kt"), key=lambda item: item.as_posix().casefold())
    if not files:
        raise FileNotFoundError(f"未找到 Kether Action 源码: {actions_root}")

    actions: list[dict[str, Any]] = []
    implicit_sources: dict[str, set[str]] = {}
    for path in files:
        text = path.read_text(encoding="utf-8")
        relative = path.relative_to(repository_root).as_posix()
        compatibility = ""
        parts = path.relative_to(actions_root).parts
        if parts and parts[0].casefold() == "compat":
            compatibility = "/".join(parts[1:-1]) or path.stem.removesuffix("Actions")
        for match in ANNOTATION.finditer(text):
            annotation = match.group(1)
            names_match = re.search(r"\[([^\]]*)\]", annotation, re.DOTALL)
            aliases = QUOTED.findall(names_match.group(1) if names_match else "")
            if not aliases:
                continue
            tail = text[match.end():match.end() + 500]
            function_match = FUNCTION.search(tail)
            function_name = function_match.group(1) if function_match else ""
            parser_factory = function_match.group(2) if function_match else "unknown"
            context = "station" if "/station/" in f"/{relative.casefold()}" else "generic"
            bukkit_hits = sorted(set(BUKKIT_EVIDENCE.findall(text)))
            selector_hits = sorted(set(SELECTOR_EVIDENCE.findall(text)))
            dangerous_hits = sorted(set(DANGEROUS_EVIDENCE.findall(text)))
            action: dict[str, Any] = {
                "name": aliases[0],
                "aliases": aliases[1:],
                "namespace": _namespace(annotation),
                "shared": _shared(annotation),
                "contexts": [context],
                "parserFactory": parser_factory,
                "execution": {
                    "thread": "main" if bukkit_hits else "any",
                    "evidence": bukkit_hits[:8],
                },
                "danger": {
                    "classification": "state-mutating" if dangerous_hits else "not-detected",
                    "evidence": dangerous_hits[:8],
                },
                "selectorUsage": {
                    "detected": bool(selector_hits),
                    "evidence": selector_hits[:8],
                },
                "source": {
                    "path": relative,
                    "line": _line(text, match.start()),
                    "function": function_name,
                },
            }
            if compatibility:
                action["pluginPrerequisites"] = [{
                    "module": compatibility,
                    "required": True,
                    "evidence": relative,
                }]
            actions.append(action)

        for variable in VARIABLE.findall(text):
            implicit_sources.setdefault(variable, set()).add(relative)

    parameter_files = sorted((source_root / "parameter").glob("*.kt"), key=lambda item: item.as_posix().casefold())
    for path in parameter_files:
        text = path.read_text(encoding="utf-8")
        relative = path.relative_to(repository_root).as_posix()
        for variable in VARIABLE.findall(text):
            implicit_sources.setdefault(variable, set()).add(relative)
        for variable in re.findall(r'"([A-Za-z_][A-Za-z0-9_-]*)"\s+to\s+', text):
            implicit_sources.setdefault(variable, set()).add(relative)

    actions.sort(key=lambda item: (item["namespace"].casefold(), item["name"].casefold(), item["source"]["path"]))
    implicit = [
        {"name": name, "sources": sorted(paths, key=str.casefold)}
        for name, paths in sorted(implicit_sources.items(), key=lambda item: item[0].casefold())
    ]
    all_source_files = files + parameter_files
    return {
        "schemaVersion": "1.0",
        "generator": "scripts/build_action_schema.py",
        "sourceRoot": source_root.relative_to(repository_root).as_posix(),
        "sourceSha256": _source_digest(all_source_files, repository_root),
        "actions": actions,
        "implicitVariables": implicit,
        "summary": {
            "actionCount": len(actions),
            "aliasCount": sum(len(item["aliases"]) for item in actions),
            "implicitVariableCount": len(implicit),
            "sourceFileCount": len(all_source_files),
        },
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build Orryx Kether action schema from Kotlin parser registrations")
    parser.add_argument("--repository-root", type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument("--check", action="store_true", help="仅检查现有输出是否与源码一致")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    root = args.repository_root.resolve()
    schema = build_schema(root)
    content = json.dumps(schema, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    outputs = (
        root / "agent-skills/orryx-creation-suite/assets/contracts/actions-schema.json",
        root / "agent-skills/orryx-creation-suite/shared/orryx_toolkit/actions-schema.json",
    )
    if args.check:
        stale = [path for path in outputs if not path.is_file() or path.read_text(encoding="utf-8") != content]
        if stale:
            for path in stale:
                print(f"STALE {path.relative_to(root).as_posix()}")
            return 1
        print(f"OK actions={schema['summary']['actionCount']} sourceSha256={schema['sourceSha256']}")
        return 0
    for path in outputs:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        print(f"WROTE {path.relative_to(root).as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
