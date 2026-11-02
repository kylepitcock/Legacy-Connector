#!/bin/bash
# usage: ctl.sh "<verb line>"
cd /y/Programming/Minecraft/Retrofit/.claude/worktrees/agent-a620281832783631e || exit 1
id="c$(date +%s%N)"
printf '%s' "$1" > run-screenshots/control/in/$id.tmp
mv run-screenshots/control/in/$id.tmp run-screenshots/control/in/$id.cmd
sleep ${2:-3}
