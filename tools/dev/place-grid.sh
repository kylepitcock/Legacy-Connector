#!/bin/bash
# usage: place-grid.sh <namespace> <shot name> <comma list of block names>   (control session; CONTROL=dir)
# Places the blocks in a grid on a platform at y=200, flies the player above and takes a screenshot.
cd "$(dirname "$0")/../.." || exit 1
ns=$1; shot=$2; names=$3
dir="${CONTROL:-run-screenshots/control}"
f="$dir/place-$$.js"
sed -e "s/__NAMES__/$names/" -e "s/__NS__/$ns/" tools/dev/place-grid.js.tpl > "$f"
tools/dev/js.sh -f "$f"
tools/dev/ctl.sh "cmd gamemode creative" "cmd tp @s 0.5 202.5 ${CAMZ:--1} 0 ${PITCH:-40}" "wait 40" "shot $shot"
rm -f "$f"
