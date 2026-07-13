from __future__ import annotations

import copy
import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock


SUITE_ROOT = Path(__file__).resolve().parents[1]
SHARED_ROOT = SUITE_ROOT / "shared"
sys.dont_write_bytecode = True
if str(SHARED_ROOT) not in sys.path:
    sys.path.insert(0, str(SHARED_ROOT))

import yaml

from orryx_toolkit import ContractError, run_contract
from orryx_toolkit.cli import execute
from orryx_toolkit.contracts import artifact, check, diagnostic, empty_result
import orryx_toolkit.orchestrator as orchestrator_module


def make_contract(
    component: str,
    request: dict | None = None,
    *,
    workspace: str | Path | dict | None = None,
    operation: str | None = None,
    policy: dict | None = None,
) -> dict:
    if workspace is None:
        normalized_workspace: str | dict = {"root": ".", "mode": "standalone"}
    elif isinstance(workspace, Path):
        normalized_workspace = {"root": str(workspace), "mode": "standalone"}
    else:
        normalized_workspace = workspace
    return {
        "contractVersion": "1.0",
        "component": component,
        "operation": operation or ("validate" if component in {"validator", "kether"} else "generate"),
        "workspace": normalized_workspace,
        "request": request or {},
        "policy": {
            "strict": True,
            "network": "deny",
            "overwrite": "deny",
            "validateReferences": True,
            **(policy or {}),
        },
    }


def diagnostic_codes(result: dict) -> set[str]:
    return {str(item.get("code", "")) for item in result.get("diagnostics", [])}


def check_codes(result: dict) -> set[str]:
    return {str(item.get("code", "")) for item in result.get("checks", [])}


def artifact_for(result: dict, path: str) -> dict:
    return next(item for item in result["artifacts"] if item["path"] == path)


def write_text(root: Path, relative: str, content: str) -> Path:
    path = root.joinpath(*relative.split("/"))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")
    return path


