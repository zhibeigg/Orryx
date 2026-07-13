"""组件分派、bundle 编排与统一结果封装。"""
from __future__ import annotations

from pathlib import Path
from typing import Any, Callable, Mapping

from . import ability, combat, job, kether, materialize, progression, selector, station, ui, validation
from .contracts import (
    ContractError, canonicalize_result, diagnostic, empty_result, finalize_result,
    has_errors, merge_results, normalize_contract,
)
from .workspace import PathEscapeError, safe_join, workspace_root

Runner = Callable[[Mapping[str, Any]], dict[str, Any]]
_RUNNERS: dict[str, Runner] = {
    "validator": validation.run,
    "kether": kether.run,
    "ability": ability.run,
    "progression": progression.run,
    "job": job.run,
    "station": station.run,
    "combat": combat.run,
    "selector": selector.run,
    "ui": ui.run,
    "materialize": materialize.materialize,
}
_DEPENDENCY_ORDER = {
    name: index for index, name in enumerate(
        ("validator", "kether", "ability", "progression", "job", "station", "combat", "selector", "ui", "materialize")
    )
}


def _validate_references(contract: Mapping[str, Any], result: dict[str, Any]) -> None:
    artifact_paths = {str(item.get("path", "")).replace("\\", "/").casefold() for item in result["artifacts"]}
    root = workspace_root(contract.get("workspace", {}))
    for item in result["references"]:
        if not item.get("required", True):
            continue
        target = str(item.get("target", "")).replace("\\", "/")
        exists = target.casefold() in artifact_paths
        if not exists:
            try:
                exists = safe_join(root, target).is_file()
            except PathEscapeError:
                exists = False
        if not exists:
            result["diagnostics"].append(diagnostic(
                "REFERENCE_TARGET_MISSING", "error", f"引用目标不存在: {target}",
                suggestion="在 bundle 中生成目标 artifact，或在 workspace 中提供对应文件", path=str(item.get("source", "")),
                details={"kind": item.get("kind", ""), "target": target},
            ))


def _duplicate_artifacts(result: dict[str, Any]) -> None:
    seen: set[str] = set()
    for item in result["artifacts"]:
        path = str(item.get("path", ""))
        key = path.casefold()
        if key in seen:
            result["diagnostics"].append(diagnostic(
                "BUNDLE_ARTIFACT_CONFLICT", "error", f"bundle 生成了重复 artifact: {path}",
                suggestion="调整 step key 或移除重复生成步骤", path=path,
            ))
        seen.add(key)


