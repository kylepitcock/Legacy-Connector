#!/bin/zsh
# SKIP=sodium leaves out jars whose name contains it (client-only mods cannot start a dedicated server)
# Server test over every jar of a directory (default test-mods2, the GTNH pack): links them into run-subtest/mods and runs the
# server (block/item/entity self tests). usage: tools/dev/run-corpus.sh [dir] ; AFTER=60 waits longer after the self test
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null)}"
SRC=${1:-test-mods2}
D=run-subtest
rm -rf $D/mods $D/crash-reports; mkdir -p $D/mods $D/logs $D/config build/devtools
: > $D/logs/latest.log
for jar in $SRC/*.jar(N); do [[ -n $SKIP && ${jar:t:l} == *${SKIP:l}* ]] && continue; if [[ -n $COPYMODS ]]; then cp -L "$jar" "$D/mods/${jar:t}"; else ln -sf "$PWD/$jar" "$D/mods/${jar:t}"; fi; done
# library jars that dependency downloaders file under mods/<minecraft version>/ stay there
if [[ -d $SRC/1.7.10 ]]; then mkdir -p $D/mods/1.7.10; for jar in $SRC/1.7.10/*.jar(N); do if [[ -n $COPYMODS ]]; then cp -L "$jar" "$D/mods/1.7.10/${jar:t}"; else ln -sf "$PWD/$jar" "$D/mods/1.7.10/${jar:t}"; fi; done; fi
echo "mods: $(ls $D/mods | wc -l)"
rm -rf $D/world
./gradlew runServerSubTest > build/devtools/corpus.log 2>&1 &
for i in $(seq 1 ${TIMEOUT:-240}); do
  sleep 5
  grep -q -E "BLOCK SELF TEST|Exception generating new chunk" $D/logs/latest.log 2>/dev/null && { sleep ${AFTER:-30}; break; }
  grep -q -E "BUILD (SUCCESSFUL|FAILED)" build/devtools/corpus.log && break
done
pkill -f "serverSubTestRunProgramArg[s]"; sleep 3; pkill -9 -f "serverSubTestRunProgramArg[s]"
wait
grep -E "legacy mod\(s\) failed|failed to load|=== [0-9]+ legacy" $D/logs/latest.log | head -5
