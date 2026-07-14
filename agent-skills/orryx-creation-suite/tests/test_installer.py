from __future__ import annotations

import importlib.util
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

from orryx_toolkit.contracts import SUITE_VERSION

EXPECTED_SUITE_VERSION = "1.1.0"
INSTALLER_PATH = SUITE_ROOT / "scripts" / "install_suite.py"
SPEC = importlib.util.spec_from_file_location("install_suite", INSTALLER_PATH)
assert SPEC is not None and SPEC.loader is not None
install_suite = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(install_suite)


def skill_frontmatter(path: Path) -> dict:
    text = path.read_text(encoding="utf-8")
    parts = text.split("---", 2)
    if len(parts) != 3:
        raise AssertionError(f"SKILL.md 缺少 YAML frontmatter: {path}")
    value = yaml.safe_load(parts[1])
    if not isinstance(value, dict):
        raise AssertionError(f"SKILL.md frontmatter 不是 object: {path}")
    return value


class InstallSuiteTest(unittest.TestCase):

    def test_parse_components_rejects_unknown_name(self) -> None:
        with self.assertRaises(ValueError):
            install_suite.parse_components("orryx-unknown-skill")

    def test_dry_run_does_not_write(self) -> None:
        with tempfile.TemporaryDirectory(prefix="orryx suite dry run ") as directory:
            root = Path(directory) / "skills"
            code = install_suite.main([
                "--path",
                str(root),
                "--components",
                "orryx-config-validator-skill",
                "--dry-run",
            ])
            self.assertEqual(0, code)
            self.assertFalse(root.exists())

    def test_install_marks_runtime_and_component(self) -> None:
        with tempfile.TemporaryDirectory(prefix="Orryx技能套件-") as directory:
            root = Path(directory) / "skills"
            component = "orryx-config-validator-skill"
            code = install_suite.main([
                "--path",
                str(root),
                "--components",
                component,
            ])
            self.assertEqual(0, code)
            self.assertTrue((root / "orryx-creation-suite" / "SKILL.md").is_file())
            self.assertTrue((root / "orryx-creation-suite-runtime" / "orryx_toolkit").is_dir())
            self.assertTrue((root / component / "SKILL.md").is_file())
            marker_path = root / component / ".orryx-suite-install.json"
            self.assertTrue(marker_path.is_file())
            marker = json.loads(marker_path.read_text(encoding="utf-8"))
            self.assertEqual(EXPECTED_SUITE_VERSION, marker["suiteVersion"])

            rejected = install_suite.main([
                "--path",
                str(root),
                "--components",
                component,
            ])
            self.assertEqual(1, rejected)

            updated = install_suite.main([
                "--path",
                str(root),
                "--components",
                component,
                "--force",
            ])
            self.assertEqual(0, updated)

    def test_suite_version_is_consistent_across_runtime_installer_manifest_and_skill_metadata(self) -> None:
        self.assertEqual(EXPECTED_SUITE_VERSION, SUITE_VERSION)
        self.assertEqual(EXPECTED_SUITE_VERSION, install_suite.VERSION)

        manifest_path = SUITE_ROOT / "assets" / "contracts" / "orchestrator-manifest.json"
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        self.assertEqual("1.0", manifest["contractVersion"])
        self.assertEqual(EXPECTED_SUITE_VERSION, manifest["suiteVersion"])

        skill_paths = [SUITE_ROOT / "SKILL.md", *sorted((SUITE_ROOT / "skills").glob("*/SKILL.md"))]
        self.assertEqual(11, len(skill_paths))
        for path in skill_paths:
            with self.subTest(path=path.relative_to(SUITE_ROOT).as_posix()):
                metadata = skill_frontmatter(path)
                self.assertEqual(EXPECTED_SUITE_VERSION, metadata["metadata"]["version"])
                if path == SUITE_ROOT / "SKILL.md":
                    self.assertEqual(EXPECTED_SUITE_VERSION, metadata["provenance"]["version"])

        for name in ("component-input.schema.json", "input-contract.schema.json"):
            with self.subTest(schema=name):
                schema_path = SUITE_ROOT / "assets" / "contracts" / name
                schema = json.loads(schema_path.read_text(encoding="utf-8"))
                compatibility = schema["x-runtimeCompatibility"]
                self.assertEqual("1.0", compatibility["runtimeContractVersion"])
                self.assertEqual(EXPECTED_SUITE_VERSION, compatibility["runtimeSuiteVersion"])

        eval_source = (SUITE_ROOT / "scripts" / "run_evals.py").read_text(encoding="utf-8")
        self.assertIn(f'"suiteVersion": "{EXPECTED_SUITE_VERSION}"', eval_source)


if __name__ == "__main__":
    unittest.main()
