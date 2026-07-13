"""Bukkit/DragonCore/GermPlugin/ArcartX UI 配置生成器。"""
from __future__ import annotations

from pathlib import PurePosixPath
from typing import Any, Mapping

from .contracts import artifact, check, diagnostic, empty_result, reference, requirement
from .workspace import normalize_relative_path, PathEscapeError
from .yaml_io import stable_dump

_BACKENDS = {"bukkit", "dragoncore", "germplugin", "arcartx"}


def _stable(value: Any) -> Any:
    if isinstance(value, Mapping):
        return {str(key): _stable(item) for key, item in sorted(value.items(), key=lambda pair: str(pair[0]).casefold())}
    if isinstance(value, list):
        return [_stable(item) for item in value]
    return value


def generate_ui(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    request = contract.get("request", {})
    backend = str(request.get("backend", "bukkit")).casefold()
    if backend not in _BACKENDS:
        result["diagnostics"].append(diagnostic(
            "UI_BACKEND_INVALID", "error", f"未知 UI backend: {backend}", suggestion="使用 bukkit/dragoncore/germplugin/arcartx",
        ))
        return result
    files = request.get("files")
    generated: list[str] = []
    if isinstance(files, Mapping) and files:
        for relative, config in sorted(files.items(), key=lambda item: str(item[0]).casefold()):
            try:
                normalized = normalize_relative_path(str(relative))
            except PathEscapeError as exc:
                result["diagnostics"].append(diagnostic("UI_PATH_INVALID", "error", str(exc), suggestion="使用 UI 后端目录内的相对路径"))
                continue
            if PurePosixPath(normalized).suffix.lower() not in {".yml", ".yaml"}:
                normalized += ".yml"
            path = f"ui/{backend}/{normalized}"
            result["artifacts"].append(artifact(path, stable_dump(_stable(config)), metadata={"component": "ui", "backend": backend}))
            generated.append(path)
    else:
        config = request.get("config")
        if not isinstance(config, Mapping):
            config = {
                "JoinOpenHud": bool(request.get("joinOpenHud", True)),
                "SkillUI": {
                    "title": str(request.get("title", "技能界面")),
                    "Skills": {"Slots": list(request.get("skillSlots", [28, 29, 30, 31, 32, 33, 34]))},
                    "BindSkills": {"Slots": list(request.get("bindSlots", [10, 11, 12, 13, 14, 15, 16]))},
                },
            }
        path = f"ui/{backend}/setting.yml"
        result["artifacts"].append(artifact(path, stable_dump(_stable(config)), metadata={"component": "ui", "backend": backend}))
        generated.append(path)
    for skill in sorted((str(value) for value in request.get("skills", [])), key=str.casefold):
        for source in generated:
            result["references"].append(reference(source, f"skills/{skill}.yml", "ui-skill", required=False))
    if backend != "bukkit":
        result["requirements"].append(requirement(
            "UI_BACKEND_REQUIRED", f"UI backend {backend} 需要对应兼容插件与客户端资源", component="ui", details={"backend": backend},
        ))
    result["checks"].append(check("UI_YAML_GENERATED", "pass", f"已为 {backend} 生成 {len(generated)} 个 UI artifact"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return generate_ui(contract)
