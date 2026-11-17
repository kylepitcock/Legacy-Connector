#!/bin/bash
# Starts a 1.2.5 control session (real window) with the given mod jars; drive it with tools/dev/ctl.sh, js.sh / jsl.sh.
#   usage: tools/dev/session125.sh <jar in test-mods-mcarchive/1.2.5> [more jars...]
# Env: LOADWORLD=<save folder name> opens that saved world as it was (no game mode / position change: scenario "controlkeep"), for save-and-reload
#      checks: run once to create a world, quit with `ctl.sh quit`, run again with LOADWORLD. WIPE=1 deletes run-screenshots/saves first.
cd "$(dirname "$0")/../.." || exit 1
SRC=${SRC125:-test-mods-mcarchive/1.2.5}
rm -rf run-screenshots/mods run-screenshots/shots run-screenshots/control
if [ -n "$WIPE" ]; then rm -rf run-screenshots/saves run-screenshots/.legacyconnector/cache; fi
mkdir -p run-screenshots/mods/1.2.5 run-screenshots/config
for j in "$@"; do cp "$SRC/$j" run-screenshots/mods/1.2.5/; done
echo "experimental.minecraft125=true" > run-screenshots/config/legacyconnector.properties
if [ -n "$LOADWORLD" ]; then
  NOPERF=1 SCENARIO=controlkeep EXTRA="-Pmappings125 -PshotLoadWorld=$LOADWORLD" exec bash tools/dev/quick-shots.sh
fi
NOPERF=1 SCENARIO=control EXTRA="-Pmappings125" exec bash tools/dev/quick-shots.sh
