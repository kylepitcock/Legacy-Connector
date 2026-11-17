#!/bin/bash
# usage: run125.sh <label> <jar>...   (jars from test-mods-mcarchive/1.2.5)
cd /y/Programming/Minecraft/Retrofit/.claude/worktrees/agent-a497c4bd2a9e66922 || exit 1
export JAVA_HOME="/c/Program Files/Java/jdk-21"
SRC=/y/Programming/Minecraft/Retrofit/test-mods-mcarchive/1.2.5
D=run-sub125
label=$1; shift
rm -rf $D/mods $D/world $D/.legacyconnector $D/logs $D/crash-reports; mkdir -p $D/mods/1.2.5 $D/logs $D/config
for j in "$@"; do cp "$SRC/$j" $D/mods/1.2.5/; done
echo "experimental.minecraft125=true" > $D/config/legacyconnector.properties
mkdir -p build/devtools
./gradlew -q runServerSubTest -PsubTestDir=$D -Pmappings125 > build/devtools/sub125-$label.log 2>&1 &
GP=$!
for i in $(seq 1 90); do
  sleep 5
  grep -q -E "BLOCK SELF TEST|ENTITY SELF TEST|Exception generating|Crash" $D/logs/latest.log 2>/dev/null && { sleep 15; break; }
  kill -0 $GP 2>/dev/null || break
done
# kill servers whose cmdline mentions our dir
for p in $(ps -W 2>/dev/null | awk '/java/{print $1}'); do :; done
powershell.exe -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*agent-a497c4bd2a9e66922*' -and \$_.CommandLine -like '*serverSubTest*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }"
sleep 3; wait $GP 2>/dev/null
cp $D/logs/latest.log build/devtools/sub125-$label.latest.log 2>/dev/null
L=$D/logs/latest.log
echo "== $label: $*"
grep -E "EXPERIMENTAL|1\.2\.5 content|Legacy content|Legacy mods loaded|SELF TEST|failed during|SUMMARY|not supported|Dropped" $L | cut -c1-260 | head -40
