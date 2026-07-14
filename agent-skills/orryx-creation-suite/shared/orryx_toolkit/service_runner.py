"""私有服务调用边界：只接受只读/候选生成合同，并注入可信工作区。"""
from __future__ import annotations

from copy import deepcopy
import json
from pathlib import Path
from typing import Any, Mapping

from .contracts import ContractError
from .orchestrator import run_contract

SERVICE_ENVELOPE_VERSION = "1.0"
SERVICE_OPERATIONS = frozenset({"generate", "validate", "plan"})
SERVICE_ERROR_CODES = frozenset({
    "SERVICE_ACTIONS_SCHEMA_FORBIDDEN",
    "SERVICE_ACTIONS_SCHEMA_PATH_FORBIDDEN",
    "SERVICE_COMPONENT_MATERIALIZE_FORBIDDEN",
    "SERVICE_CONTEXT_INVALID",
    "SERVICE_CONTRACT_FIELD_UNKNOWN",
    "SERVICE_CONTRACT_INVALID",
    "SERVICE_CONTRACT_MISSING",
    "SERVICE_CONTRACT_NOT_OBJECT",
    "SERVICE_ENVELOPE_FIELD_UNKNOWN",
    "SERVICE_ENVELOPE_NOT_OBJECT",
    "SERVICE_ENVELOPE_VERSION_UNSUPPORTED",
    "SERVICE_EXECUTION_FAILED",
    "SERVICE_OPERATION_FORBIDDEN",
    "SERVICE_NETWORK_ALLOW_FORBIDDEN",
    "SERVICE_OPERATION_UNSUPPORTED",
    "SERVICE_OVERWRITE_ALLOW_FORBIDDEN",
    "SERVICE_POLICY_MATERIALIZE_FORBIDDEN",
    "SERVICE_RELOAD_SERVER_FORBIDDEN",
    "SERVICE_STRICT_REQUIRED",
    "SERVICE_WORKSPACE_FORBIDDEN",
})

_ALLOWED_ENVELOPE_FIELDS = frozenset({"envelopeVersion", "contract"})
_ALLOWED_CONTRACT_FIELDS = frozenset({"$schema", "contractVersion", "component", "operation", "workspace", "request", "policy"})
_ACTION_SCHEMA_COMPONENTS = frozenset({"ability", "job", "kether", "station", "combat", "selector"})


def _pointer(parent: str, key: object) -> str:
    token = str(key).replace("~", "~0").replace("/", "~1")
    return f"{parent}/{token}" if parent else f"/{token}"


def _error(code: str, pointer: str, message: str) -> dict[str, str]:
    return {"code": code, "pointer": pointer, "message": message}


def _response(*, result: Mapping[str, Any] | None = None, errors: list[dict[str, str]] | None = None) -> dict[str, Any]:
    ordered_errors = sorted(errors or [], key=lambda item: (item["pointer"], item["code"], item["message"]))
    return {
        "envelopeVersion": SERVICE_ENVELOPE_VERSION,
        "status": "rejected" if ordered_errors else "completed",
        "result": deepcopy(dict(result)) if result is not None else None,
        "errors": ordered_errors,
    }


def _allows_overwrite(value: Any) -> bool:
    return value is True or str(value).casefold() in {"allow", "true", "overwrite"}


