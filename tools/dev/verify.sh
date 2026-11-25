#!/usr/bin/env bash
# Unattended regression runner for the headless server self test. See docs/wiki/Development.md ("Regression suites").
# usage: tools/dev/verify.sh [--update] [--list] [--resume <run-id>] [suite ...]      (no suite = all of tools/dev/verify/suites/*.txt)
# Every run has an id (printed first, with the git revision). Each suite's result is written to build/devtools/verify-<suite>-result.json the moment it finishes, so a run
# that dies midway loses nothing: `--resume <run-id> [same suites]` skips suites that already finished in that run at the same revision (SETUP, SKIP and timed-out suites run
# again) and prints the full table at the end. `python tools/dev/verify/verify.py report --run <id>` rebuilds the table of a run (also a dead one) without running anything.
# Test hooks: VERIFY_FAKE=<dir> reads <dir>/<suite>.log instead of resolving jars and starting a server; VERIFY_FAKE_DIE_AFTER=<n> exits abruptly after n suites run.
# --list: dry run, only prints which jar each suite line resolves to (status ok|missing|setup, see tools/dev/verify/picker.py); starts nothing.
# Env: GRADLE_EXTRA (extra Gradle arguments, e.g. "-Pjfr=C:/tmp/a.jfr -PmsptLog=600 -PmemoryLog"), JAR_ROOT (main checkout holding the gitignored jar folders), AUTOSTOP (ticks, default 700), SUITE_TIMEOUT (seconds, default 900).
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
SEARCH="test-mods-issues/298 test-mods-issues test-mods test-mods2 test-mods3 test-mods-1122 test-mods-147 test-mods-152 test-mods-164 test-mods-1710-extra test-mods-1201-a test-mods-1201-terramity test-mods-b173 run-subtest/mods run-subtest/mods.aside-20261113 run-screenshots/mods run-screenshots/mods.aside-20261113 test-mods-mcarchive/b1.7.3 test-mods-mcarchive/1.2.5"
# read-only jar folders outside the checkout (absolute): ATM 9 holds the 1.20.1 mods, ATM 10 the native 1.21.1 libraries a 1.20.1 mod needs (suite lines `lib <fragment>`)
ATM9_MODS="${ATM9_MODS:-X:/Modded games/Prism Launcher/instances/ATM 9 20026/minecraft/mods}"
ATM10_MODS="${ATM10_MODS:-X:/Modded games/Prism Launcher/instances/ATM10 2026/minecraft/mods}"

update=""; listonly=""; names=(); resume=""; expect_run=""
for a in "$@"; do
  if [ -n "$expect_run" ]; then resume="$a"; expect_run=""; continue; fi
  case "$a" in --update) update="--update";; --list) listonly=1;; --resume) expect_run=1;; --resume=*) resume="${a#--resume=}";; *) names+=("$a");; esac
