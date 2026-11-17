#!/bin/bash
# usage: run125-batch.sh label=jar ...   (one server run per mod, jar from test-mods-mcarchive/1.2.5)
cd "$(dirname "$0")/../.."
for a in "$@"; do
  bash tools/dev/run125.sh "${a%%=*}" "${a#*=}"
done
