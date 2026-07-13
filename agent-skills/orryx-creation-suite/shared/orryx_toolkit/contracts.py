"""统一 JSON 合同与稳定输出模型。"""
from __future__ import annotations

from copy import deepcopy
import hashlib
import json
from typing import Any, Iterable, Mapping

CONTRACT_VERSION = "1.0"
SUITE_VERSION = "1.0.0"
COMPONENTS = {
    "validator", "kether", "ability", "progression", "job", "station",
    "combat", "selector", "ui", "orchestrator", "materialize",
}


class ContractError(ValueError):
    """输入合同无法规范化。"""


def normalize_contract(value: Mapping[str, Any]) -> dict[str, Any]:
    if not isinstance(value, Mapping):
        raise ContractError("输入必须是 JSON object")
    contract_version = str(value.get("contractVersion", CONTRACT_VERSION))
    if contract_version != CONTRACT_VERSION:
        raise ContractError(f"不支持 contractVersion: {contract_version}")
    component = str(value.get("component", "orchestrator")).strip().lower()
    operation = str(value.get("operation", "generate")).strip().lower()
    if component not in COMPONENTS:
        raise ContractError(f"未知 component: {component}")
    if operation not in {"generate", "validate", "plan", "materialize"}:
        raise ContractError(f"未知 operation: {operation}")
    workspace = value.get("workspace", {})
    request = value.get("request", {})
    policy = value.get("policy", {})
    if not isinstance(workspace, (str, Mapping)):
        raise ContractError("workspace 必须是路径字符串或 object")
    if not isinstance(request, Mapping):
        raise ContractError("request 必须是 object")
    if not isinstance(policy, Mapping):
        raise ContractError("policy 必须是 object")
    normalized_workspace = {"root": workspace, "mode": "project"} if isinstance(workspace, str) else deepcopy(dict(workspace))
    normalized_workspace.setdefault("root", ".")
    normalized_workspace.setdefault("mode", "standalone")
    normalized_policy = deepcopy(dict(policy))
    normalized_policy.setdefault("strict", True)
    normalized_policy.setdefault("network", "deny")
    normalized_policy.setdefault("overwrite", "deny")
    normalized_policy.setdefault("minecraftVersion", "")
    normalized_policy.setdefault("plugins", [])
    return {
        "contractVersion": contract_version,
        "component": component,
        "operation": operation,
        "workspace": normalized_workspace,
        "request": deepcopy(dict(request)),
        "policy": normalized_policy,
    }


def empty_result() -> dict[str, list[dict[str, Any]]]:
    return {
        "artifacts": [],
        "references": [],
        "requirements": [],
        "diagnostics": [],
        "checks": [],
    }


def diagnostic(code: str, severity: str, message: str, *, suggestion: str = "", path: str = "", pointer: str = "", details: Any = None) -> dict[str, Any]:
    item: dict[str, Any] = {
        "severity": str(severity).lower(),
        "code": str(code),
        "path": path.replace("\\", "/"),
        "pointer": str(pointer),
        "message": str(message),
        "suggestion": str(suggestion),
    }
    if details is not None:
        item["details"] = details
    return item


def check(code: str, status: str, message: str, *, details: Any = None) -> dict[str, Any]:
    item: dict[str, Any] = {"code": code, "status": status.lower(), "message": message}
    if details is not None:
        item["details"] = details
    return item


def artifact(path: str, content: str, *, kind: str = "yaml", metadata: Mapping[str, Any] | None = None) -> dict[str, Any]:
    normalized_path = path.replace("\\", "/")
    media_types = {
        "yaml": "application/yaml",
        "json": "application/json",
        "line-chart": "application/vnd.orryx.line-chart+json",
        "markdown": "text/markdown",
        "text": "text/plain",
    }
    item: dict[str, Any] = {
        "path": normalized_path,
        "kind": kind,
        "mediaType": media_types.get(kind, "text/plain"),
        "encoding": "utf-8",
        "content": content,
        "sha256": hashlib.sha256(content.encode("utf-8")).hexdigest(),
        "metadata": dict(metadata or {}),
    }
    return item


def reference(source: str, target: str, kind: str, *, required: bool = True) -> dict[str, Any]:
    return {
        "source": source.replace("\\", "/"),
        "target": target.replace("\\", "/"),
        "kind": kind,
        "required": bool(required),
    }


def requirement(code: str, message: str, *, component: str = "", details: Any = None) -> dict[str, Any]:
    item: dict[str, Any] = {"code": code, "message": message}
    if component:
        item["component"] = component
    if details is not None:
        item["details"] = details
    return item