def _boundary_errors(value: Any, pointer: str = "", parent_key: str = "") -> list[dict[str, str]]:
    errors: list[dict[str, str]] = []
    if isinstance(value, Mapping):
        for raw_key in sorted(value, key=lambda item: str(item)):
            key = str(raw_key)
            child_pointer = _pointer(pointer, key)
            child = value[raw_key]
            if key == "workspace":
                errors.append(_error(
                    "SERVICE_WORKSPACE_FORBIDDEN",
                    child_pointer,
                    "公开服务合同不得提供 workspace；工作区根目录只能由可信服务配置注入",
                ))
            elif key == "actionsSchemaPath":
                errors.append(_error(
                    "SERVICE_ACTIONS_SCHEMA_PATH_FORBIDDEN",
                    child_pointer,
                    "公开服务合同不得通过 actionsSchemaPath 读取文件",
                ))
            elif key == "actionsSchema":
                errors.append(_error(
                    "SERVICE_ACTIONS_SCHEMA_FORBIDDEN",
                    child_pointer,
                    "公开服务合同不得提供 actionsSchema；可信 Schema 只能由服务宿主注入",
                ))
            elif key == "reloadServer":
                errors.append(_error(
                    "SERVICE_RELOAD_SERVER_FORBIDDEN",
                    child_pointer,
                    "公开服务合同不得请求服务器重载",
                ))
            elif key == "materialize" and parent_key == "policy":
                errors.append(_error(
                    "SERVICE_POLICY_MATERIALIZE_FORBIDDEN",
                    child_pointer,
                    "公开服务合同不得提供 policy.materialize",
                ))
            elif key == "network" and parent_key == "policy" and str(child).casefold() != "deny":
                errors.append(_error(
                    "SERVICE_NETWORK_ALLOW_FORBIDDEN",
                    child_pointer,
                    "私有服务入口只接受 policy.network=deny",
                ))
            elif key == "strict" and parent_key == "policy" and child is not True:
                errors.append(_error(
                    "SERVICE_STRICT_REQUIRED",
                    child_pointer,
                    "私有服务入口要求 policy.strict=true",
                ))
            elif key == "overwrite" and _allows_overwrite(child):
                errors.append(_error(
                    "SERVICE_OVERWRITE_ALLOW_FORBIDDEN",
                    child_pointer,
                    "公开服务合同不得允许覆盖文件",
                ))
            elif key == "operation":
                operation = str(child).strip().casefold()
                if operation == "materialize":
                    errors.append(_error(
                        "SERVICE_OPERATION_FORBIDDEN",
                        child_pointer,
                        "私有服务入口不接受 materialize operation",
                    ))
                elif operation not in SERVICE_OPERATIONS:
                    errors.append(_error(
                        "SERVICE_OPERATION_UNSUPPORTED",
                        child_pointer,
                        "私有服务入口只接受 generate、validate 或 plan operation",
                    ))
            elif key == "component" and str(child).strip().casefold() == "materialize":
                errors.append(_error(
                    "SERVICE_COMPONENT_MATERIALIZE_FORBIDDEN",
                    child_pointer,
                    "私有服务入口不接受 materialize component",
                ))
            errors.extend(_boundary_errors(child, child_pointer, key))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            errors.extend(_boundary_errors(child, _pointer(pointer, index), parent_key))
    return errors


def _load_default_actions_schema() -> Mapping[str, Any]:
    path = Path(__file__).with_name("actions-schema.json")
    with path.open("r", encoding="utf-8-sig") as stream:
        value = json.load(stream)
    if not isinstance(value, Mapping):
        raise ValueError("内置 actions-schema.json 必须是 JSON object")
    return value


def _inject_actions_schema(value: Any, schema: Mapping[str, Any]) -> Any:
    if isinstance(value, Mapping):
        copied = {str(key): _inject_actions_schema(child, schema) for key, child in value.items()}
        component = str(copied.get("component", "")).strip().casefold()
        request = copied.get("request")
        if component in _ACTION_SCHEMA_COMPONENTS and isinstance(request, Mapping):
            request_copy = dict(request)
            request_copy["actionsSchema"] = deepcopy(dict(schema))
            copied["request"] = request_copy
        return copied
    if isinstance(value, list):
        return [_inject_actions_schema(child, schema) for child in value]
    return deepcopy(value)


