#!/usr/bin/env bash
# Stop hook: before Claude finishes a turn that changed code, runs the same quality gate as CI
# (spotlessCheck, detekt, unit tests, translations). On failure it blocks the stop (exit 2) and
# feeds the errors back to Claude. A diff that already passed is not re-verified, and after
# MAX_BLOCKS consecutive failures it only warns the user, so it never loops forever.
set -uo pipefail

MAX_BLOCKS=3
cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}" || exit 0
state_dir=".claude/.verify"
mkdir -p "$state_dir"

changed=$(git status --porcelain --untracked-files=all | awk '{print $NF}' |
    grep -E '\.(kt|kts|xml|toml|py|pro)$' || true)
[[ -z "$changed" ]] && exit 0

diff_hash=$({
    git diff HEAD
    git ls-files --others --exclude-standard -z | xargs -0 -r cat
} | sha256sum | cut -d' ' -f1)
[[ "$(cat "$state_dir/passed" 2>/dev/null)" == "$diff_hash" ]] && exit 0

tasks=(spotlessCheck detekt testDebugUnitTest)
grep -q '\.gradle\.kts$' <<<"$changed" && tasks=(sortDependencies "${tasks[@]}")

log="$state_dir/last-run.log"
status=0
./gradlew "${tasks[@]}" --parallel --continue -q >"$log" 2>&1 || status=1
strings_out=$(python3 check_strings.py 2>&1) || status=1

if [[ $status -eq 0 ]]; then
    echo "$diff_hash" >"$state_dir/passed"
    rm -f "$state_dir/blocks"
    exit 0
fi

blocks=$(($(cat "$state_dir/blocks" 2>/dev/null || echo 0) + 1))
echo "$blocks" >"$state_dir/blocks"

if [[ $blocks -gt $MAX_BLOCKS ]]; then
    rm -f "$state_dir/blocks"
    jq -n --arg log "$log" \
        '{systemMessage: ("Quality gate still failing after several attempts. See " + $log)}'
    exit 0
fi

{
    echo "Quality gate failed (./gradlew ${tasks[*]} + check_strings.py). Fix it before finishing."
    echo "Do not weaken tests, detekt rules or baselines to make this pass. Full log: $log"
    grep -E 'FAILED|error|Error|e: |w: |failed|\.kt:[0-9]+' "$log" | grep -v '^\s*at ' | head -60
    [[ -n "$strings_out" ]] && echo "$strings_out" | head -30
} >&2
exit 2
