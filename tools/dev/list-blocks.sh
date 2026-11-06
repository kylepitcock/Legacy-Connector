#!/bin/bash
# usage: list-blocks.sh <namespace> [item]   (control session; CONTROL=dir): prints the namespace's block (or item) registry paths
cd "$(dirname "$0")/../.." || exit 1
dir="${CONTROL:-run-screenshots/control}"
reg=BLOCK
[ "$2" = "item" ] && reg=ITEM
f="$dir/list-$$.js"
cat > "$f" <<EOF
var R = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var out = [];
R.$reg.keySet().forEach(function(k){ if (k.getNamespace() == "$1") out.push(k.getPath()); });
out.length + ": " + out.join(",");
EOF
tools/dev/js.sh -f "$f"
rm -f "$f"