def run_service_request(
    envelope: Any,
    *,
    workspace_root: str | Path,
    workspace_mode: str = "project",
    actions_schema: Mapping[str, Any] | None = None,
) -> dict[str, Any]:
    """执行公开服务 envelope；可信路径与 Action Schema 必须通过关键字参数注入。"""
    if not isinstance(envelope, Mapping):
        return _response(errors=[_error(
            "SERVICE_ENVELOPE_NOT_OBJECT",
            "",
            "service runner envelope 必须是 JSON object",
        )])

    errors: list[dict[str, str]] = []
    version = str(envelope.get("envelopeVersion", ""))
    if version != SERVICE_ENVELOPE_VERSION:
        errors.append(_error(
            "SERVICE_ENVELOPE_VERSION_UNSUPPORTED",
            "/envelopeVersion",
            f"不支持 service envelopeVersion: {version or '<missing>'}",
        ))
    for key in sorted(set(envelope) - _ALLOWED_ENVELOPE_FIELDS, key=str):
        errors.append(_error(
            "SERVICE_ENVELOPE_FIELD_UNKNOWN",
            _pointer("", key),
            f"未知 service envelope 字段: {key}",
        ))

    if "contract" not in envelope:
        errors.append(_error("SERVICE_CONTRACT_MISSING", "/contract", "缺少公开 service contract"))
        return _response(errors=errors)
    public_contract = envelope.get("contract")
    if not isinstance(public_contract, Mapping):
        errors.append(_error("SERVICE_CONTRACT_NOT_OBJECT", "/contract", "service contract 必须是 JSON object"))
        return _response(errors=errors)

    errors.extend(_boundary_errors(public_contract, "/contract"))
    for key in sorted(set(public_contract) - _ALLOWED_CONTRACT_FIELDS, key=str):
        errors.append(_error(
            "SERVICE_CONTRACT_FIELD_UNKNOWN",
            _pointer("/contract", key),
            f"公开 service contract 不支持字段: {key}",
        ))
    for key in ("contractVersion", "component", "operation", "request", "policy"):
        if key not in public_contract:
            errors.append(_error(
                "SERVICE_CONTRACT_INVALID",
                _pointer("/contract", key),
                f"公开 service contract 缺少必填字段: {key}",
            ))
    if "request" in public_contract and not isinstance(public_contract.get("request"), Mapping):
        errors.append(_error("SERVICE_CONTRACT_INVALID", "/contract/request", "request 必须是 object"))
    if "policy" in public_contract and not isinstance(public_contract.get("policy"), Mapping):
        errors.append(_error("SERVICE_CONTRACT_INVALID", "/contract/policy", "policy 必须是 object"))
    if errors:
        return _response(errors=errors)

    try:
        root = Path(workspace_root).expanduser().resolve()
        mode = str(workspace_mode).strip().casefold()
        if mode not in {"standalone", "project"}:
            raise ValueError("workspace_mode 必须是 standalone 或 project")
        trusted_schema = deepcopy(dict(actions_schema)) if isinstance(actions_schema, Mapping) else _load_default_actions_schema()
        if actions_schema is not None and not isinstance(actions_schema, Mapping):
            raise ValueError("actions_schema 必须是 object")
    except (OSError, TypeError, ValueError, json.JSONDecodeError):
        return _response(errors=[_error(
            "SERVICE_CONTEXT_INVALID",
            "",
            "可信服务配置无效；未返回主机路径或原始异常信息",
        )])

    trusted_contract = _inject_actions_schema(public_contract, trusted_schema)
    trusted_contract["workspace"] = {"root": str(root), "mode": mode}
    policy = dict(trusted_contract.get("policy", {}))
    policy["strict"] = True
    policy["network"] = "deny"
    policy["overwrite"] = "deny"
    trusted_contract["policy"] = policy
    try:
        result = run_contract(trusted_contract)
    except ContractError as exc:
        return _response(errors=[_error("SERVICE_CONTRACT_INVALID", "/contract", str(exc))])
    except Exception:
        return _response(errors=[_error(
            "SERVICE_EXECUTION_FAILED",
            "",
            "服务执行失败；未返回未结构化异常信息",
        )])
    return _response(result=result)


def run_service(
    envelope: Any,
    *,
    workspace_root: str | Path,
    workspace_mode: str = "project",
    actions_schema: Mapping[str, Any] | None = None,
) -> dict[str, Any]:
    """run_service_request 的稳定短别名。"""
    return run_service_request(
        envelope,
        workspace_root=workspace_root,
        workspace_mode=workspace_mode,
        actions_schema=actions_schema,
    )