def merge_results(results: Iterable[Mapping[str, Any]]) -> dict[str, Any]:
    merged = empty_result()
    for result in results:
        for key in merged:
            values = result.get(key, [])
            if isinstance(values, list):
                merged[key].extend(deepcopy(values))
    return canonicalize_result(merged)


def _sort_key(item: Mapping[str, Any]) -> tuple[str, ...]:
    return tuple(str(item.get(key, "")) for key in (
        "path", "source", "target", "component", "code", "severity", "status", "message"
    ))


def canonicalize_result(result: Mapping[str, Any]) -> dict[str, Any]:
    normalized = empty_result()
    for key in normalized:
        values = result.get(key, [])
        if not isinstance(values, list):
            raise ContractError(f"输出字段 {key} 必须是 list")
        normalized[key] = sorted((deepcopy(v) for v in values if isinstance(v, Mapping)), key=_sort_key)
    return normalized


def has_errors(result: Mapping[str, Any]) -> bool:
    return any(item.get("severity") == "error" for item in result.get("diagnostics", []))


def finalize_result(contract: Mapping[str, Any], result: Mapping[str, Any]) -> dict[str, Any]:
    body = canonicalize_result(result)
    canonical_input = json.dumps(contract, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    input_digest = hashlib.sha256(canonical_input.encode("utf-8")).hexdigest()
    error_count = sum(item.get("severity") == "error" for item in body["diagnostics"])
    warning_count = sum(item.get("severity") == "warning" for item in body["diagnostics"])
    status = "invalid" if error_count else "ok"
    artifacts_are_candidates = any(
        not isinstance(item.get("metadata"), Mapping) or not item.get("metadata", {}).get("materialized", False)
        for item in body["artifacts"]
    )
    next_steps: list[dict[str, Any]] = []
    if error_count:
        next_steps.append({
            "code": "FIX_ERRORS",
            "operation": "validate",
            "message": "修复 diagnostics 中的 error 后重新运行校验",
            "required": True,
        })
    if body["requirements"]:
        next_steps.append({
            "code": "VERIFY_REQUIREMENTS",
            "operation": "validate",
            "message": "人工或通过目标服务器确认 requirements 中的外部前置条件",
            "required": True,
        })
    if body["artifacts"] and not error_count and str(contract.get("operation", "generate")) != "materialize":
        next_steps.append({
            "code": "MATERIALIZE_CANDIDATES",
            "operation": "materialize",
            "message": "在 staging workspace 中显式物化候选 artifacts，再执行服务器侧预检",
            "required": False,
        })
    return {
        "contractVersion": str(contract.get("contractVersion", CONTRACT_VERSION)),
        "suiteVersion": SUITE_VERSION,
        "component": str(contract.get("component", "orchestrator")),
        "operation": str(contract.get("operation", "generate")),
        "status": status,
        "summary": {
            "message": "合同执行成功" if status == "ok" else "合同执行发现阻断错误",
            "artifactCount": len(body["artifacts"]),
            "errorCount": error_count,
            "warningCount": warning_count,
            "checkCount": len(body["checks"]),
        },
        "artifacts": body["artifacts"],
        "references": body["references"],
        "requirements": body["requirements"],
        "diagnostics": body["diagnostics"],
        "checks": body["checks"],
        "nextSteps": next_steps,
        "metadata": {
            "artifactsAreCandidates": artifacts_are_candidates,
            "writePolicy": "materialize-only",
            "networkPolicy": str(contract.get("policy", {}).get("network", "deny")),
            "strict": bool(contract.get("policy", {}).get("strict", True)),
        },
        "provenance": {
            "suiteVersion": SUITE_VERSION,
            "inputDigest": input_digest,
            "inputSha256": input_digest,
            "deterministic": True,
            "runtime": "shared/orryx_toolkit",
        },
    }


def invalid_result(raw: Any, message: str, code: str = "CONTRACT_INVALID") -> dict[str, Any]:
    base = raw if isinstance(raw, Mapping) else {}
    contract = {
        "contractVersion": str(base.get("contractVersion", CONTRACT_VERSION)),
        "component": str(base.get("component", "orchestrator")),
        "operation": str(base.get("operation", "generate")),
        "workspace": base.get("workspace", {}),
        "request": base.get("request", {}),
        "policy": base.get("policy", {}),
    }
    result = empty_result()
    result["diagnostics"].append(diagnostic(code, "error", message, suggestion="修正输入合同后重试"))
    return finalize_result(contract, result)
