#!/usr/bin/env python3
"""Thin launcher for the selector component of orryx-creation-suite."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

COMPONENT = "selector"


def _load_runtime() -> object:
    skill_root = Path(__file__).resolve().parent.parent
    suite_shared = skill_root.parent.parent / "shared"
    installed_runtime = skill_root.parent / "orryx-creation-suite-runtime"
    candidates = (suite_shared, installed_runtime, installed_runtime / "shared")
    for candidate in candidates:
        if (candidate / "orryx_toolkit").is_dir():
            sys.path.insert(0, str(candidate))
            break
    try:
        from orryx_toolkit import run_contract
    except ImportError as exc:
        raise SystemExit(
            "无法加载 orryx_toolkit。请从完整 orryx-creation-suite 源码树运行，"
            "或在当前技能同级安装 orryx-creation-suite-runtime。"
        ) from exc
    return run_contract


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the Orryx selector contract pipeline.")
    parser.add_argument("--input", required=True, help="Input contract JSON file")
    parser.add_argument("--output", required=True, help="Output report JSON file")
    args = parser.parse_args()

    input_path = Path(args.input.strip("'\""))
    output_path = Path(args.output.strip("'\""))
    payload = json.loads(input_path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise SystemExit("输入必须是 JSON object。")
    payload["component"] = COMPONENT

    run_contract = _load_runtime()
    result = run_contract(payload)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
