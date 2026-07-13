"""经验曲线计算与 Orryx Experience YAML 生成。"""
from __future__ import annotations

import ast
import json
import operator
import re
from decimal import Decimal, InvalidOperation, ROUND_CEILING, ROUND_FLOOR, ROUND_HALF_UP
from typing import Any, Mapping

from .contracts import artifact, check, diagnostic, empty_result, requirement
from .yaml_io import literal, stable_dump


def _decimal(value: Any, default: str = "0") -> Decimal:
    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError):
        return Decimal(default)


def _round(value: Decimal, mode: str) -> int:
    rounding = {"floor": ROUND_FLOOR, "ceil": ROUND_CEILING, "round": ROUND_HALF_UP}.get(mode, ROUND_HALF_UP)
    return int(value.quantize(Decimal("1"), rounding=rounding))


def _piece_value(level: int, curve: Mapping[str, Any]) -> Decimal:
    for segment in curve.get("segments", []):
        if not isinstance(segment, Mapping):
            continue
        start = int(segment.get("from", segment.get("min", level)))
        end = int(segment.get("to", segment.get("max", level)))
        if start <= level <= end:
            nested = segment.get("curve", segment)
            return curve_value(level, nested, origin=start)
    return Decimal(0)


def curve_value(level: int, curve: Mapping[str, Any], *, origin: int = 1) -> Decimal:
    curve_type = str(curve.get("type", "exponential")).casefold()
    if curve_type == "exponential":
        base = _decimal(curve.get("base", curve.get("initial", 100)))
        growth = _decimal(curve.get("growth", curve.get("ratio", 1.2)), "1.2")
        offset = int(curve.get("offset", origin))
        return base * (growth ** max(0, level - offset))
    if curve_type == "polynomial":
        coefficients = curve.get("coefficients")
        if isinstance(coefficients, list):
            return sum((_decimal(value) * (Decimal(level) ** index) for index, value in enumerate(coefficients)), Decimal(0))
        a = _decimal(curve.get("a", 0))
        b = _decimal(curve.get("b", 0))
        c = _decimal(curve.get("c", curve.get("base", 0)))
        power = _decimal(curve.get("power", 2), "2")
        return a * (Decimal(level) ** int(power)) + b * Decimal(level) + c
    if curve_type == "piecewise":
        return _piece_value(level, curve)
    if curve_type == "table":
        values = curve.get("values", curve.get("table", {}))
        if isinstance(values, Mapping):
            return _decimal(values.get(str(level), values.get(level, 0)))
        if isinstance(values, list):
            index = level - int(curve.get("startLevel", origin))
            return _decimal(values[index]) if 0 <= index < len(values) else Decimal(0)
    return Decimal(0)


_BINARY = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod,
    ast.Pow: operator.pow,
}
_UNARY = {ast.UAdd: operator.pos, ast.USub: operator.neg}


def _arithmetic(node: ast.AST, level: int) -> Decimal:
    if isinstance(node, ast.Expression):
        return _arithmetic(node.body, level)
    if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
        return Decimal(str(node.value))
    if isinstance(node, ast.Name) and node.id == "level":
        return Decimal(level)
    if isinstance(node, ast.BinOp) and type(node.op) in _BINARY:
        return Decimal(str(_BINARY[type(node.op)](_arithmetic(node.left, level), _arithmetic(node.right, level))))
    if isinstance(node, ast.UnaryOp) and type(node.op) in _UNARY:
        return Decimal(str(_UNARY[type(node.op)](_arithmetic(node.operand, level))))
    if isinstance(node, ast.Call) and isinstance(node.func, ast.Name) and node.func.id in {"min", "max"}:
        values = [_arithmetic(arg, level) for arg in node.args]
        return (min if node.func.id == "min" else max)(values)
    raise ValueError("unsupported expression")


def _action_value(expression: Any, level: int, rounding: str) -> int | None:
    if isinstance(expression, (int, float)) and not isinstance(expression, bool):
        return _round(Decimal(str(expression)), rounding)
    if not isinstance(expression, str):
        return None
    text = expression.strip()
    match = re.fullmatch(r"calc\s+(['\"])(.*?)\1", text, re.DOTALL)
    if match:
        text = match.group(2)
    elif not re.fullmatch(r"[0-9A-Za-z_+\-*/%().,^\s]+", text):
        return None
    try:
        tree = ast.parse(text.replace("^", "**"), mode="eval")
        return _round(_arithmetic(tree, level), rounding)
    except (SyntaxError, ValueError, ArithmeticError, InvalidOperation):
        return None


def _formula(curve: Mapping[str, Any], values: Mapping[int, int]) -> str:
    del curve
    rows = ["case &level ["]
    rows.extend(f"  when {level} -> {value}" for level, value in sorted(values.items()))
    rows.extend(("  else 0", "]"))
    return "\n".join(rows)


