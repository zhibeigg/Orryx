"""Orryx 工作区静态验证。"""
from __future__ import annotations

import re
from collections import defaultdict
from pathlib import Path
from typing import Any, Iterable, Mapping

from .contracts import check, diagnostic, empty_result
from .workspace import load_yaml_documents, relative_to_root

_PRIORITIES = {"LOWEST", "LOW", "NORMAL", "HIGH", "HIGHEST", "MONITOR"}
_SENSITIVE_KEYS = re.compile(r"(?:password|passwd|secret|token|api[_-]?key|private[_-]?key|credential)", re.I)
_SENSITIVE_VALUES = re.compile(r"(?:-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|(?:mongodb|mysql|redis|https?)://[^\s:@]+:[^\s@]+@)", re.I)
_RUNNING = re.compile(r"\brunning\s+(?:['\"]([^'\"]+)['\"]|([^\s\]}]+))", re.I)
_BUFF_REFERENCE = re.compile(r"\bbuff\s+(?:send|clear|has)\s+(?:['\"]([^'\"]+)['\"]|([^\s;\]}]+))", re.I)
_SKILL_TYPES = {"PASSIVE", "DIRECT", "DIRECT AIM", "PRESSING", "PRESSING AIM"}
_ASYNC_BUKKIT_ACTION = re.compile(
    r"(?im)(?:^|[;\n]\s*)(?:damage|heal|teleport|velocity|command|dispatch|set\s*block|"
    r"spawn|inventory|give|take|message|title|sound|particle|ignite|set\s*health)\b"
)


def _options(data: Any) -> Mapping[str, Any]:
    return data.get("Options", {}) if isinstance(data, Mapping) and isinstance(data.get("Options", {}), Mapping) else {}


def _names(documents: Iterable[tuple[Path, Any]]) -> set[str]:
    return {path.stem.casefold() for path, _ in documents}


def _walk_sensitive(value: Any, *, pointer: str = "") -> Iterable[tuple[str, str]]:
    if isinstance(value, Mapping):
        for key in sorted(value, key=lambda item: str(item).casefold()):
            child = f"{pointer}/{key}"
            item = value[key]
            if _SENSITIVE_KEYS.search(str(key)) and item not in (None, "", False):
                yield child, "key"
            yield from _walk_sensitive(item, pointer=child)
    elif isinstance(value, list):
        for index, item in enumerate(value):
            yield from _walk_sensitive(item, pointer=f"{pointer}/{index}")
    elif isinstance(value, str) and _SENSITIVE_VALUES.search(value):
        yield pointer, "value"


def _duplicate_basenames(root: Path, loaded: Mapping[str, list[tuple[Path, Any]]], result: dict[str, Any]) -> None:
    for kind, documents in sorted(loaded.items()):
        seen: dict[str, list[Path]] = defaultdict(list)
        for path, _ in documents:
            seen[path.stem.casefold()].append(path)
        for basename, paths_for_name in sorted(seen.items()):
            if len(paths_for_name) < 2:
                continue
            paths = sorted(relative_to_root(path, root) for path in paths_for_name)
            result["diagnostics"].append(diagnostic(
                "WORKSPACE_DUPLICATE_BASENAME", "error", f"{kind} 目录存在重复 basename {basename}: {', '.join(paths)}",
                suggestion=f"为 {kind} 配置使用唯一文件名；不同配置域可以同名", path=paths[0], details={"kind": kind, "paths": paths},
            ))


