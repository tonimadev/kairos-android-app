#!/usr/bin/env bash
# PostToolUse (Write|Edit): formats the edited Kotlin file with the project's Spotless/ktlint setup.
# Exit 2 feeds violations that ktlint cannot auto-fix back to Claude.
set -uo pipefail

file=$(jq -r '.tool_response.filePath // .tool_input.file_path // empty')
case "$file" in
    *.kt | *.kts) ;;
    *) exit 0 ;;
esac
[[ -f "$file" ]] || exit 0

cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}" || exit 0

if ! out=$(./gradlew -q spotlessApply -PspotlessIdeHook="$file" 2>&1); then
    echo "Spotless could not format $file:" >&2
    echo "$out" | tail -40 >&2
    exit 2
fi
exit 0