def generate_progression(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    request = contract.get("request", {})
    key = str(request.get("key", request.get("id", "default"))).strip()
    if not key or "/" in key or "\\" in key:
        result["diagnostics"].append(diagnostic(
            "PROGRESSION_KEY_INVALID", "error", f"非法经验配置 key: {key}", suggestion="使用不含路径分隔符的 key",
        ))
        return result
    min_level = int(request.get("minLevel", request.get("min", 0)))
    max_level = int(request.get("maxLevel", request.get("max", 20)))
    if min_level >= max_level:
        result["diagnostics"].append(diagnostic(
            "PROGRESSION_RANGE_INVALID", "error", "minLevel 必须小于 maxLevel", suggestion="调整等级范围",
        ))
        return result
    curve = request.get("curve", {"type": "exponential", "base": 100, "growth": 1.2})
    if not isinstance(curve, Mapping):
        result["diagnostics"].append(diagnostic(
            "PROGRESSION_CURVE_INVALID", "error", "curve 必须是 object", suggestion="提供 type 与曲线参数",
        ))
        return result
    curve_type = str(curve.get("type", "exponential")).casefold()
    if curve_type not in {"exponential", "polynomial", "piecewise", "table"}:
        result["diagnostics"].append(diagnostic(
            "PROGRESSION_TYPE_INVALID", "error", f"未知曲线类型: {curve_type}",
            suggestion="使用 exponential/polynomial/piecewise/table",
        ))
        return result
    rounding = str(request.get("rounding", "round")).casefold()
    values: dict[int, int] = {}
    cumulative: dict[int, int] = {}
    total = 0
    non_positive: list[int] = []
    overflow: list[int] = []
    for level in range(min_level, max_level + 1):
        value = _round(curve_value(level, curve, origin=min_level), rounding)
        if value <= 0:
            non_positive.append(level)
        value = max(0, value)
        if value > 2_147_483_647:
            overflow.append(level)
        values[level] = value
        total += value
        cumulative[level] = total
    if non_positive:
        result["diagnostics"].append(diagnostic(
            "PROGRESSION_NON_POSITIVE", "warning", f"以下等级经验值非正数: {', '.join(map(str, non_positive[:20]))}",
            suggestion="调整曲线，确保可升级等级的经验需求大于 0",
        ))
    if overflow or total > 2_147_483_647:
        result["diagnostics"].append(diagnostic(
            "PROGRESSION_INT_OVERFLOW", "error", "单级或累计经验超过 Int 安全范围",
            suggestion="降低曲线增长率或等级上限", details={"levels": overflow, "total": total},
        ))
    formula = str(request.get("formula", _formula(curve, values)))
    data = {"Options": {"Min": min_level, "Max": max_level, "ExperienceOfLevel": literal(formula)}}
    path = f"experiences/{key}.yml"
    per_level = [{"level": level, "experience": values[level], "cumulative": cumulative[level]} for level in sorted(values)]
    configured_resources = request.get("resources", {}) if isinstance(request.get("resources", {}), Mapping) else {}
    resource_fields = {
        "maxMana": request.get("maxManaActions", configured_resources.get("maxMana", "100")),
        "regainMana": request.get("regainManaActions", configured_resources.get("regainMana", "1")),
        "maxSpirit": request.get("maxSpiritActions", configured_resources.get("maxSpirit", "100")),
        "regainSpirit": request.get("regainSpiritActions", configured_resources.get("regainSpirit", "1")),
        "upgradePoint": request.get("upgradePointActions", configured_resources.get("upgradePoint", "1")),
    }
    resource_rows: list[dict[str, Any]] = []
    unresolved: set[str] = set()
    for level in range(min_level, max_level + 1):
        row: dict[str, Any] = {"level": level}
        for name, expression in resource_fields.items():
            value = _action_value(expression, level, rounding)
            row[name] = value
            if value is None:
                unresolved.add(name)
        resource_rows.append(row)
    for name in sorted(unresolved):
        result["diagnostics"].append(diagnostic(
            "PROGRESSION_RESOURCE_SIMULATION_UNKNOWN", "warning", f"无法静态模拟资源公式: {name}",
            suggestion="使用数字或 calc 中仅含 level 的算术表达式",
        ))
    second_class = request.get("secondClass", request.get("advancement", {}))
    if isinstance(second_class, Mapping) and second_class:
        result["requirements"].append(requirement(
            "PROGRESSION_ADVANCEMENT_EXTERNAL", "二转由 Job/Station 或外部职业切换流程实现，Experience YAML 不保存父职业",
            component="job", details=dict(second_class),
        ))
    report = {
        "key": key,
        "curveType": curve_type,
        "minLevel": min_level,
        "maxLevel": max_level,
        "totalExperience": total,
        "perLevel": per_level,
        "resources": resource_rows,
        "secondClass": dict(second_class) if isinstance(second_class, Mapping) else {},
        "chart": {
            "type": "line",
            "xKey": "level",
            "series": [
                {"name": "experience", "data": [[row["level"], row["experience"]] for row in per_level]},
                {"name": "cumulative", "data": [[row["level"], row["cumulative"]] for row in per_level]},
                *[
                    {"name": name, "data": [[row["level"], row[name]] for row in resource_rows]}
                    for name in sorted(resource_fields) if name not in unresolved
                ],
            ],
        },
    }
    result["artifacts"].extend([
        artifact(path, stable_dump(data), metadata={"component": "progression", "curve": curve_type}),
        artifact(
            f"reports/progression/{key}.json",
            json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n",
            kind="line-chart",
            metadata={"component": "progression", "artifact": "line-chart"},
        ),
    ])
    result["checks"].append(check(
        "PROGRESSION_CURVE_COMPUTED", "pass", f"已计算 {len(values)} 个等级经验值与资源模拟",
        details={"perLevel": per_level, "total": total, "formula": formula, "resources": resource_rows},
    ))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return generate_progression(contract)
