#!/bin/bash
# Evaluates JavaScript inside a running control-session client (SCENARIO=control tools/dev/quick-shots.sh) and prints the answer.
#   tools/dev/js.sh 'server.getPlayerCount()'          inline
#   tools/dev/js.sh -f probe.js                         from a file (several lines)
# Bindings: mc (client), server (integrated server), log. Java classes: Java.type("net.minecraft.core.BlockPos").
# CONTROL overrides the control directory (default run-screenshots/control).
cd "$(dirname "$0")/../.." || exit 1
dir="${CONTROL:-run-screenshots/control}"
mkdir -p "$dir/in"
if [ "$1" = "-f" ]; then code="$(cat "$2")"; else code="$*"; fi
id="js$(date +%s%N)"
printf 'js %s' "$code" > "$dir/in/$id.tmp" && mv "$dir/in/$id.tmp" "$dir/in/$id.cmd"
for _ in $(seq 1 600); do
  if [ -f "$dir/out/$id.txt" ]; then cat "$dir/out/$id.txt"; exit 0; fi
  sleep 0.2
done
echo "no answer (is a control session running?)" >&2
exit 1
