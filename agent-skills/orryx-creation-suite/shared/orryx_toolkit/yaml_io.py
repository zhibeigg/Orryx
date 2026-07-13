"""仅使用 PyYAML 安全 API 的确定性 YAML I/O。"""
from __future__ import annotations

from pathlib import Path
from typing import Any, TextIO

import yaml


class LiteralString(str):
    """请求 YAML literal block 样式的字符串。"""


class StableSafeDumper(yaml.SafeDumper):
    def ignore_aliases(self, data: Any) -> bool:
        return True


def _represent_literal(dumper: StableSafeDumper, value: LiteralString) -> yaml.nodes.ScalarNode:
    return dumper.represent_scalar("tag:yaml.org,2002:str", str(value), style="|")


StableSafeDumper.add_representer(LiteralString, _represent_literal)


def literal(value: Any) -> Any:
    if isinstance(value, str) and "\n" in value:
        return LiteralString(value.rstrip("\n"))
    return value


def stable_dump(value: Any) -> str:
    text = yaml.dump(
        value,
        Dumper=StableSafeDumper,
        allow_unicode=True,
        default_flow_style=False,
        sort_keys=False,
        width=120,
        indent=2,
    )
    return text.replace("\r\n", "\n")


def safe_load_text(text: str, *, source: str = "<memory>") -> Any:
    try:
        return yaml.safe_load(text)
    except yaml.YAMLError as exc:
        raise ValueError(f"无法解析 YAML {source}: {exc}") from exc


def safe_load_file(path: str | Path) -> Any:
    file_path = Path(path)
    with file_path.open("r", encoding="utf-8-sig") as stream:
        return safe_load_stream(stream, source=str(file_path))


def safe_load_stream(stream: TextIO, *, source: str = "<stream>") -> Any:
    try:
        return yaml.safe_load(stream)
    except yaml.YAMLError as exc:
        raise ValueError(f"无法解析 YAML {source}: {exc}") from exc
