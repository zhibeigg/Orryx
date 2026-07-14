#!/usr/bin/env python3
"""聚合运行十个 Orryx 组件技能的 Eval/Golden 回归门禁。"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path
from typing import Any

SUITE_ROOT = Path(__file__).resolve().parents[1]
SKILLS_ROOT = SUITE_ROOT / "skills"
SKILLS = (
    "orryx-config-validator-skill",
    "orryx-kether-authoring-skill",
    "orryx-ability-authoring-skill",
    "orryx-progression-curve-skill",
    "orryx-job-kit-skill",
    "orryx-station-mechanic-skill",
    "orryx-combat-state-controller-skill",
    "orryx-selector-library-skill",
    "orryx-ui-adapter-skill",
    "orryx-workflow-orchestrator-skill",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run all Orryx creation-suite skill evals")
    parser.add_argument("--validate", action="store_true", help="只验证每个 eval spec")
    parser.add_argument("--rollout", action="store_true", help="执行每个 golden case 的真实 pipeline")
    parser.add_argument("--skill", action="append", choices=SKILLS, help="只运行指定技能，可重复")
    parser.add_argument("--case", default=None, help="只运行指定 golden case id")
    parser.add_argument("--timeout", type=int, default=120, help="每个 golden pipeline 超时秒数")
    parser.add_argument("--json", action="store_true", help="输出机器可读 JSON")
    return parser.parse_args()


def _invoke(skill: str, mode: str, args: argparse.Namespace) -> dict[str, Any]:
    runner = SKILLS_ROOT / skill / "scripts" / "run_evals.py"
    command = [sys.executable, str(runner), "--json"]
    if mode == "validate":
        command.append("--validate")
    else:
        command.extend(["--rollout", "--timeout", str(args.timeout)])
        if args.case:
            command.extend(["--case", args.case])
    process = subprocess.run(
        command,
        cwd=str(SKILLS_ROOT / skill),
        capture_output=True,
        text=True,
        encoding="utf-8",
        timeout=max(args.timeout * 4, 120),
        check=False,
    )
    try:
        payload = json.loads(process.stdout) if process.stdout.strip() else {}
    except json.JSONDecodeError:
        payload = {"rawStdout": process.stdout}
    return {
        "skill": skill,
        "mode": mode,
        "exitCode": process.returncode,
        "ok": process.returncode == 0,
        "result": payload,
        "stderr": process.stderr.strip(),
    }


def main() -> int:
    args = parse_args()
    selected = tuple(args.skill or SKILLS)
    modes: list[str]
    if args.validate and not args.rollout:
        modes = ["validate"]
    elif args.rollout and not args.validate:
        modes = ["rollout"]
    else:
        modes = ["validate", "rollout"]

    results: list[dict[str, Any]] = []
    for mode in modes:
        for skill in selected:
            results.append(_invoke(skill, mode, args))

    failed = [item for item in results if not item["ok"]]
    summary = {
        "status": "ok" if not failed else "invalid",
        "suiteVersion": "1.1.0",
        "skills": len(selected),
        "runs": len(results),
        "passedRuns": len(results) - len(failed),
        "failedRuns": len(failed),
        "results": results,
    }
    if args.json:
        sys.stdout.write(json.dumps(summary, ensure_ascii=False, sort_keys=True, indent=2) + "\n")
    else:
        for item in results:
            marker = "PASS" if item["ok"] else "FAIL"
            print(f"{marker} {item['mode']:8} {item['skill']}")
            if item["stderr"] and not item["ok"]:
                print(f"  {item['stderr']}")
        print(f"SUMMARY {summary['status']} {summary['passedRuns']}/{summary['runs']} runs")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
