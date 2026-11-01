#!/usr/bin/env bash
# Survival probe of one mod namespace in the running control session (SCENARIO=control tools/dev/quick-shots.sh):
# block drops, mob kill drops, chest/fishing loot tables and recipes. Usage: tools/dev/survival-probe.sh <namespace> [block-drops|mob-drops|loot-tables|recipes ...]
# Run from the project root; a survival-mode player must exist. Results print to the terminal.
set -e
NS="$1"; shift
PARTS=("$@"); [ ${#PARTS[@]} -eq 0 ] && PARTS=(recipes block-drops mob-drops loot-tables)
TMP=$(mktemp -d)
for P in "${PARTS[@]}"; do
  { echo "var ns_to_test=\"$NS\";"; cat "tools/dev/probe/$P.js"; } > "$TMP/$P.js"
  echo "== $NS: $P"
  tools/dev/js.sh -f "$TMP/$P.js" | tail -5 | cut -c1-1200
done
rm -rf "$TMP"
