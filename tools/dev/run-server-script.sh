#!/usr/bin/env bash
# Runs a gameplay scenario of tools/dev/server-scripts on a headless server (no window): the scenario is a JavaScript file evaluated on every
# server tick (see core/ServerScript), prefixed by lib-rotary.js (helpers: platform, place with a fake player, tile/field access, task scheduler) and
# rig.js (engine -> shafts -> machine builders). It places real machines, powers them, ticks for hundreds of ticks and prints what it reads.
#
# LIBS="lib" replaces the default prelude (lib-rotary + rig) by those files of server-scripts (ChromatiCraft scenarios use LIBS=lib).
# usage: tools/dev/run-server-script.sh <scenario> [autostopTicks=600] [firstScriptTick=100] [--fresh]
#   scenario: a file name without .js in tools/dev/server-scripts/scenarios (or a path to a .js file)
#   --fresh: deletes run-subtest/world first (scenarios that place at fixed coordinates want that after a failed run)
# Put the jars under test in run-subtest/mods/1.7.10 (and set server-port in run-subtest/server.properties when other servers run).
# The scripts only call the scenario "ticks" counted from the first script run; the server needs ~100 ticks of its own to finish loading.
set -e
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Java/jdk-21}"
scen="$1"; ticks="${2:-600}"; from="${3:-100}"
[ -z "$scen" ] && { sed -n 2,12p "$0"; exit 1; }
file="tools/dev/server-scripts/scenarios/$scen.js"; [ -f "$file" ] || file="$scen"
out="build/devtools/serverscript-$(basename "$file" .js).js"
mkdir -p build/devtools run-subtest/logs
if [ -n "$LIBS" ]; then : > "$out"; for l in $LIBS; do cat "tools/dev/server-scripts/$l.js" >> "$out"; echo >> "$out"; done; cat "$file" >> "$out"; else cat tools/dev/server-scripts/lib-rotary.js tools/dev/server-scripts/rig.js "$file" > "$out"; fi
[[ " $* " == *" --fresh "* ]] && rm -rf run-subtest/world
rm -f run-subtest/logs/latest.log
./gradlew runServerSubTest "-PautostopTicks=$ticks" "-PserverTickScriptFrom=$from" "-PserverTickScript=$PWD/$out" > "build/devtools/serverscript-run.log" 2>&1 || true
# the script's output is the "SERVERSCRIPT:" log lines (a multi-line result continues on lines that do not start with a timestamp)
awk '/^\[[0-9]+[A-Za-z]+[0-9]+ /{show=($0 ~ /SERVERSCRIPT/ || $0 ~ /Server thread\/(ERROR|WARN)/)} show && !/^\tat |^Caused by|more$/' run-subtest/logs/latest.log | cut -c1-400
