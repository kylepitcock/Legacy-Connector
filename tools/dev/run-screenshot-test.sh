#!/bin/zsh
# Graphical test: boots the client, creates a world, builds showcases of legacy blocks and items and writes screenshots
# to run-screenshots/shots (look at them with the image viewer). It quits by itself when the script is done.
# usage: run-screenshot-test.sh [name-fragment...]   -- mods are linked from run/mods (and $MODSRC); fragments select jars
# env:   WORLDTYPE=biomesoplenty:biomesop  MODS=agricraft,appliedenergistics2  BLOCKS=24  ITEMS=36  TOUR=6  RENDER=16  SCENARIO=portal (Twilight Forest style portal test instead of the tour)
cd "$(dirname "$0")/../.."
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null)}"
rm -rf run-screenshots/mods run-screenshots/shots
mkdir -p run-screenshots/mods run-screenshots/logs build/devtools
patterns=("$@" ${=EXTRA} codechicken baubles jade jei curios neoforge)
for jar in run/mods/*.jar(N) run/mods/1.7.10/*.jar(N) ${=MODSRC:+${MODSRC}/*.jar}(N); do
  name=${jar:t:l}
  for p in $patterns; do
    if [[ $name == *${p:l}* ]]; then ln -sf "$PWD/$jar" "run-screenshots/mods/${jar:t}"; break; fi
  done
done
# HEADLESS=1 (software rendering, see docker-headless.sh): cheap graphics settings
if [[ -n $HEADLESS ]]; then
  printf 'menuBackgroundBlurriness:0\nrenderDistance:%s\ngraphicsMode:0\nparticles:2\nentityShadows:false\nenableVsync:false\nmaxFps:260\nsoundCategory_master:0.0\n' "${RENDER:-8}" > run-screenshots/options.txt
fi
echo "mods: $(ls run-screenshots/mods | wc -l)"
args=()
[[ -n $WORLDTYPE ]] && args+=(-PshotWorldType=$WORLDTYPE)
[[ -n $MODS ]] && args+=(-PshotMods=$MODS)
[[ -n $BLOCKS ]] && args+=(-PshotBlocksPerMod=$BLOCKS)
[[ -n $ITEMS ]] && args+=(-PshotItemsPerMod=$ITEMS)
[[ -n $TOUR ]] && args+=(-PshotTour=$TOUR)
[[ -n $SCENARIO ]] && args+=(-PshotScenario=$SCENARIO)
[[ -n $RENDER ]] && args+=(-PshotRenderDistance=$RENDER)
[[ -n $SHOWITEMS ]] && args+=(-PshotItems=$SHOWITEMS)   # gallery scenario: ordered ids of items / entities to show
[[ -n $SHOWENT ]] && args+=(-PshotEntities=$SHOWENT)
[[ -n $WIDTH && -n $HEIGHT ]] && args+=(-PshotWidth=$WIDTH -PshotHeight=$HEIGHT)
[[ -n $SHOW ]] && args+=(-PshotBlocks=$SHOW)   # SHOW=ns:block,ns:block: only these blocks in the showcase
: > run-screenshots/logs/latest.log
./gradlew runScreenshotTest $args > build/devtools/screenshottest.log 2>&1 &
for i in $(seq 1 360); do
  sleep 5
  grep -q "Screenshot test finished\|Screenshot test timed out" run-screenshots/logs/latest.log 2>/dev/null && { sleep 5; break; }
  grep -q -E "BUILD (SUCCESSFUL|FAILED)" build/devtools/screenshottest.log && break
done
pkill -f screenshotTestRunProgramArgs 2>/dev/null; sleep 2; pkill -9 -f screenshotTestRunProgramArgs 2>/dev/null
wait
ls run-screenshots/shots 2>/dev/null | head -100
