#!/bin/bash
# Windows counterpart of session.sh: one long-lived headless client in a Docker container that you drive without restarting it.
#   tools/dev/win-session.sh start [jar-name-fragments...]   stage the project and boot the game (background container lc-session)
#       HEAP=8g sets the heap; the fragment "jar" selects every mod in the lc-mods volume
#   tools/dev/win-session.sh send <verb> [args]              run one control command and print its answer
#   tools/dev/win-session.sh shot <name>                     screenshot -> prints the png path on this machine
#   tools/dev/win-session.sh logs [n]                        last n lines of the game log
#   tools/dev/win-session.sh stop                            quit the game and remove the container
# verbs: cmd <slash command> | shot <name> | wait <ticks> | look <yaw> <pitch> | use | mouse <x> <y> | click <x> <y> [button]
#        key <glfw code> | type <text> | close | info | quit          (see RemoteControl.java)
cd "$(dirname "$0")/../.." || exit 1
export MSYS_NO_PATHCONV=1
NAME=lc-session
IMAGE=legacyconnector-headless
CTL=/work/run-screenshots/control
OUT=run-screenshots/control
case $1 in
  start)
    shift
    docker rm -f $NAME >/dev/null 2>&1
    tools/dev/win-headless.sh --stage || exit 1
    docker run --rm -v lc-work:/work $IMAGE bash -c "rm -rf $CTL && mkdir -p $CTL/in $CTL/out"
    envs=()
    for v in WORLDTYPE EXTRA MODSRC WIDTH HEIGHT RENDER LC_DEBUG; do [ -n "${!v}" ] && envs+=(-e "$v=${!v}"); done
    docker run -d --name $NAME --shm-size=1g -v lc-work:/work -v lc-mods:/mods:ro -v legacyconnector-gradle:/root/.gradle -v legacyconnector-cache:/work/run-screenshots/.legacyconnector \
      -w /work -e HEADLESS=1 -e JAVA_TOOL_OPTIONS="-Dorg.lwjgl.librarypath=/opt/lwjgl -Xmx${HEAP:-4g}" -e SCENARIO=control -e LOOPS=${LOOPS:-4000} -e WIDTH=${WIDTH:-1280} -e HEIGHT=${HEIGHT:-720} -e RENDER=${RENDER:-6} "${envs[@]}" $IMAGE \
      bash -c 'Xvfb :99 -screen 0 ${WIDTH}x${HEIGHT}x24 +extension GLX -nolisten tcp >/dev/null 2>&1 & sleep 2; export DISPLAY=:99; mkdir -p run; [ -e run/mods ] || ln -sfn /mods run/mods; exec zsh tools/dev/run-screenshot-test.sh "$@"' x "$@" >/dev/null
    echo "starting; wait for it with: tools/dev/win-session.sh send info"
    ;;
  send|shot)
    verb=$1; shift
    if [ "$verb" = shot ]; then set -- shot "$@"; fi
    id=$(date +%s%N)$RANDOM
    docker exec $NAME bash -c "mkdir -p $CTL/in $CTL/out && echo \"\$1\" > $CTL/in/$id.tmp && mv $CTL/in/$id.tmp $CTL/in/$id.cmd" x "$*" || exit 1
    for i in $(seq 1 ${TIMEOUT:-600}); do
      ans=$(docker exec $NAME bash -c "[ -f $CTL/out/$id.txt ] && cat $CTL/out/$id.txt" 2>/dev/null) && [ -n "$ans" -o "$?" = 0 ] && {
        echo "$ans"
        if [ "$1" = shot ]; then
          mkdir -p $OUT; docker cp "$NAME:$CTL/out/${2:-shot}.png" "$OUT/${2:-shot}.png" >/dev/null 2>&1 && echo "$PWD/$OUT/${2:-shot}.png"
        fi
        exit 0
      }
      sleep 1
    done
    echo "timeout (is the session running? tools/dev/win-session.sh start)"; exit 1
    ;;
  logs) docker exec $NAME tail -n ${2:-60} /work/run-screenshots/logs/latest.log ;;
  stop) TIMEOUT=8 "$0" send quit >/dev/null; sleep 3; docker rm -f $NAME >/dev/null 2>&1 ;;
  *) sed -n 2,9p "$0" ;;
esac
