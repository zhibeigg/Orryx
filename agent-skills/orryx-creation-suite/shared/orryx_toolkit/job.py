"""职业 YAML 生成器。"""
from __future__ import annotations

import json
from typing import Any, Mapping

from .contracts import artifact, check, diagnostic, empty_result, reference, requirement
from .kether import validate_kether
from .yaml_io import literal, stable_dump


def generate_job(contract: Mapping[str, Any]) -> dict[str, Any]:
    result = empty_result()
    request = contract.get("request", {})
    key = str(request.get("key", request.get("id", request.get("name", "job")))).strip()
    if not key or "/" in key or "\\" in key:
        result["diagnostics"].append(diagnostic("JOB_KEY_INVALID", "error", f"非法职业 key: {key}", suggestion="使用不含路径分隔符的 key"))
        return result
    if any(str(name).casefold() == "parentjob" for name in request):
        result["diagnostics"].append(diagnostic(
            "JOB_PARENT_JOB_UNSUPPORTED", "error", "Orryx JobLoader 不支持 ParentJob 字段",
            suggestion="使用 advancementFrom 记录外部迁移 scaffold，不要写入职业 YAML",
        ))
        return result
    skills = [str(value) for value in request.get("skills", [])]
    experience = str(request.get("experience", "default"))
    options: dict[str, Any] = {
        "Name": str(request.get("name", key)),
        "Icon": str(request.get("icon", request.get("name", key))),
        "Skills": skills,
        "Attributes": [str(value) for value in request.get("attributes", [])],
        "RegainManaActions": literal(str(request.get("regainManaActions", "1"))),
        "MaxManaActions": literal(str(request.get("maxManaActions", "100"))),
        "RegainSpiritActions": literal(str(request.get("regainSpiritActions", "1"))),
        "MaxSpiritActions": literal(str(request.get("maxSpiritActions", "100"))),
        "UpgradePointActions": literal(str(request.get("upgradePointActions", "1"))),
        "Experience": experience,
    }
    path = f"jobs/{key}.yml"
    result["artifacts"].append(artifact(path, stable_dump({"Options": options}), metadata={"component": "job"}))
    for skill in sorted(skills, key=str.casefold):
        result["references"].append(reference(path, f"skills/{skill}.yml", "job-skill"))
    result["references"].append(reference(path, f"experiences/{experience}.yml", "job-experience"))
    result["requirements"].append(requirement(
        "JOB_ICON_EXTERNAL", "职业 ID 取文件 basename；Options.Name/Icon 仅是显示或外部 UI 约定，JobLoader 不以 Icon 解析职业",
        component="job", details={"id": key, "name": options["Name"], "icon": options["Icon"]},
    ))
    advancement = request.get("advancement")
    if not isinstance(advancement, Mapping) and request.get("advancementFrom"):
        advancement = {"parentJob": str(request["advancementFrom"])}
    if isinstance(advancement, Mapping):
        plan = {
            "job": key,
            "parentJob": str(advancement.get("parentJob", "")),
            "retainSkills": [str(value) for value in advancement.get("retainSkills", [])],
            "replaceSkills": dict(advancement.get("replaceSkills", {})) if isinstance(advancement.get("replaceSkills", {}), Mapping) else {},
            "bindingMigration": str(advancement.get("bindingMigration", "manual")),
            "stationAllowlistUpdates": [str(value) for value in advancement.get("stationAllowlistUpdates", [])],
            "controllerStrategy": str(advancement.get("controllerStrategy", "reuse")),
        }
        result["artifacts"].append(artifact(
            f"plans/jobs/{key}-advancement.json",
            json.dumps(plan, ensure_ascii=False, sort_keys=True, indent=2) + "\n",
            kind="json",
            metadata={"component": "job", "kind": "advancement-scaffold"},
        ))
        result["requirements"].append(requirement(
            "JOB_ADVANCEMENT_NOT_NATIVE",
            "Orryx JobLoader 没有 ParentJob、技能继承或绑定迁移字段；二转信息仅作为显式实施计划输出",
            component="job",
            details=plan,
        ))
    scripts = {name: str(options[name]) for name in (
        "RegainManaActions", "MaxManaActions", "RegainSpiritActions", "MaxSpiritActions", "UpgradePointActions"
    )}
    kether_contract = dict(contract)
    kether_request = dict(request)
    kether_request.update({"scripts": scripts, "context": "job"})
    kether_contract["request"] = kether_request
    checked = validate_kether(kether_contract)
    result["diagnostics"].extend(checked["diagnostics"])
    result["checks"].extend(checked["checks"])
    result["checks"].append(check("JOB_YAML_GENERATED", "pass", f"已生成职业 {key}"))
    return result


def run(contract: Mapping[str, Any]) -> dict[str, Any]:
    return generate_job(contract)
