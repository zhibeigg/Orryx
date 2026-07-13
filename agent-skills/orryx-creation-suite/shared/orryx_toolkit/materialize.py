"""显式 artifact 落盘，默认拒绝覆盖并使用同目录原子替换。"""
from __future__ import annotations

import hashlib
import os
from pathlib import Path
from typing import Any, Mapping

from .contracts import artifact, check, diagnostic, empty_result
from .workspace import PathEscapeError, safe_join, workspace_root


def _artifacts(contract: Mapping[str, Any]) -> list[Mapping[str, Any]]:
    request = contract.get("request", {})
    values = request.get("artifacts")
    if values is None and isinstance(request.get("result"), Mapping):
        values = request["result"].get("artifacts")
    if values is None:
        values = contract.get("artifacts")
    return [item for item in values if isinstance(item, Mapping)] if isinstance(values, list) else []


def materialize(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    root = workspace_root(contract.get("workspace", {}))
    policy = contract.get("policy", {})
    overwrite_value = policy.get("overwrite", "deny")
    overwrite = overwrite_value is True or str(overwrite_value).casefold() in {"allow", "true", "overwrite"}
    create_parents_value = policy.get("createParents", True)
    create_parents = create_parents_value is True or str(create_parents_value).casefold() in {"allow", "true", "yes"}
    values = _artifacts(contract)
    if not values:
        result["diagnostics"].append(diagnostic(
            "MATERIALIZE_ARTIFACTS_MISSING", "error", "未提供 artifacts", suggestion="将 run 输出的 artifacts 放入 request.artifacts",
        ))
        return result

    planned: list[tuple[Mapping[str, Any], Path]] = []
    seen: set[str] = set()
    for item in values:
        relative = item.get("path")
        content = item.get("content")
        if not isinstance(relative, str) or not isinstance(content, str):
            result["diagnostics"].append(diagnostic(
                "MATERIALIZE_ARTIFACT_INVALID", "error", "artifact 必须包含 string path/content",
                suggestion="使用 run 的原始 artifacts", pointer=str(relative or ""),
            ))
            continue
        expected_hash = str(item.get("sha256", ""))
        actual_hash = hashlib.sha256(content.encode("utf-8")).hexdigest()
        if expected_hash and expected_hash != actual_hash:
            result["diagnostics"].append(diagnostic(
                "MATERIALIZE_HASH_MISMATCH", "error", "artifact content 与 sha256 不一致",
                suggestion="重新运行计算阶段获得匹配 artifact", path=relative,
                details={"expected": expected_hash, "actual": actual_hash},
            ))
            continue
        try:
            target = safe_join(root, relative)
        except PathEscapeError as exc:
            result["diagnostics"].append(diagnostic(
                "MATERIALIZE_PATH_ESCAPE", "error", str(exc), suggestion="使用工作区内的相对路径", path=relative,
            ))
            continue
        key = str(target).casefold()
        if key in seen:
            result["diagnostics"].append(diagnostic(
                "MATERIALIZE_DUPLICATE_PATH", "error", f"重复 artifact 路径: {relative}", suggestion="每个目标路径只保留一个 artifact", path=relative,
            ))
            continue
        seen.add(key)
        if target.exists() and not overwrite:
            result["diagnostics"].append(diagnostic(
                "MATERIALIZE_OVERWRITE_REFUSED", "error", f"目标已存在，默认拒绝覆盖: {relative}",
                suggestion="确认内容后显式设置 policy.overwrite=allow", path=relative,
            ))
        if not target.parent.exists() and not create_parents:
            result["diagnostics"].append(diagnostic(
                "MATERIALIZE_PARENT_MISSING", "error", f"父目录不存在: {target.parent}",
                suggestion="设置 policy.createParents=true 或预先创建目录", path=relative,
            ))
        planned.append((item, target))

    if any(item.get("severity") == "error" for item in result["diagnostics"]):
        return result

    for item, target in planned:
        if create_parents:
            target.parent.mkdir(parents=True, exist_ok=True)
        temp = target.with_name(f".{target.name}.orryx-toolkit.tmp")
        if temp.exists():
            result["diagnostics"].append(diagnostic(
                "MATERIALIZE_TEMP_EXISTS", "error", f"原子写入临时文件已存在: {temp.name}",
                suggestion="确认无运行中的 materialize 后删除该临时文件", path=item["path"],
            ))
            break
        try:
            with temp.open("x", encoding="utf-8", newline="\n") as stream:
                stream.write(item["content"])
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temp, target)
        except OSError as exc:
            if temp.exists():
                try:
                    temp.unlink()
                except OSError as cleanup_exc:
                    result["diagnostics"].append(diagnostic(
                        "MATERIALIZE_TEMP_CLEANUP_FAILED", "warning", f"临时文件清理失败: {cleanup_exc}",
                        suggestion="手动删除临时文件后重试", path=item["path"],
                    ))
            result["diagnostics"].append(diagnostic(
                "MATERIALIZE_IO_FAILED", "error", f"写入失败: {exc}", suggestion="检查文件权限与磁盘状态", path=item["path"],
            ))
            break
        metadata = dict(item.get("metadata", {})) if isinstance(item.get("metadata"), Mapping) else {}
        metadata["materialized"] = True
        result["artifacts"].append(artifact(item["path"], item["content"], kind=str(item.get("kind", "yaml")), metadata=metadata))
        result["checks"].append(check("MATERIALIZE_WRITTEN", "pass", f"已写入 {item['path']}"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return materialize(contract)
