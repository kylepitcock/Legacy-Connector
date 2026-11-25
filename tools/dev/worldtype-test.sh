#!/usr/bin/env bash
# World type server test: boots a headless server with the given jars and level-type, probes the overworld (tools/dev/server-scripts/worldtype-probe.js:
# presets, spawn, surface heights/biomes over chunk areas) and stops. Output: build/devtools/worldtype-<label>.txt (also the WORLDTYPE log block).
# usage: tools/dev/worldtype-test.sh <label> <level-type|default> <mc-version dir, e.g. 1.7.10> "<areas, space separated, e.g. "-4,-4,4,4 20,20,22,22">" [--keep-world] <jar>...
#   level-type: a preset id such as atg:atg, or `default` for minecraft:normal (the vanilla baseline); --keep-world reuses run-wt/<label>/world and mods (restart check)
# Env: SEED (default 1710), PORT (default 42190), TICKS (autostop, default 400), FROM (first script tick, default 60). One java at a time; check free RAM first.
set -u
cd "$(dirname "$0")/../.."
ROOT="$(pwd -W 2>/dev/null || pwd)"
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Java/jdk-21}"
JAR_ROOT="${JAR_ROOT:-Y:/Programming/Minecraft/Retrofit}"
label="$1"; ltype="$2"; ver="$3"; areas="$4"; shift 4
keep=""; if [ "${1:-}" = "--keep-world" ]; then keep=1; shift; fi
D="run-wt/$label"
mkdir -p "$D" build/devtools
rm -rf "$D/crash-reports" "$D/logs"; mkdir -p "$D/logs"
if [ -z "$keep" ]; then
  rm -rf "$D/world" "$D/mods" "$D/config"; mkdir -p "$D/mods/$ver"
  for j in "$@"; do cp -f "$j" "$D/mods/$ver/"; done
fi
echo "eula=true" > "$D/eula.txt"
[ "$ltype" = "default" ] && ltype="minecraft:normal"
{ sed -e '/^level-type=/d' -e '/^max-tick-time=/d' -e '/^level-seed=/d' -e '/^server-port=/d' -e '/^query.port=/d' -e '/^online-mode=/d' "$JAR_ROOT/run-subtest/server.properties" 2>/dev/null
  echo "level-type=${ltype//:/\\:}"; echo "level-seed=${SEED:-1710}"; echo "max-tick-time=-1"; echo "server-port=${PORT:-42190}"; echo "online-mode=false"; } > "$D/server.properties"
js="build/devtools/worldtype-$label.js"
{
  printf 'var AREAS = ['
  read -ra A <<< "$areas"
  for a in "${A[@]}"; do printf '[%s],' "$a"; done
  printf '];\nvar LABEL = "%s";\nvar OUTDIR = "%s";\n' "$label" "$ROOT/build/devtools"
  cat tools/dev/server-scripts/worldtype-probe.js
} > "$js"
rm -f "build/devtools/worldtype-$label.txt"
./gradlew runServerSubTest "-PsubTestDir=$D" "-PautostopTicks=${TICKS:-400}" "-PserverTickScriptFrom=${FROM:-60}" "-PserverTickScript=$ROOT/$js" > "build/devtools/worldtype-$label-gradle.log" 2>&1
echo "gradle exit $?"
if [ -f "build/devtools/worldtype-$label.txt" ]; then
  cat "build/devtools/worldtype-$label.txt"
else
  echo "NO PROBE OUTPUT; log:"; grep -a -E "ERROR|Exception|SERVERSCRIPT" "$D/logs/latest.log" | cut -c1-260 | head -20
fi
echo "--- log excerpts"
grep -a -E "WorldTypes|Offering|Done \(|ERROR|FATAL" "$D/logs/latest.log" | cut -c1-260 | head -30
