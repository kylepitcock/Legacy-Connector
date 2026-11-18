#!/bin/bash
# Sends player verbs / commands (one per argument) to a running control session and prints the answers.
#   tools/dev/ctl.sh "cmd /gamemode survival" "walk forward 20" "shot name"
cd "$(dirname "$0")/../.." || exit 1
dir="${CONTROL:-run-screenshots/control}"
mkdir -p "$dir/in"
for line in "$@"; do
  id="c$(date +%s%N)"
  printf '%s' "$line" > "$dir/in/$id.tmp" && mv "$dir/in/$id.tmp" "$dir/in/$id.cmd"
  for _ in $(seq 1 600); do
    if [ -f "$dir/out/$id.txt" ]; then echo "[$line] $(cat "$dir/out/$id.txt")"; break; fi
    sleep 0.2
  done
done
