#!/usr/bin/env bash
# usage: run.sh <script.js> [timeoutSeconds]: prepends lib.js, drops the script into the long-lived server's script dir
# (gradlew runServerSubTest -PselfTests=false -PserverScript=<abs run-subtest/ss> -PserverScriptMod=midnight) and prints the output
cd "$(dirname "$0")/../../../.."
n=$(basename "$1" .js)
rm -f run-subtest/ss/$n.out run-subtest/ss/$n.js.done
cat tools/dev/server-scripts/midnight/lib.js "$1" > run-subtest/ss/$n.js
for i in $(seq 1 ${2:-60}); do [ -f run-subtest/ss/$n.out ] && { sleep 1; cat run-subtest/ss/$n.out; exit 0; }; sleep 1; done
echo TIMEOUT
