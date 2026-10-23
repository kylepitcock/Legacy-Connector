#!/bin/zsh
# One long-lived headless client you can drive without restarting it (see RemoteControl.java).
#   tools/dev/session.sh start [jar-name-fragments...]   boot the game in Docker (background); env as for docker-headless.sh
#   tools/dev/session.sh send <verb> [args]              run one control command and print its answer
#   tools/dev/session.sh shot <name>                     screenshot -> prints the png path (open it with the image viewer)
#   tools/dev/session.sh stop                            quit the game
# verbs: cmd <slash command> | shot <name> | wait <ticks> | look <yaw> <pitch> | use | mouse <x> <y> | click <x> <y> [button]
#        key <glfw code> | type <text> | close | info | quit          (x, y: GUI-scaled coordinates, see 'info')
cd "$(dirname "$0")/../.."
CTL="$HOME/.cache/legacyconnector-docker/work/run-screenshots/control"
case $1 in
  start)
    shift
    rm -rf "$CTL"; mkdir -p "$CTL/in" "$CTL/out"
    SCENARIO=control LOOPS=${LOOPS:-4000} WIDTH=${WIDTH:-1280} HEIGHT=${HEIGHT:-720} RENDER=${RENDER:-6} \
      nohup tools/dev/docker-headless.sh run-screenshot-test.sh "$@" > build/devtools/session.log 2>&1 &
    echo "starting; wait for it with: tools/dev/session.sh send info"
    ;;
  send|shot)
    verb=$1; shift
    [[ $verb == shot ]] && set -- shot "$@" || set -- "$@"
    id=$(date +%s%N 2>/dev/null || date +%s)$RANDOM
    mkdir -p "$CTL/in" "$CTL/out"
    echo "$@" > "$CTL/in/$id.tmp"; mv "$CTL/in/$id.tmp" "$CTL/in/$id.cmd"
    for i in $(seq 1 ${TIMEOUT:-600}); do
      [ -f "$CTL/out/$id.txt" ] && { cat "$CTL/out/$id.txt"; [[ $1 == shot ]] && echo "$CTL/out/${2:-shot}.png"; exit 0; }
      sleep 1
    done
    echo "timeout (is the session running? tools/dev/session.sh start)"; exit 1
    ;;
  stop) "$0" send quit ;;
  *) sed -n 2,9p "$0" ;;
esac
