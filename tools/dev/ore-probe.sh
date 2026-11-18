#!/usr/bin/env bash
# Ore census and vein connectivity of one mod set over an 11x11 chunk area (same seed every run), on a headless server: the before/after
# check of the deep layer (docs/wiki/World-Generation.md, "The deep layer"). One java at a time; needs >= 4000 MB free RAM.
# usage: tools/dev/ore-probe.sh <1.7.10|1.12.2> <label> <jar>[,<jar>...] [config line ...]
#   config lines go to config/legacyconnector.properties of the run, e.g. "deepLayer.mode = split" (the default is shifted)
# Output: build/devtools/ore-probe-<label>.json (see tools/dev/server-scripts/ore-connectivity.js) and a summary line;
#   py -3 tools/dev/render_ore_slice.py build/devtools/ore-probe-<label>.json out.png draws the vertical slice.
set -u
cd "$(dirname "$0")/../.."
ROOT="$(pwd -W 2>/dev/null || pwd)"
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Java/jdk-21}"
ver="$1"; label="$2"; jars="$3"; shift 3
D="run-oreprobe-$label"
OUT="$ROOT/build/devtools/ore-probe-$label.json"
mkdir -p build/devtools
rm -rf "$D/mods" "$D/world" "$D/config" "$D/logs" "$D/.legacyconnector/cache"
mkdir -p "$D/mods/$ver" "$D/config" "$D/logs"
IFS=',' read -r -a list <<< "$jars"
for j in "${list[@]}"; do cp -f "$j" "$D/mods/$ver/"; done
echo "eula=true" > "$D/eula.txt"
printf 'server-port=%s\nlevel-seed=1710\nonline-mode=false\nmax-tick-time=-1\nspawn-protection=0\n' "${ORE_PROBE_PORT:-25777}" > "$D/server.properties"
: > "$D/config/legacyconnector.properties"
for line in "$@"; do echo "$line" >> "$D/config/legacyconnector.properties"; done
rm -f "$OUT"
script="build/devtools/serverscript-ore-$label.js"
sed "s#@OUT@#$OUT#" tools/dev/server-scripts/ore-connectivity.js > "$script"
./gradlew runServerSubTest "-PsubTestDir=$D" "-PautostopTicks=${AUTOSTOP:-1500}" "-PserverTickScriptFrom=100" "-PserverTickScript=$ROOT/$script" > "build/devtools/ore-probe-$label-gradle.log" 2>&1
grep -h "OREPROBE" "$D/logs/latest.log" | cut -c1-600
[ -f "$OUT" ] && echo "wrote $OUT" || echo "NO RESULT (see $D/logs/latest.log)"
