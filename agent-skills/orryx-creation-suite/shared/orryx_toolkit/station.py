"""Station 中转站 YAML 生成器。"""
from __future__ import annotations

from typing import Any, Mapping

from .contracts import artifact, check, diagnostic, empty_result, reference, requirement
from .kether import validate_kether
from .yaml_io import literal, stable_dump

_PRIORITIES = {"LOWEST", "LOW", "NORMAL", "HIGH", "HIGHEST", "MONITOR"}


def generate_station(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    request = contract.get("request", {})
    key = str(request.get("key", request.get("id", "station"))).strip()
    event = str(request.get("event", "")).strip()
    actions = request.get("actions")
    if not key or "/" in key or "\\" in key:
        result["diagnostics"].append(diagnostic("STATION_KEY_INVALID", "error", f"非法 Station key: {key}", suggestion="使用不含路径分隔符的 key"))
        return result
    if not event:
        result["diagnostics"].append(diagnostic("STATION_EVENT_MISSING", "error", "Station 必须提供 event", suggestion="填写 Orryx 触发器名称"))
    if not isinstance(actions, str) or not actions.strip():
        result["diagnostics"].append(diagnostic("STATION_ACTIONS_MISSING", "error", "Station 必须提供 actions", suggestion="填写 request.actions Kether 脚本"))
        actions = "true"
    priority = str(request.get("priority", "NORMAL")).upper()
    if priority not in _PRIORITIES:
        result["diagnostics"].append(diagnostic(
            "STATION_PRIORITY_INVALID", "error", f"非法 Priority: {priority}", suggestion="使用 LOWEST/LOW/NORMAL/HIGH/HIGHEST/MONITOR",
        ))
    options: dict[str, Any] = {
        "Event": event,
        "Weight": int(request.get("weight", 0)),
        "Priority": priority,
        "IgnoreCancelled": bool(request.get("ignoreCancelled", False)),
        "Async": bool(request.get("async", False)),
    }
    if "baffleAction" in request:
        options["BaffleAction"] = literal(str(request["baffleAction"]))
    variables = request.get("variables", {})
    if isinstance(variables, Mapping) and variables:
        options["Variables"] = {
            str(name): literal(value) if isinstance(value, str) else value
            for name, value in sorted(variables.items(), key=lambda item: str(item[0]).casefold())
        }
    path = f"stations/{key}.yml"
    result["artifacts"].append(artifact(path, stable_dump({"Options": options, "Actions": literal(str(actions))}), metadata={"component": "station"}))
    skill = request.get("skill") or request.get("passiveSkill")
    if skill:
        result["references"].append(reference(path, f"skills/{skill}.yml", "station-skill"))
    if options["Async"]:
        result["requirements"].append(requirement(
            "STATION_ASYNC_THREAD_SAFETY", "异步 Station 脚本不得直接访问 Bukkit 主线程敏感 API", component="station",
        ))
    scripts = {"Actions": str(actions)}
    if "baffleAction" in request:
        scripts["BaffleAction"] = str(request["baffleAction"])
    kether_contract = dict(contract)
    kether_request = dict(request)
    kether_request.update({"scripts": scripts, "context": {"type": "station", "async": options["Async"], "variables": variables}})
    kether_contract["request"] = kether_request
    checked = validate_kether(kether_contract)
    result["diagnostics"].extend(checked["diagnostics"])
    result["checks"].extend(checked["checks"])
    result["checks"].append(check("STATION_YAML_GENERATED", "pass", f"已生成 Station {key}"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return generate_station(contract)
