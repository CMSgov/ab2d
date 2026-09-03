#!/usr/bin/env bash

# Regenerate Gradle dependency lockfiles across projects
# Run this script whenever changes are made to gradle project dependencies
set -euo pipefail

# Add any future gradle projects to this list
BUILDS=(lambdas libs contracts events idr-db-importer)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

REPO_ROOT="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel 2>/dev/null || (cd "${SCRIPT_DIR}/.." && pwd))"


regenerate() {
    local dir="$1"
    local path="${REPO_ROOT}/${dir}"

    if [[ ! -x "${path}/gradlew" ]]; then
        echo "!! ${dir}: no gradlew wrapper found, skipping" >&2
        return 1
    fi

    local task="dependencies"
    if "${path}/gradlew" -p "${path}" -q help --task resolveAndLockAll >/dev/null 2>&1; then
        task="resolveAndLockAll"
    fi

    echo "==> ${dir}: ./gradlew ${task} --write-locks"
    "${path}/gradlew" -p "${path}" "${task}" --write-locks --no-configuration-cache
}

failed=()
for dir in "${BUILDS[@]}"; do
    if ! regenerate "${dir}"; then
        failed+=("${dir}")
    fi
done

echo
if [[ ${#failed[@]} -eq 0 ]]; then
    echo "All lockfiles regenerated."
else
    echo "Completed with failures: ${failed[*]}" >&2
    exit 1
fi
