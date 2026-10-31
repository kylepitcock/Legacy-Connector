#!/bin/bash
# Waits until the running scenario (lib.js) has no steps left, then prints and clears its output lines.
cd "$(dirname "$0")/../../.." || exit 1
for _ in $(seq 1 ${WAIT_TRIES:-120}); do
  r=$(tools/dev/server-script.sh 'STEPS.length' 2>&1 | head -1)
  [ "$r" = "=> 0" ] && break
  sleep 3
done
tools/dev/server-script.sh 'flush()' 2>&1 | cut -c1-${WIDTH:-600}