done
if [ -n "$expect_run" ]; then echo "--resume needs a run id" >&2; exit 2; fi
if [ ${#names[@]} -eq 0 ]; then for f in $SUITES_DIR/*.txt; do names+=("$(basename "$f" .txt)"); done; fi

mkdir -p build/devtools run-verify
RESULTS=build/devtools/verify-results.json
RDIR=build/devtools
REV="$(git rev-parse HEAD 2>/dev/null || echo unknown)"
RUN_ID="${resume:-$(date +%Y%m%d-%H%M%S)}"
ran=0

# resolve_suites <suite file ...>: TSV `suite status ver fragment path note`, one line per suite line. The picker (tools/dev/verify/picker.py)
# prefers jars from a folder or file name of the line's Minecraft version, never picks a jar labelled with another version, and reports
# `setup` when a pick is ambiguous (see its docstring).
resolve_suites() {
  local search=() d
  for d in $SEARCH; do search+=("$JAR_ROOT/$d"); done
  "$PY" tools/dev/verify/picker.py resolve --search "${search[@]}" --big "$JAR_ROOT/1.12.2 test mods" --atm9 "$ATM9_MODS" --lib "$ATM10_MODS" -- "$@"
}

# kill only the server java of THIS worktree (its program-args file lives under this checkout's build directory)
kill_server() {
  powershell -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w "$ROOT/tools/dev/verify/kill-server.ps1")" -Root "$(cygpath -w "$ROOT")" >&2 2>/dev/null
}

emit() { # emit <suite> <json object>: the suite's result file is written right away (resumable runs)
  printf '%s' "$2" | "$PY" tools/dev/verify/verify.py result-write "$RDIR" "$1" "$RUN_ID" "$REV"
}

if [ -n "$listonly" ]; then
  files=(); for s in "${names[@]}"; do if [ -f "$SUITES_DIR/$s.txt" ]; then files+=("$SUITES_DIR/$s.txt"); else echo "unknown suite $s" >&2; fi; done
  [ ${#files[@]} -gt 0 ] && resolve_suites "${files[@]}" | tr -d '\r' | awk -F'\t' '{printf "%-18s %-8s %-7s %-34s %s%s\n", $1, $2, $3, $4, $5, ($6==""?"":"   NOTE: " $6)}'
  exit 0
fi

echo "verify run $RUN_ID at $REV${resume:+ (resumed)}: ${names[*]}" >&2
idx=0
for s in "${names[@]}"; do
  idx=$((idx+1))
  file="$SUITES_DIR/$s.txt"
  if [ ! -f "$file" ]; then echo "unknown suite $s" >&2; emit "$s" "{\"suite\":\"$s\",\"skipped\":\"suite file\"}"; continue; fi
  if [ -n "$resume" ] && "$PY" tools/dev/verify/verify.py resume-check "$RDIR" "$s" "$RUN_ID" "$REV"; then echo "=== suite $s (already finished in run $RUN_ID, skipped)" >&2; continue; fi
  if [ -n "${VERIFY_FAKE_DIE_AFTER:-}" ] && [ "$ran" -ge "$VERIFY_FAKE_DIE_AFTER" ]; then echo "VERIFY_FAKE_DIE_AFTER: dying after $ran suites" >&2; exit 137; fi
  ran=$((ran+1))
  echo "=== suite $s" >&2
  if [ -n "${VERIFY_FAKE:-}" ]; then
    D="run-verify/$s"; mkdir -p "$D/logs" "$D/crash-reports"
    if [ -f "$VERIFY_FAKE/$s.log" ]; then cp -f "$VERIFY_FAKE/$s.log" "$D/logs/latest.log"; else : > "$D/logs/latest.log"; fi
    "$PY" tools/dev/verify/verify.py parse "$D/logs/latest.log" "$D/crash-reports" > "build/devtools/verify-$s-metrics.json"
    emit "$s" "$("$PY" -c 'import json,sys; print(json.dumps({"suite": sys.argv[1], "metrics": json.load(open(sys.argv[2]))}))' "$s" "build/devtools/verify-$s-metrics.json")"
    continue
  fi
  jars=(); missing=""; setup=""
  chosen="build/devtools/verify-$s-jars.txt"; : > "$chosen"
  while IFS=$'\t' read -r _ status ver frag j note; do
    j="${j%$'\r'}"; note="${note%$'\r'}"
    echo "  jar [$status] $ver $frag -> ${j:-<none>}${note:+   NOTE: $note}" | tee -a "$chosen" >&2
    case "$status" in
      missing) missing="$missing $frag";;
      setup) setup="$setup $frag";;
      *) jars+=("$ver|$j");;
    esac
  done < <(resolve_suites "$file")
  if [ -n "$setup" ]; then echo "  SETUP: ambiguous jar pick for:$setup (see the NOTE lines above; fix the suite file)" >&2; emit "$s" "{\"suite\":\"$s\",\"setup\":\"${setup# }\"}"; continue; fi
  if [ -n "$missing" ]; then echo "  skipped, missing:$missing" >&2; emit "$s" "{\"suite\":\"$s\",\"skipped\":\"${missing# }\"}"; continue; fi

  D="run-verify/$s"
  rm -rf "$D/mods" "$D/world" "$D/config" "$D/crash-reports" "$D/logs" "$D/.legacyconnector/cache"
  mkdir -p "$D/mods" "$D/logs"
  cp -f "$chosen" "$D/logs/verify-jars.txt"
  for e in "${jars[@]}"; do
    ver="${e%%|*}"; j="${e#*|}"
    if [ "$ver" = "lib" ]; then cp -f "$j" "$D/mods/"; else mkdir -p "$D/mods/$ver"; cp -f "$j" "$D/mods/$ver/"; fi
  done
  [ -f "$JAR_ROOT/run-subtest/eula.txt" ] && cp -f "$JAR_ROOT/run-subtest/eula.txt" "$D/eula.txt" || echo "eula=true" > "$D/eula.txt"
  if [ -f "$JAR_ROOT/run-subtest/server.properties" ]; then
    sed -e "s/^server-port=.*/server-port=$((${VERIFY_PORT_BASE:-25570}+idx))/" -e "s/^query.port=.*/query.port=$((${VERIFY_PORT_BASE:-25570}+idx))/" -e "s/^rcon.port=.*/rcon.port=$((${VERIFY_PORT_BASE:-25570}+100+idx))/" \
        -e "s/^level-seed=.*/level-seed=1710/" "$JAR_ROOT/run-subtest/server.properties" > "$D/server.properties"
  else
    printf 'server-port=%s\nlevel-seed=1710\nonline-mode=false\n' $((${VERIFY_PORT_BASE:-25570}+idx)) > "$D/server.properties"
  fi
  # the dimension self test of big packs blocks the main thread for longer than the 60 s watchdog: never let it kill the test run
  sed -i '/^max-tick-time=/d' "$D/server.properties"; echo "max-tick-time=-1" >> "$D/server.properties"

  # Minecraft 1.2.5 is on by default; a dev build has no join table unless -Pmappings125 puts it on the classpath
  extra=(); for e in "${jars[@]}"; do [ "${e%%|*}" = "1.2.5" ] && extra=("-Pmappings125"); done
  glog="build/devtools/verify-$s-gradle.log"
  if [ -n "${LEGACYCONNECTOR_SHARED_CACHE:-}" ]; then echo "shared translation cache: $LEGACYCONNECTOR_SHARED_CACHE"; fi  # passed to the game as -Dlegacyconnector.sharedCache by build.gradle
  ./gradlew runServerSubTest "-PsubTestDir=$D" "-PautostopTicks=$AUTOSTOP" ${extra[@]+"${extra[@]}"} ${GRADLE_EXTRA:-} > "$glog" 2>&1 &
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
  "$PY" - "$s" "build/devtools/verify-$s-metrics.json" "$note" > "$RESULTS.tmp" <<'PYEOF'
import json, sys
print(json.dumps({"suite": sys.argv[1], "metrics": json.load(open(sys.argv[2])), **({"note": sys.argv[3]} if sys.argv[3] else {})}))
PYEOF
  emit "$s" "$(cat "$RESULTS.tmp")"; rm -f "$RESULTS.tmp"
done

# the table covers every named suite, those finished earlier in a resumed run included
"$PY" tools/dev/verify/verify.py collect "$RDIR" "$RUN_ID" "${names[@]}" > "$RESULTS"
echo "verify run $RUN_ID done (rebuild this table later: python tools/dev/verify/verify.py report --run $RUN_ID)" >&2
"$PY" tools/dev/verify/verify.py report "$BASELINE" "$RESULTS" $update | tee build/devtools/verify-report.txt
rc=${PIPESTATUS[0]}
mkdir -p build/devtools
exit $rc