def _skills_and_experiences(
    root: Path,
    loaded: Mapping[str, list[tuple[Path, Any]]],
    contract: Mapping[str, Any],
    result: dict[str, Any],
) -> None:
    buff_keys: set[str] = set()
    for _, data in loaded.get("buff", []):
        if isinstance(data, Mapping):
            buff_keys.update(str(key).casefold() for key in data)
    workspace = contract.get("workspace", {})
    policy = contract.get("policy", {})
    version = ""
    if isinstance(workspace, Mapping):
        version = str(workspace.get("serverVersion", workspace.get("minecraftVersion", "")))
    if isinstance(policy, Mapping):
        version = str(policy.get("minecraftVersion", policy.get("serverVersion", version)))
    for path, data in loaded.get("skill", []):
        relative = relative_to_root(path, root)
        options = _options(data)
        skill_type = str(options.get("Type", "")).upper()
        if skill_type not in _SKILL_TYPES:
            result["diagnostics"].append(diagnostic(
                "SKILL_TYPE_INVALID", "error", f"非法技能 Type: {options.get('Type', '')}",
                suggestion="使用 PASSIVE/DIRECT/DIRECT AIM/PRESSING/PRESSING AIM", path=relative, pointer="/Options/Type",
            ))
        if skill_type != "PASSIVE" and (not isinstance(data, Mapping) or not isinstance(data.get("Actions"), str) or not data.get("Actions", "").strip()):
            result["diagnostics"].append(diagnostic(
                "SKILL_ACTIONS_MISSING", "error", "主动技能缺少 Actions", suggestion="填写可编译的 Kether Actions",
                path=relative, pointer="/Actions",
            ))
        if "AIM" in skill_type and version and version != "1.12.2":
            result["diagnostics"].append(diagnostic(
                "SKILL_AIM_VERSION_UNSUPPORTED", "error", f"Aim 技能仅支持 Minecraft 1.12.2，当前为 {version}",
                suggestion="改用 1.12.2 或非 Aim 技能", path=relative, pointer="/Options/Type",
            ))
        sort_value = options.get("Sort")
        if sort_value is not None and (not isinstance(sort_value, int) or isinstance(sort_value, bool)):
            result["diagnostics"].append(diagnostic(
                "SKILL_SORT_TYPE", "error", "Options.Sort 必须是整数", suggestion="使用唯一整数排序",
                path=relative, pointer="/Options/Sort",
            ))
        if isinstance(data, Mapping):
            for script_key in ("Actions", "ExtendActions"):
                script = data.get(script_key)
                if not isinstance(script, str):
                    continue
                for match in _BUFF_REFERENCE.finditer(script):
                    buff = (match.group(1) or match.group(2) or "").strip()
                    if buff and buff.casefold() not in buff_keys:
                        result["diagnostics"].append(diagnostic(
                            "SKILL_BUFF_MISSING", "error", f"技能引用了 buffs.yml 中不存在的 Buff: {buff}",
                            suggestion="在 buffs.yml 定义同名 key 或修正 Kether 脚本", path=relative,
                            pointer=f"/{script_key}", details={"buff": buff},
                        ))
    for path, data in loaded.get("experience", []):
        relative = relative_to_root(path, root)
        options = _options(data)
        minimum = options.get("Min")
        maximum = options.get("Max")
        if not isinstance(minimum, int) or isinstance(minimum, bool) or not isinstance(maximum, int) or isinstance(maximum, bool) or minimum >= maximum:
            result["diagnostics"].append(diagnostic(
                "EXPERIENCE_RANGE_INVALID", "error", "Experience Options.Min/Max 必须是整数且 Min < Max",
                suggestion="修正等级范围", path=relative, pointer="/Options",
            ))
        formula = options.get("ExperienceOfLevel")
        if not isinstance(formula, str) or not formula.strip():
            result["diagnostics"].append(diagnostic(
                "EXPERIENCE_FORMULA_MISSING", "error", "Experience 缺少 ExperienceOfLevel Kether 公式",
                suggestion="提供基于 level 的正经验公式", path=relative, pointer="/Options/ExperienceOfLevel",
            ))


def _job_references(root: Path, loaded: Mapping[str, list[tuple[Path, Any]]], result: dict[str, Any]) -> None:
    skills = _names(loaded.get("skill", []))
    experiences = _names(loaded.get("experience", []))
    skill_sort: dict[str, int] = {}
    for path, data in loaded.get("skill", []):
        value = _options(data).get("Sort", 0)
        if isinstance(value, int) and not isinstance(value, bool):
            skill_sort[path.stem.casefold()] = value
    for path, data in loaded.get("job", []):
        relative = relative_to_root(path, root)
        options = _options(data)
        job_skills = options.get("Skills", [])
        if not isinstance(job_skills, list):
            result["diagnostics"].append(diagnostic(
                "JOB_SKILLS_TYPE", "error", "Options.Skills 必须是列表", suggestion="改为 YAML list", path=relative, pointer="/Options/Skills",
            ))
            job_skills = []
        used_sorts: dict[int, list[str]] = defaultdict(list)
        for skill in job_skills:
            name = str(skill)
            key = name.casefold()
            if key not in skills:
                result["diagnostics"].append(diagnostic(
                    "JOB_SKILL_MISSING", "error", f"职业引用了不存在的技能: {name}",
                    suggestion="创建对应 skills/<name>.yml 或修正引用", path=relative, pointer="/Options/Skills",
                ))
            elif key in skill_sort:
                used_sorts[skill_sort[key]].append(name)
        for sort, names in sorted(used_sorts.items()):
            if len(names) > 1:
                result["diagnostics"].append(diagnostic(
                    "JOB_SKILL_SORT_CONFLICT", "error", f"职业内技能 Sort={sort} 冲突: {', '.join(sorted(names))}",
                    suggestion="为同一职业中的技能分配唯一 Sort", path=relative, pointer="/Options/Skills",
                ))
        experience = str(options.get("Experience", "default"))
        if experience.casefold() not in experiences:
            result["diagnostics"].append(diagnostic(
                "JOB_EXPERIENCE_MISSING", "error", f"职业引用了不存在的经验配置: {experience}",
                suggestion="创建对应 experiences/<name>.yml 或修正 Options.Experience", path=relative, pointer="/Options/Experience",
            ))


