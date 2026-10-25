#!/bin/bash
# Sends remote-control commands to a running control-session client (SCENARIO=control tools/dev/quick-shots.sh), one per
# argument, each waiting for its answer:
#   tools/dev/ctl.sh "cmd give @s thaumcraft:wandcasting" "slot 0" "useat 10 64 10" "shot after"
# Screenshots land in run-screenshots/control/out/<name>.png. CONTROL overrides the control directory.
cd "$(dirname "$0")/../.." || exit 1
dir="${CONTROL:-run-screenshots/control}"
mkdir -p "$dir/in"
for line in "$@"; do
  id="c$(date +%s%N)"
  printf '%s' "$line" > "$dir/in/$id.tmp" && mv "$dir/in/$id.tmp" "$dir/in/$id.cmd"
  ok=
  for _ in $(seq 1 900); do
    if [ -f "$dir/out/$id.txt" ]; then echo "$line -> $(cat "$dir/out/$id.txt")"; ok=1; break; fi
    sleep 0.2
  done
  [ -z "$ok" ] && { echo "$line -> no answer (is a control session running?)" >&2; exit 1; }
done
