#!/bin/zsh
# Runs the client smoke test (run-clienttest); it quits by itself after the report.
# usage: run-client-test.sh [name-fragment...]  -- mods are linked from run/mods; with fragments only the matching jars
# (plus CodeChicken*, Baubles, the LegacyTest fixture, and any non-1.7.10 mod such as Jade/JEI/Curios) are loaded.
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null)}"
rm -rf run-clienttest/mods
if [ $# -eq 0 ]; then
  ln -s ../run/mods run-clienttest/mods
else
  mkdir -p run-clienttest/mods
  patterns=("$@" ${=EXTRA} codechicken baubles legacytest jade jei curios neoforge)
  for jar in run/mods/*.jar(N) run/mods/1.7.10/*.jar(N); do
    name=${jar:t:l}
    for p in $patterns; do
      if [[ $name == *${p:l}* ]]; then ln -sf "$PWD/$jar" "run-clienttest/mods/${jar:t}"; break; fi
    done
  done
  echo "mods: $(ls run-clienttest/mods | wc -l)"
fi
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
