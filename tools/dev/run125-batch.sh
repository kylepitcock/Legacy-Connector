#!/bin/bash
# usage: run125-batch.sh label=jar ...   (one server run per mod, jar from test-mods-mcarchive/1.2.5; toggle on, fresh world/cache)
# RATE=1 also writes the run's result to docs/data/mods.json (tools/compat/from_client.py, mc 1.2.5, source "client").
cd "$(dirname "$0")/../.."
for a in "$@"; do
  label="${a%%=*}"
  bash tools/dev/run125.sh "$label" "${a#*=}"
  [ -n "$RATE" ] && python tools/compat/from_client.py build/devtools/sub125-$label.report.txt build/devtools/sub125-$label.latest.log "1.2.5 dedicated server self test" 1.2.5 2026-11-17
done
