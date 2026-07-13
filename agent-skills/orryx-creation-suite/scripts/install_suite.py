#!/usr/bin/env python3
"""Install the Orryx creation suite into supported agent skill roots."""

from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
from pathlib import Path

SUITE_NAME = "orryx-creation-suite"
RUNTIME_NAME = "orryx-creation-suite-runtime"
VERSION = "1.0.0"
COMPONENTS = (
    "orryx-config-validator-skill",
    "orryx-kether-authoring-skill",
    "orryx-ability-authoring-skill",
    "orryx-progression-curve-skill",
    "orryx-job-kit-skill",
    "orryx-station-mechanic-skill",
    "orryx-combat-state-controller-skill",
    "orryx-workflow-orchestrator-skill",
    "orryx-selector-library-skill",
    "orryx-ui-adapter-skill",
)

PROJECT_PATHS = {
    "claude-code": Path(".claude/skills"),
    "copilot": Path(".github/skills"),
    "cursor": Path(".cursor/skills"),
    "windsurf": Path(".windsurf/rules"),
    "cline": Path(".clinerules/skills"),
    "codex": Path(".agents/skills"),
    "universal": Path(".agents/skills"),
    "gemini": Path(".gemini/skills"),
    "kiro": Path(".kiro/skills"),
    "trae": Path(".trae/rules"),
    "roo-code": Path(".roo/skills"),
    "opencode": Path(".opencode/skills"),
    "goose": Path(".goose/skills"),
}

USER_PATHS = {
    "claude-code": Path("~/.claude/skills"),
    "copilot": Path("~/.copilot/skills"),
    "cursor": Path("~/.cursor/skills"),
    "windsurf": Path("~/.codeium/windsurf/skills"),
    "cline": Path("~/.cline/skills"),
    "codex": Path("~/.agents/skills"),
    "universal": Path("~/.agents/skills"),
    "gemini": Path("~/.gemini/skills"),
    "kiro": Path("~/.kiro/skills"),
    "trae": Path("~/.trae/rules"),
    "roo-code": Path("~/.roo/skills"),
    "opencode": Path("~/.config/opencode/skills"),
    "goose": Path("~/.config/goose/skills"),
}


def suite_root() -> Path:
    return Path(__file__).resolve().parents[1]


def parse_components(value: str | None) -> tuple[str, ...]:
    if not value or value.strip().lower() == "all":
        return COMPONENTS
    selected = tuple(item.strip() for item in value.split(",") if item.strip())
    unknown = sorted(set(selected) - set(COMPONENTS))
    if unknown:
        raise ValueError(f"未知组件: {', '.join(unknown)}")
    return selected


def resolve_base(platform: str, project: bool, custom_path: str | None) -> Path:
    if custom_path:
        return Path(custom_path).expanduser().resolve()
    table = PROJECT_PATHS if project else USER_PATHS
    if platform not in table:
        raise ValueError(f"不支持的平台: {platform}")
    path = table[platform].expanduser()
    if project:
        path = Path.cwd() / path
    return path.resolve()


def detected_platforms() -> tuple[str, ...]:
    home = Path.home()
    detected: list[str] = []
    if (home / ".claude").is_dir():
        detected.append("claude-code")
    if (home / ".copilot").is_dir() or Path(".github").is_dir():
        detected.append("copilot")
    if (home / ".cursor").is_dir() or Path(".cursor").is_dir():
        detected.append("cursor")
    if (home / ".codeium/windsurf").is_dir() or Path(".windsurf").is_dir():
        detected.append("windsurf")
    if (home / ".gemini").is_dir():
        detected.append("gemini")
    if (home / ".kiro").is_dir() or Path(".kiro").is_dir():
        detected.append("kiro")
    if (home / ".agents").is_dir():
        detected.append("universal")
    if not detected:
        detected.append("universal")
    return tuple(dict.fromkeys(detected))


def marker_payload(kind: str, name: str) -> str:
    return json.dumps(
        {"suite": SUITE_NAME, "suiteVersion": VERSION, "kind": kind, "name": name},
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def can_replace(destination: Path) -> bool:
    marker = destination / ".orryx-suite-install.json"
    if not marker.is_file():
        return False
    try:
        data = json.loads(marker.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return False
    return data.get("suite") == SUITE_NAME


def atomic_copy(source: Path, destination: Path, kind: str, force: bool, dry_run: bool) -> None:
    if destination.exists() and not force:
        raise FileExistsError(f"目标已存在，默认拒绝覆盖: {destination}")
    if destination.exists() and force and not can_replace(destination):
        raise PermissionError(f"目标不是本套件安装产物，拒绝覆盖: {destination}")
    if dry_run:
        print(f"PLAN copy {source} -> {destination}")
        return

    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.parent / f".{destination.name}.tmp-{os.getpid()}"
    backup = destination.parent / f".{destination.name}.backup-{os.getpid()}"
    if temporary.exists():
        shutil.rmtree(temporary)
    if backup.exists():
        shutil.rmtree(backup)

    shutil.copytree(
        source,
        temporary,
        ignore=shutil.ignore_patterns("__pycache__", "*.pyc", ".eval-output", ".skill-dist"),
    )
    (temporary / ".orryx-suite-install.json").write_text(
        marker_payload(kind, destination.name), encoding="utf-8"
    )

    try:
        if destination.exists():
            destination.rename(backup)
        temporary.rename(destination)
    except BaseException:
        if destination.exists() and not backup.exists():
            shutil.rmtree(destination, ignore_errors=True)
        if backup.exists() and not destination.exists():
            backup.rename(destination)
        shutil.rmtree(temporary, ignore_errors=True)
        raise
    else:
        shutil.rmtree(backup, ignore_errors=True)


def install_to_base(
    base: Path,
    components: tuple[str, ...],
    force: bool,
    dry_run: bool,
) -> None:
    root = suite_root()
    skills_root = root / "skills"
    shared_root = root / "shared"
    if not (root / "SKILL.md").is_file() or not shared_root.is_dir():
        raise FileNotFoundError(f"套件目录不完整: {root}")

    atomic_copy(root, base / SUITE_NAME, "suite", force, dry_run)
    atomic_copy(shared_root, base / RUNTIME_NAME, "runtime", force, dry_run)
    for component in components:
        source = skills_root / component
        if not (source / "SKILL.md").is_file():
            raise FileNotFoundError(f"组件尚未构建完整: {source}")
        atomic_copy(source, base / component, "component", force, dry_run)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="安装 Orryx Agent SKILL 套件")
    parser.add_argument("--platform", choices=sorted(USER_PATHS), default="claude-code")
    parser.add_argument("--project", action="store_true", help="安装到当前项目")
    parser.add_argument("--path", help="自定义技能根目录")
    parser.add_argument("--components", default="all", help="逗号分隔组件名，默认全部")
    parser.add_argument("--all", action="store_true", help="安装到检测到的全部平台")
    parser.add_argument("--force", action="store_true", help="仅覆盖本套件以前安装的同名目录")
    parser.add_argument("--dry-run", action="store_true", help="只打印安装计划")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        components = parse_components(args.components)
        platforms = detected_platforms() if args.all else (args.platform,)
        seen_bases: set[Path] = set()
        for platform in platforms:
            base = resolve_base(platform, args.project, args.path)
            if base in seen_bases:
                continue
            seen_bases.add(base)
            print(f"Installing profile={platform} base={base}")
            install_to_base(base, components, args.force, args.dry_run)
        if args.dry_run:
            print("Dry-run complete; no files were written.")
        else:
            print("Orryx creation suite installed successfully.")
        return 0
    except (ValueError, OSError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
