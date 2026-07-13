"""工作区发现、读取与路径边界保护。"""
from __future__ import annotations

import json
import os
from pathlib import Path, PurePosixPath
from typing import Any, Iterable, Mapping

from .yaml_io import safe_load_file

CONFIG_LOCATIONS = {
    "skill": ("skills",),
    "job": ("jobs",),
    "experience": ("experiences",),
    "station": ("stations",),
    "status": ("status",),
    "controller": ("controllers",),
    "ui": ("ui",),
}
SINGLE_FILES = {"selector": "selectors.yml", "buff": "buffs.yml"}


class PathEscapeError(ValueError):
    """目标路径越出工作区。"""


def workspace_root(workspace: str | Mapping[str, Any]) -> Path:
    if isinstance(workspace, str):
        raw = workspace
    else:
        raw = workspace.get("root") or workspace.get("path") or "."
    return Path(str(raw)).expanduser().resolve()


def normalize_relative_path(value: str) -> str:
    raw = str(value).replace("\\", "/")
    pure = PurePosixPath(raw)
    if not raw or pure.is_absolute() or any(part in ("", ".", "..") for part in pure.parts):
        raise PathEscapeError(f"非法相对路径: {value}")
    if ":" in pure.parts[0]:
        raise PathEscapeError(f"不允许盘符路径: {value}")
    return pure.as_posix()


def safe_join(root: str | Path, relative: str) -> Path:
    base = Path(root).resolve()
    normalized = normalize_relative_path(relative)
    target = (base / Path(*PurePosixPath(normalized).parts)).resolve()
    try:
        common = os.path.commonpath((str(base), str(target)))
    except ValueError as exc:
        raise PathEscapeError(f"目标路径不在工作区: {relative}") from exc
    if common != str(base):
        raise PathEscapeError(f"目标路径逃逸工作区: {relative}")
    return target


def relative_to_root(path: Path, root: Path) -> str:
    return path.resolve().relative_to(root.resolve()).as_posix()


def yaml_files(directory: Path) -> list[Path]:
    if not directory.is_dir():
        return []
    return sorted(
        (p for p in directory.rglob("*") if p.is_file() and p.suffix.lower() in (".yml", ".yaml")),
        key=lambda p: p.as_posix().casefold(),
    )


def discover(workspace: str | Mapping[str, Any]) -> dict[str, list[Path]]:
    root = workspace_root(workspace)
    result: dict[str, list[Path]] = {}
    for kind, parts in CONFIG_LOCATIONS.items():
        result[kind] = yaml_files(root.joinpath(*parts))
    for kind, filename in SINGLE_FILES.items():
        path = root / filename
        result[kind] = [path] if path.is_file() else []
    return result


def load_yaml_documents(workspace: str | Mapping[str, Any]) -> tuple[Path, dict[str, list[tuple[Path, Any]]]]:
    root = workspace_root(workspace)
    loaded: dict[str, list[tuple[Path, Any]]] = {}
    for kind, paths in discover(workspace).items():
        loaded[kind] = [(path, safe_load_file(path)) for path in paths]
    return root, loaded


def load_json_file(path: str | Path) -> Any:
    with Path(path).open("r", encoding="utf-8-sig") as stream:
        return json.load(stream)


def find_actions_schema(workspace: str | Mapping[str, Any], explicit: str | None = None) -> Path | None:
    root = workspace_root(workspace)
    candidates: Iterable[Path]
    if explicit:
        try:
            candidates = (safe_join(root, explicit), Path(explicit).expanduser().resolve())
        except PathEscapeError:
            candidates = (Path(explicit).expanduser().resolve(),)
    else:
        candidates = (
            root / "actions-schema.json",
            root / "build" / "generated-docs" / "kether" / "actions-schema.json",
            Path(__file__).with_name("actions-schema.json"),
        )
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    return None
