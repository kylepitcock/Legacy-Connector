#!/bin/bash
# usage: mkset.sh <name> <fragment>... ; fragments prefixed "L:" are native 1.21.1 libs from ATM10 (copied into mods/), others 1.20.1 jars from ATM 9
A9="X:/Modded games/Prism Launcher/instances/ATM 9 20026/minecraft/mods"
A10="X:/Modded games/Prism Launcher/instances/ATM10 2026/minecraft/mods"
JR=/y/Programming/Minecraft/Retrofit
N=$1; shift
D=run-verify/$N
rm -rf $D/mods $D/world $D/config $D/crash-reports $D/logs $D/.legacyconnector/cache
mkdir -p $D/mods/1.20.1 $D/logs
for f in "$@"; do
  case "$f" in
    L:*) cp -f "$A10"/${f#L:}*.jar $D/mods/ ;;
    *) cp -f "$A9"/$f*.jar $D/mods/1.20.1/ ;;
  esac
done
cp -f $JR/run-subtest/eula.txt $D/eula.txt 2>/dev/null || echo "eula=true" > $D/eula.txt
sed -e "s/^server-port=.*/server-port=25671/" -e "s/^query.port=.*/query.port=25671/" -e "s/^rcon.port=.*/rcon.port=25771/" -e "s/^level-seed=.*/level-seed=1710/" $JR/run-subtest/server.properties > $D/server.properties
ls $D/mods $D/mods/1.20.1
