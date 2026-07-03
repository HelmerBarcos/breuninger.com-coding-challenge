#!/usr/bin/env bash
# Stop: run fast unit tests (integration *IT tests are skipped — too slow for a
# hook). If tests fail, exit 2 blocks the stop and feeds the failure back to
# Claude so no turn ends with a red build.
input=$(cat)
# Prevent infinite loops: if we are already stopping because of this hook, pass.
[[ $(printf '%s' "$input" | jq -r '.stop_hook_active // false') == "true" ]] && exit 0
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
# No-op until the Maven project is scaffolded (phase A1).
[[ -x ./mvnw && -f pom.xml ]] || exit 0
if ! out=$(./mvnw -q -DskipITs test 2>&1); then
  echo "Unit tests failed after this turn:" >&2
  printf '%s\n' "$out" | tail -40 >&2
  exit 2
fi
