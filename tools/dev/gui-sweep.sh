#!/bin/zsh
# Opens the GUI of every block of groups of legacy mods (headless, see docker-headless.sh) and writes one report:
# build/devtools/gui-sweep.txt (per group: how many screens opened, which blocks opened none, which GUIs failed).
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null)}"
OUT=build/devtools/gui-sweep.txt
: > $OUT
# group: jar fragments (plus dependencies) | blocks per mod
groups=(
  "ae2:appliedenergistics"
  "thermal:thermalexpansion thermalfoundation thermaldynamics cofh"
  "ic2:industrialcraft"
  "buildcraft:buildcraft"
  "forestry:forestry"
  "ntm:HBM"
  "enderio:EnderIO EnderCore"
  "railcraft:Railcraft"
  "thaumcraft:Thaumcraft"
  "botania:Botania Baubles"
  "bloodmagic:BloodMagic"
  "projectred:ProjectRed"
  "ie:ImmersiveEngineering"
  "drawers:StorageDrawers-1 ironchest"
  "openblocks:OpenBlocks OpenModsLib"
  "mfr:MineFactoryReloaded cofh"
  "rftools:rftools mcjtylib cofh"
  "steves:Steves"
  "mystcraft:mystcraft"
  "chisel:Chisel"
  "tinkers:TConstruct Mantle ThaumicTinkerer"
)
for g in $groups; do
  name=${g%%:*}; frags=${g#*:}
  echo "=== $name" | tee -a $OUT
  EXTRA="codechickenlib codechickencore forgemultipart cofh" SCENARIO=gui METAS=${METAS:-variants} BLOCKS=${BLOCKS:-120} RENDER=4 MODSRC=test-mods \
    tools/dev/docker-headless.sh run-screenshot-test.sh ${=frags} > build/devtools/gui-sweep-$name.log 2>&1
  L=run-screenshots/logs/latest.log
  echo "opened: $(grep -c 'GUI TEST.*opened .*Screen' $L)  no screen: $(grep -c 'GUI TEST.*opened no screen' $L)  failed: $(grep -c 'legacy GUI.*failed' $L)" | tee -a $OUT
  grep "legacy GUI.*failed\|A legacy GUI failed" -A1 $L | cut -c60-260 | head -20 >> $OUT
  grep -E "failed during|Exception in thread|FATAL" $L | cut -c60-240 | head -5 >> $OUT
  cp $L build/devtools/gui-sweep-$name-game.log
  mkdir -p build/devtools/gui-shots/$name; cp run-screenshots/shots/*.png build/devtools/gui-shots/$name/ 2>/dev/null
done
