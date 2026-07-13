"""五种 Orryx 技能 YAML 生成器。"""
from __future__ import annotations

from typing import Any, Mapping

from .contracts import artifact, check, diagnostic, empty_result, reference, requirement
from .kether import validate_kether
from .yaml_io import literal, stable_dump

_TYPE_ALIASES = {
    "PASSIVE": "PASSIVE",
    "DIRECT": "DIRECT",
    "DIRECT AIM": "DIRECT AIM",
    "DIRECT_AIM": "DIRECT AIM",
    "AIM": "DIRECT AIM",
    "PRESSING": "PRESSING",
    "PRESS": "PRESSING",
    "CHARGE": "PRESSING",
    "PRESSING AIM": "PRESSING AIM",
    "PRESSING_AIM": "PRESSING AIM",
    "CHARGE_AIM": "PRESSING AIM",
}
_TYPE_FIELDS = {
    "DIRECT AIM": ("AimSizeAction", "AimRadiusAction"),
    "PRESSING": ("PressBrockTriggers", "Period", "PressPeriodAction", "MaxPressTickAction"),
    "PRESSING AIM": (
        "AimMinAction", "AimMaxAction", "AimRadiusAction", "PressBrockTriggers",
        "Period", "PressPeriodAction", "MaxPressTickAction",
    ),
}


def _key(request: Mapping[str, Any]) -> str:
    return str(request.get("key", request.get("id", request.get("name", "ability")))).strip()


def _valid_key(key: str) -> bool:
    return bool(key) and "/" not in key and "\\" not in key and key not in {".", ".."}