def run_orchestrator(contract: Mapping[str, Any]) -> dict[str, Any]:
    request = contract.get("request", {})
    steps = request.get("steps")
    result = empty_result()
    if not isinstance(steps, list) or not steps:
        result["diagnostics"].append(diagnostic(
            "ORCHESTRATOR_STEPS_MISSING", "error", "request.steps 必须是非空数组",
            suggestion="按 validator→kether→ability→progression→job→station→combat→selector→ui 提供步骤",
        ))
        return result
    fragments = []
    validated_steps: list[tuple[int, int, str, Runner, Mapping[str, Any]]] = []
    for index, step in enumerate(steps):
        if not isinstance(step, Mapping):
            result["diagnostics"].append(diagnostic(
                "ORCHESTRATOR_STEP_INVALID", "error", f"steps[{index}] 必须是 object", suggestion="提供 component 与 request",
            ))
            continue
        component = str(step.get("component", "")).casefold()
        if component == "orchestrator":
            result["diagnostics"].append(diagnostic(
                "ORCHESTRATOR_RECURSION", "error", f"steps[{index}] 不允许递归 orchestrator", suggestion="展开为具体组件步骤",
            ))
            continue
        runner = _RUNNERS.get(component)
        if runner is None:
            result["diagnostics"].append(diagnostic(
                "ORCHESTRATOR_COMPONENT_UNKNOWN", "error", f"steps[{index}] 未知 component: {component}", suggestion="使用受支持组件",
            ))
            continue
        step_request = step.get("request", {})
        if not isinstance(step_request, Mapping):
            result["diagnostics"].append(diagnostic(
                "ORCHESTRATOR_REQUEST_INVALID", "error", f"steps[{index}].request 必须是 object", suggestion="修正步骤输入",
            ))
            continue
        step_operation = str(step.get("operation", "validate" if component == "validator" else "generate")).casefold()
        if step_operation not in {"generate", "validate", "plan", "materialize"}:
            result["diagnostics"].append(diagnostic(
                "ORCHESTRATOR_OPERATION_INVALID", "error", f"steps[{index}] 使用未知 operation: {step_operation}",
                suggestion="使用 generate/validate/plan/materialize",
            ))
            continue
        validated_steps.append((_DEPENDENCY_ORDER[component], index, component, runner, step))
    for _, index, component, runner, step in sorted(validated_steps):
        step_request = dict(step.get("request", {}))
        step_contract = {
            "contractVersion": contract["contractVersion"],
            "component": component,
            "operation": str(step.get("operation", "validate" if component == "validator" else "generate")),
            "workspace": step.get("workspace", contract.get("workspace", {})),
            "request": step_request,
            "policy": {**contract.get("policy", {}), **(step.get("policy", {}) if isinstance(step.get("policy", {}), Mapping) else {})},
        }
        if component != "materialize":
            fragments.append(runner(step_contract))
            continue
        if str(contract.get("operation", "generate")) != "materialize":
            result["diagnostics"].append(diagnostic(
                "ORCHESTRATOR_MATERIALIZE_OPERATION_REQUIRED", "error", "materialize 步骤要求顶层 operation=materialize",
                suggestion="显式选择 materialize 操作，并优先使用 staging workspace",
            ))
            continue
        preflight = merge_results([result, *fragments])
        _duplicate_artifacts(preflight)
        _validate_references(contract, preflight)
        if has_errors(preflight):
            fragments = [preflight]
            result = empty_result()
            result["diagnostics"].append(diagnostic(
                "ORCHESTRATOR_MATERIALIZE_BLOCKED", "warning", "前序或发布预检存在阻断错误，已跳过 materialize 步骤",
                suggestion="修复重复路径、缺失引用和其他 error 后重新执行",
            ))
            continue
        if "artifacts" not in step_request and "result" not in step_request:
            step_request["artifacts"] = preflight["artifacts"]
        materialized = runner(step_contract)
        written = {str(item.get("path", "")).casefold() for item in materialized.get("artifacts", [])}
        if written:
            preflight["artifacts"] = [
                item for item in preflight["artifacts"] if str(item.get("path", "")).casefold() not in written
            ]
        fragments = [preflight, materialized]
        result = empty_result()
    fragments.append(result)
    merged = merge_results(fragments)
    _duplicate_artifacts(merged)
    _validate_references(contract, merged)
    return canonicalize_result(merged)


def dispatch(contract: Mapping[str, Any]) -> dict[str, Any]:
    component = str(contract.get("component", "")).casefold()
    if component == "orchestrator":
        return run_orchestrator(contract)
    if component == "materialize":
        result = empty_result()
        result["diagnostics"].append(diagnostic(
            "MATERIALIZE_CLI_REQUIRED", "error", "materialize 仅允许通过显式 materialize 子命令或受控编排步骤执行",
            suggestion="使用 CLI materialize，并将 workspace.root 指向 staging 目录",
        ))
        return result
    runner = _RUNNERS.get(component)
    if runner is None:
        result = empty_result()
        result["diagnostics"].append(diagnostic(
            "COMPONENT_UNSUPPORTED", "error", f"run 不支持 component: {component}", suggestion="使用受支持的 run component",
        ))
        return result
    result = runner(contract)
    _duplicate_artifacts(result)
    if bool(contract.get("policy", {}).get("validateReferences", True)):
        _validate_references(contract, result)
    return canonicalize_result(result)


def run_contract(value: Mapping[str, Any]) -> dict[str, Any]:
    contract = normalize_contract(value)
    try:
        result = dispatch(contract)
    except (ValueError, TypeError, ArithmeticError, OSError) as exc:
        result = empty_result()
        result["diagnostics"].append(diagnostic(
            "COMPONENT_EXECUTION_FAILED", "error", f"组件执行失败: {exc}",
            suggestion="检查 request 字段类型、数值范围与工作区可读性",
            details={"exception": type(exc).__name__},
        ))
    return finalize_result(contract, result)
