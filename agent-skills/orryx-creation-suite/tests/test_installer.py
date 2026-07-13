from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


SUITE_ROOT = Path(__file__).resolve().parents[1]
INSTALLER_PATH = SUITE_ROOT / "scripts" / "install_suite.py"
SPEC = importlib.util.spec_from_file_location("install_suite", INSTALLER_PATH)
assert SPEC is not None and SPEC.loader is not None
install_suite = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(install_suite)


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
            self.assertTrue((root / component / ".orryx-suite-install.json").is_file())

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


if __name__ == "__main__":
    unittest.main()
