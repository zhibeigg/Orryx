from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

import yaml

SUITE_ROOT = Path(__file__).resolve().parents[1]
SHARED_ROOT = SUITE_ROOT / "shared"
if str(SHARED_ROOT) not in sys.path:
    sys.path.insert(0, str(SHARED_ROOT))

from orryx_toolkit import run_contract
from orryx_toolkit.cli import run_component_file
from orryx_toolkit.materialize import materialize
from orryx_toolkit.progression import curve_value
from orryx_toolkit.contracts import normalize_contract


def contract(component: str, request: dict, root: str = ".", **policy: object) -> dict:
    return {
        "contractVersion": "1.0",
        "component": component,
        "operation": "validate" if component in {"validator", "kether"} else "generate",
        "workspace": {"root": root, "mode": "standalone"},
        "request": request,
        "policy": {
            "strict": True,
            "network": "deny",
            "overwrite": "deny",
            "minecraftVersion": "1.20.4",
            "plugins": [],
            "validateReferences": False,
            **policy,
        },
    }


class ToolkitTests(unittest.TestCase):
    def test_same_input_is_stable(self) -> None:
        payload = contract("ability", {
            "id": "稳定技能",
            "type": "DIRECT",
            "variables": {"damage": "calc \"20+level\""},
            "actions": "tell stable\ndamage lazy *damage they \"@range 2 !@self\"",
        })
        first = run_contract(payload)
        second = run_contract(payload)
        self.assertEqual(first, second)
        self.assertEqual(first["status"], "ok")
        self.assertTrue({"suiteVersion", "inputDigest", "deterministic"}.issubset(first["provenance"]))
        self.assertNotIn("timestamp", json.dumps(first).lower())

    def test_five_ability_types_are_parseable(self) -> None:
        types = ["PASSIVE", "DIRECT", "DIRECT_AIM", "PRESSING", "PRESSING_AIM"]
        for index, skill_type in enumerate(types):
            request = {"id": f"skill-{index}", "type": skill_type, "actions": "tell ok"}
            if skill_type == "PASSIVE":
                request.update({
                    "station": "passive-trigger",
                    "stationEvent": "Player Damage Pre",
                    "stationActions": "tell passive",
                })
            result = run_contract(contract("ability", request, minecraftVersion="1.12.2"))
            skill = next(item for item in result["artifacts"] if item["path"].startswith("skills/"))
            parsed = yaml.safe_load(skill["content"])
            self.assertIsInstance(parsed, dict)
            self.assertIn("Options", parsed)
            if skill_type == "PASSIVE":
                self.assertTrue(any(item["path"].startswith("stations/") for item in result["artifacts"]))

    def test_aim_version_is_rejected(self) -> None:
        result = run_contract(contract("ability", {"id": "aim", "type": "DIRECT_AIM", "actions": "tell aim"}))
        self.assertEqual(result["status"], "invalid")
        self.assertIn("ABILITY_AIM_VERSION_UNSUPPORTED", {item["code"] for item in result["diagnostics"]})

    def test_progression_curves_and_cumulative_data(self) -> None:
        self.assertEqual(curve_value(3, {"type": "polynomial", "a": 2, "b": 3, "c": 4}), 31)
        self.assertEqual(curve_value(2, {"type": "table", "values": {"2": 75}}), 75)
        self.assertEqual(curve_value(4, {
            "type": "piecewise",
            "segments": [{"from": 1, "to": 5, "curve": {"type": "table", "startLevel": 1, "values": [10, 20, 30, 40, 50]}}],
        }), 40)
        result = run_contract(contract("progression", {
            "id": "curve",
            "minLevel": 1,
            "maxLevel": 3,
            "curve": {"type": "table", "startLevel": 1, "values": [10, 20, 30]},
            "maxManaActions": "calc \"100+10*level\"",
            "chart": True,
        }))
        self.assertEqual(result["status"], "ok")
        yaml_artifact = next(item for item in result["artifacts"] if item["path"].endswith("curve.yml"))
        self.assertIsInstance(yaml.safe_load(yaml_artifact["content"]), dict)
        computed = next(item for item in result["checks"] if item["code"] == "PROGRESSION_CURVE_COMPUTED")
        rows = computed["details"]["perLevel"]
        self.assertEqual([row["cumulative"] for row in rows], [10, 30, 60])
        self.assertEqual(computed["details"]["resources"][2]["maxMana"], 130)
        self.assertTrue(any(item["kind"] == "line-chart" for item in result["artifacts"]))

    def test_backend_generators_emit_yaml_and_dependencies(self) -> None:
        cases = [
            ("job", {"id": "warrior", "icon": "sword", "skills": ["slash"], "experience": "curve"}),
            ("station", {"id": "trigger", "event": "Player Damage Pre", "actions": "tell hit"}),
            ("combat", {"status": {"key": "sword", "states": {"attack": {"Type": "General Attack", "Animation": {"Key": "attack", "Duration": "10"}}}}, "controller": {"key": "sword-controller", "backend": "dragoncore"}}),
            ("selector", {"selectors": {"near": {"Actions": "container"}}}),
            ("ui", {"backend": "bukkit", "config": {"JoinOpenHud": True}}),
        ]
        for component, request in cases:
            result = run_contract(contract(component, request))
            self.assertTrue(result["artifacts"], component)
            for item in result["artifacts"]:
                if item["kind"] == "yaml":
                    self.assertIsNotNone(yaml.safe_load(item["content"]), item["path"])
        job_result = run_contract(contract("job", {"id": "elite", "icon": "elite", "advancementFrom": "base", "skills": [], "experience": "curve"}))
        self.assertNotIn("ParentJob", next(item for item in job_result["artifacts"] if item["path"] == "jobs/elite.yml")["content"])
        self.assertTrue(job_result["requirements"])

    def test_orchestrator_resolves_generated_references(self) -> None:
        payload = contract("orchestrator", {
            "steps": [
                {"component": "progression", "operation": "generate", "request": {"id": "curve", "minLevel": 1, "maxLevel": 2, "curve": {"type": "table", "values": [10, 20], "startLevel": 1}}},
                {"component": "ability", "operation": "generate", "request": {"id": "slash", "type": "DIRECT", "actions": "tell slash"}},
                {"component": "job", "operation": "generate", "request": {"id": "warrior", "skills": ["slash"], "experience": "curve"}},
            ]
        }, validateReferences=True)
        result = run_contract(payload)
        self.assertEqual(result["status"], "ok")
        self.assertNotIn("REFERENCE_TARGET_MISSING", {item["code"] for item in result["diagnostics"]})
        recursive = contract("orchestrator", {"steps": [{"component": "orchestrator", "request": {}}]})
        self.assertEqual(run_contract(recursive)["status"], "invalid")

    def test_materialize_protects_paths_and_overwrite(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            escaped = normalize_contract(contract("materialize", {"artifacts": [{"path": "../escape.yml", "content": "x: 1\n"}]}, directory))
            escaped["operation"] = "materialize"
            result = materialize(escaped)
            self.assertIn("MATERIALIZE_PATH_ESCAPE", {item["code"] for item in result["diagnostics"]})
            valid = normalize_contract(contract("materialize", {"artifacts": [{"path": "skills/a.yml", "content": "Options:\n  Type: DIRECT\n"}]}, directory))
            valid["operation"] = "materialize"
            first = materialize(valid)
            self.assertFalse(first["diagnostics"])
            target = Path(directory) / "skills" / "a.yml"
            self.assertTrue(target.is_file())
            self.assertFalse(any(path.name.endswith("orryx-toolkit.tmp") for path in target.parent.iterdir()))
            second = materialize(valid)
            self.assertIn("MATERIALIZE_OVERWRITE_REFUSED", {item["code"] for item in second["diagnostics"]})

    def test_workspace_validator_codes_are_stable(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for folder in ("skills", "jobs", "experiences", "stations", "status", "controllers"):
                (root / folder).mkdir()
            (root / "skills" / "same.yml").write_text("Options:\n  Type: DIRECT\n  Sort: 1\nActions: tell ok\n", encoding="utf-8")
            (root / "skills" / "nested").mkdir()
            (root / "skills" / "nested" / "same.yaml").write_text("Options:\n  Type: DIRECT\n  Sort: 1\nActions: tell ok\n", encoding="utf-8")
            (root / "jobs" / "job.yml").write_text("Options:\n  Skills: [missing]\n  Experience: absent\n", encoding="utf-8")
            (root / "stations" / "station.yml").write_text("Options:\n  Event: Test\n  Priority: impossible\n  Async: text\n  Token: secret\nActions: tell ok\n", encoding="utf-8")
            (root / "status" / "status.yml").write_text("Options:\n  Controller: absent\nStates: {}\nAction: running missing\n", encoding="utf-8")
            result = run_contract(contract("validator", {}, directory))
            codes = {item["code"] for item in result["diagnostics"]}
            self.assertTrue({
                "WORKSPACE_DUPLICATE_BASENAME", "JOB_SKILL_MISSING", "JOB_EXPERIENCE_MISSING",
                "STATION_PRIORITY_INVALID", "STATION_ASYNC_TYPE", "STATUS_CONTROLLER_MISSING",
                "STATUS_RUNNING_TARGET_MISSING", "WORKSPACE_SENSITIVE_VALUE",
            }.issubset(codes))

    def test_component_file_api_writes_semantic_invalid_json_with_zero(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            input_path = Path(directory) / "input.json"
            output_path = Path(directory) / "output.json"
            input_path.write_text(json.dumps(contract("ability", {"id": "bad/path", "type": "DIRECT", "actions": "tell x"}), ensure_ascii=False), encoding="utf-8")
            code = run_component_file("ability", input_path, output_path)
            self.assertEqual(code, 0)
            output = json.loads(output_path.read_text(encoding="utf-8"))
            self.assertEqual(output["status"], "invalid")
            self.assertEqual(output["component"], "ability")


if __name__ == "__main__":
    unittest.main()
