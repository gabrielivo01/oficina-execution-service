#!/usr/bin/env sh
set -eu
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
repo_root=$(git -C "$script_dir" rev-parse --show-toplevel)
if [ "$repo_root" != "$script_dir" ]; then
    printf '%s\n' "Este diretório ainda não é um repositório Git próprio; hooks não configurados." >&2
    exit 1
fi
git config core.hooksPath .githooks
printf '%s\n' "Hooks configurados para .githooks."
