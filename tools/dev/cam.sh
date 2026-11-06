#!/bin/bash
# usage: cam.sh <x> <y> <z> <pitch> <shot name>   (control session; CONTROL=dir): teleports the player, waits, takes a screenshot
cd "$(dirname "$0")/../.." || exit 1
tools/dev/ctl.sh "cmd gamemode creative" "cmd tp @s $1 $2 $3 0 $4" "wait 30" "shot $5"
