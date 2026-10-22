#!/bin/zsh
# Runs one of the graphical dev scripts inside a Linux container with a virtual display (Xvfb + Mesa llvmpipe), so no
# window opens on the desktop. Needs colima/docker running. The project is staged to a cache directory (the container
# has its own build/ and Gradle caches); screenshots and logs are copied back next to the host's run directories.
# usage: docker-headless.sh run-screenshot-test.sh [args...]   |   docker-headless.sh run-client-test.sh [args...]
# env vars understood by those scripts (MODS, BLOCKS, WORLDTYPE, EXTRA, MODSRC ...) are passed through.
cd "$(dirname "$0")/../.."
[ $# -eq 0 ] && { echo "usage: $0 <script in tools/dev> [args]"; exit 1; }
STAGE="$HOME/.cache/legacyconnector-docker/work"
IMAGE=legacyconnector-headless
mkdir -p "$STAGE"
docker image inspect $IMAGE >/dev/null 2>&1 || docker build -q -t $IMAGE tools/docker || exit 1
rsync -a --delete --exclude build --exclude .gradle --exclude '/run*' --exclude .git --exclude testmods --exclude test-mods2 \
  --exclude '*.log' ./ "$STAGE/"
mkdir -p "$STAGE/run/mods" "$STAGE/run-screenshots" "$STAGE/run-clienttest"
rsync -aL --delete run/mods/ "$STAGE/run/mods/"
# keep each run directory's config and world between runs, but not its mods and logs
for d in run-screenshots run-clienttest; do [ -d $d/config ] && rsync -a $d/config/ "$STAGE/$d/config/"; done
envs=()
for v in MODS BLOCKS ITEMS TOUR WORLDTYPE EXTRA MODSRC SCENARIO RENDER; do [[ -n ${(P)v} ]] && envs+=(-e "$v=${(P)v}"); done
docker run --rm --shm-size=1g -v "$STAGE:/work" -v legacyconnector-gradle:/root/.gradle -w /work -e HEADLESS=1 $envs $IMAGE \
  bash -c 'Xvfb :99 -screen 0 854x480x24 +extension GLX -nolisten tcp >/dev/null 2>&1 & sleep 2; export DISPLAY=:99; exec tools/dev/"$0" "$@"' "$@"
rc=$?
mkdir -p build/devtools; [ -d "$STAGE/build/devtools" ] && rsync -a "$STAGE/build/devtools/" build/devtools/
for d in run-screenshots run-clienttest; do
  mkdir -p $d/logs
  [ -d "$STAGE/$d/shots" ] && rsync -a --delete "$STAGE/$d/shots/" $d/shots/
  [ -d "$STAGE/$d/logs" ] && rsync -a "$STAGE/$d/logs/" $d/logs/
done
exit $rc
