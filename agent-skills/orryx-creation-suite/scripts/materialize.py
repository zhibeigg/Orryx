#!/usr/bin/env python3
"""显式执行 Orryx Artifact materialize；默认拒绝覆盖。"""
from __future__ import annotations

import sys
from pathlib import Path

SUITE_ROOT = Path(__file__).resolve().parents[1]
SHARED_ROOT = SUITE_ROOT / "shared"
if str(SHARED_ROOT) not in sys.path:
    sys.path.insert(0, str(SHARED_ROOT))

from orryx_toolkit.cli import main


if __name__ == "__main__":
    raise SystemExit(main(["materialize", *sys.argv[1:]]))
