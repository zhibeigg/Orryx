from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
SHARED = ROOT / "shared"
if str(SHARED) not in sys.path:
    sys.path.insert(0, str(SHARED))

from orryx_toolkit.service_http import ServiceHttpConfig, serve


if __name__ == "__main__":
    serve(ServiceHttpConfig.from_environment())
