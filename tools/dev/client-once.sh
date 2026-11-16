#!/bin/bash
# One real-client run of tools/dev/quick-shots.sh that never waits on a hung game.
#   tools/dev/client-once.sh            (same env as quick-shots.sh: BLOCKS, ITEMS, SCENARIO, WORLD, EXTRA, NOPERF...)
# NeoForge keeps the game open on its mod loading error screen ("broken mod state"), so quick-shots.sh may never return. The game runs in the background and
# run-screenshots/logs/latest.log is polled every 20 s: the game is killed at once when a failure marker shows up, or after STALL seconds (default 360)
# without the log growing. Exit status: 0 the run finished by itself, 2 failure marker, 3 stalled. The marker line and the last log lines are printed.
cd "$(dirname "$0")/../.." || exit 1
ROOT="$(pwd -W 2>/dev/null || pwd)"
LOG="run-screenshots/logs/latest.log"
STALL="${STALL:-360}"
MARKERS='broken mod state|Mod loading issue|Preparing crash report|Failed to wait for future|Caught exception during event|Failed to create mod instance|Crash Report'

kill_game() {
  powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { \$_.Name -eq 'java.exe' -and \$_.CommandLine -like '*$(echo "$ROOT" | sed 's#/#\\\\#g')*' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" >/dev/null 2>&1
}

rm -f "$LOG"
tools/dev/quick-shots.sh > build/client-once.out 2>&1 &
runner=$!
start=$SECONDS
last_size=0
last_change=$SECONDS
status=0
while kill -0 $runner 2>/dev/null; do
  sleep 20
  if [ -f "$LOG" ]; then
    size=$(stat -c %s "$LOG" 2>/dev/null || echo 0)
    if [ "$size" != "$last_size" ]; then last_size=$size; last_change=$SECONDS; fi
    hit=$(grep -a -m1 -E "$MARKERS" "$LOG")
    if [ -n "$hit" ]; then
      echo "client-once: failure marker after $((SECONDS-start)) s: $hit" | cut -c1-300
      status=2
      break
    fi
  fi
  if [ $((SECONDS-last_change)) -ge "$STALL" ]; then
    echo "client-once: no log progress for $STALL s, killing the game"
    status=3
    break
  fi
done
if [ $status -ne 0 ]; then
  sleep 3   # let the log flush the lines around the marker
  kill_game
  kill $runner 2>/dev/null
  wait $runner 2>/dev/null
fi
echo "client-once: status $status after $((SECONDS-start)) s; shots: $(ls run-screenshots/shots 2>/dev/null | wc -l)"
tail -3 build/client-once.out | cut -c1-200
exit $status
