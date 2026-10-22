#!/bin/sh
# Regenerates loader/src/main/resources/legacyconnector/legacy/1.7.10/static-arrays.txt (vanilla static literal arrays) from the
# vanilla 1.7.10 client jar and the notch->SRG mappings in build/devtools (see tools/fetch_devtools.sh).
set -e
cd "$(dirname "$0")/.."
ASM=$(find ~/.gradle -name 'asm-9*.jar' | head -1)
TREE=$(find ~/.gradle -name 'asm-tree-9*.jar' | head -1)
TMP=$(mktemp)
unzip -p build/devtools/mcp/srg1710.zip joined.srg > "$TMP"
"${JAVA_HOME:-/usr}/bin/java" -cp "$ASM:$TREE" tools/ExtractStaticArrays.java build/devtools/mojang/client-1.7.10.jar "$TMP" \
  loader/src/main/resources/legacyconnector/legacy/1.7.10/static-arrays.txt
rm -f "$TMP"
