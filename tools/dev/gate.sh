#!/bin/bash
# The merge gate with quiet output: compileJava --rerun, generateShimIndex, check. Prints one line per step and, on failure, only the
# compiler errors / failed tests (the full log stays in build/devtools/gate.log). Made for agents: the raw Gradle output is hundreds of
# warning lines per run.   usage: tools/dev/gate.sh [compile|index|check ...]   (default: all three)
cd "$(dirname "$0")/../.." || exit 1
export JAVA_HOME="${JAVA_HOME_21:-C:/Program Files/Java/jdk-21}"
[ -d "$JAVA_HOME" ] || export JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null)"
mkdir -p build/devtools; LOG=build/devtools/gate.log; : > "$LOG"
steps=("$@"); [ ${#steps[@]} -eq 0 ] && steps=(compile index check)
for s in "${steps[@]}"; do
  case "$s" in compile) args="compileJava --rerun";; index) args="generateShimIndex";; check) args="check";; *) echo "unknown step $s"; exit 2;; esac
  start=$(date +%s)
  if ./gradlew $args -q >> "$LOG" 2>&1; then echo "gate $s: ok ($(( $(date +%s) - start )) s)"; else
    echo "gate $s: FAILED ($(( $(date +%s) - start )) s)"
    grep -E "error:|FAILED|What went wrong|phantom field|mod-specific|AssertionFailedError|expected:" -A4 "$LOG" | grep -v -E "^\s*$|warning:" | head -60
    exit 1
  fi
done
