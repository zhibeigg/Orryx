"""orryx_toolkit 命令行入口。"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Mapping, Sequence

from .contracts import ContractError, finalize_result, invalid_result, normalize_contract
from .materialize import materialize
from .orchestrator import run_contract


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="orryx_toolkit")
    subparsers = parser.add_subparsers(dest="command", required=True)
    run_parser = subparsers.add_parser("run")
    run_parser.add_argument("--input", "-i", help="输入 JSON 文件；省略时读取 stdin")
    run_parser.add_argument("--output", "-o", help="输出 JSON 文件；省略时写 stdout")
    run_parser.add_argument("--component", help="覆盖输入顶层 component")
    for name in ("validate-workspace", "materialize"):
        command = subparsers.add_parser(name)
        command.add_argument("--input", "-i", help="输入 JSON 文件；省略时读取 stdin")
        command.add_argument("--output", "-o", help="输出 JSON 文件；省略时写 stdout")
    return parser


def _read_json(path: str | None) -> Any:
    if path:
        with Path(path).open("r", encoding="utf-8-sig") as stream:
            return json.load(stream)
    return json.load(sys.stdin)


def _write_json(value: Any, path: str | None) -> None:
    text = json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    if path:
        Path(path).write_text(text, encoding="utf-8", newline="\n")
    else:
        sys.stdout.write(text)


def execute(command: str, payload: Any) -> dict[str, Any]:
    if not isinstance(payload, Mapping):
        return invalid_result(payload, "输入必须是 JSON object")
    if command == "run":
        try:
            return run_contract(payload)
        except ContractError as exc:
            return invalid_result(payload, str(exc))
    if command == "validate-workspace":
        wrapped = dict(payload)
        wrapped["component"] = "validator"
        wrapped["operation"] = "validate"
        try:
            return run_contract(wrapped)
        except ContractError as exc:
            return invalid_result(wrapped, str(exc))
    if command == "materialize":
        wrapped = dict(payload)
        wrapped["component"] = "materialize"
        wrapped["operation"] = "materialize"
        try:
            contract = normalize_contract(wrapped)
        except ContractError as exc:
            return invalid_result(wrapped, str(exc))
        return finalize_result(contract, materialize(contract))
    return invalid_result(payload, f"未知命令: {command}")


def run_component_file(component: str, input_path: Path, output_path: Path) -> int:
    try:
        with Path(input_path).open("r", encoding="utf-8-sig") as stream:
            payload = json.load(stream)
    except (OSError, json.JSONDecodeError) as exc:
        sys.stderr.write(f"输入读取失败: {exc}\n")
        return 2
    if isinstance(payload, Mapping):
        payload = dict(payload)
        payload["component"] = component
    output = execute("run", payload)
    try:
        _write_json(output, str(output_path))
    except OSError as exc:
        sys.stderr.write(f"输出写入失败: {exc}\n")
        return 3
    return 0


def main(argv: Sequence[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    try:
        payload = _read_json(args.input)
    except (OSError, json.JSONDecodeError) as exc:
        sys.stderr.write(f"输入读取失败: {exc}\n")
        return 2
    if getattr(args, "component", None) and isinstance(payload, Mapping):
        payload = dict(payload)
        payload["component"] = args.component
    try:
        output = execute(args.command, payload)
        _write_json(output, args.output)
    except OSError as exc:
        sys.stderr.write(f"输出写入失败: {exc}\n")
        return 3
    except Exception as exc:
        sys.stderr.write(f"运行时基础设施错误: {exc}\n")
        return 4
    return 0
