#!/usr/bin/env bash
# PostToolUse (Edit|Write): auto-format Kotlin files with ktlint.
# Best-effort: exits 0 silently if ktlint is not installed (brew install ktlint)
# or the edited file is not a Kotlin file.
file=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty')
[[ "$file" == *.kt || "$file" == *.kts ]] || exit 0
command -v ktlint >/dev/null 2>&1 || exit 0
ktlint --format "$file" >/dev/null 2>&1 || true
