#!/bin/bash
# Runs JavaScript on a headless server started with -PserverScript (see core/ServerScript.java) and prints the answer.
#   ./gradlew runServerSubTest -PautostopTicks=0 -PserverScript=probe -PserverScriptMod=fiskheroes   (directory is relative to run-subtest)
#   tools/dev/server-script.sh -f probe.js        or        tools/dev/server-script.sh 'server.getPlayerCount()'
# Globals persist between calls (one Nashorn engine); a script that defines onTick(n) is called every tick until it returns true.
cd "$(dirname "$0")/../.." || exit 1
dir="${PROBE_DIR:-run-subtest/probe}"
mkdir -p "$dir"
if [ "$1" = "-f" ]; then code="$(cat "$2")"; else code="$*"; fi
id="s$(date +%s%N)"
printf '%s' "$code" > "$dir/$id.tmp" && mv "$dir/$id.tmp" "$dir/$id.js"
for _ in $(seq 1 ${PROBE_WAIT:-300}); do
  if [ -f "$dir/$id.out" ]; then cat "$dir/$id.out"; echo; rm -f "$dir/$id.out" "$dir/$id.js.done"; exit 0; fi
  sleep 0.2
done
echo "no answer (is a server with -PserverScript running?)" >&2
exit 1
