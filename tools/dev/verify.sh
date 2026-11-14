#!/usr/bin/env bash
# Unattended regression runner for the headless server self test. See docs/wiki/Development.md ("Regression suites").
# usage: tools/dev/verify.sh [--update] [suite ...]      (no suite = all of tools/dev/verify/suites/*.txt)
# Env: JAR_ROOT (main checkout holding the gitignored jar folders), AUTOSTOP (ticks, default 700), SUITE_TIMEOUT (seconds, default 900).
set -u
cd "$(dirname "$0")/../.."
ROOT="$PWD"
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Java/jdk-21}"
JAR_ROOT="${JAR_ROOT:-Y:/Programming/Minecraft/Retrofit}"
PY="${PYTHON:-python}"
SUITES_DIR=tools/dev/verify/suites
BASELINE=tools/dev/verify/baseline.json
TIMEOUT="${SUITE_TIMEOUT:-900}"
AUTOSTOP="${AUTOSTOP:-700}"
SEARCH="test-mods test-mods2 test-mods3 test-mods-1122 test-mods-147 test-mods-152 test-mods-164 test-mods-1710-extra test-mods-1201-a test-mods-1201-terramity test-mods-b173 run-subtest/mods run-subtest/mods.aside-20261113 run-screenshots/mods run-screenshots/mods.aside-20261113"
# read-only jar folders outside the checkout (absolute): ATM 9 holds the 1.20.1 mods, ATM 10 the native 1.21.1 libraries a 1.20.1 mod needs (suite lines `lib <fragment>`)
ATM9_MODS="${ATM9_MODS:-X:/Modded games/Prism Launcher/instances/ATM 9 20026/minecraft/mods}"
ATM10_MODS="${ATM10_MODS:-X:/Modded games/Prism Launcher/instances/ATM10 2026/minecraft/mods}"

update=""; names=()
for a in "$@"; do if [ "$a" = "--update" ]; then update="--update"; else names+=("$a"); fi; done
if [ ${#names[@]} -eq 0 ]; then for f in $SUITES_DIR/*.txt; do names+=("$(basename "$f" .txt)"); done; fi

mkdir -p build/devtools run-verify
RESULTS=build/devtools/verify-results.json
echo "[" > "$RESULTS"
first=1

# find_jar <fragment>: first file (jar/zip/litemod) whose name contains the fragment, case-insensitively, searching the folders in order
find_jar() {
  local frag="$1" dir="${2:-}" r f
  frag="${frag//\[/?}"; frag="${frag//\]/?}"   # glob brackets in a jar name match any character
  for r in ${dir:+__dir__} $SEARCH __atm9__; do
    case "$r" in
      __dir__) r="$dir";;
      __atm9__) r="$ATM9_MODS";;
      *) r="$JAR_ROOT/$r";;
    esac
    [ -d "$r" ] || continue
    f=$(find "$r" -type f \( -iname "*.jar" -o -iname "*.zip" -o -iname "*.litemod" \) -iname "*$frag*" 2>/dev/null | sort | head -1)
    if [ -n "$f" ]; then echo "$f"; return 0; fi
  done
  return 1
}

# kill only the server java of THIS worktree (its program-args file lives under this checkout's build directory)
kill_server() {
  powershell -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$ROOT/tools/dev/verify/kill-server.ps1")" -Root "$(cygpath -w "$ROOT")" >&2 2>/dev/null
}

emit() { # emit <json object>
  [ $first -eq 1 ] || echo "," >> "$RESULTS"
  first=0
  echo "$1" >> "$RESULTS"
}

idx=0
for s in "${names[@]}"; do
  idx=$((idx+1))
  file="$SUITES_DIR/$s.txt"
  if [ ! -f "$file" ]; then echo "unknown suite $s" >&2; emit "{\"suite\":\"$s\",\"skipped\":\"suite file\"}"; continue; fi
  echo "=== suite $s" >&2
  jars=(); missing=""
  while read -r ver frag _; do
    frag="${frag%$'\r'}"; ver="${ver%$'\r'}"
    case "$ver" in ""|\#*) continue;; esac
    if [ "$ver" = "lib" ]; then j=$(find_jar "$frag" "$ATM10_MODS") || { missing="$missing $frag"; continue; }
    else j=$(find_jar "$frag") || { missing="$missing $frag"; continue; }; fi
    jars+=("$ver|$j")
  done < "$file"
  if [ -n "$missing" ]; then echo "  skipped, missing:$missing" >&2; emit "{\"suite\":\"$s\",\"skipped\":\"${missing# }\"}"; continue; fi

  D="run-verify/$s"
  rm -rf "$D/mods" "$D/world" "$D/crash-reports" "$D/logs" "$D/.legacyconnector/cache"
  mkdir -p "$D/mods" "$D/logs"
  for e in "${jars[@]}"; do
    ver="${e%%|*}"; j="${e#*|}"
    if [ "$ver" = "lib" ]; then cp -f "$j" "$D/mods/"; else mkdir -p "$D/mods/$ver"; cp -f "$j" "$D/mods/$ver/"; fi
  done
  [ -f "$JAR_ROOT/run-subtest/eula.txt" ] && cp -f "$JAR_ROOT/run-subtest/eula.txt" "$D/eula.txt" || echo "eula=true" > "$D/eula.txt"
  if [ -f "$JAR_ROOT/run-subtest/server.properties" ]; then
    sed -e "s/^server-port=.*/server-port=$((25570+idx))/" -e "s/^query.port=.*/query.port=$((25570+idx))/" -e "s/^rcon.port=.*/rcon.port=$((25670+idx))/" \
        -e "s/^level-seed=.*/level-seed=1710/" "$JAR_ROOT/run-subtest/server.properties" > "$D/server.properties"
  else
    printf 'server-port=%s\nlevel-seed=1710\nonline-mode=false\n' $((25570+idx)) > "$D/server.properties"
  fi

  glog="build/devtools/verify-$s-gradle.log"
  ./gradlew runServerSubTest "-PsubTestDir=$D" "-PautostopTicks=$AUTOSTOP" > "$glog" 2>&1 &
  gpid=$!
  start=$SECONDS; timedout=""
  while kill -0 $gpid 2>/dev/null; do
    if [ $((SECONDS-start)) -ge "$TIMEOUT" ]; then timedout="timed out after ${TIMEOUT}s"; break; fi
    sleep 5
  done
  if [ -n "$timedout" ]; then kill_server; sleep 5; kill $gpid 2>/dev/null; fi
  wait $gpid 2>/dev/null
  kill_server

  log="$D/logs/latest.log"; [ -f "$log" ] || : > "$log"
  "$PY" tools/dev/verify/verify.py parse "$log" "$D/crash-reports" > "build/devtools/verify-$s-metrics.json"
  note=""; [ -n "$timedout" ] && note="$timedout"
  "$PY" - "$s" "build/devtools/verify-$s-metrics.json" "$note" >> "$RESULTS.tmp" <<'PYEOF'
import json, sys
print(json.dumps({"suite": sys.argv[1], "metrics": json.load(open(sys.argv[2])), **({"note": sys.argv[3]} if sys.argv[3] else {})}))
PYEOF
  emit "$(cat "$RESULTS.tmp")"; rm -f "$RESULTS.tmp"
done
echo "]" >> "$RESULTS"

"$PY" tools/dev/verify/verify.py report "$BASELINE" "$RESULTS" $update | tee build/devtools/verify-report.txt
rc=${PIPESTATUS[0]}
mkdir -p build/devtools
exit $rc
