"""轻量、确定性的 Kether 静态检查器。"""
from __future__ import annotations

import re
from pathlib import Path
from typing import Any, Iterable, Mapping

from .contracts import check, diagnostic, empty_result
from .workspace import find_actions_schema, load_json_file

_BALANCED = {"{": "}", "[": "]", "(": ")"}
_BASE_ACTIONS = {
    "all", "any", "await", "break", "calc", "call", "case", "check", "container",
    "continue", "else", "exit", "false", "for", "if", "lazy", "map", "math", "not",
    "null", "optional", "range", "repeat", "return", "set", "sleep", "sync", "then",
    "true", "try", "when", "while", "with",
}
_IMPLICIT_BY_CONTEXT = {
    "ability": {"level", "to", "from", "player", "skill", "pressTick", "aim"},
    "skill": {"level", "to", "from", "player", "skill", "pressTick", "aim"},
    "station": {"player", "event", "message"},
    "progression": {"level"},
    "experience": {"level"},
    "state": {"player", "input", "pressTick"},
    "controller": {"player", "input", "pressTick"},
}
_THREAD_SENSITIVE = {"damage", "launch", "particle", "potion", "running", "sound", "teleport", "velocity"}


def _schema_actions(schema: Any) -> tuple[dict[str, Mapping[str, Any]], set[str]]:
    actions: dict[str, Mapping[str, Any]] = {}
    implicit: set[str] = set()
    if not isinstance(schema, Mapping):
        return actions, implicit
    for item in schema.get("actions", []):
        if not isinstance(item, Mapping):
            continue
        names = [item.get("name"), *item.get("aliases", [])]
        for name in names:
            if isinstance(name, str) and name:
                actions[name.casefold()] = item
    variables = schema.get("implicitVariables", schema.get("variables", []))
    if isinstance(variables, Mapping):
        implicit.update(str(key).casefold() for key in variables)
    elif isinstance(variables, list):
        for value in variables:
            if isinstance(value, str):
                implicit.add(value.casefold())
            elif isinstance(value, Mapping) and value.get("name"):
                implicit.add(str(value["name"]).casefold())
    return actions, implicit


def load_actions_schema(contract: Mapping[str, Any]) -> tuple[Any | None, str | None, str | None]:
    request = contract.get("request", {})
    supplied = request.get("actionsSchema")
    if isinstance(supplied, Mapping):
        return supplied, "<request>", None
    explicit = supplied if isinstance(supplied, str) else request.get("actionsSchemaPath")
    try:
        path = find_actions_schema(contract.get("workspace", {}), explicit)
        if path is None:
            return None, None, None
        return load_json_file(path), str(path), None
    except (OSError, ValueError, TypeError) as exc:
        return None, str(explicit or "actions-schema.json"), str(exc)


def _scripts(request: Mapping[str, Any]) -> list[tuple[str, str]]:
    found: list[tuple[str, str]] = []
    script = request.get("script", request.get("actions"))
    if isinstance(script, str):
        found.append(("script", script))
    scripts = request.get("scripts")
    if isinstance(scripts, Mapping):
        found.extend((str(key), str(value)) for key, value in scripts.items() if isinstance(value, str))
    elif isinstance(scripts, list):
        for index, value in enumerate(scripts):
            if isinstance(value, str):
                found.append((f"scripts[{index}]", value))
            elif isinstance(value, Mapping) and isinstance(value.get("script"), str):
                found.append((str(value.get("name", f"scripts[{index}]")), value["script"]))
    return sorted(found, key=lambda pair: pair[0].casefold())


def _context(request: Mapping[str, Any]) -> tuple[str, set[str], bool]:
    raw = request.get("context", "generic")
    if isinstance(raw, Mapping):
        name = str(raw.get("type", raw.get("name", "generic"))).casefold()
        variables = raw.get("variables", [])
        async_context = bool(raw.get("async", False))
    else:
        name = str(raw).casefold()
        variables = []
        async_context = "async" in name
    declared = set()
    if isinstance(variables, Mapping):
        declared.update(str(key).casefold() for key in variables)
    elif isinstance(variables, list):
        declared.update(str(value).casefold() for value in variables)
    request_vars = request.get("variables", {})
    if isinstance(request_vars, Mapping):
        declared.update(str(key).casefold() for key in request_vars)
    for key, values in _IMPLICIT_BY_CONTEXT.items():
        if key in name:
            declared.update(value.casefold() for value in values)
    return name, declared, async_context


def _strip_strings_and_comments(script: str) -> str:
    rows = []
    for row in script.splitlines():
        row = row.split("#", 1)[0]
        rows.append(re.sub(r"(['\"]).*?\1", "", row))
    return "\n".join(rows)