def _controller_animation_ids(data: Any) -> set[str]:
    found: set[str] = set()
    if not isinstance(data, Mapping):
        return found
    layers = data.get("Layer", {})
    if isinstance(layers, Mapping):
        for layer in layers.values():
            if not isinstance(layer, Mapping):
                continue
            animations = layer.get("animations", layer.get("Animations", []))
            if isinstance(animations, list):
                found.update(str(value).casefold() for value in animations)
    return found


def _status_controllers(root: Path, loaded: Mapping[str, list[tuple[Path, Any]]], result: dict[str, Any]) -> None:
    controller_documents = {path.stem.casefold(): data for path, data in loaded.get("controller", [])}
    for path, data in loaded.get("status", []):
        relative = relative_to_root(path, root)
        states_section = data.get("States", {}) if isinstance(data, Mapping) else {}
        local_states = {str(key).casefold() for key in states_section} if isinstance(states_section, Mapping) else set()
        controller = _options(data).get("Controller")
        controller_key = str(controller).casefold() if controller else ""
        if controller and controller_key not in controller_documents:
            result["diagnostics"].append(diagnostic(
                "STATUS_CONTROLLER_MISSING", "error", f"状态配置引用了不存在的 Controller: {controller}",
                suggestion="创建对应 controllers/<name>.yml 或修正引用", path=relative, pointer="/Options/Controller",
            ))
        controller_animations = _controller_animation_ids(controller_documents.get(controller_key))
        scripts: list[tuple[str, str]] = []
        if isinstance(data, Mapping):
            if isinstance(data.get("Action"), str):
                scripts.append(("/Action", data["Action"]))
            states = data.get("States", {})
            if isinstance(states, Mapping):
                for state_name, state in states.items():
                    if not isinstance(state, Mapping):
                        continue
                    for key in ("Action", "BlockAction"):
                        if isinstance(state.get(key), str):
                            scripts.append((f"/States/{state_name}/{key}", state[key]))
                    animation = state.get("Animation", {})
                    if controller_animations and isinstance(animation, Mapping):
                        for animation_key, animation_value in animation.items():
                            if str(animation_key).casefold().endswith("duration"):
                                continue
                            if isinstance(animation_value, str) and animation_value.casefold() not in controller_animations:
                                result["diagnostics"].append(diagnostic(
                                    "STATUS_ANIMATION_MISSING", "error", f"Controller {controller} 未声明动画: {animation_value}",
                                    suggestion="在 Controller.Layer.*.animations 中加入动画或修正 Status 引用",
                                    path=relative, pointer=f"/States/{state_name}/Animation/{animation_key}",
                                ))
        for pointer, script in scripts:
            for match in _RUNNING.finditer(script):
                target = (match.group(1) or match.group(2) or "").strip()
                if target and target.casefold() not in local_states:
                    result["diagnostics"].append(diagnostic(
                        "STATUS_RUNNING_TARGET_MISSING", "error", f"running 引用了不存在的状态: {target}",
                        suggestion="在 States 中定义目标状态或修正脚本", path=relative, pointer=pointer,
                    ))


