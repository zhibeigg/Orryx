#!/usr/bin/env python3
"""对 Orryx Creation Suite 执行离线秘密、网络、写盘与危险命令扫描。"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

SUITE_ROOT = Path(__file__).resolve().parents[1]
TEXT_SUFFIXES = {".py", ".json", ".md", ".yml", ".yaml", ".sh", ".ps1", ".txt"}
SECRET_PATTERNS = {
    "PRIVATE_KEY": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "AWS_ACCESS_KEY": re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    "GITHUB_TOKEN": re.compile(r"\bgh[pousr]_[A-Za-z0-9]{30,}\b"),
    "OPENAI_KEY": re.compile(r"\bsk-[A-Za-z0-9_-]{20,}\b"),
    "CREDENTIAL_URL": re.compile(r"(?:mysql|mongodb|redis|https?)://[^\s/:@]+:[^\s@]+@", re.I),
}
NETWORK_CODE = re.compile(
    r"(?:^|\n)\s*(?:import|from)\s+(?:requests|httpx|aiohttp|socket|urllib\.request)\b|"
    r"\b(?:curl|wget|Invoke-WebRequest|Invoke-RestMethod)\b",
    re.I,
)
DESTRUCTIVE = re.compile(r"\b(?:git\s+reset\s+--hard|git\s+clean\s+-f|rm\s+-rf|Remove-Item\s+[^\n]*-Recurse[^\n]*-Force)\b", re.I)
DIRECT_WRITE = re.compile(r"\.(?:write_text|write_bytes)\s*\(|\.open\(\s*[rubf]*[\"'](?:w|a|x)|\bos\.replace\s*\(")


def diagnostic(code: str, path: Path, message: str) -> dict[str, str]:
    return {"severity": "error", "code": code, "path": path.relative_to(SUITE_ROOT).as_posix(), "message": message}


def main() -> int:
    diagnostics: list[dict[str, str]] = []
    checks: list[dict[str, str]] = []
    files = sorted(
        (path for path in SUITE_ROOT.rglob("*") if path.is_file() and path.suffix.casefold() in TEXT_SUFFIXES),
        key=lambda path: path.as_posix().casefold(),
    )
    for path in files:
        relative = path.relative_to(SUITE_ROOT).as_posix()
        if "__pycache__" in path.parts:
            diagnostics.append(diagnostic("GENERATED_CACHE_PRESENT", path, "发布目录不得包含 __pycache__"))
            continue
        try:
            text = path.read_text(encoding="utf-8-sig")
        except UnicodeError as exc:
            diagnostics.append(diagnostic("TEXT_ENCODING_INVALID", path, str(exc)))
            continue
        for name, pattern in SECRET_PATTERNS.items():
            if pattern.search(text):
                diagnostics.append(diagnostic(f"SECRET_{name}", path, "检测到疑似真实秘密或凭据"))
        if path.suffix.casefold() in {".py", ".sh", ".ps1"} and path.name != "security_scan.py":
            if NETWORK_CODE.search(text):
                diagnostics.append(diagnostic("NETWORK_CODE_FOUND", path, "套件必须离线运行，禁止网络客户端或下载命令"))
            if DESTRUCTIVE.search(text):
                diagnostics.append(diagnostic("DESTRUCTIVE_COMMAND_FOUND", path, "检测到破坏性命令"))
            if "shell=True" in text and path.name != "run_evals.py":
                diagnostics.append(diagnostic("UNSCOPED_SHELL_EXECUTION", path, "只有受控 Eval runner 可执行本地 criterion shell"))
        if relative.startswith("shared/orryx_toolkit/") and path.suffix.casefold() == ".py":
            allowed_write_modules = {"materialize.py", "cli.py"}
            if path.name not in allowed_write_modules and DIRECT_WRITE.search(text):
                diagnostics.append(diagnostic("DOMAIN_RUNNER_DIRECT_WRITE", path, "领域 Runtime 不得直接写盘；只能返回 artifacts"))

    contracts = (SUITE_ROOT / "shared" / "orryx_toolkit" / "contracts.py").read_text(encoding="utf-8")
    materialize = (SUITE_ROOT / "shared" / "orryx_toolkit" / "materialize.py").read_text(encoding="utf-8")
    workspace = (SUITE_ROOT / "shared" / "orryx_toolkit" / "workspace.py").read_text(encoding="utf-8")
    invariants = {
        "NETWORK_DEFAULT_DENY": 'setdefault("network", "deny")' in contracts,
        "OVERWRITE_DEFAULT_DENY": 'get("overwrite", "deny")' in materialize,
        "HASH_VERIFIED_BEFORE_WRITE": "MATERIALIZE_HASH_MISMATCH" in materialize and "hashlib.sha256" in materialize,
        "PATH_CONTAINMENT_USED": "safe_join(root, relative)" in materialize and "os.path.commonpath" in workspace,
        "ATOMIC_REPLACE_USED": "os.replace(temp, target)" in materialize,
        "NO_AUTOMATIC_RELOAD": "reloadServer(" not in "\n".join(
            path.read_text(encoding="utf-8-sig")
            for path in files
            if path.suffix.casefold() == ".py" and "shared/orryx_toolkit/" in path.relative_to(SUITE_ROOT).as_posix()
        ),
    }
    for code, passed in invariants.items():
        if passed:
            checks.append({"code": code, "status": "pass"})
        else:
            diagnostics.append({"severity": "error", "code": code, "path": "shared/orryx_toolkit", "message": "安全不变量未满足"})

    diagnostics.sort(key=lambda item: (item["path"], item["code"], item["message"]))
    checks.sort(key=lambda item: item["code"])
    result = {
        "status": "invalid" if diagnostics else "ok",
        "scannedFiles": len(files),
        "diagnostics": diagnostics,
        "checks": checks,
    }
    sys.stdout.write(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n")
    return 1 if diagnostics else 0


if __name__ == "__main__":
    raise SystemExit(main())