def _check_balance(script: str, label: str, result: dict[str, Any]) -> None:
    stack: list[tuple[str, int]] = []
    quote = ""
    escaped = False
    for index, char in enumerate(script):
        if escaped:
            escaped = False
            continue
        if char == "\\" and quote:
            escaped = True
            continue
        if char in ("'", '"'):
            if not quote:
                quote = char
            elif quote == char:
                quote = ""
            continue
        if quote:
            continue
        if char in _BALANCED:
            stack.append((char, index))
        elif char in _BALANCED.values():
            if not stack or _BALANCED[stack[-1][0]] != char:
                result["diagnostics"].append(diagnostic(
                    "KETHER_STRUCTURE_UNBALANCED", "error", f"{label} 在字符 {index} 存在未配对的 {char}",
                    suggestion="检查括号与代码块边界", pointer=label,
                ))
                return
            stack.pop()
    if quote:
        result["diagnostics"].append(diagnostic(
            "KETHER_STRUCTURE_QUOTE", "error", f"{label} 存在未闭合字符串", suggestion="闭合引号", pointer=label,
        ))
    if stack:
        opener, index = stack[-1]
        result["diagnostics"].append(diagnostic(
            "KETHER_STRUCTURE_UNBALANCED", "error", f"{label} 在字符 {index} 存在未闭合的 {opener}",
            suggestion="补齐对应括号或代码块", pointer=label,
        ))


def _action_tokens(script: str) -> list[str]:
    clean = _strip_strings_and_comments(script)
    tokens: set[str] = set()
    pattern = re.compile(r"(?:^|[;{}]|\bthen\b|\belse\b|->)\s*([A-Za-z][A-Za-z0-9_-]*)", re.MULTILINE)
    for match in pattern.finditer(clean):
        token = match.group(1).casefold()
        if token not in {"when"}:
            tokens.add(token)
    return sorted(tokens)


def validate_kether(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    request = contract.get("request", {})
    scripts = _scripts(request)
    schema, schema_source, schema_error = load_actions_schema(contract)
    known, schema_implicit = _schema_actions(schema)
    context, declared, async_context = _context(request)
    declared.update(schema_implicit)

    if schema_error:
        result["diagnostics"].append(diagnostic(
            "KETHER_SCHEMA_INVALID", "warning", f"无法加载 actions schema: {schema_error}",
            suggestion="提供可解析的 actions-schema.json", path=schema_source or "",
        ))
    elif schema is None:
        result["diagnostics"].append(diagnostic(
            "KETHER_SCHEMA_MISSING", "warning", "未找到 actions-schema.json，无法确认动作完整性",
            suggestion="在 request.actionsSchema 或工作区提供 schema",
        ))
    else:
        result["checks"].append(check("KETHER_SCHEMA_LOADED", "pass", f"已加载动作 schema: {schema_source}"))

    if not scripts:
        result["diagnostics"].append(diagnostic(
            "KETHER_SCRIPT_MISSING", "error", "未提供待校验的 Kether 脚本", suggestion="填写 request.script 或 request.scripts",
        ))
        return result

    version = ""
    workspace = contract.get("workspace", {})
    policy = contract.get("policy", {})
    if isinstance(workspace, Mapping):
        version = str(workspace.get("serverVersion", workspace.get("minecraftVersion", "")))
    if isinstance(policy, Mapping):
        version = str(policy.get("minecraftVersion", policy.get("serverVersion", version)))
    version = str(request.get("minecraftVersion", request.get("serverVersion", version)))
    skill_type = str(request.get("skillType", request.get("type", ""))).casefold()
    if ("aim" in context or "aim" in skill_type) and version and version != "1.12.2":
        result["diagnostics"].append(diagnostic(
            "KETHER_AIM_VERSION_UNSUPPORTED", "error", f"Aim 仅支持 Minecraft 1.12.2，当前为 {version}",
            suggestion="改用 1.12.2 或非 Aim 技能类型",
        ))

    for label, script in scripts:
        _check_balance(script, label, result)
        clean = _strip_strings_and_comments(script)
        for variable in sorted(set(re.findall(r"(?<![A-Za-z0-9_])[&*]([A-Za-z_][A-Za-z0-9_-]*)", clean)), key=str.casefold):
            if variable.casefold() not in declared:
                result["diagnostics"].append(diagnostic(
                    "KETHER_IMPLICIT_VARIABLE_UNKNOWN", "warning", f"{label} 使用无法确认的隐式变量 {variable}",
                    suggestion="在 context.variables 或 request.variables 中声明", pointer=label,
                ))
        for token in _action_tokens(script):
            action = known.get(token)
            if schema is not None and token not in known and token not in _BASE_ACTIONS:
                result["diagnostics"].append(diagnostic(
                    "KETHER_ACTION_UNKNOWN", "warning", f"{label} 使用 schema 中未知的 action token: {token}",
                    suggestion="确认命名空间、别名或更新 actions-schema.json", pointer=label,
                ))
            execution = action.get("execution", {}) if isinstance(action, Mapping) else {}
            thread = str(execution.get("thread", "any")).casefold() if isinstance(execution, Mapping) else "any"
            if async_context and (thread in {"main", "sync", "bukkit"} or token in _THREAD_SENSITIVE):
                result["diagnostics"].append(diagnostic(
                    "KETHER_THREAD_RISK", "warning", f"异步上下文中的 action {token} 可能访问 Bukkit 主线程敏感 API",
                    suggestion="使用 sync 包裹主线程操作或将 Station Async 设为 false", pointer=label,
                ))
    result["checks"].append(check("KETHER_SCRIPT_SCANNED", "pass", f"已扫描 {len(scripts)} 段脚本"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return validate_kether(contract)
