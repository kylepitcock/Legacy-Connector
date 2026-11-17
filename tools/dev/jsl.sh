#!/bin/bash
# js.sh with the helper library run-screenshots/js/lib.js prepended (craft/click/inventory helpers for 1.2.5 survival sessions).
#   tools/dev/jsl.sh 'put("obsidian",[1,2,3,5]); takeResult()'
cd "$(dirname "$0")/../.." || exit 1
{ cat run-screenshots/js/lib.js; echo; echo "$*"; } > run-screenshots/js/_run.js
exec bash tools/dev/js.sh -f run-screenshots/js/_run.js
