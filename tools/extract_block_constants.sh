#!/bin/sh
# Regenerates src/main/resources/mccompat/legacy/1.7.10/block-constants.txt from the vanilla 1.7.10 client jar
# (build/devtools/mojang/client-1.7.10.jar) and the notch->SRG mappings (build/devtools/mcp/srg1710.zip).
set -e
cd "$(dirname "$0")/.."
ASM=$(find ~/.gradle -name 'asm-9*.jar' | head -1)
TREE=$(find ~/.gradle -name 'asm-tree-9*.jar' | head -1)
TMP=$(mktemp)
unzip -p build/devtools/mcp/srg1710.zip joined.srg > "$TMP"
"${JAVA_HOME:-/usr}/bin/java" -cp "$ASM:$TREE" tools/ExtractBlockConstants.java build/devtools/mojang/client-1.7.10.jar "$TMP" \
  src/main/resources/mccompat/legacy/1.7.10/block-constants.txt
rm -f "$TMP"
