from __future__ import annotations

import copy
import json
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

from orryx_toolkit import SERVICE_ERROR_CODES, run_service_request
from orryx_toolkit.cli import execute
from orryx_toolkit.contracts import artifact
import orryx_toolkit.service_runner as service_runner_module


def public_contract(component: str, request: dict | None = None, *, operation: str = "generate", policy: dict | None = None) -> dict:
    return {
        "contractVersion": "1.0",
        "component": component,
        "operation": operation,
        "request": request or {},
        "policy": {
            "strict": True,
            "network": "deny",
            "overwrite": "deny",
            "minecraftVersion": "1.20.4",
            "plugins": [],
            "validateReferences": False,
            **(policy or {}),
        },
    }


def envelope(contract: dict) -> dict:
    return {"envelopeVersion": "1.0", "contract": contract}


def error_codes(response: dict) -> set[str]:
    return {str(item.get("code", "")) for item in response.get("errors", [])}


class ServiceRunnerPositiveTest(unittest.TestCase):

    def test_normal_ability_request_uses_trusted_context_and_output_contract(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-service-ability-") as directory:
            payload = envelope(public_contract(
                "ability",
                {"key": "服务技能", "type": "DIRECT", "actions": "true"},
            ))
            response = run_service_request(payload, workspace_root=directory, actions_schema={"actions": []})

        self.assertEqual(
            {"envelopeVersion", "status", "result", "errors"},
            set(response),
        )
        self.assertEqual("1.0", response["envelopeVersion"])
        self.assertEqual("completed", response["status"])
        self.assertEqual([], response["errors"])
        result = response["result"]
        self.assertIsInstance(result, dict)
        self.assertEqual("ok", result["status"])
        self.assertEqual("ability", result["component"])
        self.assertEqual("generate", result["operation"])
        self.assertEqual(
            {
                "contractVersion", "suiteVersion", "component", "operation", "status", "summary",
                "artifacts", "references", "requirements", "diagnostics", "checks", "nextSteps",
                "metadata", "provenance",
            },
            set(result),
        )
        self.assertTrue(any(item["path"] == "skills/服务技能.yml" for item in result["artifacts"]))
        self.assertNotIn(str(Path(directory).resolve()), json.dumps(response, ensure_ascii=False))

    def test_normal_orchestrator_request_is_accepted(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-service-orchestrator-") as directory:
            payload = envelope(public_contract(
                "orchestrator",
                {
                    "steps": [
                        {
                            "component": "ability",
                            "operation": "generate",
                            "request": {"key": "编排技能", "type": "DIRECT", "actions": "true"},
                        },
                        {
                            "component": "progression",
                            "operation": "plan",
                            "request": {
                                "key": "编排经验",
                                "minLevel": 1,
                                "maxLevel": 2,
                                "curve": {"type": "table", "values": [10, 20]},
                            },
                        },
                    ]
                },
                operation="plan",
            ))
            response = run_service_request(payload, workspace_root=directory, actions_schema={"actions": []})

        self.assertEqual("completed", response["status"])
        self.assertEqual("ok", response["result"]["status"])
        paths = {item["path"] for item in response["result"]["artifacts"]}
        self.assertIn("skills/编排技能.yml", paths)
        self.assertIn("experiences/编排经验.yml", paths)

    def test_same_input_and_trusted_context_are_deterministic_without_mutation(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-service-deterministic-") as directory:
            payload = envelope(public_contract(
                "ability",
                {
                    "key": "稳定服务技能",
                    "type": "DIRECT",
                    "actions": "true",
                },
            ))
            original = copy.deepcopy(payload)
            trusted_schema = {"actions": [{"name": "trusted"}]}
            first = run_service_request(payload, workspace_root=directory, actions_schema=trusted_schema)
            second = run_service_request(copy.deepcopy(payload), workspace_root=directory, actions_schema=copy.deepcopy(trusted_schema))

        self.assertEqual(first, second)
        self.assertEqual(original, payload)
        self.assertEqual("completed", first["status"])
        self.assertTrue(first["result"]["provenance"]["deterministic"])

    def test_local_cli_materialize_remains_available(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-service-local-materialize-") as directory:
            item = artifact("skills/local.yml", "Options:\n  Type: DIRECT\nActions: true\n")
            payload = {
                "contractVersion": "1.0",
                "component": "materialize",
                "operation": "materialize",
                "workspace": {"root": directory, "mode": "standalone"},
                "request": {"artifacts": [item]},
                "policy": {"strict": True, "network": "deny", "overwrite": "deny"},
            }
            result = execute("materialize", payload)
            self.assertEqual("ok", result["status"])
            self.assertTrue((Path(directory) / "skills" / "local.yml").is_file())


class ServiceRunnerBoundaryTest(unittest.TestCase):

    def assert_rejected(self, contract: dict, expected_code: str) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-service-rejected-") as directory:
            with mock.patch.object(service_runner_module, "run_contract") as run_contract_mock:
                response = run_service_request(envelope(contract), workspace_root=directory)
        self.assertEqual("rejected", response["status"])
        self.assertIsNone(response["result"])
        self.assertIn(expected_code, error_codes(response))
        run_contract_mock.assert_not_called()

    def test_rejects_top_level_materialize_component_or_operation_case_insensitively(self) -> None:
        cases = [
            (public_contract("materialize", {}, operation="generate"), "SERVICE_COMPONENT_MATERIALIZE_FORBIDDEN"),
            (public_contract("ability", {}, operation="MaTeRiAlIzE"), "SERVICE_OPERATION_FORBIDDEN"),
        ]
        for payload, code in cases:
            with self.subTest(code=code):
                self.assert_rejected(payload, code)

    def test_recursively_rejects_materialize_in_orchestrator_steps(self) -> None:
        cases = [
            {
                "component": "materialize",
                "operation": "generate",
                "request": {},
            },
            {
                "component": "ability",
                "operation": "MATERIALIZE",
                "request": {},
            },
            {
                "component": "ability",
                "operation": "generate",
                "request": {"nested": {"steps": [{"component": "materialize", "request": {}}]}},
            },
        ]
        for step in cases:
            with self.subTest(step=step):
                payload = public_contract("orchestrator", {"steps": [step]})
                expected = (
                    "SERVICE_COMPONENT_MATERIALIZE_FORBIDDEN"
                    if str(step.get("component", "")).casefold() == "materialize"
                    or "materialize" in json.dumps(step.get("request", {})).casefold()
                    else "SERVICE_OPERATION_FORBIDDEN"
                )
                self.assert_rejected(payload, expected)

    def test_rejects_user_controlled_workspace_at_top_or_step(self) -> None:
        top = public_contract("ability", {"key": "x", "type": "DIRECT", "actions": "true"})
        top["workspace"] = {"root": "C:/untrusted", "mode": "project"}
        step = public_contract("orchestrator", {
            "steps": [{
                "component": "ability",
                "workspace": {"root": "../untrusted", "mode": "project"},
                "request": {},
            }]
        })
        for payload in (top, step):
            with self.subTest(payload=payload):
                self.assert_rejected(payload, "SERVICE_WORKSPACE_FORBIDDEN")

    def test_recursively_rejects_actions_schema_and_paths_without_reading_them(self) -> None:
        cases = [
            (
                public_contract("kether", {"script": "true", "actionsSchemaPath": "../../secret.json"}, operation="validate"),
                "SERVICE_ACTIONS_SCHEMA_PATH_FORBIDDEN",
            ),
            (
                public_contract("kether", {"script": "true", "actionsSchema": "../../secret.json"}, operation="validate"),
                "SERVICE_ACTIONS_SCHEMA_FORBIDDEN",
            ),
            (
                public_contract("ability", {"actionsSchema": {"actions": [{"name": "untrusted"}]}}),
                "SERVICE_ACTIONS_SCHEMA_FORBIDDEN",
            ),
            (
                public_contract("orchestrator", {
                    "steps": [{
                        "component": "ability",
                        "request": {"deep": {"actionsSchemaPath": "C:/secret.json"}},
                    }]
                }),
                "SERVICE_ACTIONS_SCHEMA_PATH_FORBIDDEN",
            ),
        ]
        for payload, expected_code in cases:
            with self.subTest(payload=payload):
                with tempfile.TemporaryDirectory(prefix="orryx-service-schema-path-") as directory:
                    with mock.patch.object(service_runner_module, "_load_default_actions_schema") as load_schema:
                        response = run_service_request(envelope(payload), workspace_root=directory)
                self.assertEqual("rejected", response["status"])
                self.assertIn(expected_code, error_codes(response))
                load_schema.assert_not_called()

    def test_rejects_policy_materialize_at_top_or_step_even_when_false(self) -> None:
        cases = [
            public_contract("ability", {}, policy={"materialize": False}),
            public_contract("orchestrator", {
                "steps": [{"component": "ability", "request": {}, "policy": {"materialize": True}}]
            }),
        ]
        for payload in cases:
            with self.subTest(payload=payload):
                self.assert_rejected(payload, "SERVICE_POLICY_MATERIALIZE_FORBIDDEN")

    def test_rejects_all_local_overwrite_allow_spellings_at_any_depth(self) -> None:
        for value in (True, "allow", "TRUE", "overwrite"):
            with self.subTest(value=value):
                self.assert_rejected(
                    public_contract("ability", {}, policy={"overwrite": value}),
                    "SERVICE_OVERWRITE_ALLOW_FORBIDDEN",
                )
        step = public_contract("orchestrator", {
            "steps": [{"component": "ability", "request": {}, "policy": {"overwrite": "allow"}}]
        })
        self.assert_rejected(step, "SERVICE_OVERWRITE_ALLOW_FORBIDDEN")

    def test_requires_strict_mode_and_denies_network_access(self) -> None:
        cases = [
            (public_contract("ability", {}, policy={"strict": False}), "SERVICE_STRICT_REQUIRED"),
            (public_contract("ability", {}, policy={"network": "allow"}), "SERVICE_NETWORK_ALLOW_FORBIDDEN"),
            (public_contract("orchestrator", {
                "steps": [{"component": "ability", "request": {}, "policy": {"network": "allow"}}]
            }), "SERVICE_NETWORK_ALLOW_FORBIDDEN"),
        ]
        for payload, code in cases:
            with self.subTest(code=code):
                self.assert_rejected(payload, code)

    def test_recursively_rejects_reload_server(self) -> None:
        cases = [
            public_contract("ability", {}, policy={"reloadServer": False}),
            public_contract("ability", {"deployment": {"reloadServer": True}}),
            public_contract("orchestrator", {
                "steps": [{"component": "ability", "request": {}, "policy": {"reloadServer": True}}]
            }),
        ]
        for payload in cases:
            with self.subTest(payload=payload):
                self.assert_rejected(payload, "SERVICE_RELOAD_SERVER_FORBIDDEN")

    def test_rejects_unknown_operations_at_top_or_step(self) -> None:
        cases = [
            public_contract("ability", {}, operation="execute-server"),
            public_contract("orchestrator", {
                "steps": [{"component": "ability", "operation": "write", "request": {}}]
            }),
        ]
        for payload in cases:
            with self.subTest(payload=payload):
                self.assert_rejected(payload, "SERVICE_OPERATION_UNSUPPORTED")

    def test_invalid_trusted_context_does_not_leak_host_details(self) -> None:
        response = run_service_request(
            envelope(public_contract("ability", {"key": "x", "type": "DIRECT", "actions": "true"})),
            workspace_root=object(),
        )
        self.assertEqual("rejected", response["status"])
        self.assertEqual({"SERVICE_CONTEXT_INVALID"}, error_codes(response))
        self.assertNotIn("object at", json.dumps(response, ensure_ascii=False))

    def test_unexpected_runtime_failure_is_returned_as_stable_envelope(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx-service-failure-") as directory:
            with mock.patch.object(service_runner_module, "run_contract", side_effect=RuntimeError("host path must not leak")):
                response = run_service_request(
                    envelope(public_contract("ability", {"key": "x", "type": "DIRECT", "actions": "true"})),
                    workspace_root=directory,
                    actions_schema={"actions": []},
                )
        self.assertEqual("rejected", response["status"])
        self.assertEqual({"SERVICE_EXECUTION_FAILED"}, error_codes(response))
        self.assertNotIn("host path", json.dumps(response, ensure_ascii=False))


class ServiceRunnerSchemaTest(unittest.TestCase):

    def test_schema_declares_output_envelope_and_all_runtime_error_codes(self) -> None:
        schema_path = SUITE_ROOT / "assets" / "contracts" / "service-runner-envelope.schema.json"
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
        self.assertEqual("1.0", schema["$defs"]["RequestEnvelope"]["properties"]["envelopeVersion"]["const"])
        self.assertEqual(
            ["generate", "validate", "plan"],
            schema["$defs"]["PublicOperation"]["enum"],
        )
        self.assertNotIn("materialize", schema["$defs"]["PublicComponent"]["enum"])
        declared_codes = set(schema["$defs"]["ServiceError"]["properties"]["code"]["enum"])
        self.assertEqual(set(SERVICE_ERROR_CODES), declared_codes)
        response_variants = schema["$defs"]["ResponseEnvelope"]["oneOf"]
        self.assertEqual({"completed", "rejected"}, {
            variant["properties"]["status"]["const"] for variant in response_variants
        })


if __name__ == "__main__":
    unittest.main()
