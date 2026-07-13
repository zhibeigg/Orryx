#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

if command -v py >/dev/null 2>&1; then
    exec py -3 "${SCRIPT_DIR}/scripts/install_suite.py" "$@"
elif command -v python3 >/dev/null 2>&1; then
    exec python3 "${SCRIPT_DIR}/scripts/install_suite.py" "$@"
elif command -v python >/dev/null 2>&1; then
    exec python "${SCRIPT_DIR}/scripts/install_suite.py" "$@"
else
    printf '%s\n' "ERROR: Python 3.10 or newer is required." >&2
    exit 1
fi