def generate_ability(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    request = contract.get("request", {})
    key = _key(request)
    if not _valid_key(key):
        result["diagnostics"].append(diagnostic(
            "ABILITY_KEY_INVALID", "error", f"非法技能 key: {key}", suggestion="使用不含路径分隔符的文件名 key",
        ))
        return result
    raw_type = str(request.get("type", "DIRECT")).strip().upper().replace("-", "_")
    skill_type = _TYPE_ALIASES.get(raw_type)
    if skill_type is None:
        result["diagnostics"].append(diagnostic(
            "ABILITY_TYPE_INVALID", "error", f"未知技能类型: {raw_type}",
            suggestion="使用 PASSIVE/DIRECT/DIRECT_AIM/PRESSING/PRESSING_AIM",
        ))
        return result

    options: dict[str, Any] = {
        "Type": skill_type,
        "Name": str(request.get("name", key)),
        "Sort": int(request.get("sort", 0)),
        "Icon": str(request.get("icon", request.get("name", key))),
        "XMaterial": str(request.get("xMaterial", "BLAZE_ROD")),
        "Description": [str(value) for value in request.get("description", [])],
        "IsLocked": bool(request.get("isLocked", False)),
        "MinLevel": int(request.get("minLevel", 1)),
        "MaxLevel": int(request.get("maxLevel", 5)),
    }
    optional = {
        "UpgradePointAction": "upgradePointAction",
        "UpLevelCheckAction": "upLevelCheckAction",
        "DownLevelCheckAction": "downLevelCheckAction",
        "UpLevelSuccessAction": "upLevelSuccessAction",
        "DownLevelSuccessAction": "downLevelSuccessAction",
        "CastCheckAction": "castCheckAction",
        "IgnoreSilence": "ignoreSilence",
    }
    for yaml_key, request_key in optional.items():
        if request_key in request:
            value = request[request_key]
            options[yaml_key] = literal(value) if isinstance(value, str) else value
    for field in _TYPE_FIELDS.get(skill_type, ()):
        camel = field[0].lower() + field[1:]
        if camel in request:
            value = request[camel]
            options[field] = literal(value) if isinstance(value, str) else value
    variables = request.get("variables", {})
    if isinstance(variables, Mapping) and variables:
        options["Variables"] = {
            str(name): literal(value) if isinstance(value, str) else value
            for name, value in sorted(variables.items(), key=lambda item: str(item[0]).casefold())
        }

    data: dict[str, Any] = {"Options": options}
    actions = request.get("actions")
    if skill_type != "PASSIVE":
        if not isinstance(actions, str) or not actions.strip():
            result["diagnostics"].append(diagnostic(
                "ABILITY_ACTIONS_MISSING", "error", f"{skill_type} 技能必须提供 actions", suggestion="填写 request.actions Kether 脚本",
            ))
        else:
            data["Actions"] = literal(actions)
    elif isinstance(actions, str) and actions.strip():
        data["Actions"] = literal(actions)

    extend = request.get("extendActions", {})
    if isinstance(extend, Mapping) and extend:
        data["ExtendActions"] = {
            str(name): literal(str(script))
            for name, script in sorted(extend.items(), key=lambda item: str(item[0]).casefold())
        }

    policy = contract.get("policy", {})
    policy_version = policy.get("minecraftVersion", policy.get("serverVersion", "")) if isinstance(policy, Mapping) else ""
    version = str(request.get("minecraftVersion", request.get("serverVersion", policy_version)))
    if "AIM" in skill_type and version and version != "1.12.2":
        result["diagnostics"].append(diagnostic(
            "ABILITY_AIM_VERSION_UNSUPPORTED", "error", f"Aim 技能仅支持 1.12.2，当前为 {version}",
            suggestion="切换到 Minecraft 1.12.2 或使用非 Aim 类型",
        ))
    if skill_type == "PRESSING AIM" and any(field in options for field in ("Period", "PressPeriodAction", "PressBrockTriggers")):
        result["diagnostics"].append(diagnostic(
            "ABILITY_PRESSING_AIM_FIELDS_UNUSED", "warning",
            "当前 PressingAimSkillCaster 不消费 Period、PressPeriodAction 或 PressBrockTriggers",
            suggestion="不要依赖这些字段实现周期蓄力或打断；先确认并扩展源码 Caster",
        ))

    path = f"skills/{key}.yml"
    result["artifacts"].append(artifact(path, stable_dump(data), metadata={"component": "ability", "type": skill_type}))
    if skill_type == "PASSIVE":
        station_spec = request.get("station")
        station_key = request.get("stationKey")
        if isinstance(station_spec, Mapping):
            station_key = str(station_spec.get("key", station_spec.get("id", f"{key}机制")))
            from .station import generate_station
            station_contract = dict(contract)
            station_request = dict(station_spec)
            station_request.setdefault("key", station_key)
            station_request.setdefault("skill", key)
            station_contract["request"] = station_request
            station_result = generate_station(station_contract)
            for field in ("artifacts", "references", "requirements", "diagnostics", "checks"):
                result[field].extend(station_result[field])
        elif isinstance(station_spec, str) and station_spec.strip():
            station_key = station_spec.strip()
            if request.get("stationEvent") or request.get("stationActions"):
                from .station import generate_station
                station_contract = dict(contract)
                station_contract["request"] = {
                    "key": station_key,
                    "event": str(request.get("stationEvent", "Player Damage Pre")),
                    "actions": str(request.get("stationActions", "true")),
                    "async": bool(request.get("stationAsync", False)),
                    "priority": str(request.get("stationPriority", "NORMAL")),
                    "skill": key,
                }
                station_result = generate_station(station_contract)
                for field in ("artifacts", "references", "requirements", "diagnostics", "checks"):
                    result[field].extend(station_result[field])
        if station_key:
            result["references"].append(reference(path, f"stations/{station_key}.yml", "passive-station"))
        result["requirements"].append(requirement(
            "PASSIVE_STATION_REQUIRED", "被动技能需要 Station 或其他事件入口触发效果", component="station",
            details={"skill": key, "station": str(station_key or "")},
        ))

    scripts = {}
    if isinstance(actions, str) and actions.strip():
        scripts["Actions"] = actions
    if isinstance(extend, Mapping):
        scripts.update({f"ExtendActions.{name}": value for name, value in extend.items() if isinstance(value, str)})
    if scripts:
        kether_contract = dict(contract)
        kether_request = dict(request)
        kether_request.update({"scripts": scripts, "context": "ability", "skillType": skill_type})
        kether_contract["request"] = kether_request
        checked = validate_kether(kether_contract)
        result["diagnostics"].extend(checked["diagnostics"])
        result["checks"].extend(checked["checks"])
    result["checks"].append(check("ABILITY_YAML_GENERATED", "pass", f"已生成 {skill_type} 技能 {key}"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return generate_ability(contract)
