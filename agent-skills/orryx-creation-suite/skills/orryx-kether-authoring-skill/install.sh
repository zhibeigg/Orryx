#!/usr/bin/env bash
set -euo pipefail
skill_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
installer="$skill_dir/../../install.sh"
if [[ ! -f "$installer" ]]; then
  printf '%s\n' 'ERROR: 此组件不能单独安装；请保留完整 orryx-creation-suite 并运行套件根 install.sh。' >&2
  exit 2
fi
exec bash "$installer" "$@"
