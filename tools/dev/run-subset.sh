#!/bin/zsh
# Server test over a subset of the corpus, which loads in a fraction of the time of the whole of run-servertest/mods.
# usage: tools/dev/run-subset.sh <name-fragment>...   e.g. run-subset.sh hbm thaumcraft
# Jars whose file name contains a fragment (case-insensitive) are linked into run-subtest/mods, plus the shared
# libraries (CodeChicken*, Baubles, the LegacyTest fixture). EXTRA="fragment ..." adds more; the world is kept.
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null)}"
[ $# -eq 0 ] && { echo "usage: $0 <name-fragment>..."; exit 1; }
patterns=("$@" ${=EXTRA} codechicken baubles legacytest)
D=run-subtest
rm -rf $D/mods $D/crash-reports; mkdir -p $D/mods $D/logs $D/config
: > $D/logs/latest.log
for src in run-servertest/mods run-servertest/mods/1.7.10; do
  for jar in $src/*.jar(N); do
    name=${jar:t:l}
    for p in $patterns; do
      if [[ $name == *${p:l}* ]]; then ln -sf "$PWD/$jar" "$D/mods/${jar:t}"; break; fi
    done
  done
done
echo "mods: $(ls $D/mods | wc -l)"; ls $D/mods | sed 's/^/  /'
rm -rf $D/world
# LEVELTYPE=biomesoplenty:biomesop generates the world with a mod's world type
[[ -n $LEVELTYPE ]] && sed -i '' "s#^level-type=.*#level-type=${LEVELTYPE//:/\\\\:}#" $D/server.properties
[[ -z $LEVELTYPE ]] && sed -i '' 's#^level-type=.*#level-type=minecraft\\:normal#' $D/server.properties
./gradlew runServerSubTest > build/devtools/subtest.log 2>&1 &
GPID=$!
for i in $(seq 1 144); do
  sleep 5
  grep -q -E "BLOCK SELF TEST|Exception generating new chunk" $D/logs/latest.log 2>/dev/null && { sleep ${AFTER:-10}; break; }
  grep -q -E "BUILD (SUCCESSFUL|FAILED)" build/devtools/subtest.log && break
done
# kill only the server started here: the one whose working directory is this checkout's run-subtest
for p in $(pgrep -f serverSubTestRunProgramArgs); do [[ "$(lsof -p $p 2>/dev/null | awk '/ cwd /{print $NF}')" == "$PWD/$D" ]] && kill $p; done; sleep 3
for p in $(pgrep -f serverSubTestRunProgramArgs); do [[ "$(lsof -p $p 2>/dev/null | awk '/ cwd /{print $NF}')" == "$PWD/$D" ]] && kill -9 $p; done
wait
L=$D/logs/latest.log
grep -E "Legacy mods loaded|Legacy content:|LegacyTest SUMMARY|ENTITY SELF TEST|SILENT FAILURE CHECK|WORLDGEN SELF TEST" $L | cut -c40-300
echo "--- mod failures"
grep -E "\] \[LegacyConnector/\]: Legacy mod .* failed during" $L | sed -E 's/.*Legacy mod ([^ ]+) \(.*failed during ([a-zA-Z]+): (.*)/  \1 \2: \3/' | cut -c1-260 | sort -u
grep -A12 "DIMENSION SELF TEST" $L | head -14 | cut -c40-300
awk '/BLOCK SELF TEST/{f=1} f&&/^\[/&&!/BLOCK SELF TEST/{exit} f' $L > build/devtools/subtest-blocks.txt
echo "--- $(head -1 build/devtools/subtest-blocks.txt | sed -E 's/.*BLOCK SELF TEST: //')"
sed -n 2,60p build/devtools/subtest-blocks.txt | cut -c1-240
awk '/ITEM SELF TEST/{f=1} f&&/^\[/&&!/ITEM SELF TEST/{exit} f' $L > build/devtools/subtest-items.txt
echo "--- $(head -1 build/devtools/subtest-items.txt | sed -E 's/.*ITEM SELF TEST: //')"
sed -n 2,60p build/devtools/subtest-items.txt | cut -c1-240
ls $D/crash-reports 2>/dev/null
