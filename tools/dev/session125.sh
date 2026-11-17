#!/bin/bash
# Starts a 1.2.5 control session (real window) with the given mod jars; drive it with tools/dev/js.sh and run-screenshots/control/in/*.cmd.
#   usage: tools/dev/session125.sh <jar in test-mods-mcarchive/1.2.5> [more jars...]     (KEEPWORLD=1 keeps run-screenshots/saves for a reload test)
cd "$(dirname "$0")/../.." || exit 1
SRC=${SRC125:-test-mods-mcarchive/1.2.5}
rm -rf run-screenshots/mods run-screenshots/shots run-screenshots/control
if [ -z "$KEEPWORLD" ]; then rm -rf run-screenshots/saves run-screenshots/.legacyconnector/cache; fi
mkdir -p run-screenshots/mods/1.2.5 run-screenshots/config
for j in "$@"; do cp "$SRC/$j" run-screenshots/mods/1.2.5/; done
echo "experimental.minecraft125=true" > run-screenshots/config/legacyconnector.properties
NOPERF=1 SCENARIO=control EXTRA="-Pmappings125" exec bash tools/dev/quick-shots.sh
