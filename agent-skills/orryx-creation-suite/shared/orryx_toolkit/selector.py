"""selectors.yml 后端生成器。"""
from __future__ import annotations

from typing import Any, Mapping

from .contracts import artifact, check, diagnostic, empty_result, requirement
from .kether import validate_kether
from .yaml_io import literal, stable_dump


def generate_selector(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    request = contract.get("request", {})
    raw = request.get("selectors")
    if raw is None:
        key = str(request.get("key", request.get("id", "selector")))
        raw = {key: {"Actions": request.get("actions", "container")}}
    if not isinstance(raw, Mapping) or not raw:
        result["diagnostics"].append(diagnostic(
            "SELECTOR_REQUEST_INVALID", "error", "selectors 必须是非空 object", suggestion="提供 selector key 与 Actions",
        ))
        return result
    data: dict[str, Any] = {}
    scripts: dict[str, str] = {}
    for key, config in sorted(raw.items(), key=lambda item: str(item[0]).casefold()):
        name = str(key)
        if not name or "/" in name or "\\" in name:
            result["diagnostics"].append(diagnostic(
                "SELECTOR_KEY_INVALID", "error", f"非法 selector key: {name}", suggestion="使用普通标识符",
            ))
            continue
        if isinstance(config, str):
            actions = config
            item = {"Actions": literal(actions)}
        elif isinstance(config, Mapping):
            actions = config.get("Actions", config.get("actions"))
            if not isinstance(actions, str):
                result["diagnostics"].append(diagnostic(
                    "SELECTOR_ACTIONS_MISSING", "error", f"selector {name} 缺少 Actions", suggestion="填写 Kether selector 脚本",
                ))
                continue
            item = {"Actions": literal(actions)}
            for extra_key, value in sorted(config.items(), key=lambda pair: str(pair[0]).casefold()):
                if str(extra_key) not in {"Actions", "actions"}:
                    item[str(extra_key)] = value
        else:
            result["diagnostics"].append(diagnostic(
                "SELECTOR_CONFIG_INVALID", "error", f"selector {name} 配置必须是 string/object", suggestion="使用 Actions 字段",
            ))
            continue
        data[name] = item
        scripts[name] = actions
    result["artifacts"].append(artifact("selectors.yml", stable_dump(data), metadata={"component": "selector"}))
    result["requirements"].append(requirement(
        "SELECTOR_ARGUMENTS_RUNTIME", "selector 脚本中的 &v0、&v1 等参数由调用方按顺序提供", component="selector",
    ))
    if scripts:
        kether_contract = dict(contract)
        kether_request = dict(request)
        implicit = {f"v{index}": True for index in range(int(request.get("argumentCount", 8)))}
        kether_request.update({"scripts": scripts, "context": {"type": "selector", "variables": implicit}})
        kether_contract["request"] = kether_request
        checked = validate_kether(kether_contract)
        result["diagnostics"].extend(checked["diagnostics"])
        result["checks"].extend(checked["checks"])
    result["checks"].append(check("SELECTOR_YAML_GENERATED", "pass", f"已生成 {len(data)} 个 selector"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return generate_selector(contract)
