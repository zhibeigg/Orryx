#!/usr/bin/env python3
"""Thin launcher for the Orryx creation-suite validator component."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

COMPONENT = "validator"
RUNTIME_NAME = "orryx-creation-suite-runtime"


def locate_toolkit() -> Path:
    script = Path(__file__).resolve()
    for parent in script.parents:
        source = parent / "shared" / "orryx_toolkit"
        if source.is_dir():
            return source.parent
        installed = parent / RUNTIME_NAME / "orryx_toolkit"
        if installed.is_dir():
            return installed.parent
    raise RuntimeError(
        "找不到 orryx_toolkit：请从 orryx-creation-suite 源码树运行，或安装完整套件，"
        f"使 {RUNTIME_NAME}/orryx_toolkit 与本技能目录同级。"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run the Orryx config validator pipeline.")
    parser.add_argument("--input", required=True, type=Path, help="UTF-8 JSON request file")
    parser.add_argument("--output", required=True, type=Path, help="UTF-8 JSON result file")
    return parser.parse_args()


def normalize_cli_path(value: Path) -> Path:
    text = str(value)
    if len(text) >= 2 and text[0] == text[-1] and text[0] in {"'", '"'}:
        text = text[1:-1]
    return Path(text)


def main() -> int:
    args = parse_args()
    input_path = normalize_cli_path(args.input)
    output_path = normalize_cli_path(args.output)
    try:
        request = json.loads(input_path.read_text(encoding="utf-8"))
        if not isinstance(request, dict):
            raise ValueError("请求根节点必须是 JSON object")
        request["component"] = COMPONENT
        toolkit_parent = locate_toolkit()
        sys.path.insert(0, str(toolkit_parent))
        from orryx_toolkit import run_contract

        result = run_contract(request)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(
            json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        return 0
    except (OSError, ValueError, json.JSONDecodeError, ImportError, RuntimeError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
