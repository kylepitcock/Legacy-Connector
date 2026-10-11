#!/bin/zsh
# Runs the dev server test, stops it once world generation has been sampled (or after 12 minutes).
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null)}"
rm -rf run-servertest/world run-servertest/crash-reports
: > run-servertest/logs/latest.log 2>/dev/null
./gradlew runServerTest > build/devtools/servertest.log 2>&1 &
for i in $(seq 1 144); do
  sleep 5
  grep -q -E "BLOCK SELF TEST|Exception generating new chunk" run-servertest/logs/latest.log 2>/dev/null && { sleep 10; break; }
  grep -q -E "BUILD (SUCCESSFUL|FAILED)" build/devtools/servertest.log && break
done
pkill -f serverTestRunProgramArgs; sleep 3; pkill -9 -f serverTestRunProgramArgs
wait
L=run-servertest/logs/latest.log
grep -E "Legacy mods loaded|Done \(|LegacyTest SUMMARY|Retrofit/Biomes|layers failed|chunk manager failed|Offering|ENTITY SELF TEST|Spawned .* legacy entities|WORLDGEN SELF TEST" $L | cut -c40-600
ls run-servertest/crash-reports 2>/dev/null
echo "decoration failures: $(grep -c 'Legacy biome decoration failed' $L)"
awk '/ENTITY SELF TEST/{f=1} f&&/^\[/&&!/ENTITY SELF TEST/{exit} f' $L > build/devtools/entity-selftest.txt
awk '/BLOCK SELF TEST/{f=1} f&&/^\[/&&!/BLOCK SELF TEST/{exit} f' $L > build/devtools/block-selftest.txt
echo "block test: $(head -1 build/devtools/block-selftest.txt | sed -E 's/.*BLOCK SELF TEST: //')"
echo "self test: $(head -1 build/devtools/entity-selftest.txt | sed -E 's/.*ENTITY SELF TEST: //')"
