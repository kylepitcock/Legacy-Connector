#!/bin/zsh
# Runs the client smoke test (run-clienttest, mods linked from run/mods); it quits by itself after the report.
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null)}"
mkdir -p run-clienttest/logs; : > run-clienttest/logs/latest.log
./gradlew runClientTest > build/devtools/clienttest.log 2>&1 &
for i in $(seq 1 180); do
  sleep 5
  grep -q -E "CLIENT SELF TEST|Client self test failed" run-clienttest/logs/latest.log 2>/dev/null && { sleep 15; break; }
  grep -q -E "BUILD (SUCCESSFUL|FAILED)" build/devtools/clienttest.log && break
done
pkill -f clientTestRunProgramArgs 2>/dev/null; sleep 2; pkill -9 -f clientTestRunProgramArgs 2>/dev/null
wait
awk '/CLIENT SELF TEST/{f=1} f&&/^\[/&&!/CLIENT SELF TEST/{exit} f' run-clienttest/logs/latest.log > build/devtools/client-selftest.txt
ls run-clienttest/crash-reports 2>/dev/null