def _stations(root: Path, loaded: Mapping[str, list[tuple[Path, Any]]], result: dict[str, Any]) -> None:
    for path, data in loaded.get("station", []):
        relative = relative_to_root(path, root)
        options = _options(data)
        priority = str(options.get("Priority", "NORMAL")).upper()
        if priority not in _PRIORITIES:
            result["diagnostics"].append(diagnostic(
                "STATION_PRIORITY_INVALID", "error", f"非法 Station Priority: {priority}",
                suggestion="使用 LOWEST/LOW/NORMAL/HIGH/HIGHEST/MONITOR", path=relative, pointer="/Options/Priority",
            ))
        if "Async" not in options:
            if "async" in options:
                result["diagnostics"].append(diagnostic(
                    "STATION_ASYNC_KEY_CASE", "error", "Loader 读取 Options.Async，但配置使用了小写 async",
                    suggestion="将 async 改为 Async", path=relative, pointer="/Options/async",
                ))
            else:
                result["diagnostics"].append(diagnostic(
                    "STATION_ASYNC_MISSING", "warning", "Station 未显式声明 Options.Async",
                    suggestion="显式填写 Async: false 或确认事件允许异步后设为 true", path=relative, pointer="/Options/Async",
                ))
        elif not isinstance(options["Async"], bool):
            result["diagnostics"].append(diagnostic(
                "STATION_ASYNC_TYPE", "error", "Options.Async 必须是布尔值", suggestion="使用 true 或 false", path=relative, pointer="/Options/Async",
            ))
        event = str(options.get("Event", "")).strip()
        if not event:
            result["diagnostics"].append(diagnostic(
                "STATION_EVENT_MISSING", "error", "Station 缺少 Options.Event", suggestion="填写已注册 Trigger 名称", path=relative, pointer="/Options/Event",
            ))
        if not isinstance(data, Mapping) or not isinstance(data.get("Actions"), str) or not data.get("Actions", "").strip():
            result["diagnostics"].append(diagnostic(
                "STATION_ACTIONS_MISSING", "error", "Station 缺少 Actions", suggestion="填写 Kether Actions", path=relative, pointer="/Actions",
            ))
        async_value = options.get("Async")
        actions = data.get("Actions", "") if isinstance(data, Mapping) else ""
        if async_value is True and isinstance(actions, str) and _ASYNC_BUKKIT_ACTION.search(actions):
            result["diagnostics"].append(diagnostic(
                "STATION_ASYNC_THREAD_RISK", "error", "异步 Station Actions 含疑似 Bukkit 主线程敏感动作",
                suggestion="将 Station 设为 Async: false，或改用明确的主线程调度动作并人工复核",
                path=relative, pointer="/Actions",
            ))
        if event.casefold().startswith("async ") and async_value is False:
            result["diagnostics"].append(diagnostic(
                "STATION_ASYNC_EVENT_MISMATCH", "warning", f"事件 {event} 本身为异步事件但 Options.Async=false",
                suggestion="确认脚本线程要求；需要异步执行时显式设为 true", path=relative, pointer="/Options/Async",
            ))


def validate_workspace(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    try:
        root, loaded = load_yaml_documents(contract.get("workspace", {}))
    except (OSError, ValueError) as exc:
        result["diagnostics"].append(diagnostic(
            "WORKSPACE_LOAD_FAILED", "error", str(exc), suggestion="检查工作区路径与 YAML 语法",
        ))
        return result
    if not root.is_dir():
        result["diagnostics"].append(diagnostic(
            "WORKSPACE_NOT_FOUND", "error", f"工作区不存在: {root}", suggestion="提供 Orryx 配置根目录",
        ))
        return result

    _duplicate_basenames(root, loaded, result)
    _skills_and_experiences(root, loaded, contract, result)
    _job_references(root, loaded, result)
    _status_controllers(root, loaded, result)
    _stations(root, loaded, result)
    for kind in sorted(loaded):
        for path, data in loaded[kind]:
            relative = relative_to_root(path, root)
            for pointer, source in _walk_sensitive(data):
                result["diagnostics"].append(diagnostic(
                    "WORKSPACE_SENSITIVE_VALUE", "error", f"检测到可能的敏感值（{source}）",
                    suggestion="改用环境变量、密钥管理或部署时注入，不要提交明文", path=relative, pointer=pointer,
                ))
    count = sum(len(values) for values in loaded.values())
    result["checks"].append(check("WORKSPACE_SCANNED", "pass", f"已扫描 {count} 个 Orryx YAML 文件"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return validate_workspace(contract)
