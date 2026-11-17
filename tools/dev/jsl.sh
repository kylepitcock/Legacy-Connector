#!/bin/bash
# js.sh with the helper libraries tools/dev/js125/lib.js and lib2.js prepended (craft/click/inventory/dump helpers for 1.2.5 survival sessions).
#   tools/dev/jsl.sh 'put("obsidian",[1,2,3,5]); takeResult()'
cd "$(dirname "$0")/../.." || exit 1
{ cat tools/dev/js125/lib.js; echo; cat tools/dev/js125/lib2.js; echo; echo "$*"; } > run-screenshots/js/_run.js
exec bash tools/dev/js.sh -f run-screenshots/js/_run.js
