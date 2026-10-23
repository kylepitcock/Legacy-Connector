#!/bin/bash
# Windows (Git Bash + Docker Desktop) counterpart of docker-headless.sh, which needs zsh, rsync and Colima.
# Keeps the project in the docker volume lc-work and the test mods in lc-mods; screenshots and logs are copied to run-screenshots/.
# usage: win-headless.sh <script in tools/dev> [args]    (MODS, BLOCKS, SCENARIO, ... as for docker-headless.sh)
#        win-headless.sh --stage-mods <dir>             copy a mods directory (with its 1.7.10 subdirectory) into the lc-mods volume
cd "$(dirname "$0")/../.." || exit 1
IMAGE=legacyconnector-headless
export MSYS_NO_PATHCONV=1
if [ "$1" = "--stage-mods" ]; then
  docker volume create lc-mods >/dev/null
  MSYS_NO_PATHCONV=0 docker run --rm -v lc-mods:/mods -v "$(cygpath -w "$2"):/src:ro" $IMAGE bash -c 'rm -rf /mods/* && cp -rL /src/. /mods/ && ls /mods | wc -l'
  exit $?
fi
[ $# -eq 0 ] && { echo "usage: $0 <script in tools/dev> [args]"; exit 1; }
docker image inspect $IMAGE >/dev/null 2>&1 || docker build -q -t $IMAGE tools/docker || exit 1
docker volume create lc-work >/dev/null
# stage the project (the container has its own build/ and Gradle caches)
tar --exclude=./build --exclude=./.gradle --exclude='./run*' --exclude=./.git --exclude=./testmods --exclude=./docs --exclude='./legacyconnector-*.jar' --exclude='*.log' -cf - . \
  | docker run --rm -i -v lc-work:/work $IMAGE bash -c 'cd /work && find . -mindepth 1 -maxdepth 1 ! -name build ! -name .gradle ! -name "run*" -exec rm -rf {} + ; tar xf - && sed -i "s/\\r\$//" tools/dev/*.sh gradlew && mkdir -p run-screenshots run-clienttest build && ln -sfn /mods run/mods 2>/dev/null || (mkdir -p run && ln -sfn /mods run/mods)'
envs=()
for v in MODS BLOCKS ITEMS TOUR WORLDTYPE EXTRA MODSRC SCENARIO RENDER SHOW SHOWITEMS SHOWENT METAS COMMANDS LOOPS LC_DEBUG WIDTH HEIGHT; do [ -n "${!v}" ] && envs+=(-e "$v=${!v}"); done
docker run --rm --shm-size=1g -v lc-work:/work -v lc-mods:/mods:ro -v legacyconnector-gradle:/root/.gradle -v legacyconnector-cache:/work/run-screenshots/.legacyconnector \
  -w /work -e HEADLESS=1 "${envs[@]}" $IMAGE \
  bash -c 'Xvfb :99 -screen 0 ${WIDTH:-854}x${HEIGHT:-480}x24 +extension GLX -nolisten tcp >/dev/null 2>&1 & sleep 2; export DISPLAY=:99; mkdir -p run; [ -e run/mods ] || ln -sfn /mods run/mods; exec zsh tools/dev/"$0" "$@"' "$@"
rc=$?
mkdir -p build/devtools run-screenshots
for d in run-screenshots run-clienttest; do
  rm -rf "$d/shots" "$d/logs"
  docker run --rm -v lc-work:/work $IMAGE tar -C /work -cf - "$d/shots" "$d/logs" 2>/dev/null | tar xf - 2>/dev/null
done
docker run --rm -v lc-work:/work $IMAGE tar -C /work -cf - build/devtools 2>/dev/null | tar xf - 2>/dev/null
exit $rc
