"""Status + Controller 战斗配置生成器。"""
from __future__ import annotations

from typing import Any, Mapping

from .contracts import artifact, check, diagnostic, empty_result, reference, requirement
from .kether import validate_kether
from .yaml_io import literal, stable_dump

_SCRIPT_KEYS = {"Action", "BlockAction", "Condition"}


def _convert(value: Any, key: str = "") -> Any:
    if isinstance(value, Mapping):
        return {str(k): _convert(v, str(k)) for k, v in sorted(value.items(), key=lambda item: str(item[0]).casefold())}
    if isinstance(value, list):
        return [_convert(item) for item in value]
    if isinstance(value, str) and ("\n" in value or key in _SCRIPT_KEYS):
        return literal(value)
    return value


def _valid_tick_range(value: Any, *, allow_single: bool) -> bool:
    if isinstance(value, int) and not isinstance(value, bool):
        return allow_single and value >= 0
    if not isinstance(value, str):
        return False
    parts = value.split("-")
    if len(parts) == 1:
        return allow_single and parts[0].strip().isdigit()
    if len(parts) != 2 or not all(part.strip().isdigit() for part in parts):
        return False
    start, end = (int(part.strip()) for part in parts)
    return 0 <= start <= end


def generate_combat(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    request = contract.get("request", {})
    status_request = request.get("status", request)
    controller_request = request.get("controller", {})
    if not isinstance(status_request, Mapping) or not isinstance(controller_request, Mapping):
        result["diagnostics"].append(diagnostic(
            "COMBAT_REQUEST_INVALID", "error", "status 与 controller 必须是 object", suggestion="提供 request.status/request.controller",
        ))
        return result
    status_key = str(status_request.get("key", request.get("key", "combat"))).strip()
    controller_key = str(controller_request.get("key", status_request.get("controller", f"{status_key}-controller"))).strip()
    if any(not key or "/" in key or "\\" in key for key in (status_key, controller_key)):
        result["diagnostics"].append(diagnostic(
            "COMBAT_KEY_INVALID", "error", "Status/Controller key 不得为空或包含路径分隔符", suggestion="使用安全文件名 key",
        ))
        return result

    options = {
        "Condition": literal(str(status_request.get("condition", "true"))),
        "CancelHeldEventWhenPlaying": bool(status_request.get("cancelHeldEventWhenPlaying", True)),
        "Controller": controller_key,
        "CancelBukkitAttack": bool(status_request.get("cancelBukkitAttack", False)),
        "Armourers": list(status_request.get("armourers", [])),
        "AttackSpeed": literal(status_request.get("attackSpeed")) if isinstance(status_request.get("attackSpeed"), str) else status_request.get("attackSpeed", 1.0),
        "AnimationState": str(status_request.get("animationState", status_key)),
    }
    states = status_request.get("states", {})
    if not isinstance(states, Mapping) or not states:
        result["diagnostics"].append(diagnostic(
            "COMBAT_STATES_MISSING", "error", "Status 至少需要一个 state", suggestion="填写 request.status.states",
        ))
        states = {}
    for state_name, state in states.items():
        if not isinstance(state, Mapping):
            result["diagnostics"].append(diagnostic(
                "COMBAT_STATE_INVALID", "error", f"状态 {state_name} 必须是 object", suggestion="提供 Type、Animation 和 Action",
            ))
            continue
        state_type = str(state.get("Type", "")).casefold()
        if state_type not in {"general attack", "press attack", "block", "dodge", "vertigo"}:
            result["diagnostics"].append(diagnostic(
                "COMBAT_STATE_TYPE_INVALID", "error", f"状态 {state_name} 使用未知 Type: {state.get('Type', '')}",
                suggestion="使用 General Attack/Press Attack/Block/Dodge/Vertigo",
            ))
        for range_key in ("Connection", "Check"):
            if range_key in state and not _valid_tick_range(state[range_key], allow_single=False):
                result["diagnostics"].append(diagnostic(
                    "COMBAT_RANGE_INVALID", "error", f"状态 {state_name} 的 {range_key} 必须是 start-end 且 start<=end",
                    suggestion="例如 8-16", pointer=f"States.{state_name}.{range_key}",
                ))
        if "Invincible" in state and not _valid_tick_range(state["Invincible"], allow_single=True):
            result["diagnostics"].append(diagnostic(
                "COMBAT_RANGE_INVALID", "error", f"状态 {state_name} 的 Invincible 必须是非负整数或 start-end",
                suggestion="例如 4 或 0-10", pointer=f"States.{state_name}.Invincible",
            ))
    status_data: dict[str, Any] = {"Options": options, "States": _convert(states)}
    action = status_request.get("action")
    if isinstance(action, str):
        status_data["Action"] = literal(action)

    backend = str(controller_request.get("backend", request.get("backend", "dragoncore"))).lower()
    controller_config = controller_request.get("config", {})
    if not isinstance(controller_config, Mapping):
        controller_config = {}
    if "Layer" in controller_config or "Trigger" in controller_config:
        controller_data = _convert(controller_config)
    else:
        layers = controller_request.get("layers", {})
        triggers = controller_request.get("triggers", {})
        if not isinstance(layers, Mapping) or not layers:
            animations: set[str] = set()
            for state in states.values():
                if not isinstance(state, Mapping):
                    continue
                animation = state.get("Animation", {})
                if isinstance(animation, Mapping):
                    for animation_key, animation_value in animation.items():
                        if str(animation_key).casefold().endswith("duration"):
                            continue
                        if isinstance(animation_value, str) and animation_value:
                            animations.add(animation_value)
            layers = {
                "base": {
                    "blendType": "OVERRIDE",
                    "weight": 1,
                    "animations": sorted(animations, key=str.casefold),
                }
            }
        controller_data = {"Layer": _convert(layers), "Trigger": _convert(triggers)}
    status_path = f"status/{status_key}.yml"
    controller_path = f"controllers/{controller_key}.yml"
    result["artifacts"].extend([
        artifact(status_path, stable_dump(status_data), metadata={"component": "combat", "kind": "status"}),
        artifact(controller_path, stable_dump(controller_data), metadata={"component": "combat", "kind": "controller", "backend": backend}),
    ])
    result["references"].append(reference(status_path, controller_path, "status-controller"))
    if backend != "bukkit":
        result["requirements"].append(requirement(
            "COMBAT_BACKEND_PLUGIN_REQUIRED", f"Controller backend {backend} 需要对应客户端/插件支持", component="combat", details={"backend": backend},
        ))
    scripts: dict[str, str] = {"Condition": str(status_request.get("condition", "true"))}
    if isinstance(action, str):
        scripts["Action"] = action
    for state_name, state in states.items():
        if isinstance(state, Mapping):
            for key in _SCRIPT_KEYS:
                if isinstance(state.get(key), str):
                    scripts[f"States.{state_name}.{key}"] = state[key]
    kether_contract = dict(contract)
    kether_request = dict(request)
    kether_request.update({"scripts": scripts, "context": "state"})
    kether_contract["request"] = kether_request
    checked = validate_kether(kether_contract)
    result["diagnostics"].extend(checked["diagnostics"])
    result["checks"].extend(checked["checks"])
    result["checks"].append(check("COMBAT_YAML_GENERATED", "pass", f"已生成 Status {status_key} 与 Controller {controller_key}"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return generate_combat(contract)
