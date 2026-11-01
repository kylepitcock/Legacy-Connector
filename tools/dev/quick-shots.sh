#!/bin/bash
# Fast graphical smoke test on a desktop (no Docker): superflat world, no terrain tour, short render distance, a few blocks/items per mod.
#   tools/dev/quick-shots.sh [mods-dir]      mods-dir (default run-screenshots/mods) is the mod set; shots land in run-screenshots/shots
# Override with env: WIDTH/HEIGHT (window, default 640x360) BLOCKS (per mod, default 12) ITEMS (default 12) TOUR (default 0) RD (render distance, default 4) WORLD (preset, default minecraft:flat)
cd "$(dirname "$0")/../.." || exit 1
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Java/jdk-21}"
rm -rf run-screenshots/shots
# The pack's usual performance mods (Sodium, Iris, ImmediatelyFast, FerriteCore, ModernFix, C2ME...) are part of every test: several defects only
# show with them (Sodium's multithreaded meshing, C2ME's off-thread world generation). run-perfmods/ holds their jars (copied from a real pack, never
# committed); NOPERF=1 tests without them.
GAME_DIR="run-screenshots"
for a in ${EXTRA:-}; do case "$a" in -PshotDir=*) GAME_DIR="${a#-PshotDir=}";; esac; done
mkdir -p "$GAME_DIR/mods"
rm -f "$GAME_DIR"/mods/.perf-*.jar
if [ -z "$NOPERF" ] && [ -d run-perfmods ]; then
  for j in run-perfmods/*.jar; do [ -f "$j" ] && cp -f "$j" "$GAME_DIR/mods/$(basename "$j")"; done
else
  for j in run-perfmods/*.jar; do rm -f "$GAME_DIR/mods/$(basename "$j")"; done
fi
./gradlew runScreenshotTest -q -PshotScenario="${SCENARIO:-full}" -PshotWorldType="${WORLD:-minecraft:flat}" -PshotTour="${TOUR:-0}" \
  -PshotRenderDistance="${RD:-4}" -PshotBlocksPerMod="${BLOCKS:-12}" -PshotItemsPerMod="${ITEMS:-12}" ${EXTRA:-} ${COMMANDS:+"-PshotCommands=$COMMANDS"} -PshotWidth="${WIDTH:-640}" -PshotHeight="${HEIGHT:-360}"
echo "shots: $(ls run-screenshots/shots 2>/dev/null | wc -l) in run-screenshots/shots"