class RunContractTest(unittest.TestCase):

    def test_ten_components_have_basic_positive_run_contract_case(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-runtime-positive-") as directory:
            root = Path(directory)
            schema = {"actions": []}
            cases = {
                "validator": (
                    make_contract("validator", workspace=root),
                    "WORKSPACE_SCANNED",
                ),
                "kether": (
                    make_contract("kether", {"context": "generic", "script": "true", "actionsSchema": schema}, workspace=root),
                    "KETHER_SCRIPT_SCANNED",
                ),
                "ability": (
                    make_contract(
                        "ability",
                        {"key": "基础技能", "type": "DIRECT", "actions": "true", "actionsSchema": schema},
                        workspace=root,
                    ),
                    "ABILITY_YAML_GENERATED",
                ),
                "progression": (
                    make_contract(
                        "progression",
                        {"key": "基础经验", "minLevel": 1, "maxLevel": 3, "curve": {"type": "exponential", "base": 100, "growth": 1.2}},
                        workspace=root,
                    ),
                    "PROGRESSION_CURVE_COMPUTED",
                ),
                "job": (
                    make_contract(
                        "job",
                        {"key": "基础职业", "skills": [], "experience": "default", "actionsSchema": schema},
                        workspace=root,
                        policy={"validateReferences": False},
                    ),
                    "JOB_YAML_GENERATED",
                ),
                "station": (
                    make_contract(
                        "station",
                        {"key": "基础中转站", "event": "Player Join", "async": False, "actions": "true", "actionsSchema": schema},
                        workspace=root,
                    ),
                    "STATION_YAML_GENERATED",
                ),
                "combat": (
                    make_contract(
                        "combat",
                        {
                            "actionsSchema": schema,
                            "status": {
                                "key": "基础状态",
                                "condition": "true",
                                "states": {
                                    "普攻": {
                                        "Type": "General Attack",
                                        "Connection": "0-1",
                                        "Animation": {"Key": "基础动画", "Duration": "2"},
                                    }
                                },
                            },
                            "controller": {
                                "key": "基础控制器",
                                "backend": "bukkit",
                                "layers": {"base": {"animations": ["基础动画"]}},
                                "triggers": {},
                            },
                        },
                        workspace=root,
                    ),
                    "COMBAT_YAML_GENERATED",
                ),
                "selector": (
                    make_contract(
                        "selector",
                        {"selectors": {"附近目标": "container"}, "actionsSchema": schema},
                        workspace=root,
                    ),
                    "SELECTOR_YAML_GENERATED",
                ),
                "ui": (
                    make_contract(
                        "ui",
                        {"backend": "bukkit", "config": {"SkillUI": {"title": "技能", "Skills": {"Slots": [10]}}}},
                        workspace=root,
                    ),
                    "UI_YAML_GENERATED",
                ),
                "orchestrator": (
                    make_contract(
                        "orchestrator",
                        {
                            "steps": [
                                {"component": "validator", "operation": "validate", "request": {}},
                                {
                                    "component": "kether",
                                    "operation": "validate",
                                    "request": {"context": "generic", "script": "true", "actionsSchema": schema},
                                },
                            ]
                        },
                        workspace=root,
                        policy={"validateReferences": False},
                    ),
                    "WORKSPACE_SCANNED",
                ),
            }

            self.assertEqual(10, len(cases))
            for component, (payload, expected_check) in cases.items():
                with self.subTest(component=component):
                    result = run_contract(payload)
                    self.assertEqual(
                        "ok",
                        result["status"],
                        msg=json.dumps(result["diagnostics"], ensure_ascii=False, indent=2),
                    )
                    self.assertEqual(component, result["component"])
                    self.assertIn(expected_check, check_codes(result))

    def test_unknown_component_is_rejected(self) -> None:
        with self.assertRaisesRegex(ContractError, "未知 component"):
            run_contract(make_contract("not-a-component"))

    def test_illegal_operation_is_rejected(self) -> None:
        payload = make_contract("ability", {"key": "技能", "type": "DIRECT", "actions": "true"})
        payload["operation"] = "execute-server"
        with self.assertRaisesRegex(ContractError, "未知 operation"):
            run_contract(payload)

    def test_cli_wraps_invalid_contract_as_structured_result(self) -> None:
        result = execute("run", make_contract("not-a-component"))
        self.assertEqual("invalid", result["status"])
        self.assertEqual({"CONTRACT_INVALID"}, diagnostic_codes(result))

    def test_equivalent_input_is_deterministic(self) -> None:
        payload = make_contract(
            "ui",
            {
                "backend": "dragoncore",
                "files": {
                    "z-last.yml": {"second": 2, "first": 1},
                    "a-first.yml": {"items": [3, 2, 1]},
                },
            },
            policy={"validateReferences": False},
        )
        original = copy.deepcopy(payload)
        first = run_contract(payload)
        second = run_contract(copy.deepcopy(payload))
        self.assertEqual(first, second)
        self.assertEqual(original, payload)

    def test_artifacts_include_sha256_media_type_and_utf8_encoding(self) -> None:
        result = run_contract(
            make_contract(
                "job",
                {
                    "key": "进阶职业",
                    "skills": [],
                    "experience": "default",
                    "advancementFrom": "基础职业",
                    "actionsSchema": {"actions": []},
                },
                policy={"validateReferences": False},
            )
        )
        self.assertEqual("ok", result["status"])
        yaml_artifact = artifact_for(result, "jobs/进阶职业.yml")
        json_artifact = artifact_for(result, "plans/jobs/进阶职业-advancement.json")
        self.assertEqual("application/yaml", yaml_artifact["mediaType"])
        self.assertEqual("application/json", json_artifact["mediaType"])
        for item in result["artifacts"]:
            with self.subTest(path=item["path"]):
                self.assertEqual("utf-8", item["encoding"])
                self.assertEqual(
                    hashlib.sha256(item["content"].encode("utf-8")).hexdigest(),
                    item["sha256"],
                )

    def test_progression_line_chart_uses_declared_vendor_media_type(self) -> None:
        result = run_contract(
            make_contract(
                "progression",
                {"key": "图表经验", "minLevel": 1, "maxLevel": 2, "curve": {"type": "table", "values": [10, 20]}},
            )
        )
        chart = artifact_for(result, "reports/progression/图表经验.json")
        self.assertEqual("application/vnd.orryx.line-chart+json", chart["mediaType"])


class ProgressionTest(unittest.TestCase):

    def test_exponential_progression_is_monotonic(self) -> None:
        result = run_contract(
            make_contract(
                "progression",
                {"key": "单调经验", "minLevel": 1, "maxLevel": 8, "curve": {"type": "exponential", "base": 100, "growth": 1.25}},
            )
        )
        computed = next(item for item in result["checks"] if item["code"] == "PROGRESSION_CURVE_COMPUTED")
        rows = computed["details"]["perLevel"]
        experience = [row["experience"] for row in rows]
        cumulative = [row["cumulative"] for row in rows]
        self.assertEqual(sorted(experience), experience)
        self.assertTrue(all(right > left for left, right in zip(cumulative, cumulative[1:])))

    def test_zero_progression_is_clamped_and_warned(self) -> None:
        result = run_contract(
            make_contract(
                "progression",
                {"key": "零经验", "minLevel": 0, "maxLevel": 3, "curve": {"type": "polynomial", "a": 0, "b": 0, "c": 0}},
            )
        )
        self.assertEqual("ok", result["status"])
        self.assertIn("PROGRESSION_NON_POSITIVE", diagnostic_codes(result))
        computed = next(item for item in result["checks"] if item["code"] == "PROGRESSION_CURVE_COMPUTED")
        self.assertTrue(all(row["experience"] == 0 for row in computed["details"]["perLevel"]))

    def test_progression_int_overflow_is_an_error(self) -> None:
        result = run_contract(
            make_contract(
                "progression",
                {
                    "key": "溢出经验",
                    "minLevel": 1,
                    "maxLevel": 2,
                    "curve": {"type": "table", "values": [2_147_483_648, 1]},
                },
            )
        )
        self.assertEqual("invalid", result["status"])
        self.assertIn("PROGRESSION_INT_OVERFLOW", diagnostic_codes(result))

    def test_progression_rejects_reversed_or_empty_range(self) -> None:
        for min_level, max_level in ((2, 2), (5, 4)):
            with self.subTest(minLevel=min_level, maxLevel=max_level):
                result = run_contract(
                    make_contract(
                        "progression",
                        {"key": "错误范围", "minLevel": min_level, "maxLevel": max_level, "curve": {"type": "table", "values": []}},
                    )
                )
                self.assertEqual("invalid", result["status"])
                self.assertIn("PROGRESSION_RANGE_INVALID", diagnostic_codes(result))


class MaterializeTest(unittest.TestCase):

    def materialize(self, root: Path, artifacts: list[dict], **policy: object) -> dict:
        return execute(
            "materialize",
            make_contract(
                "materialize",
                {"artifacts": artifacts},
                workspace=root,
                operation="materialize",
                policy={"createParents": True, **policy},
            ),
        )

    def test_materialize_rejects_parent_traversal(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-materialize-traversal-") as directory:
            base = Path(directory)
            root = base / "root"
            root.mkdir()
            result = self.materialize(root, [artifact("../escaped.yml", "blocked\n")])
            self.assertEqual("invalid", result["status"])
            self.assertIn("MATERIALIZE_PATH_ESCAPE", diagnostic_codes(result))
            self.assertFalse((base / "escaped.yml").exists())

    def test_materialize_rejects_absolute_drive_and_unc_paths(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-materialize-absolute-") as directory:
            root = Path(directory)
            paths = ("/absolute.yml", "C:/absolute.yml", r"\\server\share\absolute.yml")
            for path in paths:
                with self.subTest(path=path):
                    result = self.materialize(root, [artifact(path, "blocked\n")])
                    self.assertEqual("invalid", result["status"])
                    self.assertIn("MATERIALIZE_PATH_ESCAPE", diagnostic_codes(result))

    def test_materialize_rejects_symlink_escape(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-materialize-symlink-") as directory:
            base = Path(directory)
            root = base / "root"
            outside = base / "outside"
            root.mkdir()
            outside.mkdir()
            link = root / "linked"
            try:
                os.symlink(outside, link, target_is_directory=True)
            except (NotImplementedError, OSError) as exc:
                self.skipTest(f"当前 Windows 环境不能创建测试 symlink: {exc}")
            result = self.materialize(root, [artifact("linked/escaped.yml", "blocked\n")])
            self.assertEqual("invalid", result["status"])
            self.assertIn("MATERIALIZE_PATH_ESCAPE", diagnostic_codes(result))
            self.assertFalse((outside / "escaped.yml").exists())

    def test_materialize_refuses_overwrite_by_default_and_allows_explicit_overwrite(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-materialize-overwrite-") as directory:
            root = Path(directory)
            target = write_text(root, "configs/value.yml", "old\n")
            item = artifact("configs/value.yml", "new\n")

            denied = self.materialize(root, [item])
            self.assertEqual("invalid", denied["status"])
            self.assertIn("MATERIALIZE_OVERWRITE_REFUSED", diagnostic_codes(denied))
            self.assertEqual("old\n", target.read_text(encoding="utf-8"))

            allowed = self.materialize(root, [item], overwrite="allow")
            self.assertEqual("ok", allowed["status"])
            self.assertEqual("new\n", target.read_text(encoding="utf-8"))
            self.assertIn("MATERIALIZE_WRITTEN", check_codes(allowed))

    def test_materialize_rejects_hash_mismatch(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-materialize-hash-") as directory:
            root = Path(directory)
            item = artifact("configs/value.yml", "content\n")
            item["sha256"] = "0" * 64
            result = self.materialize(root, [item])
            self.assertEqual("invalid", result["status"])
            self.assertIn("MATERIALIZE_HASH_MISMATCH", diagnostic_codes(result))
            self.assertFalse((root / "configs" / "value.yml").exists())


class KetherTest(unittest.TestCase):

    def test_kether_reports_action_outside_schema_allowlist(self) -> None:
        result = run_contract(
            make_contract(
                "kether",
                {
                    "context": "generic",
                    "script": "forbidden-action value",
                    "actionsSchema": {"actions": [{"name": "allowed-action"}]},
                },
            )
        )
        self.assertEqual("ok", result["status"])
        self.assertIn("KETHER_ACTION_UNKNOWN", diagnostic_codes(result))

    def test_kether_reports_async_bukkit_thread_risk(self) -> None:
        result = run_contract(
            make_contract(
                "kether",
                {
                    "context": {"type": "station", "async": True},
                    "script": "damage 10",
                    "actionsSchema": {"actions": [{"name": "damage", "execution": {"thread": "main"}}]},
                },
            )
        )
        self.assertEqual("ok", result["status"])
        self.assertIn("KETHER_THREAD_RISK", diagnostic_codes(result))


class ValidatorTest(unittest.TestCase):

    def test_validator_reports_yaml_parse_failure(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-validator-parse-") as directory:
            root = Path(directory)
            write_text(root, "skills/broken.yml", "Options: [\n")
            result = run_contract(make_contract("validator", workspace=root))
            self.assertEqual("invalid", result["status"])
            self.assertIn("WORKSPACE_LOAD_FAILED", diagnostic_codes(result))

    def test_validator_reports_missing_job_references(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-validator-reference-") as directory:
            root = Path(directory)
            write_text(
                root,
                "jobs/战士.yml",
                "Options:\n  Skills:\n    - 不存在技能\n  Experience: 不存在经验\n",
            )
            result = run_contract(make_contract("validator", workspace=root))
            self.assertEqual("invalid", result["status"])
            self.assertTrue(
                {"JOB_SKILL_MISSING", "JOB_EXPERIENCE_MISSING"}.issubset(diagnostic_codes(result))
            )

    def test_validator_reports_case_insensitive_duplicate_basename(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-validator-duplicate-") as directory:
            root = Path(directory)
            write_text(root, "skills/a/Slash.yml", "Options:\n  Sort: 1\n")
            write_text(root, "skills/b/slash.yaml", "Options:\n  Sort: 2\n")
            result = run_contract(make_contract("validator", workspace=root))
            self.assertEqual("invalid", result["status"])
            self.assertIn("WORKSPACE_DUPLICATE_BASENAME", diagnostic_codes(result))

    def test_validator_rejects_mis_cased_station_async_key(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-validator-async-case-") as directory:
            root = Path(directory)
            write_text(
                root,
                "stations/wrong-case.yml",
                "Options:\n  Event: Player Join\n  Priority: NORMAL\n  async: false\nActions: true\n",
            )
            result = run_contract(make_contract("validator", workspace=root))
            self.assertEqual("invalid", result["status"])
            self.assertIn("STATION_ASYNC_KEY_CASE", diagnostic_codes(result))

    def test_validator_reports_bukkit_sensitive_action_in_async_station(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-validator-async-risk-") as directory:
            root = Path(directory)
            write_text(
                root,
                "stations/risky.yml",
                "Options:\n  Event: Player Damage Pre\n  Priority: NORMAL\n  Async: true\nActions: damage 10\n",
            )
            result = run_contract(make_contract("validator", workspace=root))
            risks = [
                item
                for item in result["diagnostics"]
                if item.get("severity") in {"warning", "error"}
                and ("THREAD" in str(item.get("code", "")) or "ASYNC" in str(item.get("code", "")))
                and item.get("path") == "stations/risky.yml"
            ]
            self.assertTrue(risks)

    def test_validator_reports_missing_buff_and_invalid_experience(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-validator-domain-") as directory:
            root = Path(directory)
            write_text(root, "buffs.yml", "已定义:\n  Description: []\n")
            write_text(
                root,
                "skills/测试.yml",
                "Options:\n  Type: DIRECT\n  Sort: 1\nActions: |-\n  buff send 未定义 20\n",
            )
            write_text(
                root,
                "experiences/broken.yml",
                "Options:\n  Min: 10\n  Max: 1\n  ExperienceOfLevel: ''\n",
            )
            result = run_contract(make_contract("validator", workspace=root))
            self.assertEqual("invalid", result["status"])
            self.assertTrue(
                {"SKILL_BUFF_MISSING", "EXPERIENCE_RANGE_INVALID", "EXPERIENCE_FORMULA_MISSING"}.issubset(
                    diagnostic_codes(result)
                )
            )

    def test_validator_rejects_aim_skill_outside_1122(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-validator-aim-") as directory:
            root = Path(directory)
            write_text(
                root,
                "skills/瞄准.yml",
                "Options:\n  Type: DIRECT AIM\nActions: true\n",
            )
            result = run_contract(
                make_contract("validator", workspace=root, policy={"minecraftVersion": "1.20.4"})
            )
            self.assertEqual("invalid", result["status"])
            self.assertIn("SKILL_AIM_VERSION_UNSUPPORTED", diagnostic_codes(result))


class CombatTest(unittest.TestCase):

    def test_combat_controller_artifact_uses_layer_and_trigger_sections(self) -> None:
        result = run_contract(
            make_contract(
                "combat",
                {
                    "actionsSchema": {"actions": []},
                    "status": {
                        "key": "战斗状态",
                        "condition": "true",
                        "states": {
                            "挥砍": {
                                "Type": "General Attack",
                                "Connection": "2-6",
                                "Animation": {"Key": "挥砍动画", "Duration": "8"},
                            }
                        },
                    },
                    "controller": {
                        "key": "战斗控制器",
                        "backend": "dragoncore",
                        "config": {
                            "Layer": {"base": {"animations": ["挥砍动画"], "weight": 1}},
                            "Trigger": {"attack": {"animation": "挥砍动画"}},
                        },
                    },
                },
            )
        )
        self.assertEqual("ok", result["status"])
        controller = artifact_for(result, "controllers/战斗控制器.yml")
        data = yaml.safe_load(controller["content"])
        self.assertEqual({"Layer", "Trigger"}, set(data))
        self.assertIn("base", data["Layer"])
        self.assertIn("attack", data["Trigger"])


class OrchestratorTest(unittest.TestCase):

    @staticmethod
    def runner(name: str, calls: list[str], *, error: bool = False):
        def run(contract: dict) -> dict:
            calls.append(name)
            result = empty_result()
            if error:
                result["diagnostics"].append(
                    diagnostic(f"{name.upper()}_FAILED", "error", f"{name} failed")
                )
            else:
                result["checks"].append(check(f"{name.upper()}_RAN", "pass", f"{name} ran"))
            return result

        return run

    def test_orchestrator_executes_requested_components_in_fixed_dependency_order(self) -> None:
        calls: list[str] = []
        replacements = {
            name: self.runner(name, calls)
            for name in ("validator", "kether", "ability", "ui")
        }
        payload = make_contract(
            "orchestrator",
            {
                "steps": [
                    {"component": "ui", "request": {}},
                    {"component": "ability", "request": {}},
                    {"component": "kether", "operation": "validate", "request": {}},
                    {"component": "validator", "operation": "validate", "request": {}},
                ]
            },
            policy={"validateReferences": False},
        )
        with mock.patch.dict(orchestrator_module._RUNNERS, replacements, clear=False):
            run_contract(payload)
        self.assertEqual(["validator", "kether", "ability", "ui"], calls)

    def test_orchestrator_propagates_stage_errors_and_continues_read_only_analysis(self) -> None:
        calls: list[str] = []
        replacements = {
            "ability": self.runner("ability", calls, error=True),
            "ui": self.runner("ui", calls),
        }
        payload = make_contract(
            "orchestrator",
            {"steps": [{"component": "ability", "request": {}}, {"component": "ui", "request": {}}]},
            policy={"validateReferences": False},
        )
        with mock.patch.dict(orchestrator_module._RUNNERS, replacements, clear=False):
            result = run_contract(payload)
        self.assertEqual(["ability", "ui"], calls)
        self.assertEqual("invalid", result["status"])
        self.assertIn("ABILITY_FAILED", diagnostic_codes(result))
        self.assertIn("UI_RAN", check_codes(result))

    def test_orchestrator_blocks_materialize_stage_after_prior_error(self) -> None:
        calls: list[str] = []
        replacements = {
            "ability": self.runner("ability", calls, error=True),
            "materialize": self.runner("materialize", calls),
        }
        payload = make_contract(
            "orchestrator",
            {
                "steps": [
                    {"component": "ability", "request": {}},
                    {"component": "materialize", "operation": "materialize", "request": {}},
                ]
            },
            operation="materialize",
            policy={"validateReferences": False},
        )
        with mock.patch.dict(orchestrator_module._RUNNERS, replacements, clear=False):
            result = run_contract(payload)
        self.assertEqual(["ability"], calls)
        self.assertNotIn("MATERIALIZE_RAN", check_codes(result))

    def test_orchestrator_materialize_injects_prior_artifacts_after_preflight(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-orchestrator-materialize-") as directory:
            root = Path(directory)
            result = run_contract(
                make_contract(
                    "orchestrator",
                    {
                        "steps": [
                            {
                                "component": "ability",
                                "request": {"key": "落盘技能", "type": "DIRECT", "actions": "true"},
                            },
                            {"component": "materialize", "operation": "materialize", "request": {}},
                        ]
                    },
                    workspace=root,
                    operation="materialize",
                    policy={"validateReferences": False, "overwrite": "deny"},
                )
            )
            self.assertEqual("ok", result["status"])
            target = root / "skills" / "落盘技能.yml"
            self.assertTrue(target.is_file())
            item = artifact_for(result, "skills/落盘技能.yml")
            self.assertTrue(item["metadata"].get("materialized"))
            self.assertIn("MATERIALIZE_WRITTEN", check_codes(result))


if __name__ == "__main__": 
    unittest.main()
