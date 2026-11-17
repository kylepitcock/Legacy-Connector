#!/bin/bash
# 1.2.5 client smoke test, one mod per run (headless-free desktop run, see tools/dev/quick-shots.sh).
#   usage: tools/dev/run125-client.sh <label>=<jar in test-mods-mcarchive/1.2.5> ...
# Each run: fresh game dir mods (only that jar in mods/1.2.5), no perf mods unless PERF=1; shots are copied to build/devtools/c125/<label>/ and the log to
# build/devtools/c125/<label>.log; a summary line per run is printed.
cd "$(dirname "$0")/../.." || exit 1
SRC=/y/Programming/Minecraft/Retrofit/test-mods-mcarchive/1.2.5
for a in "$@"; do
  label=${a%%=*}; jar=${a#*=}
  rm -rf run-screenshots/mods run-screenshots/shots run-screenshots/.legacyconnector/cache
  mkdir -p run-screenshots/mods/1.2.5 run-screenshots/config build/devtools/c125/$label
  cp "$SRC/$jar" run-screenshots/mods/1.2.5/
  echo "experimental.minecraft125=true" > run-screenshots/config/legacyconnector.properties
  if [ -n "$PERF" ]; then EXTRA="-Pmappings125" bash tools/dev/quick-shots.sh > build/devtools/c125/$label.log 2>&1
  else NOPERF=1 EXTRA="-Pmappings125" bash tools/dev/quick-shots.sh > build/devtools/c125/$label.log 2>&1; fi
  cp run-screenshots/shots/*.png build/devtools/c125/$label/ 2>/dev/null
  L=build/devtools/c125/$label.log
  echo "== $label ($jar): shots=$(ls build/devtools/c125/$label | wc -l) missing-texture-models=$(grep -c 'Missing textures in model' $L) failed=$(grep -c 'failed during' $L)"
  grep -E "Legacy content|Legacy mods loaded|failed during|Skipping|not loaded" $L | sed -E 's/^\[[^]]*\] \[[^]]*\] //' | cut -c1-200 | head -6
done
