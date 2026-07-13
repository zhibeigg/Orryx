#!/bin/sh
set -eu

SKILL_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
SUITE_INSTALLER="$SKILL_DIR/../../install.sh"

if [ ! -f "$SUITE_INSTALLER" ]; then
    printf '%s\n' "错误：未找到 orryx-creation-suite 根安装器。此组件不能单独提取安装；请恢复完整套件目录，或安装同级 orryx-creation-suite-runtime。" >&2
    exit 1
fi

exec sh "$SUITE_INSTALLER" "$@"
