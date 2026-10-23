#!/bin/zsh
# usage: survey.sh <namespace> <screenshot name> [blocks...]  -- places the given blocks (all legacy blocks of the namespace when none)
# in a grid on a sky platform of the running session and takes a screenshot.
cd "$(dirname "$0")/../.."
S=tools/dev/session.sh; ns=$1; name=$2; shift 2
$S send close >/dev/null; $S send cmd "gamemode creative" >/dev/null; $S send cmd "tp @s 0 201 0" >/dev/null; $S send wait 30 >/dev/null
$S send cmd "fill -12 199 -4 12 199 12 minecraft:smooth_stone" >/dev/null; $S send cmd "fill -12 200 -4 12 210 12 minecraft:air" >/dev/null
if [ $# -eq 0 ]; then
  $S send cmd "legacyconnector list blocks $ns" >/dev/null
  set -- $(grep -a "CHAT\] [0-9]* blocks of $ns:" ~/.cache/legacyconnector-docker/work/run-screenshots/logs/latest.log | tail -1 | sed -E 's/.*blocks of [a-z0-9_]+: //')
fi
x=-11; z=4
for b in "$@"; do
  $S send cmd "setblock $x 200 $z $ns:$b" > /dev/null; x=$((x+3)); [ $x -gt 11 ] && { x=-11; z=$((z+3)); }
done
$S send cmd "tp @s 0 204 -3" >/dev/null; $S send look 0 40 >/dev/null; $S send wait 40 >/dev/null; $S shot $name
